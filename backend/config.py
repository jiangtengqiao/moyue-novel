"""
应用配置模块
集中管理后端所有可配置参数
"""
import os
from pathlib import Path
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # 服务配置
    APP_NAME: str = "墨阅小说后端服务"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # 安全配置
    SECRET_KEY: str = "mo-yue-novel-secret-key-change-in-production-2024"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 10080  # 7天

    # 数据库
    DATABASE_URL: str = "sqlite:///./data/moyue.db"

    # 文件存储
    BASE_DIR: Path = Path(__file__).resolve().parent
    DATA_DIR: Path = BASE_DIR / "data"
    UPLOAD_DIR: Path = DATA_DIR / "uploads"
    NOVEL_DIR: Path = DATA_DIR / "novels"
    COVER_DIR: Path = DATA_DIR / "covers"
    APK_DIR: Path = DATA_DIR / "apk"

    # 分页默认值
    DEFAULT_PAGE_SIZE: int = 20
    MAX_PAGE_SIZE: int = 100

    # 上传限制
    MAX_FILE_SIZE_MB: int = 50
    ALLOWED_TEXT_EXTENSIONS: list = [".txt", ".md", ".markdown", ".docx"]

    # 更新配置
    CURRENT_APP_VERSION: str = "1.0.0"
    APK_FILENAME: str = "moyue-latest.apk"

    class Config:
        env_file = ".env"

    def ensure_dirs(self):
        """确保所有存储目录存在"""
        for d in [self.DATA_DIR, self.UPLOAD_DIR, self.NOVEL_DIR, self.COVER_DIR, self.APK_DIR]:
            d.mkdir(parents=True, exist_ok=True)


settings = Settings()
settings.ensure_dirs()
