const express = require('express');
const cors = require('cors');
const path = require('path');
const { initDB } = require('./db');

const app = express();

app.use(cors());

// 内联 JSON body 解析（兼容旧版 Express，不依赖 body-parser）
app.use((req, res, next) => {
  if (req.method === 'GET' || req.method === 'HEAD' || req.method === 'DELETE') return next();
  const ct = req.headers['content-type'] || '';
  let data = '';
  req.on('data', chunk => { data += chunk; });
  req.on('end', () => {
    if (data) {
      if (ct.indexOf('application/json') >= 0) {
        try { req.body = JSON.parse(data); } catch(e) { req.body = {}; }
      } else {
        req.rawBody = data;
      }
    }
    next();
  });
  req.on('error', () => next());
});

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
