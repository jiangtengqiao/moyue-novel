const express = require('express');
const router = express.Router();
const { pool } = require('../db');

function compareVer(v1, v2) {
  const a = v1.split('.').map(Number);
  const b = v2.split('.').map(Number);
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const x = a[i] || 0, y = b[i] || 0;
    if (x > y) return 1;
    if (x < y) return -1;
  }
  return 0;
}

router.get('/check', async (req, res, next) => {
  try {
    const current = req.query.current_version || '1.0.0';
    const { rows } = await pool.query('SELECT * FROM app_versions WHERE is_active = TRUE ORDER BY version_code DESC LIMIT 1');
    if (rows.length === 0) return res.json({ has_update: false, message: '当前已是最新版本' });
    
    const latest = rows[0];
    const hasUpdate = compareVer(latest.version_name, current) > 0;
    const forceUpdate = compareVer(current, latest.min_supported_version) < 0;
    
    if (hasUpdate) {
      res.json({ has_update: true, latest_version: { ...latest, force_update: forceUpdate }, message: `发现新版本 v${latest.version_name}` });
    } else {
      res.json({ has_update: false, message: '当前已是最新版本' });
    }
  } catch (e) { next(e); }
});

router.get('/download', async (req, res, next) => {
  try {
    const { rows } = await pool.query('SELECT * FROM app_versions WHERE is_active = TRUE ORDER BY version_code DESC LIMIT 1');
    if (rows.length === 0) return res.status(404).json({ message: '暂无安装包' });
    
    const latest = rows[0];
    if (latest.download_url && latest.download_url.startsWith('http')) {
      return res.redirect(latest.download_url);
    }
    
    const path = require('path');
    const fs = require('fs');
    const apkPath = path.join(__dirname, '..', 'data', 'moyue-latest.apk');
    if (!fs.existsSync(apkPath)) return res.status(404).json({ message: '安装包文件不存在' });
    res.download(apkPath, `moyue-v${latest.version_name}.apk`);
  } catch (e) { next(e); }
});

module.exports = router;
