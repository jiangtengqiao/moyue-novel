"""
Pydantic 数据模型 (请求/响应 schema)
"""
from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field, ConfigDict


# ==================== 用户 ====================

class UserRegister(BaseModel):
    username: str = Field(min_length=3, max_length=50)
    password: str = Field(min_length=6, max_length=100)
    nickname: Optional[str] = None


class UserLogin(BaseModel):
    username: str
    password: str


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    username: str
    nickname: Optional[str]
    avatar: Optional[str]
    bio: Optional[str]
    is_creator: bool
    created_at: datetime


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut


# ==================== 创作者 ====================

class CreatorRegister(BaseModel):
    pen_name: str = Field(min_length=1, max_length=50)
    real_name: Optional[str] = None
    introduction: Optional[str] = None
    contact_email: Optional[str] = None
    contact_phone: Optional[str] = None
    social_accounts: Optional[dict] = None


class CreatorOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    user_id: str
    pen_name: str
    real_name: Optional[str]
    introduction: Optional[str]
    contact_email: Optional[str]
    verified: bool
    total_words: int
    total_novels: int
    total_readers: int
    created_at: datetime


# ==================== 分类 ====================

class CategoryOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    name: str
    display_name: str
    sort_order: int


# ==================== 小说 ====================

class NovelCreate(BaseModel):
    title: str = Field(min_length=1, max_length=100)
    author: str = Field(min_length=1, max_length=50)
    category_id: Optional[str] = None
    description: Optional[str] = ""
    tags: Optional[List[str]] = []
    status: Optional[str] = "ongoing"


class NovelUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    cover_url: Optional[str] = None
    tags: Optional[List[str]] = None
    status: Optional[str] = None
    category_id: Optional[str] = None


class NovelOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    title: str
    author: str
    creator_id: str
    category_id: Optional[str]
    cover_url: Optional[str]
    description: Optional[str]
    tags: List[str] = []
    status: str
    word_count: int
    chapter_count: int
    view_count: int
    like_count: int
    collect_count: int
    rating: float
    featured: bool
    created_at: datetime
    updated_at: datetime


class NovelBrief(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    title: str
    author: str
    cover_url: Optional[str]
    description: Optional[str]
    status: str
    word_count: int
    chapter_count: int
    view_count: int
    rating: float


# ==================== 章节 ====================

class ChapterCreate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    content: str = ""
    sort_order: Optional[int] = None
    is_free: Optional[bool] = True


class ChapterOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    novel_id: str
    title: str
    word_count: int
    sort_order: int
    is_free: bool
    status: str
    created_at: datetime


class ChapterContent(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    novel_id: str
    title: str
    content: str
    word_count: int
    sort_order: int
    is_free: bool


# ==================== 公告 ====================

class AnnouncementOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    title: str
    content: str
    type: str
    is_pinned: bool
    created_at: datetime


# ==================== 应用更新 ====================

class AppVersionOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    version_name: str
    version_code: int
    download_url: str
    file_size: int
    md5: Optional[str]
    update_title: Optional[str]
    update_log: str
    force_update: bool
    min_supported_version: str
    created_at: datetime


class UpdateCheckResponse(BaseModel):
    has_update: bool
    latest_version: Optional[AppVersionOut] = None
    message: str = ""


# ==================== 上传任务 ====================

class UploadTaskOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    creator_id: str
    novel_id: Optional[str]
    status: str
    total_files: int
    processed_files: int
    failed_files: int
    current_file: Optional[str]
    progress: float
    message: str
    file_list: list = []
    created_at: datetime
    updated_at: datetime


# ==================== 通用 ====================

class PaginatedResponse(BaseModel):
    total: int
    page: int
    page_size: int
    items: list


class MessageResponse(BaseModel):
    message: str
    success: bool = True


class ErrorResponse(BaseModel):
    detail: str
