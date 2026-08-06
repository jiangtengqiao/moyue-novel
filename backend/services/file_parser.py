"""
文件处理服务
支持 txt / markdown / docx 文件的解析与章节切分
"""
import os
import re
import io
from pathlib import Path
from typing import List, Tuple, Optional
from dataclasses import dataclass

from docx import Document
import markdown
from bs4 import BeautifulSoup


@dataclass
class ParsedChapter:
    """解析后的章节数据"""
    title: str
    content: str
    word_count: int


@dataclass
class ParsedFile:
    """解析后的文件结果"""
    filename: str
    title: str
    chapters: List[ParsedChapter]
    total_words: int
    success: bool
    error: str = ""


# 章节标题正则 - 匹配常见的中文章节标题格式
CHAPTER_PATTERNS = [
    re.compile(r'^第[一二三四五六七八九十百千零\d]+[章节回卷集部篇]\s*.*$', re.MULTILINE),
    re.compile(r'^Chapter\s+\d+', re.MULTILINE | re.IGNORECASE),
    re.compile(r'^第\d+章', re.MULTILINE),
    re.compile(r'^\d+[、.．]\s*.+$', re.MULTILINE),  # 1、标题  或 1.标题
    re.compile(r'^【.+】$', re.MULTILINE),
    re.compile(r'^\[.+]$', re.MULTILINE),
]


def clean_text(text: str) -> str:
    """清理文本：统一换行、去除多余空行"""
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(r'\n{3,}', '\n\n', text)
    return text.strip()


def split_chapters(text: str, default_title: str = "正文") -> List[ParsedChapter]:
    """
    将长文本按章节标题切分
    若无法识别章节标题，则整体作为一个章节
    """
    text = clean_text(text)
    chapters = []

    # 尝试匹配章节标题
    positions = []
    for pattern in CHAPTER_PATTERNS:
        for m in pattern.finditer(text):
            line_start = text.rfind('\n', 0, m.start()) + 1
            line_end = text.find('\n', m.end())
            if line_end == -1:
                line_end = len(text)
            title_line = text[line_start:line_end].strip()
            positions.append((line_start, line_end, title_line))

    if not positions:
        # 无章节标题，整体作为一章
        wc = len(text.replace('\n', '').replace(' ', ''))
        chapters.append(ParsedChapter(title=default_title, content=text, word_count=wc))
        return chapters

    # 按位置排序并去重
    positions.sort(key=lambda x: x[0])
    deduped = []
    last_start = -1
    for start, end, title in positions:
        if start > last_start:
            deduped.append((start, end, title))
            last_start = start

    # 前言部分（第一个章节标题之前的内容）
    if deduped[0][0] > 0:
        preface = text[:deduped[0][0]].strip()
        if len(preface) > 50:
            wc = len(preface.replace('\n', '').replace(' ', ''))
            chapters.append(ParsedChapter(title="引言", content=preface, word_count=wc))

    # 切分各章节
    for i, (start, end, title) in enumerate(deduped):
        content_start = end
        content_end = deduped[i + 1][0] if i + 1 < len(deduped) else len(text)
        chapter_content = text[content_start:content_end].strip()
        wc = len(chapter_content.replace('\n', '').replace(' ', ''))
        if wc > 0 or title:
            chapters.append(ParsedChapter(
                title=title[:200],
                content=chapter_content,
                word_count=wc
            ))

    return chapters if chapters else [ParsedChapter(title=default_title, content=text, word_count=len(text))]


def parse_txt(content: bytes, filename: str) -> ParsedFile:
    """解析 TXT 文件"""
    # 尝试多种编码
    for encoding in ['utf-8', 'gbk', 'gb2312', 'big5', 'utf-16']:
        try:
            text = content.decode(encoding)
            break
        except (UnicodeDecodeError, LookupError):
            continue
    else:
        return ParsedFile(filename=filename, title="", chapters=[], total_words=0, success=False, error="无法识别文件编码")

    title = Path(filename).stem
    chapters = split_chapters(text, default_title=title)
    total = sum(c.word_count for c in chapters)
    return ParsedFile(filename=filename, title=title, chapters=chapters, total_words=total, success=True)


def parse_markdown(content: bytes, filename: str) -> ParsedFile:
    """解析 Markdown 文件"""
    try:
        text = content.decode('utf-8')
    except UnicodeDecodeError:
        try:
            text = content.decode('gbk')
        except UnicodeDecodeError:
            return ParsedFile(filename=filename, title="", chapters=[], total_words=0, success=False, error="编码错误")

    # 提取标题
    title = Path(filename).stem
    for line in text.split('\n'):
        if line.startswith('# '):
            title = line.lstrip('# ').strip()
            break

    # 将 markdown 转为纯文本（去掉标记语法）
    html = markdown.markdown(text)
    soup = BeautifulSoup(html, 'html.parser')
    plain_text = soup.get_text('\n')

    chapters = split_chapters(plain_text, default_title=title)
    total = sum(c.word_count for c in chapters)
    return ParsedFile(filename=filename, title=title, chapters=chapters, total_words=total, success=True)


def parse_docx(content: bytes, filename: str) -> ParsedFile:
    """解析 DOCX 文件"""
    try:
        doc = Document(io.BytesIO(content))
    except Exception as e:
        return ParsedFile(filename=filename, title="", chapters=[], total_words=0, success=False, error=f"DOCX解析失败: {e}")

    # 收集所有段落文本
    paragraphs = []
    for para in doc.paragraphs:
        text = para.text.strip()
        if text:
            style = para.style.name.lower() if para.style else ""
            # 标记标题样式
            if 'heading' in style or 'title' in style:
                paragraphs.append(f"\n{text}\n")
            else:
                paragraphs.append(text)

    full_text = '\n'.join(paragraphs)
    title = Path(filename).stem

    # docx 的 heading 会被章节切分器识别
    chapters = split_chapters(full_text, default_title=title)
    total = sum(c.word_count for c in chapters)
    return ParsedFile(filename=filename, title=title, chapters=chapters, total_words=total, success=True)


def parse_file(content: bytes, filename: str) -> ParsedFile:
    """根据扩展名自动选择解析器"""
    ext = Path(filename).suffix.lower()
    if ext == '.txt':
        return parse_txt(content, filename)
    elif ext in ('.md', '.markdown'):
        return parse_markdown(content, filename)
    elif ext == '.docx':
        return parse_docx(content, filename)
    else:
        return ParsedFile(
            filename=filename, title="", chapters=[], total_words=0,
            success=False, error=f"不支持的文件格式: {ext}"
        )


def scan_folder_files(filenames: List[str]) -> List[str]:
    """
    扫描上传的文件列表，过滤出支持的文本文件并排序
    支持的格式: txt, md, markdown, docx
    """
    supported = {'.txt', '.md', '.markdown', '.docx'}
    result = []
    for f in filenames:
        ext = Path(f).suffix.lower()
        if ext in supported:
            result.append(f)
    # 按文件名自然排序
    def natural_key(s):
        return [int(c) if c.isdigit() else c.lower() for c in re.split(r'(\d+)', s)]
    result.sort(key=natural_key)
    return result
