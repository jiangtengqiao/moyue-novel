"""
章节路由: 章节列表 / 阅读内容 / 创建章节
"""
from typing import List
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from database import get_db
from models import Novel, Chapter
from schemas import ChapterOut, ChapterContent, ChapterCreate, MessageResponse
from security import get_current_creator

router = APIRouter(prefix="/api/novels/{novel_id}/chapters", tags=["章节"])


@router.get("/", response_model=List[ChapterOut])
def list_chapters(
    novel_id: str,
    db: Session = Depends(get_db),
):
    novel = db.query(Novel).filter(Novel.id == novel_id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在")
    chapters = db.query(Chapter).filter(
        Chapter.novel_id == novel_id,
        Chapter.status == "published",
    ).order_by(Chapter.sort_order).all()
    return chapters


@router.get("/{chapter_id}", response_model=ChapterContent)
def read_chapter(
    novel_id: str,
    chapter_id: str,
    db: Session = Depends(get_db),
):
    chapter = db.query(Chapter).filter(
        Chapter.id == chapter_id,
        Chapter.novel_id == novel_id,
    ).first()
    if not chapter:
        raise HTTPException(status_code=404, detail="章节不存在")
    return chapter


@router.get("/index/{chapter_index}", response_model=ChapterContent)
def read_chapter_by_index(
    novel_id: str,
    chapter_index: int,
    db: Session = Depends(get_db),
):
    """按序号读取章节（从0开始）"""
    chapter = db.query(Chapter).filter(
        Chapter.novel_id == novel_id,
        Chapter.sort_order == chapter_index,
        Chapter.status == "published",
    ).first()
    if not chapter:
        raise HTTPException(status_code=404, detail="章节不存在")
    return chapter


@router.post("/", response_model=ChapterOut, status_code=201)
def create_chapter(
    novel_id: str,
    data: ChapterCreate,
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    novel = db.query(Novel).filter(Novel.id == novel_id, Novel.creator_id == creator.id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在或无权操作")

    sort_order = data.sort_order if data.sort_order is not None else novel.chapter_count

    chapter = Chapter(
        novel_id=novel_id,
        title=data.title,
        content=data.content,
        word_count=len(data.content.replace('\n', '').replace(' ', '')),
        sort_order=sort_order,
        is_free=data.is_free if data.is_free is not None else True,
    )
    db.add(chapter)

    novel.chapter_count += 1
    novel.word_count += chapter.word_count
    db.commit()
    db.refresh(chapter)
    return chapter
