# 墨阅小说 (MoYue Novel)

> 原创文学阅读平台 - 安卓客户端 + 后端服务

**当前版本: v1.1.0**  
**更新日期: 2026-08-06**

---

## 项目简介

墨阅小说是一款专注于原创文学的阅读平台，包含完整的安卓客户端和后端服务。平台支持创作者注册、作品发布、多格式文件批量上传与自动解析，为读者提供纯净流畅的阅读体验。

设计理念: 极简、高级、墨色为底。全应用无任何 emoji，采用丝滑的 UI 流动特效与克制的视觉语言。

---

## 技术架构

### 客户端 (Android)

| 技术 | 说明 |
|------|------|
| Kotlin | 100% Kotlin |
| Jetpack Compose | 声明式 UI 框架 |
| Material 3 | Material Design 3 设计系统 |
| Hilt | 依赖注入 |
| Retrofit + OkHttp | 网络请求 |
| Kotlinx Serialization | JSON 序列化 |
| Coil | 图片加载 |
| DataStore | 本地持久化 |
| Navigation Compose | 页面导航 |
| Coroutines | 异步编程 |

### 后端服务

| 技术 | 说明 |
|------|------|
| Python 3.13 | 运行时 |
| FastAPI | Web 框架 |
| SQLAlchemy 2.0 | ORM |
| SQLite / PostgreSQL | 数据库(开发用 SQLite，生产用 PostgreSQL) |
| psycopg2 | PostgreSQL 驱动 |
| python-docx | DOCX 文件解析 |
| markdown | Markdown 解析 |
| bcrypt | 密码哈希 |
| python-jose | JWT 令牌 |

---

## 功能特性

### 阅读功能
- 书城首页: 精选推荐、分类筛选、热门排行
- 小说详情: 封面、简介、标签、章节目录
- 沉浸式阅读器: 手势翻页、字号调节、阅读进度
- 书架管理: 收藏作品、快速续读
- 全文搜索: 按书名/作者搜索

### 创作者中心
- 创作者注册: 笔名、简介、联系方式
- 数据仪表盘: 作品数、总字数、阅读量、收藏量
- 发布作品: 创建小说基本信息
- 多格式上传: 支持 TXT / Markdown / DOCX
- 文件夹批量上传: 多文件同时上传，自动解析章节
- 实时进度: 上传处理进度可视化，文件明细展示
- 作品管理: 查看和管理已发布作品

### 自动更新系统
- 启动时自动检查新版本
- 应用内下载 APK 安装包
- 下载进度实时显示
- 支持强制更新
- 版本更新日志展示

### 公告系统
- 实时公告拉取
- 启动时公告弹窗提醒
- 公告中心列表浏览
- 公告详情查看
- 支持置顶、分类(信息/警告/更新/活动)

### UI / 体验
- 极简高级感设计: 墨色主调，纸张背景
- 丝滑流动特效: 入场动画、弹性缩放、渐变转场
- 列表项依次入场动画
- 深色/浅色主题自适应
- 衬线字体标题，提升文学质感
- 全应用零 emoji 使用

---

## 项目结构

```
novel-reader/
|-- android/                    # Android 客户端
|   |-- app/
|   |   |-- build.gradle.kts    # 应用构建配置
|   |   `-- src/main/
|   |       |-- AndroidManifest.xml
|   |       |-- java/com/novel/reader/
|   |       |   |-- MoYueApp.kt             # Application 入口
|   |       |   |-- MainActivity.kt          # 主 Activity + 导航图
|   |       |   |-- MainViewModel.kt         # 启动逻辑(更新检查/公告)
|   |       |   |-- Constants.kt             # 全局常量
|   |       |   |-- data/
|   |       |   |   |-- api/MoYueApi.kt      # Retrofit API 接口
|   |       |   |   |-- model/Models.kt      # 数据模型
|   |       |   |   `-- repository/          # 数据仓库
|   |       |   |-- di/                       # Hilt 依赖注入模块
|   |       |   |-- service/UpdateManager.kt # 自动更新管理器
|   |       |   `-- ui/
|   |       |       |-- theme/               # 主题(颜色/字体/形状)
|   |       |       |-- animations/          # 动画工具
|   |       |       |-- components/           # 通用组件
|   |       |       `-- screens/             # 所有页面
|   |       `-- res/                          # 资源文件
|   |-- gradle/libs.versions.toml             # 依赖版本目录
|   `-- settings.gradle.kts
|
|-- backend/                    # 后端服务
|   |-- main.py                # FastAPI 入口
|   |-- config.py              # 配置
|   |-- database.py            # 数据库连接
|   |-- models.py              # ORM 模型
|   |-- schemas.py             # Pydantic 模型
|   |-- security.py            # 认证与加密
|   |-- init_data.py           # 初始化数据
|   |-- routers/               # API 路由
|   |   |-- auth.py            # 认证
|   |   |-- novels.py          # 小说
|   |   |-- chapters.py        # 章节
|   |   |-- creators.py        # 创作者
|   |   |-- uploads.py         # 文件上传
|   |   |-- updates.py         # 应用更新
|   |   `-- announcements.py   # 公告
|   |-- services/              # 业务服务
|   |   |-- file_parser.py     # 文件解析(txt/md/docx)
|   |   `-- upload_processor.py# 上传处理与进度
|   `-- requirements.txt       # Python 依赖
|
|-- docs/                      # 文档
|-- .gitignore
`-- README.md
```

