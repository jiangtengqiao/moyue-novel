"""
数据库连接与会话管理
支持 SQLite(本地开发) 和 PostgreSQL(生产) 双模式
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

from config import settings

# SQLite 需要 check_same_thread=False，PostgreSQL 不需要
if settings.is_postgres:
    engine = create_engine(
        settings.DATABASE_URL,
        pool_pre_ping=True,       # 自动检测断连并重连
        pool_size=10,
        max_overflow=20,
        echo=False,
    )
else:
    engine = create_engine(
        settings.DATABASE_URL,
        connect_args={"check_same_thread": False},
        echo=False,
    )

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """获取数据库会话的依赖注入函数"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """初始化数据库，创建所有表"""
    Base.metadata.create_all(bind=engine)
