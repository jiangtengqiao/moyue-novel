"""
创作者路由: 注册创作者 / 创作者档案 / 创作者作品列表 / 仪表盘数据
"""
import math
from typing import List
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func

from database import get_db
from models import Creator, User, Novel
from schemas import CreatorRegister, CreatorOut, NovelBrief, MessageResponse
from security import get_current_user, get_current_creator

router = APIRouter(prefix="/api/creator", tags=["创作者"])


@router.post("/register", response_model=CreatorOut, status_code=201)
def register_creator(
    data: CreatorRegister,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if user.is_creator and user.creator_profile:
        raise HTTPException(status_code=400, detail="您已经是创作者")

    creator = Creator(
        user_id=user.id,
        pen_name=data.pen_name,
        real_name=data.real_name,
        introduction=data.introduction or "",
        contact_email=data.contact_email,
        contact_phone=data.contact_phone,
        social_accounts=data.social_accounts or {},
    )
    db.add(creator)
    user.is_creator = True
    if not user.nickname:
        user.nickname = data.pen_name
    db.commit()
    db.refresh(creator)
    return creator


@router.get("/profile", response_model=CreatorOut)
def my_profile(creator=Depends(get_current_creator)):
    return creator


@router.put("/profile", response_model=CreatorOut)
def update_profile(
    data: dict,
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    allowed = ["pen_name", "introduction", "contact_email", "contact_phone", "social_accounts"]
    for k, v in data.items():
        if k in allowed:
            setattr(creator, k, v)
    db.commit()
    db.refresh(creator)
    return creator


@router.get("/novels", response_model=dict)
def my_novels(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    query = db.query(Novel).filter(Novel.creator_id == creator.id).order_by(Novel.created_at.desc())
    total = query.count()
    items = query.offset((page - 1) * page_size).limit(page_size).all()
    return {
        "total": total,
        "page": page,
        "page_size": page_size,
        "items": [NovelBrief.model_validate(n).model_dump() for n in items],
    }


@router.get("/dashboard", response_model=dict)
def dashboard(creator=Depends(get_current_creator), db: Session = Depends(get_db)):
    """创作者仪表盘统计数据"""
    novels = db.query(Novel).filter(Novel.creator_id == creator.id).all()
    total_novels = len(novels)
    total_words = sum(n.word_count for n in novels)
    total_views = sum(n.view_count for n in novels)
    total_likes = sum(n.like_count for n in novels)
    total_collects = sum(n.collect_count for n in novels)
    total_chapters = sum(n.chapter_count for n in novels)

    # 更新创作者统计
    creator.total_words = total_words
    creator.total_novels = total_novels
    creator.total_readers = total_views
    db.commit()

    return {
        "total_novels": total_novels,
        "total_words": total_words,
        "total_views": total_views,
        "total_likes": total_likes,
        "total_collects": total_collects,
        "total_chapters": total_chapters,
        "recent_novels": [NovelBrief.model_validate(n).model_dump() for n in novels[:5]],
    }


@router.get("/list", response_model=List[CreatorOut])
def list_creators(
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """公开的创作者列表"""
    creators = db.query(Creator).filter(
        Creator.status == "active"
    ).order_by(Creator.total_readers.desc()).limit(limit).all()
    return creators
