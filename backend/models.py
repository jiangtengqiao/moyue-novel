"""
数据库模型定义
定义所有实体表结构
"""
import uuid
import datetime
from sqlalchemy import (
    Column, String, Text, Integer, Float, Boolean, DateTime,
    ForeignKey, JSON, Index
)
from sqlalchemy.orm import relationship

from database import Base


def gen_uuid():
    return str(uuid.uuid4())


class TimestampMixin:
    """时间戳混入"""
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)


class User(Base, TimestampMixin):
    """用户表"""
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=gen_uuid)
    username = Column(String(50), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)
    nickname = Column(String(50))
    avatar = Column(String(500))
    bio = Column(Text, default="")
    is_creator = Column(Boolean, default=False)
    is_admin = Column(Boolean, default=False)
    last_login_at = Column(DateTime)

    # 关系
    creator_profile = relationship("Creator", back_populates="user", uselist=False, cascade="all, delete-orphan")
    bookmarks = relationship("Bookmark", back_populates="user", cascade="all, delete-orphan")
    history = relationship("ReadingHistory", back_populates="user", cascade="all, delete-orphan")


class Creator(Base, TimestampMixin):
    """创作者档案表"""
    __tablename__ = "creators"

    id = Column(String, primary_key=True, default=gen_uuid)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    pen_name = Column(String(50), nullable=False)
    real_name = Column(String(50))
    introduction = Column(Text, default="")
    contact_email = Column(String(100))
    contact_phone = Column(String(20))
    social_accounts = Column(JSON, default=dict)  # {wechat: "", weibo: "", qq: ""}
    status = Column(String(20), default="active")  # active / suspended / pending
    total_words = Column(Integer, default=0)
    total_novels = Column(Integer, default=0)
    total_readers = Column(Integer, default=0)
    verified = Column(Boolean, default=False)

    user = relationship("User", back_populates="creator_profile")
    novels = relationship("Novel", back_populates="creator", cascade="all, delete-orphan")


class Category(Base):
    """小说分类表"""
    __tablename__ = "categories"

    id = Column(String, primary_key=True, default=gen_uuid)
    name = Column(String(30), unique=True, nullable=False)
    display_name = Column(String(30), nullable=False)
    sort_order = Column(Integer, default=0)

    novels = relationship("Novel", back_populates="category")


class Novel(Base, TimestampMixin):
    """小说表"""
    __tablename__ = "novels"
    __table_args__ = (
        Index("idx_novels_status", "status"),
        Index("idx_novels_creator", "creator_id"),
    )

    id = Column(String, primary_key=True, default=gen_uuid)
    title = Column(String(100), nullable=False, index=True)
    author = Column(String(50), nullable=False)
    creator_id = Column(String, ForeignKey("creators.id"), nullable=False, index=True)
    category_id = Column(String, ForeignKey("categories.id"))
    cover_url = Column(String(500))
    description = Column(Text, default="")
    tags = Column(JSON, default=list)
    status = Column(String(20), default="ongoing")  # ongoing / completed / hiatus
    word_count = Column(Integer, default=0)
    chapter_count = Column(Integer, default=0)
    view_count = Column(Integer, default=0)
    like_count = Column(Integer, default=0)
    collect_count = Column(Integer, default=0)
    rating = Column(Float, default=0.0)
    rating_count = Column(Integer, default=0)
    is_original = Column(Boolean, default=True)
    source = Column(String(20), default="upload")  # upload / manual
    featured = Column(Boolean, default=False)
    sort_order = Column(Integer, default=0)

    creator = relationship("Creator", back_populates="novels")
    category = relationship("Category", back_populates="novels")
    chapters = relationship("Chapter", back_populates="novel", cascade="all, delete-orphan", order_by="Chapter.sort_order")
    bookmarks = relationship("Bookmark", back_populates="novel", cascade="all, delete-orphan")
    history = relationship("ReadingHistory", back_populates="novel", cascade="all, delete-orphan")


class Chapter(Base, TimestampMixin):
    """章节表"""
    __tablename__ = "chapters"
    __table_args__ = (
        Index("idx_chapters_novel", "novel_id", "sort_order"),
    )

    id = Column(String, primary_key=True, default=gen_uuid)
    novel_id = Column(String, ForeignKey("novels.id"), nullable=False, index=True)
    title = Column(String(200), nullable=False)
    content = Column(Text, default="")
    word_count = Column(Integer, default=0)
    sort_order = Column(Integer, nullable=False, default=0)
    is_free = Column(Boolean, default=True)
    price = Column(Integer, default=0)
    status = Column(String(20), default="published")  # published / draft

    novel = relationship("Novel", back_populates="chapters")


class Bookmark(Base, TimestampMixin):
    """书签/收藏表"""
    __tablename__ = "bookmarks"

    id = Column(String, primary_key=True, default=gen_uuid)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    novel_id = Column(String, ForeignKey("novels.id"), nullable=False, index=True)

    user = relationship("User", back_populates="bookmarks")
    novel = relationship("Novel", back_populates="bookmarks")


class ReadingHistory(Base, TimestampMixin):
    """阅读历史表"""
    __tablename__ = "reading_histories"

    id = Column(String, primary_key=True, default=gen_uuid)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    novel_id = Column(String, ForeignKey("novels.id"), nullable=False, index=True)
    chapter_id = Column(String, ForeignKey("chapters.id"))
    chapter_index = Column(Integer, default=0)
    scroll_position = Column(Float, default=0.0)
    read_percent = Column(Float, default=0.0)

    user = relationship("User", back_populates="history")
    novel = relationship("Novel", back_populates="history")


class Announcement(Base, TimestampMixin):
    """公告表"""
    __tablename__ = "announcements"

    id = Column(String, primary_key=True, default=gen_uuid)
    title = Column(String(200), nullable=False)
    content = Column(Text, default="")
    type = Column(String(20), default="info")  # info / warning / update / activity
    is_pinned = Column(Boolean, default=False)
    is_active = Column(Boolean, default=True)
    start_time = Column(DateTime)
    end_time = Column(DateTime)
    sort_order = Column(Integer, default=0)


class AppVersion(Base, TimestampMixin):
    """应用版本表 - 用于自动更新"""
    __tablename__ = "app_versions"

    id = Column(String, primary_key=True, default=gen_uuid)
    version_name = Column(String(20), nullable=False)        # 1.0.0
    version_code = Column(Integer, nullable=False)            # 1
    download_url = Column(String(500), nullable=False)
    file_size = Column(Integer, default=0)                    # 字节
    md5 = Column(String(64))
    update_title = Column(String(200))
    update_log = Column(Text, default="")                     # 更新说明
    force_update = Column(Boolean, default=False)             # 是否强制更新
    is_active = Column(Boolean, default=True)
    min_supported_version = Column(String(20), default="1.0.0")


class UploadTask(Base, TimestampMixin):
    """上传任务表 - 跟踪文件夹批量上传进度"""
    __tablename__ = "upload_tasks"

    id = Column(String, primary_key=True, default=gen_uuid)
    creator_id = Column(String, ForeignKey("creators.id"), nullable=False, index=True)
    novel_id = Column(String, ForeignKey("novels.id"))
    status = Column(String(20), default="pending")  # pending / processing / completed / failed
    total_files = Column(Integer, default=0)
    processed_files = Column(Integer, default=0)
    failed_files = Column(Integer, default=0)
    current_file = Column(String(500))
    progress = Column(Float, default=0.0)           # 0-100
    message = Column(Text, default="")
    file_list = Column(JSON, default=list)          # 处理结果明细
    error_log = Column(Text, default="")
