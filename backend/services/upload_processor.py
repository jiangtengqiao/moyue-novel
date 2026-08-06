"""
上传处理服务
处理文件夹批量上传，实时更新任务进度
"""
import os
import time
import asyncio
from typing import List
from pathlib import Path

from sqlalchemy.orm import Session

from config import settings
from models import UploadTask, Novel, Chapter, Creator
from services.file_parser import parse_file, ParsedFile, scan_folder_files


def create_upload_task(
    db: Session,
    creator: Creator,
    novel_id: str,
    filenames: List[str],
) -> UploadTask:
    """创建上传任务"""
    supported_files = scan_folder_files(filenames)
    task = UploadTask(
        creator_id=creator.id,
        novel_id=novel_id,
        status="pending",
        total_files=len(supported_files),
        processed_files=0,
        failed_files=0,
        progress=0.0,
        message=f"共检测到 {len(supported_files)} 个可处理文件",
        file_list=[{"filename": f, "status": "pending"} for f in supported_files],
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task


def process_upload_task(
    db: Session,
    task_id: str,
    files_data: List[tuple],
):
    """
    处理上传任务（同步执行，由后台线程调用）
    files_data: [(filename, file_bytes), ...]
    """
    task = db.query(UploadTask).filter(UploadTask.id == task_id).first()
    if not task:
        return

    novel = db.query(Novel).filter(Novel.id == task.novel_id).first()
    if not novel:
        task.status = "failed"
        task.message = "关联的小说不存在"
        db.commit()
        return

    task.status = "processing"
    task.message = "开始处理文件..."
    db.commit()

    file_list = task.file_list or []
    total_words = 0
    chapter_sort = novel.chapter_count  # 从已有章节后继续排序

    supported_files = scan_folder_files([f[0] for f in files_data])
    file_map = {f[0]: f[1] for f in files_data}

    for idx, filename in enumerate(supported_files):
        task.current_file = filename
        task.message = f"正在处理: {filename} ({idx + 1}/{len(supported_files)})"
        db.commit()

        content = file_map.get(filename, b'')
        result: ParsedFile = parse_file(content, filename)

        # 更新文件列表状态
        for item in file_list:
            if item["filename"] == filename:
                if result.success:
                    item["status"] = "completed"
                    item["chapters"] = len(result.chapters)
                    item["words"] = result.total_words
                else:
                    item["status"] = "failed"
                    item["error"] = result.error
                break

        if not result.success:
            task.failed_files += 1
            task.error_log = (task.error_log or "") + f"\n{filename}: {result.error}"
            db.commit()
            continue

        # 将章节写入数据库
        for chapter in result.chapters:
            chapter_sort += 1
            ch = Chapter(
                novel_id=novel.id,
                title=chapter.title,
                content=chapter.content,
                word_count=chapter.word_count,
                sort_order=chapter_sort,
            )
            db.add(ch)
            total_words += chapter.word_count

        task.processed_files += 1
        task.progress = round((task.processed_files + task.failed_files) / task.total_files * 100, 1) if task.total_files else 0
        db.commit()

    # 更新小说统计
    novel.word_count += total_words
    novel.chapter_count = chapter_sort
    db.commit()

    # 完成任务
    if task.failed_files == 0:
        task.status = "completed"
        task.message = f"处理完成: 共 {task.processed_files} 个文件, 新增 {chapter_sort - (novel.chapter_count - chapter_sort + task.processed_files)} 章"
    elif task.processed_files > 0:
        task.status = "completed"
        task.message = f"部分完成: 成功 {task.processed_files} 个, 失败 {task.failed_files} 个"
    else:
        task.status = "failed"
        task.message = f"全部失败: {task.failed_files} 个文件"

    task.progress = 100.0 if task.processed_files > 0 else 0.0
    task.current_file = None
    db.commit()


def get_task_progress(db: Session, task_id: str) -> dict:
    """获取任务进度"""
    task = db.query(UploadTask).filter(UploadTask.id == task_id).first()
    if not task:
        return None
    return {
        "id": task.id,
        "status": task.status,
        "total_files": task.total_files,
        "processed_files": task.processed_files,
        "failed_files": task.failed_files,
        "current_file": task.current_file,
        "progress": task.progress,
        "message": task.message,
        "file_list": task.file_list,
        "updated_at": task.updated_at,
    }
