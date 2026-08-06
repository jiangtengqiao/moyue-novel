"""
墨阅小说 - 后端服务主入口
FastAPI 应用，提供完整的小说阅读平台 API
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from config import settings
from database import init_db, SessionLocal
from routers import auth, novels, chapters, creators, uploads, updates, announcements
from init_data import seed_data


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时初始化数据库
    init_db()
    seed_data()
    print(f"[{settings.APP_NAME}] v{settings.APP_VERSION} 启动成功")
    yield


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="墨阅小说后端服务 - 原创小说阅读平台",
    lifespan=lifespan,
)

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 静态文件
app.mount("/static", StaticFiles(directory=str(settings.DATA_DIR)), name="static")

# 注册路由
app.include_router(auth.router)
app.include_router(novels.router)
app.include_router(chapters.router)
app.include_router(creators.router)
app.include_router(uploads.router)
app.include_router(updates.router)
app.include_router(announcements.router)


@app.get("/", tags=["系统"])
def root():
    return {
        "name": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "running",
        "docs": "/docs",
    }


@app.get("/api/health", tags=["系统"])
def health():
    return {"status": "ok", "version": settings.APP_VERSION}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
    )
