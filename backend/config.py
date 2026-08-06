"""
应用配置模块
集中管理后端所有可配置参数
支持 SQLite(本地开发) 和 PostgreSQL(生产) 自动切换
"""
import os
from pathlib import Path
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # 服务配置
    APP_NAME: str = "墨阅小说后端服务"
    APP_VERSION: str = "1.1.0"
    DEBUG: bool = True
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # 安全配置 - 生产环境务必通过环境变量覆盖
    SECRET_KEY: str = "mo-yue-novel-secret-key-change-in-production-2024"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 10080  # 7天

    # 数据库 - 优先读取环境变量 DATABASE_URL
    # 本地开发: sqlite:///./data/moyue.db
    # 生产(PostgreSQL): postgresql://user:pass@host/dbname
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./data/moyue.db")

    # APK 下载地址 - 生产环境用外部链接(GitHub Releases 等)
    # 留空则走本地 /api/update/download 接口
    APK_DOWNLOAD_BASE: str = os.getenv("APK_DOWNLOAD_BASE", "")

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
    CURRENT_APP_VERSION: str = "1.1.0"
    APK_FILENAME: str = "moyue-latest.apk"

    class Config:
        env_file = ".env"

    @property
    def is_postgres(self) -> bool:
        """是否使用 PostgreSQL"""
        return self.DATABASE_URL.startswith("postgres")

    def ensure_dirs(self):
        """确保所有存储目录存在（本地开发用，生产环境文件系统是临时的）"""
        for d in [self.DATA_DIR, self.UPLOAD_DIR, self.NOVEL_DIR, self.COVER_DIR, self.APK_DIR]:
            try:
                d.mkdir(parents=True, exist_ok=True)
            except Exception:
                pass  # 只读文件系统时静默跳过


settings = Settings()
settings.ensure_dirs()
