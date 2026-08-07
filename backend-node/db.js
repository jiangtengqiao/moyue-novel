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
      [crypto.randomUUID(), '1.0.0', 1, '/api/update/download', '墨阅小说 v1.0.0', '首个正式版本', false, true, '1.0.0']);
  }

  // 示例小说（幂等：按标题查重，状态改为 published 才能在书城显示）
  const novelCheck = await pool.query("SELECT id FROM novels WHERE title = '星河彼岸'");
  let novelId;
  if (novelCheck.rows.length === 0) {
    const catRow = await pool.query("SELECT id FROM categories WHERE name = 'xuanhuan'");
    const catId = catRow.rows[0].id;
    novelId = crypto.randomUUID();
    await pool.query(`INSERT INTO novels (id, title, author, creator_id, category_id, description, tags, status, word_count, chapter_count, featured)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)`,
      [novelId, '星河彼岸', '官方编辑部', creatorId, catId, '在浩瀚星河的尽头，少年踏上一段寻找自我与真相的旅程。', JSON.stringify(['玄幻','冒险','成长']), 'published', 0, 0, true]);
  } else {
    novelId = novelCheck.rows[0].id;
  }

  // 示例章节（幂等：按小说 + 标题查重）
  const chapters = [
    ['序章 星落', '夜空如墨，繁星点点。\n\n少年站在山巅，仰望那片亘古不变的星河。风从远方吹来，带着未知的气息。\n\n"什么时候才能到达那里呢？"他喃喃自语。\n\n星光洒落，仿佛在回应他的呼唤。一颗流星划过天际，拖着长长的尾迹，坠向地平线的尽头。'],
    ['第一章 启程', '清晨的阳光穿过薄雾，洒在宁静的小镇上。\n\n少年收拾好行囊，回头看了一眼生活了十六年的家。\n\n"该走了。"他深吸一口气，迈出了第一步。\n\n路很长，但心很坚定。前方有未知的风暴，也有未曾见过的风景。'],
    ['第二章 迷雾森林', '走进森林的那一刻，光线便暗了下来。\n\n浓雾在树间流动，如同活物一般。古老的树木高耸入云，枝叶交织成穹顶。\n\n少年握紧手中的短剑，每一步都小心翼翼。'],
  ];
  let totalWords = 0;
  let chapterCount = 0;
  for (let i = 0; i < chapters.length; i++) {
    const chCheck = await pool.query('SELECT id FROM chapters WHERE novel_id = $1 AND title = $2', [novelId, chapters[i][0]]);
    if (chCheck.rows.length > 0) continue;
    const wc = chapters[i][1].replace(/\s/g, '').length;
    await pool.query('INSERT INTO chapters (id, novel_id, title, content, word_count, sort_order) VALUES ($1, $2, $3, $4, $5, $6)',
      [crypto.randomUUID(), novelId, chapters[i][0], chapters[i][1], wc, i]);
    totalWords += wc;
    chapterCount++;
  }
  if (chapterCount > 0) {
    await pool.query('UPDATE novels SET word_count = word_count + $1, chapter_count = chapter_count + $2 WHERE id = $3',
      [totalWords, chapterCount, novelId]);
  }

  console.log('[墨阅小说] 种子数据已就绪 (admin / admin123)');
}

module.exports = { pool, query, initDB, seedData };
