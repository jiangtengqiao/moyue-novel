"""
应用更新路由: 版本检查 / APK下载
支持本地文件(开发) 和 外部链接(生产, GitHub Releases 等) 双模式
"""
import os
from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, Form
from fastapi.responses import FileResponse, RedirectResponse
from sqlalchemy.orm import Session

from config import settings
from database import get_db
from models import AppVersion
from schemas import AppVersionOut, UpdateCheckResponse, MessageResponse
from security import get_admin_user

router = APIRouter(prefix="/api/update", tags=["应用更新"])


@router.get("/check", response_model=UpdateCheckResponse)
def check_update(
    current_version: str,
    db: Session = Depends(get_db),
):
    """检查是否有新版本"""
    latest = db.query(AppVersion).filter(
        AppVersion.is_active == True
    ).order_by(AppVersion.version_code.desc()).first()

    if not latest:
        return UpdateCheckResponse(has_update=False, message="当前已是最新版本")

    has_update = _compare_versions(latest.version_name, current_version) > 0

    if _compare_versions(current_version, latest.min_supported_version) < 0:
        latest.force_update = True

    if has_update:
        return UpdateCheckResponse(
            has_update=True,
            latest_version=AppVersionOut.model_validate(latest),
            message=f"发现新版本 v{latest.version_name}"
        )
    return UpdateCheckResponse(has_update=False, message="当前已是最新版本")


@router.get("/download")
def download_apk(db: Session = Depends(get_db)):
    """
    下载最新APK安装包
    生产环境: 重定向到外部下载地址(GitHub Releases 等)
    本地开发: 返回本地文件
    """
    latest = db.query(AppVersion).filter(
        AppVersion.is_active == True
    ).order_by(AppVersion.version_code.desc()).first()

    if not latest:
        raise HTTPException(status_code=404, detail="暂无可下载的安装包")

    # 优先使用数据库中存储的完整外部下载地址
    if latest.download_url and latest.download_url.startswith("http"):
        return RedirectResponse(url=latest.download_url)

    # 本地文件模式
    apk_path = settings.APK_DIR / settings.APK_FILENAME
    if not apk_path.exists():
        raise HTTPException(status_code=404, detail="安装包文件不存在")

    return FileResponse(
        path=str(apk_path),
        media_type="application/vnd.android.package-archive",
        filename=f"moyue-v{latest.version_name}.apk",
    )


@router.post("/publish", response_model=MessageResponse)
async def publish_version(
    version_name: str = Form(...),
    version_code: int = Form(...),
    update_log: str = Form(""),
    force_update: bool = Form(False),
    min_supported_version: str = Form("1.0.0"),
    download_url: str = Form(""),        # 外部下载地址(GitHub Releases 链接)
    apk: UploadFile = File(None),         # 本地上传文件(二选一)
    db: Session = Depends(get_db),
    _admin=Depends(get_admin_user),
):
    """
    管理员发布新版本
    方式1: 提供 download_url(外部链接, 推荐, GitHub Releases)
    方式2: 上传 apk 文件(本地存储, 仅开发环境)
    """
    file_size = 0
    final_download_url = download_url.strip() if download_url else ""

    if apk is not None and apk.filename:
        content = await apk.read()
        file_size = len(content)
        try:
            apk_path = settings.APK_DIR / settings.APK_FILENAME
            with open(apk_path, "wb") as f:
                f.write(content)
        except Exception:
            pass  # 只读文件系统时忽略
        if not final_download_url:
            final_download_url = "/api/update/download"
    elif not final_download_url:
        raise HTTPException(status_code=400, detail="请提供外部下载地址或上传APK文件")

    # 停用旧版本
    db.query(AppVersion).update({AppVersion.is_active: False})

    version = AppVersion(
        version_name=version_name,
        version_code=version_code,
        download_url=final_download_url,
        file_size=file_size,
        update_title=f"墨阅小说 v{version_name}",
        update_log=update_log,
        force_update=force_update,
        min_supported_version=min_supported_version,
        is_active=True,
    )
    db.add(version)
    db.commit()

    return MessageResponse(message=f"版本 v{version_name} 发布成功", success=True)


def _compare_versions(v1: str, v2: str) -> int:
    """比较版本号，返回 1(v1>v2) / 0(等于) / -1(v1<v2)"""
    parts1 = [int(x) for x in v1.split('.')]
    parts2 = [int(x) for x in v2.split('.')]
    for i in range(max(len(parts1), len(parts2))):
        a = parts1[i] if i < len(parts1) else 0
        b = parts2[i] if i < len(parts2) else 0
        if a > b:
            return 1
        elif a < b:
            return -1
    return 0