---

## 快速开始

### 后端服务启动

```bash
# 1. 进入后端目录
cd backend

# 2. 创建虚拟环境
python -m venv venv
source venv/bin/activate    # Linux/Mac
# venv\Scripts\activate     # Windows

# 3. 安装依赖
pip install -r requirements.txt

# 4. 启动服务
python main.py
# 或: uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# 5. 访问 API 文档
# http://localhost:8000/docs
```

默认管理员账户: `admin` / `admin123`

### Android 客户端构建

1. 使用 Android Studio 打开 `android/` 目录
2. 等待 Gradle 同步完成
3. 修改 `Constants.kt` 中的 `BASE_URL` 为后端服务地址
   - 模拟器: `http://10.0.2.2:8000/`
   - 真机: `http://<你的IP>:8000/`
4. 点击 Run 构建

**环境要求:**
- Android Studio Hedgehog 或更高
- JDK 17
- Android SDK 35
- Kotlin 2.1.0

---

## API 接口

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/auth/me` | 获取当前用户 |

### 小说
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/novels/categories` | 获取分类列表 |
| GET | `/api/novels/` | 小说列表(分页/筛选/排序) |
| GET | `/api/novels/search` | 搜索小说 |
| GET | `/api/novels/featured` | 精选推荐 |
| GET | `/api/novels/{id}` | 小说详情 |
| POST | `/api/novels/` | 创建小说(创作者) |
| POST | `/api/novels/{id}/bookmark` | 收藏/取消收藏 |

### 章节
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/novels/{novelId}/chapters/` | 章节列表 |
| GET | `/api/novels/{novelId}/chapters/index/{index}` | 按序号读取章节 |
| POST | `/api/novels/{novelId}/chapters/` | 创建章节 |

### 创作者
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/creator/register` | 注册创作者 |
| GET | `/api/creator/profile` | 创作者档案 |
| GET | `/api/creator/novels` | 我的作品 |
| GET | `/api/creator/dashboard` | 仪表盘数据 |

### 文件上传
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/upload/single/{novelId}` | 单文件上传 |
| POST | `/api/upload/folder/{novelId}` | 文件夹批量上传 |
| GET | `/api/upload/task/{taskId}` | 查询上传进度 |

### 公告
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/announcements/` | 公告列表 |
| GET | `/api/announcements/latest` | 最新公告 |

