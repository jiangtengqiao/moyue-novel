const express = require('express');
const cors = require('cors');
const path = require('path');
const { initDB } = require('./db');

const app = express();

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// 静态文件 (APK 下载等)
const dataDir = path.join(__dirname, 'data');
try { require('fs').mkdirSync(dataDir, { recursive: true }); } catch(e) {}

// 路由
app.use('/api/auth', require('./routes/auth'));
app.use('/api/novels', require('./routes/novels'));
app.use('/api/creator', require('./routes/creators'));
app.use('/api/upload', require('./routes/uploads'));
app.use('/api/announcements', require('./routes/announcements'));
app.use('/api/update', require('./routes/updates'));

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', version: '1.0.0' });
});

// 错误处理
app.use((err, req, res, next) => {
  console.error(err);
  res.status(err.status || 500).json({ message: err.message || '服务器错误' });
});

module.exports = app;
