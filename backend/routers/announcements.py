"""
公告路由: 实时公告列表
"""
from datetime import datetime
from typing import List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from database import get_db
from models import Announcement
from schemas import AnnouncementOut

router = APIRouter(prefix="/api/announcements", tags=["公告"])


@router.get("/", response_model=List[AnnouncementOut])
def list_announcements(
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """获取有效公告列表（按置顶和创建时间排序）"""
    now = datetime.utcnow()
    query = db.query(Announcement).filter(
        Announcement.is_active == True
    ).filter(
        (Announcement.start_time.is_(None)) | (Announcement.start_time <= now)
    ).filter(
        (Announcement.end_time.is_(None)) | (Announcement.end_time >= now)
    )
    items = query.order_by(
        Announcement.is_pinned.desc(),
        Announcement.sort_order.desc(),
        Announcement.created_at.desc(),
    ).limit(limit).all()
    return items


@router.get("/latest", response_model=List[AnnouncementOut])
def latest_announcements(db: Session = Depends(get_db)):
    """获取最新3条公告（用于启动弹窗）"""
    now = datetime.utcnow()
    items = db.query(Announcement).filter(
        Announcement.is_active == True
    ).filter(
        (Announcement.start_time.is_(None)) | (Announcement.start_time <= now)
    ).filter(
        (Announcement.end_time.is_(None)) | (Announcement.end_time >= now)
    ).order_by(
        Announcement.is_pinned.desc(),
        Announcement.created_at.desc(),
    ).limit(3).all()
    return items
