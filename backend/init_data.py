"""
数据库初始化数据
创建默认分类、管理员账户、示例公告、初始版本信息
"""
import os
from datetime import datetime
from sqlalchemy.orm import Session

from database import SessionLocal
from models import Category, User, Announcement, AppVersion, Novel, Creator, Chapter
from security import hash_password


def seed_data():
    """初始化种子数据（仅在表为空时插入）"""
    db = SessionLocal()
    try:
        # 默认分类
        if db.query(Category).count() == 0:
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
                db.add(Category(name=name, display_name=display, sort_order=order))
            db.commit()
            print("[初始化] 默认分类已创建")

        # 管理员账户
        if db.query(User).filter(User.is_admin == True).count() == 0:
            admin = User(
                username="admin",
                password_hash=hash_password("admin123"),
                nickname="管理员",
                is_admin=True,
                is_creator=True,
            )
            db.add(admin)
            db.commit()

            creator = Creator(
                user_id=admin.id,
                pen_name="官方编辑部",
                introduction="墨阅小说官方账号",
                verified=True,
            )
            db.add(creator)
            db.commit()
            print("[初始化] 管理员账户已创建 (admin / admin123)")

        # 默认公告
        if db.query(Announcement).count() == 0:
            announcements = [
                Announcement(
                    title="欢迎使用墨阅小说",
                    content="墨阅小说是一款专注于原创文学的阅读平台。我们致力于为创作者提供便捷的作品发布工具，为读者提供纯净的阅读体验。\n\n当前版本支持 TXT、Markdown、DOCX 等多种格式的批量上传，创作者可通过文件夹形式一键导入整部作品。",
                    type="info",
                    is_pinned=True,
                ),
                Announcement(
                    title="创作者招募计划",
                    content="墨阅小说现已开放创作者注册。注册成为创作者后，您将获得：\n\n1. 专属创作者中心，管理您的所有作品\n2. 支持文件夹批量上传，自动解析章节\n3. 实时数据统计，掌握作品表现\n4. 多格式文件支持(txt/md/docx)\n\n立即点击「创作者中心」加入我们。",
                    type="activity",
                    is_pinned=False,
                ),
                Announcement(
                    title="版本更新说明 v1.0.0",
                    content="v1.0.0 正式版发布\n\n新增功能：\n- 完整的阅读体验与书架管理\n- 创作者中心与作品发布\n- 文件夹批量上传与自动解析\n- 实时公告系统\n- 应用内自动更新\n\n优化项：\n- 全新极简UI设计\n- 丝滑页面转场动效\n- 阅读性能优化",
                    type="update",
                    is_pinned=False,
                ),
            ]
            for a in announcements:
                db.add(a)
            db.commit()
            print("[初始化] 默认公告已创建")

        # 初始版本信息
        if db.query(AppVersion).count() == 0:
            version = AppVersion(
                version_name="1.1.0",
                version_code=2,
                download_url="/api/update/download",
                file_size=0,
                update_title="墨阅小说 v1.1.0",
                update_log="v1.1.0 更新\n- 支持 PostgreSQL 大容量数据库\n- 支持 Render 免费托管部署\n- APK 支持外部链接下载(GitHub Releases)\n- 数据库连接池优化",
                force_update=False,
                is_active=True,
                min_supported_version="1.0.0",
            )
            db.add(version)
            db.commit()
            print("[初始化] 初始版本信息已创建")

        # 示例小说（如果没有任何小说）
        if db.query(Novel).count() == 0:
            creator = db.query(Creator).first()
            cat = db.query(Category).filter(Category.name == "xuanhuan").first()
            if creator and cat:
                sample = Novel(
                    title="星河彼岸",
                    author="官方编辑部",
                    creator_id=creator.id,
                    category_id=cat.id,
                    description="在浩瀚星河的尽头，少年踏上一段寻找自我与真相的旅程。当文明的光芒逐渐黯淡，唯有勇气与信念能照亮前路。这是一个关于成长、选择与希望的故事。",
                    tags=["玄幻", "冒险", "成长"],
                    status="ongoing",
                    featured=True,
                )
                db.add(sample)
                db.commit()

                chapters = [
                    ("序章 星落", "夜空如墨，繁星点点。\n\n少年站在山巅，仰望那片亘古不变的星河。风从远方吹来，带着未知的气息。\n\n\"什么时候才能到达那里呢？\"他喃喃自语。\n\n星光洒落，仿佛在回应他的呼唤。一颗流星划过天际，拖着长长的尾迹，坠向地平线的尽头。\n\n那是命运的起点，也是一切故事的开端。"),
                    ("第一章 启程", "清晨的阳光穿过薄雾，洒在宁静的小镇上。\n\n少年收拾好行囊，回头看了一眼生活了十六年的家。门前的老槐树依旧繁茂，树下那块青石已被岁月磨得光滑。\n\n\"该走了。\"他深吸一口气，迈出了第一步。\n\n路很长，但心很坚定。前方有未知的风暴，也有未曾见过的风景。\n\n这是属于他的旅程，也是属于每个追梦人的故事。"),
                    ("第二章 迷雾森林", "走进森林的那一刻，光线便暗了下来。\n\n浓雾在树间流动，如同活物一般。古老的树木高耸入云，枝叶交织成穹顶，将天空切割成零碎的光斑。\n\n少年握紧手中的短剑，每一步都小心翼翼。远处传来不知名的鸟鸣，在寂静中显得格外清晰。\n\n\"这片森林...好像有生命。\"他低声说。\n\n风拂过树梢，像是一声叹息。"),
                ]
                for i, (title, content) in enumerate(chapters):
                    db.add(Chapter(
                        novel_id=sample.id,
                        title=title,
                        content=content,
                        word_count=len(content.replace('\n', '').replace(' ', '')),
                        sort_order=i,
                    ))
                sample.chapter_count = len(chapters)
                sample.word_count = sum(len(c[1].replace('\n', '').replace(' ', '')) for c in chapters)
                db.commit()
                print("[初始化] 示例小说已创建")

    finally:
        db.close()
