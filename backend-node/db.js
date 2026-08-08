const { Pool } = require('pg');
const crypto = require('crypto');

if (typeof crypto.randomUUID !== 'function') {
  crypto.randomUUID = function () {
    const b = crypto.randomBytes(16);
    b[6] = (b[6] & 0x0f) | 0x40;
    b[8] = (b[8] & 0x3f) | 0x80;
    const h = b.toString('hex');
    return h.slice(0, 8) + '-' + h.slice(8, 12) + '-' + h.slice(12, 16) + '-' + h.slice(16, 20) + '-' + h.slice(20);
  };
}

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgres://localhost/moyue',
  ssl: process.env.DATABASE_URL && process.env.DATABASE_URL.includes('neon') ? { rejectUnauthorized: false } : false,
  max: 10,
  idleTimeoutMillis: 30000,
});

pool.on('error', (err) => {
  console.error('数据库连接错误:', err);
});

async function query(text, params) {
  return pool.query(text, params);
}

async function initDB() {
  const fs = require('fs');
  const sql = fs.readFileSync(require('path').join(__dirname, 'schema.sql'), 'utf8');
  await pool.query(sql);
  // 补充 reading_histories 缺失的 chapter_title 列
  try { await pool.query('ALTER TABLE reading_histories ADD COLUMN IF NOT EXISTS chapter_title TEXT'); } catch(e) {}
  // 补充 chapters 缺失的 level 和 volume 列（用于卷/章/节三级结构）
  try {
    await pool.query('ALTER TABLE chapters ADD COLUMN IF NOT EXISTS level INTEGER DEFAULT 2');
    await pool.query('ALTER TABLE chapters ADD COLUMN IF NOT EXISTS volume TEXT DEFAULT \'\'');
  } catch(e) {}
}

async function seedData() {
  // 分类（幂等：已存在则跳过）
  const catCheck = await pool.query('SELECT COUNT(*) as count FROM categories');
  if (parseInt(catCheck.rows[0].count) === 0) {
    const cats = [
      ['xuanhuan', '玄幻', 1], ['xianxia', '仙侠', 2], ['dushi', '都市', 3],
      ['lishi', '历史', 4], ['kehuan', '科幻', 5], ['junshi', '军事', 6],
      ['youxi', '游戏', 7], ['lingyi', '灵异', 8], ['duanpian', '短篇', 9], ['qita', '其他', 10],
    ];
    for (const [name, display, order] of cats) {
      await pool.query('INSERT INTO categories (id, name, display_name, sort_order) VALUES ($1, $2, $3, $4)',
        [crypto.randomUUID(), name, display, order]);
    }
  }

  // 管理员（幂等：已存在则跳过）
  const adminCheck = await pool.query("SELECT id FROM users WHERE username = 'admin'");
  let adminId;
  if (adminCheck.rows.length === 0) {
    const bcrypt = require('bcryptjs');
    adminId = crypto.randomUUID();
    const hash = bcrypt.hashSync('admin123', 10);
    await pool.query('INSERT INTO users (id, username, password_hash, nickname, is_admin, is_creator) VALUES ($1, $2, $3, $4, $5, $6)',
      [adminId, 'admin', hash, '管理员', true, true]);
  } else {
    adminId = adminCheck.rows[0].id;
  }

  // 创作者（幂等）
  let creatorId;
  const creatorCheck = await pool.query('SELECT id FROM creators WHERE user_id = $1', [adminId]);
  if (creatorCheck.rows.length === 0) {
    creatorId = crypto.randomUUID();
    await pool.query('INSERT INTO creators (id, user_id, pen_name, introduction, verified) VALUES ($1, $2, $3, $4, $5)',
      [creatorId, adminId, '官方编辑部', '墨阅小说官方账号', true]);
  } else {
    creatorId = creatorCheck.rows[0].id;
  }

  // 公告（幂等：按标题查重）
  const anns = [
    ['欢迎使用墨阅小说', '墨阅小说是一款专注于原创文学的阅读平台。我们致力于为创作者提供便捷的作品发布工具，为读者提供纯净的阅读体验。', 'info', true],
    ['创作者招募计划', '墨阅小说现已开放创作者注册。注册成为创作者后，您将获得专属创作者中心、文件夹批量上传、实时数据统计等功能。', 'activity', false],
    ['版本更新说明 v1.0.0', 'v1.0.0 正式版发布：完整阅读体验、创作者中心、文件夹批量上传、实时公告系统、应用内自动更新。', 'update', false],
  ];
  for (const [title, content, type, pinned] of anns) {
    const annCheck = await pool.query('SELECT id FROM announcements WHERE title = $1', [title]);
    if (annCheck.rows.length === 0) {
      await pool.query('INSERT INTO announcements (id, title, content, type, is_pinned, is_active) VALUES ($1, $2, $3, $4, $5, $6)',
        [crypto.randomUUID(), title, content, type, pinned, true]);
    }
  }

  // 初始版本（幂等）
  const verCheck = await pool.query("SELECT id FROM app_versions WHERE version_name = '1.0.0'");
  if (verCheck.rows.length === 0) {
    await pool.query(`INSERT INTO app_versions (id, version_name, version_code, download_url, update_title, update_log, force_update, is_active, min_supported_version)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [crypto.randomUUID(), '1.0.0', 1, '/api/update/download', '墨阅小说 v1.0.0 首发版',
        '墨阅小说首个正式版本发布\n小说阅读与创作一体化平台\n支持小说分类浏览与搜索\n章节阅读器基础功能\n用户注册登录与创作者入驻\n小说上传与章节管理\n公告与系统通知', false, true, '1.0.0']);
  }

  // v1.1.0 版本记录（幂等）
  const ver110Check = await pool.query("SELECT id FROM app_versions WHERE version_name = '1.1.0'");
  if (ver110Check.rows.length === 0) {
    await pool.query(`INSERT INTO app_versions (id, version_name, version_code, download_url, update_title, update_log, force_update, is_active, min_supported_version)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [crypto.randomUUID(), '1.1.0', 2, '/api/update/download', '墨阅小说 v1.1.0 功能完善版',
        '新增章节目录页：支持搜索、序号、字数显示与快速跳转\n阅读器夜间模式：提供4种背景色切换\n字体大小持久化：通过 DataStore 保存阅读偏好\n阅读器设置面板：字号、行距、背景一站式调节\n章节列表入口：阅读时可直接查看目录\n阅读边界检查：首末章节智能禁用翻页\n设置界面完善：GitHub 仓库、问题反馈、评分、检查更新、退出登录\n阅读历史功能：后端 API 与前端展示打通\n修复上传文件错误：Map<String, Any?> 替换为 UploadSingleResponse 数据类\n读者与创作者权益体系优化', false, true, '1.0.0']);
  }

  // 清除所有旧示例数据（用户主动要求删除预设书籍）
  try {
    await pool.query("DELETE FROM chapters WHERE novel_id IN (SELECT id FROM novels WHERE title IN ('星河彼岸','儒林外史','西游记','红楼梦','水浒传','三国演义','呐喊','彷徨','朝花夕拾','野草'))");
    await pool.query("DELETE FROM novels WHERE title IN ('星河彼岸','儒林外史','西游记','红楼梦','水浒传','三国演义','呐喊','彷徨','朝花夕拾','野草')");
  } catch(e) { console.log('清理旧示例数据:', e.message); }

  console.log('[墨阅小说] 种子数据已就绪 (admin / admin123)');
}

module.exports = { pool, query, initDB, seedData };
