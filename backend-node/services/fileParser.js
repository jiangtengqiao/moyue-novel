const path = require('path');
const { splitChapters } = require('./chapterSplitter');

const SUPPORTED = ['.txt', '.md', '.markdown', '.docx'];

function scanFolderFiles(filenames) {
  return filenames
    .filter(f => SUPPORTED.includes(path.extname(f).toLowerCase()))
    .sort((a, b) => {
      const na = a.match(/\d+/g) || [];
      const nb = b.match(/\d+/g) || [];
      for (let i = 0; i < Math.max(na.length, nb.length); i++) {
        const x = parseInt(na[i] || -1), y = parseInt(nb[i] || -1);
        if (x !== y) return x - y;
      }
      return a.localeCompare(b);
    });
}

function parseFile(content, filename) {
  const ext = path.extname(filename).toLowerCase();
  const title = path.basename(filename, ext);
  
  if (ext === '.txt') return parseTxt(content, filename, title);
  if (ext === '.md' || ext === '.markdown') return parseMd(content, filename, title);
  if (ext === '.docx') return parseDocx(content, filename, title);
  
  return { filename, title: '', chapters: [], total_words: 0, success: false, error: `不支持的格式: ${ext}` };
}

function parseTxt(content, filename, title) {
  const encodings = ['utf8', 'ucs2', 'ascii'];
  let text = null;
  
  // 尝试检测 GBK 编码
  try {
    const buf = Buffer.isBuffer(content) ? content : Buffer.from(content);
    text = buf.toString('utf8');
    if (text.includes('�')) throw new Error('bad encoding');
  } catch (e) {
    try {
      const iconv = require('iconv-lite');
      text = iconv.decode(Buffer.from(content), 'gbk');
    } catch (e2) {
      text = Buffer.from(content).toString('utf8');
    }
  }
  
  const chapters = splitChapters(text, title);
  const total = chapters.reduce((s, c) => s + c.word_count, 0);
  return { filename, title, chapters, total_words: total, success: true };
}

function parseMd(content, filename, title) {
  let text = Buffer.isBuffer(content) ? content.toString('utf8') : content;
  
  for (const line of text.split('\n')) {
    if (line.startsWith('# ')) { title = line.replace(/^#+\s*/, '').trim(); break; }
  }
  
  // 简单 markdown 转纯文本
  text = text
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`(.+?)`/g, '$1')
    .replace(/\[(.+?)\]\(.+?\)/g, '$1')
    .replace(/^>\s+/gm, '')
    .replace(/^[-*+]\s+/gm, '');
  
  const chapters = splitChapters(text, title);
  const total = chapters.reduce((s, c) => s + c.word_count, 0);
  return { filename, title, chapters, total_words: total, success: true };
}

function parseDocx(content, filename, title) {
  try {
    const AdmZip = require('adm-zip');
    const buf = Buffer.isBuffer(content) ? content : Buffer.from(content);
    const zip = new AdmZip(buf);
    const docXml = zip.getEntry('word/document.xml');
    if (!docXml) return { filename, title, chapters: [], total_words: 0, success: false, error: '无法读取DOCX' };
    
    const xml = docXml.getData().toString('utf8');
    // 提取段落文本
    const paragraphs = [];
    const pRegex = /<w:p[^>]*>([\s\S]*?)<\/w:p>/g;
    let match;
    while ((match = pRegex.exec(xml)) !== null) {
      const texts = [];
      const tRegex = /<w:t[^>]*>(.*?)<\/w:t>/g;
      let tm;
      while ((tm = tRegex.exec(match[1])) !== null) {
        texts.push(tm[1]);
      }
      const text = texts.join('');
      if (text.trim()) {
        // 检测标题样式
        const isHeading = /<w:pStyle[^>]*w:val="[^"]*[Hh]eading[^"]*"/.test(match[1]) || /<w:pStyle[^>]*w:val="[^"]*[Tt]itle[^"]*"/.test(match[1]);
        paragraphs.push(isHeading ? `\n${text}\n` : text);
      }
    }
    
    const fullText = paragraphs.join('\n');
    const chapters = splitChapters(fullText, title);
    const total = chapters.reduce((s, c) => s + c.word_count, 0);
    return { filename, title, chapters, total_words: total, success: true };
  } catch (e) {
    return { filename, title, chapters: [], total_words: 0, success: false, error: 'DOCX解析失败: ' + e.message };
  }
}

module.exports = { parseFile, scanFolderFiles };
