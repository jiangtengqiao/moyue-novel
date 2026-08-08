# 墨阅小说 (MoYue Novel)

一款专注于原创文学的 Android 阅读平台，支持创作者发布作品、读者纯净阅读。

## 功能特性

### 读者功能
- 小说浏览：按分类、排行、精选浏览小说
- 搜索：支持书名、作者搜索
- 阅读器：支持字体调节、背景色切换、夜间模式
- 书架：收藏喜欢的小说
- 阅读历史：记录阅读进度，快速继续阅读
- 章节目录：按卷/章/节折叠展示，快速跳转

### 创作者功能
- 创作者中心：注册成为创作者，管理作品
- 手动码字：在线创建、编辑、删除章节
- 文件上传：支持 TXT / MD / DOCX 格式，一文件一章节
- 批量上传：多文件批量导入
- 作品管理：编辑信息、删除作品

### 其他
- 应用内更新：检查并下载新版本
- 四份协议：用户服务协议、隐私政策、社区规范、版权声明（每份约35000字）
- 公告系统：查看平台公告

## 技术栈

- **前端**：Android (Kotlin, Jetpack Compose, Hilt, Retrofit)
- **后端**：Node.js (Express, PostgreSQL, JWT, Multer)
- **部署**：腾讯云 SCF (Serverless Cloud Function)

## 项目结构

```
novel-reader/
├── android/                    # Android 客户端
│   └── app/src/main/java/com/novel/reader/
│       ├── data/               # 数据层 (API, Repository, Model)
│       ├── ui/                 # UI层 (Screens, Routes, Theme)
│       └── Constants.kt        # 全局常量
├── backend-node/               # Node.js 后端
│   ├── routes/                 # 路由 (auth, novels, chapters, uploads, creators, announcements, updates)
│   ├── services/               # 服务 (fileParser, chapterSplitter)
│   ├── app.js                  # Express 应用
│   ├── index.js                # SCF 入口
│   ├── db.js                   # 数据库初始化与种子数据
│   └── schema.sql              # 数据库表结构
└── README.md
```

## 部署

### 后端部署
1. 安装依赖：`npm install`
2. 打包 SCF：运行打包脚本生成 `moyue-scf-deploy.zip`
3. 上传到腾讯云 SCF 控制台
4. 配置环境变量：`DATABASE_URL`、`JWT_SECRET`

### Android 构建
1. 安装 JDK 17
2. 运行：`gradle assembleDebug`
3. APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 默认管理员

- 用户名：`admin`
- 密码：`admin123`

## 版本

- v1.1.0 (code 2)

## 开源仓库

[github.com/jiangtengqiao/moyue-novel](https://github.com/jiangtengqiao/moyue-novel)