### 应用更新
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/update/check` | 检查更新 |
| GET | `/api/update/download` | 下载 APK |
| POST | `/api/update/publish` | 发布新版本(管理员) |

---

## 文件解析说明

系统支持自动解析以下格式的文件并切分章节:

### TXT
- 自动识别编码(UTF-8 / GBK / GB2312 / Big5 / UTF-16)
- 按章节标题正则切分(第X章 / Chapter X / 数字序号等)

### Markdown
- 转换 Markdown 标记为纯文本
- 提取标题作为章节名
- 支持 heading 自动分章

### DOCX
- 解析 Word 文档段落
- 识别 Heading 样式作为章节分隔
- 保留正文内容

### 章节切分规则
系统会识别以下章节标题格式:
- 第X章 / 第X节 / 第X回 / 第X卷
- Chapter X
- 数字序号 (1. / 1、 / 1.)
- 【标题】 / [标题]

无法识别标题时，整个文件作为一个章节处理。

---

## 版本历史

### v1.1.0 (2026-08-06)
- 数据库支持 PostgreSQL(生产) 与 SQLite(开发) 双模式自动切换
- 新增 psycopg2 驱动，支持 Neon / Supabase 免费 PostgreSQL
- 新增 render.yaml，支持 Render 免费托管部署
- APK 下载支持外部链接(GitHub Releases)，适配临时文件系统
- 数据库连接池优化(pool_pre_ping 自动重连)
- 版本发布接口支持外部下载地址(无需上传文件)
- 新增 .env.example 环境变量模板
- 后端版本号同步至 1.1.0

### v1.0.0 (2026-08-06)
- 首个正式版本
- 完整的阅读体验: 书城、详情、阅读器、书架
- 创作者中心: 注册、仪表盘、作品管理
- 多格式文件上传: TXT / Markdown / DOCX
- 文件夹批量上传与自动解析
- 实时处理进度展示
- 应用内自动更新系统
- 实时公告系统
- 极简高级 UI 设计与丝滑动画
- 深色/浅色主题支持

---

## 数据库模型

| 表名 | 说明 |
|------|------|
| users | 用户 |
| creators | 创作者档案 |
| categories | 分类 |
| novels | 小说 |
| chapters | 章节 |
| bookmarks | 收藏 |
| reading_histories | 阅读历史 |
| announcements | 公告 |
| app_versions | 应用版本 |
| upload_tasks | 上传任务 |

---

## 部署说明

### 免费托管部署 (推荐)

#### 第一步: 创建免费 PostgreSQL 数据库 (Neon)

1. 注册 [Neon](https://neon.tech) (免费 0.5GB，Serverless，永久有效)
2. 创建项目，获取连接串，格式:
   ```
   postgresql://用户名:密码@ep-xxx.region.aws.neon.tech/库名?sslmode=require
   ```
3. PostgreSQL 的 TEXT 类型单行可存储最大 1GB 文本，完全满足小说章节存储需求

> 也可使用 [Supabase](https://supabase.com) (免费 500MB) 或 [Render PostgreSQL](https://render.com) (免费 90 天)

#### 第二步: 部署后端到 Render

1. 注册 [Render](https://render.com)
2. New > Web Service > 连接 GitHub 仓库 `jiangtengqiao/moyue-novel`
3. 配置:
   - Root Directory: `backend`
   - Build Command: `pip install -r requirements.txt`
   - Start Command:
     ```
     python -c "from database import init_db; from init_data import seed_data; init_db(); seed_data()" && uvicorn main:app --host 0.0.0.0 --port $PORT
     ```
4. 环境变量(Environment):
   - `DATABASE_URL` = 第一步获取的 Neon 连接串
   - `SECRET_KEY` = 随便填一个长随机字符串
5. 部署完成后获得地址: `https://moyue-novel-api.onrender.com`
6. 免费套餐会在 15 分钟无请求后休眠，首次请求冷启动约 30 秒

#### 第三步: 托管 APK 安装包 (GitHub Releases)

1. 在本仓库创建 Release，上传 APK 文件
2. 获取下载链接: `https://github.com/jiangtengqiao/moyue-novel/releases/download/v1.1.0/moyue.apk`
3. 调用发布接口注册版本:
   ```
   POST /api/update/publish
   (登录 admin 账号后，提供 download_url 参数为上面的 GitHub Releases 链接)
   ```

#### 第四步: 更新 Android 客户端地址

修改 `android/app/src/main/java/com/novel/reader/Constants.kt`:
```kotlin
const val BASE_URL = "https://moyue-novel-api.onrender.com/"
```

### 本地开发部署

1. 安装 Python 3.13+
2. 安装依赖: `pip install -r requirements.txt`
3. 复制 `.env.example` 为 `.env`，按需配置
4. 启动: `uvicorn main:app --host 0.0.0.0 --port 8000 --reload`
5. 访问 API 文档: `http://localhost:8000/docs`

### Android 签名打包

1. 在 Android Studio 中 Build > Generate Signed Bundle / APK
2. 创建或选择 keystore
3. 选择 release 变体构建
4. 将 APK 上传到 GitHub Release，然后调用 `/api/update/publish` 注册版本

### 更新后端地址
修改 `android/app/src/main/java/com/novel/reader/Constants.kt`:
```kotlin
const val BASE_URL = "https://your-server.com/"
```

---

## 开源协议

本项目为私有项目，所有版权归创作者所有。
