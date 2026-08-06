"""
小说路由: 列表 / 详情 / 搜索 / 分类 / 收藏
"""
import math
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import or_, desc

from database import get_db
from models import Novel, Category, Bookmark, User
from schemas import NovelOut, NovelBrief, NovelCreate, NovelUpdate, CategoryOut, MessageResponse
from security import get_current_user, get_current_creator

router = APIRouter(prefix="/api/novels", tags=["小说"])


# ==================== 分类 ====================

@router.get("/categories", response_model=List[CategoryOut])
def list_categories(db: Session = Depends(get_db)):
    cats = db.query(Category).order_by(Category.sort_order).all()
    if not cats:
        # 初始化默认分类
        defaults = [
            ("xuanhuan", "玄幻", 1),
            ("xianxia", "仙侠", 2),
            ("dushi", "都市", 3),
            ("lishi", "历史", 4),
            ("kehuan", "科幻", 5),
            ("junshi", "军事", 6),
            ("youxi", "游戏", 7),
            ("lingyi", "灵异", 8),
            ("duanpian", "短篇", 9),
            ("qita", "其他", 10),
        ]
        for name, display, order in defaults:
            c = Category(name=name, display_name=display, sort_order=order)
            db.add(c)
        db.commit()
        cats = db.query(Category).order_by(Category.sort_order).all()
    return cats


# ==================== 小说列表 ====================

@router.get("/", response_model=dict)
def list_novels(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    category: Optional[str] = None,
    status: Optional[str] = None,
    keyword: Optional[str] = None,
    sort: str = Query("latest", regex="^(latest|hot|rating|words)$"),
    db: Session = Depends(get_db),
):
    query = db.query(Novel)

    if category:
        cat = db.query(Category).filter(Category.name == category).first()
        if cat:
            query = query.filter(Novel.category_id == cat.id)
    if status:
        query = query.filter(Novel.status == status)
    if keyword:
        kw = f"%{keyword}%"
        query = query.filter(or_(Novel.title.like(kw), Novel.author.like(kw)))

    if sort == "hot":
        query = query.order_by(desc(Novel.view_count))
    elif sort == "rating":
        query = query.order_by(desc(Novel.rating))
    elif sort == "words":
        query = query.order_by(desc(Novel.word_count))
    else:
        query = query.order_by(desc(Novel.created_at))

    total = query.count()
    items = query.offset((page - 1) * page_size).limit(page_size).all()
    pages = math.ceil(total / page_size) if total else 0

    return {
        "total": total,
        "page": page,
        "page_size": page_size,
        "pages": pages,
        "items": [NovelBrief.model_validate(n).model_dump() for n in items],
    }


@router.get("/search", response_model=dict)
def search_novels(
    keyword: str = Query(..., min_length=1),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    kw = f"%{keyword}%"
    query = db.query(Novel).filter(
        or_(Novel.title.like(kw), Novel.author.like(kw))
    ).order_by(desc(Novel.view_count))
    total = query.count()
    items = query.offset((page - 1) * page_size).limit(page_size).all()
    return {
        "total": total,
        "page": page,
        "page_size": page_size,
        "items": [NovelBrief.model_validate(n).model_dump() for n in items],
    }


@router.get("/featured", response_model=List[NovelBrief])
def featured_novels(limit: int = Query(6, ge=1, le=20), db: Session = Depends(get_db)):
    items = db.query(Novel).filter(Novel.featured == True).order_by(desc(Novel.view_count)).limit(limit).all()
    return items


@router.get("/{novel_id}", response_model=NovelOut)
def get_novel(novel_id: str, db: Session = Depends(get_db)):
    novel = db.query(Novel).filter(Novel.id == novel_id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在")
    novel.view_count += 1
    db.commit()
    return novel


# ==================== 创作者管理小说 ====================

@router.post("/", response_model=NovelOut, status_code=201)
def create_novel(
    data: NovelCreate,
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    cat_id = None
    if data.category_id:
        cat = db.query(Category).filter(Category.id == data.category_id).first()
        if cat:
            cat_id = cat.id

    novel = Novel(
        title=data.title,
        author=data.author,
        creator_id=creator.id,
        category_id=cat_id,
        description=data.description,
        tags=data.tags or [],
        status=data.status or "ongoing",
    )
    db.add(novel)
    db.commit()
    db.refresh(novel)

    creator.total_novels += 1
    db.commit()
    return novel


@router.put("/{novel_id}", response_model=NovelOut)
def update_novel(
    novel_id: str,
    data: NovelUpdate,
    creator=Depends(get_current_creator),
    db: Session = Depends(get_db),
):
    novel = db.query(Novel).filter(Novel.id == novel_id, Novel.creator_id == creator.id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在或无权操作")

    update_data = data.model_dump(exclude_unset=True)
    for k, v in update_data.items():
        setattr(novel, k, v)
    db.commit()
    db.refresh(novel)
    return novel


# ==================== 收藏 ====================

@router.post("/{novel_id}/bookmark", response_model=MessageResponse)
def toggle_bookmark(
    novel_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    novel = db.query(Novel).filter(Novel.id == novel_id).first()
    if not novel:
        raise HTTPException(status_code=404, detail="小说不存在")

    existing = db.query(Bookmark).filter(
        Bookmark.user_id == user.id, Bookmark.novel_id == novel_id
    ).first()

    if existing:
        db.delete(existing)
        novel.collect_count = max(0, novel.collect_count - 1)
        db.commit()
        return MessageResponse(message="已取消收藏", success=True)
    else:
        bm = Bookmark(user_id=user.id, novel_id=novel_id)
        db.add(bm)
        novel.collect_count += 1
        db.commit()
        return MessageResponse(message="收藏成功", success=True)


@router.get("/bookmarks/list", response_model=List[NovelBrief])
def my_bookmarks(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    bms = db.query(Bookmark).filter(Bookmark.user_id == user.id).all()
    novel_ids = [b.novel_id for b in bms]
    novels = db.query(Novel).filter(Novel.id.in_(novel_ids)).all() if novel_ids else []
    return novels
