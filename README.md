# 墨阅小说 (MoYue Novel)

> 原创文学阅读平台 — Android 客户端 + Node.js 后端服务

**当前版本: v1.1.0**
**更新日期: 2026-08-07**
**后端部署: 腾讯云 SCF (Serverless Cloud Function)**

---

## 目录

- [项目简介](#项目简介)
- [技术架构](#技术架构)
- [功能特性](#功能特性)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API 接口文档](#api-接口文档)
- [文件解析说明](#文件解析说明)
- [数据库模型](#数据库模型)
- [部署说明](#部署说明)
- [版本历史与更新日志](#版本历史与更新日志)
- [开发指南](#开发指南)
- [常见问题](#常见问题)
- [开源协议](#开源协议)

---

## 项目简介

墨阅小说是一款专注于原创文学的阅读平台，包含完整的 Android 客户端和 Node.js 后端服务。平台支持创作者注册、作品发布、多格式文件批量上传与自动解析，为读者提供纯净流畅的阅读体验。

### 设计理念

- **极简高级感**: 墨色主调，纸张背景，衬线字体标题提升文学质感
- **丝滑流动特效**: 入场动画、弹性缩放、渐变转场，全应用零 emoji
- **创作者友好**: 支持 TXT / Markdown / DOCX 多格式上传，自动切分章节
- **Serverless 架构**: 后端部署于腾讯云 SCF，按需调用，零成本 idle

### 当前内容

书城已预置 9 部公版经典文学作品，覆盖玄幻、历史、都市、短篇、灵异等分类：

| 作品 | 作者 | 章节数 | 字数 | 精选 |
|------|------|--------|------|------|
| 聊斋志异精选 | 蒲松龄 | 7 | 1752 | 是 |
| 西游记 | 吴承恩 | 3 | 514 | 是 |
| 三国演义 | 罗贯中 | 3 | 437 | 是 |
| 水浒传 | 施耐庵 | 3 | 373 | 否 |
| 呐喊 | 鲁迅 | 5 | 1055 | 是 |
| 彷徨 | 鲁迅 | 5 | 775 | 否 |
| 老残游记 | 刘鹗 | 3 | 419 | 否 |
| 儒林外史 | 吴敬梓 | 3 | 392 | 否 |
| 星河彼岸 | 官方编辑部 | 3 | 269 | 是 |

---

## 技术架构

### 客户端 (Android)

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 2.1.0 | 100% Kotlin，零 Java 代码 |
| Jetpack Compose | BOM 2024.x | 声明式 UI 框架 |
| Material 3 | 最新 | Material Design 3 设计系统 |
| Hilt | 2.x | 依赖注入 |
| Retrofit + OkHttp | 2.9+/4.x | 网络请求 |
| Kotlinx Serialization | 1.x | JSON 序列化 |
| Coil | 2.x | 图片加载 |
| DataStore | 1.x | 本地持久化（替代 SharedPreferences） |
| Navigation Compose | 2.x | 页面导航 |
| Coroutines | 1.x | 异步编程 |
| compileSdk | 35 | Android 15 |
| minSdk | 26 | Android 8.0 |
| targetSdk | 35 | Android 15 |

### 后端服务

| 技术 | 说明 |
|------|------|
| Node.js 18 | SCF 运行时环境 |
| Express | 4.x Web 框架 |
| node-postgres (pg) | PostgreSQL 驱动 |
| bcryptjs | 密码哈希 |
| jsonwebtoken | JWT 令牌认证 |
| multer | 文件上传 (multipart/form-data) |
| adm-zip | DOCX 文件解压解析 |
| iconv-lite | 多编码文本识别 (UTF-8 / GBK / GB2312 / Big5) |

### 基础设施

| 组件 | 说明 |
|------|------|
| 腾讯云 SCF | Serverless 函数计算，HTTP 触发器 |
| PostgreSQL | 关系型数据库（外部托管） |
| GitHub | 代码仓库 + APK Releases 托管 |

---

## 功能特性

### 一、阅读功能

#### 1.1 书城首页

- **精选推荐**: 横向滑动展示精选作品，配以封面与简介
- **分类筛选**: 10 大分类（玄幻 / 仙侠 / 都市 / 历史 / 科幻 / 军事 / 游戏 / 灵异 / 短篇 / 其他）
- **热门排行**: 按阅读量排序的热门作品列表
- **最新上架**: 按创建时间排序的新作品
- **下拉刷新**: 实时获取最新书库内容
- **列表项动画**: 依次入场动画，视觉流畅

#### 1.2 小说详情

- **基本信息**: 标题、作者、分类、标签、简介
- **数据统计**: 章节数、总字数、阅读量、收藏量
- **章节目录**: 完整章节列表，显示章节标题与字数
- **操作入口**: 开始阅读 / 加入书架 / 分享
- **阅读进度**: 显示上次阅读位置，支持续读

#### 1.3 沉浸式阅读器

- **手势翻页**: 左右滑动翻页，上下滚动浏览
- **字号调节**: 支持多档字号切换（小 / 中 / 大 / 超大）
- **阅读进度**: 自动记录阅读位置，跨设备同步
- **章节导航**: 上一章 / 下一章快速切换
- **沉浸模式**: 隐藏状态栏与导航栏，纯净阅读
- **主题适配**: 深色 / 浅色模式自动切换

#### 1.4 书架管理

- **收藏作品**: 一键加入书架
- **快速续读**: 显示阅读进度，点击直接跳转上次位置
- **移除收藏**: 长按或详情页取消收藏
- **排序**: 按收藏时间排序

#### 1.5 全文搜索

- **关键词搜索**: 按书名 / 作者搜索
- **实时建议**: 输入时显示搜索建议
- **搜索历史**: 记录最近搜索关键词
- **结果展示**: 搜索结果列表，显示匹配信息

### 二、创作者中心

#### 2.1 创作者注册

- **笔名设置**: 创建创作者档案
- **简介填写**: 个人介绍
- **联系方式**: 可选的联系信息

#### 2.2 数据仪表盘

- **总作品数**: 已发布的作品数量
- **总字数**: 所有作品累计字数
- **总阅读量**: 所有作品被阅读次数
- **总收藏量**: 被读者收藏的次数
- **总章节数**: 所有作品的章节总数
- **最近作品**: 最近发布的 5 部作品概览

#### 2.3 作品发布

- **创建小说**: 填写标题、作者、简介、分类、标签
- **状态管理**: 草稿 / 已发布 状态切换
- **精选设置**: 管理员可设置/取消精选推荐

#### 2.4 多格式文件上传

- **TXT 上传**: 自动识别编码（UTF-8 / GBK / GB2312 / Big5 / UTF-16）
- **Markdown 上传**: 转换 Markdown 标记为纯文本，提取标题分章
- **DOCX 上传**: 解析 Word 文档段落，识别 Heading 样式分章
- **文件夹批量上传**: 多文件同时上传，自动解析章节
- **实时进度**: 上传处理进度可视化，文件明细展示
- **自动分章**: 智能识别章节标题格式，自动切分

### 三、自动更新系统

- **启动检查**: 应用启动时自动检查新版本
- **下载安装**: 应用内下载 APK 安装包
- **进度显示**: 下载进度实时展示
- **强制更新**: 支持强制更新配置
- **更新日志**: 版本更新内容展示

### 四、公告系统

- **实时拉取**: 从后端获取最新公告
- **启动弹窗**: 启动时公告弹窗提醒（仅显示一次）
- **公告中心**: 公告列表浏览
- **详情查看**: 公告详情页面
- **分类标签**: 支持 信息 / 警告 / 更新 / 活动 四种类型
- **置顶功能**: 重要公告置顶显示

### 五、UI / 体验

- **极简高级感设计**: 墨色主调，纸张背景
- **丝滑流动特效**: 入场动画、弹性缩放、渐变转场
- **列表项依次入场动画**: 视觉层次分明
- **深色 / 浅色主题**: 自适应系统主题
- **衬线字体标题**: 提升文学质感
- **全应用零 emoji**: 克制的视觉语言
- **Material 3 设计**: 遵循最新 Material Design 规范

---

## 项目结构

```
moyue-novel/
|
|-- android/                          # Android 客户端
|   |-- app/
|   |   |-- build.gradle.kts          # 应用构建配置
|   |   |-- proguard-rules.pro        # 代码混淆规则
|   |   `-- src/main/
|   |       |-- AndroidManifest.xml   # 清单文件
|   |       |-- java/com/novel/reader/
|   |       |   |-- MoYueApp.kt              # Application 入口 (Hilt)
|   |       |   |-- MainActivity.kt          # 主 Activity + 导航图
|   |       |   |-- MainViewModel.kt         # 启动逻辑 (更新检查/公告)
|   |       |   |-- Constants.kt             # 全局常量 (BASE_URL / 版本号)
|   |       |   |-- data/
|   |       |   |   |-- api/MoYueApi.kt      # Retrofit API 接口定义
|   |       |   |   |-- model/Models.kt      # 数据模型 (序列化)
|   |       |   |   `-- repository/
|   |       |   |       |-- NovelRepository.kt  # 数据仓库
|   |       |   |       `-- SessionManager.kt   # 会话管理
|   |       |   |-- di/
|   |       |   |   |-- NetworkModule.kt     # 网络依赖注入
|   |       |   |   `-- DataStoreModule.kt   # DataStore 依赖注入
|   |       |   |-- service/
|   |       |   |   `-- UpdateManager.kt     # 自动更新管理器
|   |       |   `-- ui/
|   |       |       |-- theme/
|   |       |       |   |-- Color.kt         # 颜色定义
|   |       |       |   |-- Theme.kt         # 主题配置
|   |       |       |   `-- Type.kt          # 字体定义
|   |       |       |-- animations/
|   |       |       |   `-- Animations.kt    # 动画工具函数
|   |       |       |-- components/
|   |       |       |   `-- Components.kt    # 通用 UI 组件
|   |       |       |-- screens/
|   |       |       |   |-- SplashScreen.kt          # 启动页
|   |       |       |   |-- AuthScreens.kt           # 登录/注册
|   |       |       |   |-- MainScreen.kt            # 主框架 (底部导航)
|   |       |       |   |-- HomeScreen.kt            # 书城首页
|   |       |       |   |-- SearchScreen.kt          # 搜索页
|   |       |       |   |-- LibraryScreen.kt         # 书架
|   |       |       |   |-- ProfileScreen.kt         # 我的
|   |       |       |   |-- NovelDetailScreen.kt     # 小说详情
|   |       |       |   |-- ReaderScreen.kt          # 阅读器
|   |       |       |   |-- AnnouncementsScreen.kt   # 公告中心
|   |       |       |   |-- CreatorScreen.kt         # 创作者中心
|   |       |       |   |-- CreatorRegisterScreen.kt # 创作者注册
|   |       |       |   |-- CreatorDashboardScreen.kt# 数据仪表盘
|   |       |       |   |-- CreateNovelScreen.kt     # 创建小说
|   |       |       |   |-- MyNovelsScreen.kt        # 我的作品
|   |       |       |   |-- UploadNovelScreen.kt     # 上传文件
|   |       |       |   `-- UploadProgressScreen.kt  # 上传进度
|   |       |       `-- Routes.kt            # 路由定义
|   |       `-- res/                          # 资源文件
|   |           |-- drawable/                 # 矢量图标
|   |           |-- mipmap-anydpi-v26/       # 自适应图标
|   |           |-- values/                   # 字符串/颜色/主题
|   |           |-- values-night/             # 深色主题
|   |           `-- xml/                      # FileProvider / 网络安全配置
|   |-- gradle/
|   |   |-- wrapper/                          # Gradle Wrapper
|   |   `-- libs.versions.toml               # 依赖版本目录
|   |-- build.gradle.kts                      # 项目级构建
|   |-- settings.gradle.kts                   # 项目设置
|   `-- gradle.properties                     # Gradle 属性
|
|-- backend-node/                             # Node.js 后端服务
|   |-- index.js                              # SCF 入口 (Web 函数适配)
|   |-- app.js                                # Express 应用 + 中间件
|   |-- db.js                                 # PostgreSQL 连接池 + 种子数据
|   |-- auth.js                               # JWT 认证 + bcrypt 密码
|   |-- schema.sql                            # 数据库建表语句
|   |-- scf_bootstrap                         # SCF 启动脚本
|   |-- package.json                          # Node.js 依赖
|   |-- seed-novels.js                        # 公版文学批量导入脚本
|   |-- fix-chapters.js                       # 章节补传 + 精选设置脚本
|   |-- routes/
|   |   |-- auth.js                           # 认证路由
|   |   |-- novels.js                         # 小说路由 (含精选设置)
|   |   |-- chapters.js                       # 章节路由
|   |   |-- creators.js                       # 创作者路由
|   |   |-- uploads.js                        # 文件上传路由
|   |   |-- announcements.js                  # 公告路由
|   |   `-- updates.js                        # 版本更新路由
|   `-- services/
|       `-- fileParser.js                     # 文件解析 (TXT/MD/DOCX)
|
|-- .gitignore
`-- README.md
```

---

## 快速开始

### 后端服务 (本地开发)

```bash
# 1. 进入后端目录
cd backend-node

# 2. 安装依赖
npm install

# 3. 配置环境变量
#    创建 .env 文件或直接设置环境变量
export DATABASE_URL="postgresql://用户名:密码@主机:端口/库名"
export SECRET_KEY="你的JWT密钥"

# 4. 启动服务
node -e "const app = require('./app'); app.listen(3000, () => console.log('Server on :3000'))"

# 5. 验证
curl http://localhost:3000/api/health
# 期望: {"status":"ok","version":"1.0.0"}
```

### 后端服务 (腾讯云 SCF 部署)

详见 [部署说明](#部署说明) 章节。

### Android 客户端构建

#### 环境要求

- Android Studio Hedgehog 或更高
- JDK 17+ (推荐 JDK 21)
- Android SDK 35 (compileSdk)
- Kotlin 2.1.0
- Gradle 8.9

#### 构建步骤

```bash
# 1. 使用 Android Studio 打开 android/ 目录
# 2. 等待 Gradle 同步完成
# 3. 修改 Constants.kt 中的 BASE_URL (如需)
# 4. Build > Build Bundle(s) / APK(s) > Build APK(s)
```

#### 命令行构建

```bash
cd android

# 生成 Debug APK
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# 生成 Release APK (需配置签名)
./gradlew assembleRelease
# 输出: app/build/outputs/apk/release/app-release.apk
```

#### 命令行构建 (Windows, 无 Android Studio)

```powershell
# 前提: 已安装 JDK 17+, Android SDK (platform-35, build-tools)
# 创建 local.properties
echo "sdk.dir=C:\\Users\\你的用户名\\Android\\Sdk" > android\local.properties

# 使用缓存的 Gradle 构建-wrapper
cd android
.\gradlew.bat assembleDebug
```

### 默认管理员账户

```
用户名: admin
密码:   admin123
```

---

## API 接口文档

### 基础信息

- **Base URL**: `https://1432945062-f2koniz849.ap-guangzhou.tencentscf.com`
- **认证方式**: JWT Bearer Token
- **Content-Type**: `application/json` (文件上传除外)
- **文件上传**: `multipart/form-data`

### 认证接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 否 | 用户注册 |
| POST | `/api/auth/login` | 否 | 用户登录 |
| GET | `/api/auth/me` | 是 | 获取当前用户信息 |

#### 注册示例

```json
POST /api/auth/register
{
  "username": "reader01",
  "password": "mypassword"
}

// 响应 201
{
  "id": "uuid",
  "username": "reader01",
  "access_token": "eyJhbGci..."
}
```

#### 登录示例

```json
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}

// 响应 200
{
  "access_token": "eyJhbGci...",
  "user": {
    "id": "uuid",
    "username": "admin",
    "is_admin": true,
    "is_creator": false
  }
}
```

### 小说接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/novels/categories` | 否 | 获取全部分类 |
| GET | `/api/novels/` | 否 | 小说列表 (分页/筛选/排序) |
| GET | `/api/novels/featured` | 否 | 精选推荐 |
| GET | `/api/novels/search?keyword=` | 否 | 搜索小说 |
| GET | `/api/novels/{id}` | 否 | 小说详情 |
| POST | `/api/novels/` | 是 | 创建小说 (创作者) |
| POST | `/api/novels/{id}/bookmark` | 是 | 收藏/取消收藏 |
| GET | `/api/novels/bookmarks/list` | 是 | 我的书架 |
| PATCH | `/api/novels/{id}/featured` | 是(管理员) | 设置/取消精选 |

#### 列表查询参数

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码 (默认 1) |
| page_size | int | 每页条数 (默认 20) |
| category | string | 分类筛选 |
| sort | string | 排序: latest / hot / words |

### 章节接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/novels/{novelId}/chapters/` | 否 | 章节列表 |
| GET | `/api/novels/{novelId}/chapters/index/{index}` | 否 | 按序号读取章节 (从 1 开始) |
| POST | `/api/novels/{novelId}/chapters/` | 是(创作者) | 创建章节 |

### 创作者接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/creator/register` | 是 | 注册创作者 |
| GET | `/api/creator/profile` | 是(创作者) | 创作者档案 |
| GET | `/api/creator/novels` | 是(创作者) | 我的作品 |
| GET | `/api/creator/dashboard` | 是(创作者) | 仪表盘数据 |

### 文件上传接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/upload/single/{novelId}` | 是(创作者) | 单文件上传 |
| POST | `/api/upload/folder/{novelId}` | 是(创作者) | 文件夹批量上传 |
| GET | `/api/upload/task/{taskId}` | 是(创作者) | 查询上传进度 |

#### 单文件上传示例

```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -F "file=@小说.txt" \
  https://<base-url>/api/upload/single/<novelId>

// 响应 200
{
  "success": true,
  "chapters_added": 3,
  "words_added": 514,
  "novel_id": "uuid"
}
```

### 公告接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/announcements/` | 否 | 公告列表 |
| GET | `/api/announcements/latest` | 否 | 最新公告 |

### 版本更新接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/update/check?current_version=1.0.0` | 否 | 检查更新 |
| GET | `/api/update/download` | 否 | 下载 APK |
| POST | `/api/update/publish` | 是(管理员) | 发布新版本 |

### 健康检查

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/health` | 否 | 服务健康检查 |

---

## 文件解析说明

系统支持自动解析以下格式的文件并切分章节：

### TXT 文件

- **编码识别**: 自动检测 UTF-8 / GBK / GB2312 / Big5 / UTF-16
- **章节切分**: 按章节标题正则匹配切分
- **容错处理**: 无法识别标题时，整个文件作为一个章节

### Markdown 文件

- **标记转换**: Markdown 标记转换为纯文本
- **标题提取**: `#` / `##` / `###` 作为章节名
- **分章规则**: Heading 自动分章

### DOCX 文件

- **段落解析**: 解析 Word 文档段落
- **样式识别**: Heading 1/2/3 样式作为章节分隔
- **内容保留**: 正文内容完整保留

### 章节切分规则

系统会识别以下章节标题格式：

| 格式 | 示例 |
|------|------|
| 第X章 | 第一章 / 第1章 / 第123章 |
| 第X节 | 第一节 / 第1节 |
| 第X回 | 第一回 / 第1回 |
| 第X卷 | 第一卷 / 第1卷 |
| Chapter X | Chapter 1 / Chapter One |
| 数字序号 | 1. / 1、 / 1) |
| 方括号 | 【标题】 / [标题] |

**无法识别标题时**，整个文件作为一个章节处理，章节名取文件名。

---

## 数据库模型

### PostgreSQL 表结构

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| users | 用户 | id, username, password_hash, is_admin, is_creator, created_at |
| creators | 创作者档案 | id, user_id, pen_name, bio, contact, created_at |
| categories | 分类 | id, name, display_name, sort_order |
| novels | 小说 | id, title, author, description, category_id, tags, status, featured, word_count, chapter_count, view_count, collect_count, created_at |
| chapters | 章节 | id, novel_id, title, content, sort_order, word_count, created_at |
| bookmarks | 收藏 | id, user_id, novel_id, created_at |
| reading_histories | 阅读历史 | id, user_id, novel_id, chapter_index, updated_at |
| announcements | 公告 | id, title, content, type, is_pinned, created_at |
| app_versions | 应用版本 | id, version, version_code, download_url, update_log, is_force, created_at |

### 种子数据

系统首次启动时自动初始化：

- **1 个管理员**: admin / admin123
- **10 个分类**: 玄幻、仙侠、都市、历史、科幻、军事、游戏、灵异、短篇、其他
- **3 条公告**: 欢迎公告、使用指南、创作者招募
- **9 部公版小说**: 聊斋志异、西游记、三国演义、水浒传、呐喊、彷徨、老残游记、儒林外史、星河彼岸

---

## 部署说明

### 腾讯云 SCF 部署 (当前方案)

#### 第一步: 准备 PostgreSQL 数据库

1. 使用腾讯云 PostgreSQL、Neon、Supabase 或其他 PostgreSQL 托管服务
2. 获取连接串，格式:
   ```
   postgresql://用户名:密码@主机:端口/库名
   ```
3. 确保数据库可被 SCF 内网/公网访问

#### 第二步: 打包后端代码

```bash
cd backend-node

# 安装依赖 (生产环境)
npm install --production

# 打包为 ZIP (包含 node_modules)
# 注意: 不要包含 nm/ 或 node_modules_new/ 等冗余目录
# 需要包含: index.js, app.js, db.js, auth.js, schema.sql,
#           package.json, scf_bootstrap, routes/, services/, node_modules/
```

#### 第三步: 创建 SCF 函数

1. 进入 [腾讯云 SCF 控制台](https://console.cloud.tencent.com/scf)
2. 新建函数，按下表配置:

| 配置项 | 值 |
|--------|-----|
| 函数类型 | **Web 函数** |
| 函数名称 | moyue-novel (自定义) |
| 运行环境 | **Nodejs18.15** |
| 内存 | **512 MB** |
| 执行超时 | **60 秒** |
| 部署方式 | **上传 ZIP 包** |
| 环境变量 | `DATABASE_URL` = 数据库连接串 |
| | `SECRET_KEY` = JWT 密钥 (任意长字符串) |

3. 上传 ZIP 包，点击部署
4. 部署完成后，在「函数 URL」中获取访问地址

#### 第四步: 验证部署

```bash
# 健康检查
curl https://<your-scf-url>/api/health
# 期望: {"status":"ok","version":"1.0.0"}

# 公告检查 (验证种子数据)
curl https://<your-scf-url>/api/announcements/
# 期望: 3 条公告

# 管理员登录
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  https://<your-scf-url>/api/auth/login
# 期望: {"access_token":"...","user":{...}}
```

#### 第五步: 更新 Android 客户端

修改 `android/app/src/main/java/com/novel/reader/Constants.kt`:

```kotlin
const val BASE_URL = "https://your-scf-url.tencentscf.com/"
```

### 注意事项

1. **Node.js 版本**: 必须使用 Nodejs18.15 或更高，低版本不支持 `crypto.randomUUID()`
2. **UUID 兼容**: 代码已包含 polyfill，但建议使用 Node 18+ 运行时
3. **Body Parser**: 自定义 JSON 解析器已跳过 multipart/form-data，交由 multer 处理
4. **冷启动**: SCF 函数冷启动约 3-10 秒，首次请求可能较慢
5. **测试 URL**: SCF 函数 URL 仅供测试使用，生产环境请绑定自定义域名

---

## 版本历史与更新日志

### v1.1.0 — 2026-08-07 (当前版本)

**本次更新为重大架构迁移版本，后端从 Python/FastAPI 全面迁移至 Node.js/Express，部署平台从 Render 迁移至腾讯云 SCF (Serverless)。**

#### 一、架构变更

##### 1.1 后端语言迁移: Python → Node.js

- **原架构**: Python 3.13 + FastAPI + SQLAlchemy 2.0 + psycopg2
- **新架构**: Node.js 18 + Express 4 + node-postgres (pg) + 原生 SQL
- **迁移原因**:
  - 腾讯云 SCF 对 Node.js 运行时支持更完善
  - Node.js 冷启动速度优于 Python (3s vs 10s+)
  - Express 生态与 SCF Web 函数适配更自然
  - 减少依赖体积，部署包从 200MB+ 降至 1.8MB
- **影响范围**: 全部后端代码重写，API 路径保持兼容

##### 1.2 部署平台迁移: Render → 腾讯云 SCF

- **原平台**: Render 免费 Web Service (15 分钟无请求后休眠)
- **新平台**: 腾讯云 SCF Serverless 函数计算
- **优势**:
  - 按需调用，零成本 idle (Render 免费版有休眠问题)
  - 自动弹性伸缩，无需配置实例数量
  - 内网访问数据库延迟更低 (同地域)
  - 支持 HTTP 触发器，直接作为 API 网关
- **配置详情**:
  - 运行环境: Nodejs18.15
  - 内存: 512 MB
  - 执行超时: 60 秒
  - 触发方式: 函数 URL (HTTPS)

##### 1.3 数据库保持不变

- 继续使用 PostgreSQL (外部托管)
- 表结构完全兼容，无需数据迁移
- 连接池配置优化为 SCF 环境 (max 1 连接，避免连接数耗尽)

#### 二、后端新增功能

##### 2.1 UUID v4 Polyfill

- **问题**: SCF Node.js 运行时可能不支持 `crypto.randomUUID()`
- **解决方案**: 在 `db.js` 和 `auth.js` 中添加 polyfill
- **影响**: 修复了种子数据初始化失败、用户注册 500 错误、收藏功能异常等多个问题
- **代码位置**: `backend-node/db.js` 第 1-12 行, `backend-node/auth.js` 第 5-14 行

```javascript
// Polyfill 实现
if (!crypto.randomUUID) {
  crypto.randomUUID = function() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  };
}
```

##### 2.2 Body Parser 修复

- **问题**: 自定义 JSON body parser 消费了请求流，导致 multer 文件上传报 `Unexpected end of form`
- **解决方案**: 对 `multipart/form-data` 请求跳过自定义 parser，交由 multer 处理
- **影响**: 修复了所有文件上传功能 (TXT / Markdown / DOCX)
- **代码位置**: `backend-node/app.js` 第 11-27 行

##### 2.3 种子数据幂等化

- **问题**: 原 `seedData()` 使用单一 `COUNT(*)` 判断，部分数据存在时跳过全部初始化
- **解决方案**: 改为每个实体单独查重 (categories / admin / announcements / 示例小说)
- **影响**: 修复了部分数据缺失时无法自动补全的问题
- **代码位置**: `backend-node/db.js` `seedData()` 函数

##### 2.4 精选设置接口

- **新增**: `PATCH /api/novels/:id/featured` (管理员)
- **功能**: 设置或取消小说的精选推荐状态
- **认证**: 需要 admin 权限
- **代码位置**: `backend-node/routes/novels.js` 第 124-132 行

##### 2.5 公版文学批量导入

- **新增**: `seed-novels.js` 批量导入脚本
- **内容**: 8 部公版经典文学作品 (聊斋志异、西游记、三国演义、水浒传、呐喊、彷徨、老残游记、儒林外史)
- **功能**: 通过 API 自动创建小说 + 上传章节，支持幂等 (已存在则跳过)
- **代码位置**: `backend-node/seed-novels.js`

##### 2.6 章节补传与精选设置脚本

- **新增**: `fix-chapters.js` 补传脚本
- **功能**: 补传缺失章节 + 设置精选推荐
- **代码位置**: `backend-node/fix-chapters.js`

#### 三、后端 Bug 修复

##### 3.1 crypto.randomUUID 兼容性

- **现象**: `POST /api/auth/register` 返回 500 `crypto.randomUUID is not a function`
- **根因**: SCF Node.js 运行时版本过低，不支持 `crypto.randomUUID()` API
- **修复**: 添加 polyfill 函数 (见 2.1)
- **影响接口**: 注册、收藏、创作者注册、文件上传、章节创建等所有需要生成 UUID 的接口

##### 3.2 种子数据初始化失败

- **现象**: `admin/admin123` 登录返回 401，公告/分类为空
- **根因**: `seedData()` 在创建 admin 时抛异常 (crypto.randomUUID 不存在)，导致整个初始化失败
- **修复**: polyfill + 幂等化 (见 2.1, 2.3)
- **影响**: 修复后数据库自动初始化 admin 用户、10 个分类、3 条公告、9 部小说

##### 3.3 文件上传失败

- **现象**: `POST /api/upload/single/:novelId` 返回 500 `Unexpected end of form`
- **根因**: `app.js` 的自定义 body parser 消费了请求流，multer 无法读取 multipart 数据
- **修复**: multipart/form-data 请求跳过自定义 parser (见 2.2)
- **影响**: 修复 TXT / Markdown / DOCX 所有格式上传

##### 3.4 小说状态不匹配

- **现象**: 书城列表为空，创建的小说查不到
- **根因**: 种子数据中小说状态为 `ongoing`，但书城查询 `status='published'`
- **修复**: 种子数据小说状态改为 `published`
- **代码位置**: `backend-node/db.js` seedData 函数

#### 四、Android 客户端变更

##### 4.1 BASE_URL 更新

- **旧地址**: `https://moyue-novel-api.koyeb.app/` (已失效)
- **新地址**: `https://1432945062-f2koniz849.ap-guangzhou.tencentscf.com/`
- **代码位置**: `android/app/src/main/java/com/novel/reader/Constants.kt` 第 8 行

##### 4.2 版本号更新

- 版本名: 1.0.0 → 1.1.0
- 版本号: 1 → 2
- **代码位置**: `Constants.kt` 第 11-14 行

#### 五、基础设施变更

##### 5.1 .gitignore 完善

- 新增忽略: `node_modules/`, `*.zip`, `scf-stage/`, `scf-test/`
- 防止误提交几百 MB 的依赖包和临时文件

##### 5.2 SCF 部署包

- 标准化打包流程: 正斜杠路径、ZIP 格式、包含 `scf_bootstrap`
- 部署包体积: 1.79 MB (含 node_modules)

#### 六、数据变更

##### 6.1 新增公版文学内容

- **聊斋志异精选** (蒲松龄) — 7 章, 1752 字 — 精选
  - 考城隍 / 画皮 / 聂小倩 / 种梨
- **西游记** (吴承恩) — 3 章, 514 字 — 精选
  - 灵根育孕源流出 / 悟彻菩提真妙理 / 四海千山皆拱伏
- **三国演义** (罗贯中) — 3 章, 437 字 — 精选
  - 宴桃园豪杰三结义 / 张翼德怒鞭督邮 / 议温明董卓叱丁原
- **水浒传** (施耐庵) — 3 章, 373 字
  - 张天师祈禳瘟疫 / 王教头私走延安府 / 史大郎夜走华阴县
- **呐喊** (鲁迅) — 5 章, 1055 字 — 精选
  - 狂人日记 / 孔乙己 / 药
- **彷徨** (鲁迅) — 5 章, 775 字
  - 祝福 / 在酒楼上 / 伤逝
- **老残游记** (刘鹗) — 3 章, 419 字
  - 土不制水历年成患 / 历山山下古帝遗踪 / 金线东来寻黑虎
- **儒林外史** (吴敬梓) — 3 章, 392 字
  - 说楔子敷陈大义 / 王孝廉村学识同科 / 周学道校士拔真才
- **星河彼岸** (官方编辑部) — 3 章, 269 字 — 精选
  - 序章 星落 / 第一章 启程 / 第二章 迷雾森林

##### 6.2 精选推荐

- 共 5 部设为精选: 聊斋志异精选、西游记、三国演义、呐喊、星河彼岸

#### 七、API 验证结果

| 功能 | 端点 | 状态 |
|------|------|------|
| 健康检查 | `GET /api/health` | 通过 |
| 精选推荐 | `GET /api/novels/featured` | 通过 (5 本) |
| 分类列表 | `GET /api/novels/categories` | 通过 (10 个) |
| 公告列表 | `GET /api/announcements/` | 通过 (3 条) |
| 用户登录 | `POST /api/auth/login` | 通过 |
| 章节阅读 | `GET /api/novels/:id/chapters/index/1` | 通过 |
| 收藏功能 | `POST /api/novels/:id/bookmark` | 通过 |
| 搜索 | `GET /api/novels/search?keyword=西游` | 通过 |
| 创作者仪表盘 | `GET /api/creator/dashboard` | 通过 (10本/38章/6251字) |
| 版本检查 | `GET /api/update/check` | 通过 |
| 文件上传 | `POST /api/upload/single/:id` | 通过 (3章解析) |
| 精选设置 | `PATCH /api/novels/:id/featured` | 通过 |

---

### v1.0.0 — 2026-08-06

**首个正式版本，包含完整的阅读体验、创作者中心、文件上传、自动更新、公告系统。**

#### 一、客户端功能

##### 1.1 书城首页

- 精选推荐横向滑动展示
- 分类筛选 (10 大分类)
- 热门排行 (按阅读量排序)
- 最新上架 (按创建时间排序)
- 下拉刷新
- 列表项依次入场动画

##### 1.2 小说详情

- 基本信息: 标题、作者、分类、标签、简介
- 数据统计: 章节数、总字数、阅读量、收藏量
- 章节目录: 完整章节列表
- 操作入口: 开始阅读 / 加入书架

##### 1.3 沉浸式阅读器

- 手势翻页 (左右滑动)
- 字号调节 (小/中/大/超大)
- 阅读进度自动记录
- 章节导航 (上一章/下一章)
- 沉浸模式 (隐藏状态栏)
- 深色/浅色主题自适应

##### 1.4 书架管理

- 收藏/取消收藏
- 阅读进度显示
- 快速续读
- 按收藏时间排序

##### 1.5 全文搜索

- 按书名/作者搜索
- 搜索历史记录
- 搜索结果列表

##### 1.6 创作者中心

- 创作者注册 (笔名、简介、联系方式)
- 数据仪表盘 (作品数/总字数/阅读量/收藏量)
- 发布作品 (创建小说基本信息)
- 多格式上传 (TXT / Markdown / DOCX)
- 文件夹批量上传
- 实时处理进度展示
- 作品管理

##### 1.7 自动更新系统

- 启动时自动检查新版本
- 应用内下载 APK
- 下载进度实时显示
- 支持强制更新
- 版本更新日志展示

##### 1.8 公告系统

- 实时公告拉取
- 启动时公告弹窗 (仅显示一次)
- 公告中心列表
- 公告详情查看
- 分类: 信息/警告/更新/活动
- 置顶功能

##### 1.9 UI/UX

- 极简高级感设计: 墨色主调，纸张背景
- 丝滑流动特效: 入场动画、弹性缩放、渐变转场
- 列表项依次入场动画
- 深色/浅色主题自适应
- 衬线字体标题
- 全应用零 emoji
- Material 3 设计规范

#### 二、后端功能

##### 2.1 认证系统

- 用户注册 (用户名 + 密码)
- 用户登录 (JWT 令牌)
- 密码哈希 (bcrypt)
- 管理员权限

##### 2.2 小说管理

- CRUD 操作
- 分页/筛选/排序
- 全文搜索
- 精选推荐
- 收藏/书架

##### 2.3 章节管理

- 章节列表
- 按序号读取
- 阅读进度记录

##### 2.4 创作者系统

- 创作者注册
- 创作者档案
- 我的作品
- 数据仪表盘

##### 2.5 文件上传

- TXT/Markdown/DOCX 解析
- 自动编码识别
- 章节自动切分
- 文件夹批量上传
- 上传进度查询

##### 2.6 公告系统

- 公告 CRUD (管理员)
- 分类与置顶
- 最新公告接口

##### 2.7 版本更新

- 版本检查
- APK 下载
- 版本发布 (管理员)
- 支持外部下载链接

#### 三、数据库

- PostgreSQL (生产环境)
- 10 张表: users, creators, categories, novels, chapters, bookmarks, reading_histories, announcements, app_versions, upload_tasks
- 自动建表 (schema.sql)
- 种子数据初始化

#### 四、基础设施

- GitHub 代码仓库
- APK GitHub Releases 托管
- 原部署平台: Render (后迁移至腾讯云 SCF)

---

## 开发指南

### 项目构建

#### Android 客户端

```bash
# Debug 构建
cd android
./gradlew assembleDebug

# Release 构建 (需配置签名)
./gradlew assembleRelease
```

#### 后端本地运行

```bash
cd backend-node
npm install
DATABASE_URL=postgresql://... node -e "const app = require('./app'); app.listen(3000)"
```

### 公版文学导入

```bash
cd backend-node
node seed-novels.js   # 导入 8 部公版经典
node fix-chapters.js  # 补传缺失章节 + 设置精选
```

### 添加新书

#### 方式一: API 创建

```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  https://<base-url>/api/auth/login | jq -r .access_token)

# 2. 创建小说
NOVEL_ID=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"新书","author":"作者","description":"简介","status":"published"}' \
  https://<base-url>/api/novels/ | jq -r .id)

# 3. 上传章节文件
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F "file=@章节.txt" \
  https://<base-url>/api/upload/single/$NOVEL_ID
```

#### 方式二: APP 内操作

1. 登录 admin 账号
2. 进入「我的」→「创作者中心」
3. 注册创作者 → 创建小说 → 上传文件

### 修改后端地址

编辑 `android/app/src/main/java/com/novel/reader/Constants.kt`:

```kotlin
const val BASE_URL = "https://your-server.com/"
```

---

## 常见问题

### Q: SCF 函数冷启动慢怎么办?

A: SCF 函数在长时间无请求后会进入冷启动状态，首次请求约 3-10 秒延迟。可以通过设置预留实例来消除冷启动，但会产生费用。对于测试用途，冷启动延迟可接受。

### Q: 文件上传报 Unexpected end of form?

A: 已在 v1.1.0 修复。原因是自定义 body parser 消费了 multipart 请求流。确保 `app.js` 中对 `multipart/form-data` 请求跳过自定义 parser。

### Q: 登录返回 401?

A: 检查 `seedData()` 是否成功执行。调用 `GET /api/announcements/` 验证种子数据是否初始化。如果为空，说明 `seedData()` 失败，检查 Node.js 运行时版本和 `crypto.randomUUID` polyfill。

### Q: 如何添加更多公版书籍?

A: 修改 `backend-node/seed-novels.js` 中的 `NOVELS` 数组，添加新的书籍数据，然后运行 `node seed-novels.js`。脚本支持幂等，已存在的书籍会自动跳过。

### Q: APK 需要重新构建吗?

A: 如果 `Constants.kt` 中的 `BASE_URL` 发生变化，需要重新构建 APK。使用 `./gradlew assembleDebug` 构建。

### Q: 如何切换数据库?

A: 修改 SCF 函数的环境变量 `DATABASE_URL`，指向新的 PostgreSQL 连接串。重启函数后，`seedData()` 会自动初始化新数据库。

---

## 开源协议

本项目为私有项目，所有版权归创作者所有。

公版文学作品（聊斋志异、西游记、三国演义、水浒传、呐喊、彷徨、老残游记、儒林外史）版权已过期，属于公共领域作品。
