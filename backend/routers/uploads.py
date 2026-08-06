"""
文件上传路由: 单文件上传 / 文件夹批量上传 / 进度查询
支持 txt / md / markdown / docx 格式
"""
import threading
from typing import List
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session

from database import get_db
from models import Novel, UploadTask
from schemas import UploadTaskOut, MessageResponse
from security import get_current_creator
from services.upload_processor import create_upload_task, process_upload_task, get_task_progress
from services.file_parser import parse_file, scan_folder_files

router = APIRouter(prefix="/api/upload", tags=["文件上传"])


@router.post("/single/{novel_id}", response_model=dict)
async def upload_single_file(
    novel_id: str,
    file: UploadFile = File(...),
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    """上传单个文件，自动解析为章节"""
    novel = db.query(Novel).filter(Novel.id == novel_id, Novel.creator_id == creator.id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在或无权操作")

    content = await file.read()
    result = parse_file(content, file.filename)

    if not result.success:
        raise HTTPException(status_code=400, detail=result.error)

    from models import Chapter
    chapter_sort = novel.chapter_count
    added = 0
    total_words = 0
    for ch in result.chapters:
        chapter_sort += 1
        chapter = Chapter(
            novel_id=novel.id,
            title=ch.title,
            content=ch.content,
            word_count=ch.word_count,
            sort_order=chapter_sort,
        )
        db.add(chapter)
        added += 1
        total_words += ch.word_count

    novel.chapter_count = chapter_sort
    novel.word_count += total_words
    db.commit()

    return {
        "success": True,
        "filename": file.filename,
        "chapters_added": added,
        "words_added": total_words,
        "message": f"成功解析 {added} 章, 新增 {total_words} 字",
    }


@router.post("/folder/{novel_id}", response_model=UploadTaskOut, status_code=201)
async def upload_folder(
    novel_id: str,
    files: List[UploadFile] = File(...),
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    """
    文件夹批量上传
    接收多个文件，创建上传任务，后台处理并实时返回进度
    """
    novel = db.query(Novel).filter(Novel.id == novel_id, Novel.creator_id == creator.id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在或无权操作")

    if len(files) == 0:
        raise HTTPException(status_code=400, detail="请至少上传一个文件")

    filenames = [f.filename for f in files]
    supported = scan_folder_files(filenames)
    if not supported:
        raise HTTPException(status_code=400, detail="没有支持的文件格式(仅支持 txt/md/docx)")

    # 创建任务
    task = create_upload_task(db, creator, novel_id, filenames)

    # 读取所有文件内容
    files_data = []
    for f in files:
        content = await f.read()
        files_data.append((f.filename, content))

    # 启动后台处理线程
    def run_task():
        from database import SessionLocal
        task_db = SessionLocal()
        try:
            process_upload_task(task_db, task.id, files_data)
        except Exception as e:
            t = task_db.query(UploadTask).filter(UploadTask.id == task.id).first()
            if t:
                t.status = "failed"
                t.message = f"处理异常: {str(e)}"
                task_db.commit()
        finally:
            task_db.close()

    thread = threading.Thread(target=run_task, daemon=True)
    thread.start()

    return task


@router.get("/task/{task_id}", response_model=UploadTaskOut)
def get_task_status(
    task_id: str,
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    """查询上传任务进度"""
    task = db.query(UploadTask).filter(
        UploadTask.id == task_id,
        UploadTask.creator_id == creator.id,
    ).first()
    if not task:
        raise HTTPException(status_code=404, detail="任务不存在")
    return task


@router.get("/tasks", response_model=List[UploadTaskOut])
def list_my_tasks(
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    """创作者的所有上传任务"""
    tasks = db.query(UploadTask).filter(
        UploadTask.creator_id == creator.id
    ).order_by(UploadTask.created_at.desc()).limit(50).all()
    return tasks
