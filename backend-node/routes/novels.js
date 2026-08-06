const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, crypto } = require('../auth');

// 分类列表
router.get('/categories', async (req, res, next) => {
  try {
    const { rows } = await pool.query('SELECT * FROM categories ORDER BY sort_order');
    res.json(rows);
  } catch (e) { next(e); }
});

// 小说列表
router.get('/', async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const pageSize = Math.min(100, parseInt(req.query.page_size) || 20);
    const category = req.query.category;
    const sort = req.query.sort || 'latest';
    const keyword = req.query.keyword;
    
    let where = ["status = 'published'"];
    let params = [];
    let idx = 1;
    if (category) { where.push(`category_id = (SELECT id FROM categories WHERE name = $${idx})`); params.push(category); idx++; }
    if (keyword) { where.push(`(title ILIKE $${idx} OR author ILIKE $${idx})`); params.push(`%${keyword}%`); idx++; }
    
    let orderBy = 'created_at DESC';
    if (sort === 'popular') orderBy = 'view_count DESC';
    else if (sort === 'rating') orderBy = 'rating DESC';
    
    const offset = (page - 1) * pageSize;
    const sql = `SELECT id, title, author, cover_url, description, status, word_count, chapter_count, view_count, like_count, collect_count, rating, featured, created_at FROM novels WHERE ${where.join(' AND ')} ORDER BY ${orderBy} LIMIT $${idx} OFFSET $${idx + 1}`;
    params.push(pageSize, offset);
    
    const { rows } = await pool.query(sql, params);
    const countResult = await pool.query(`SELECT COUNT(*) as count FROM novels WHERE ${where.join(' AND ')}`, params.slice(0, idx - 1));
    const total = parseInt(countResult.rows[0].count);
    
    res.json({ total, page, page_size: pageSize, pages: Math.ceil(total / pageSize), items: rows });
  } catch (e) { next(e); }
});

// 精选
router.get('/featured', async (req, res, next) => {
  try {
    const limit = Math.min(20, parseInt(req.query.limit) || 6);
    const { rows } = await pool.query('SELECT id, title, author, cover_url, description, status, word_count, chapter_count, view_count, rating FROM novels WHERE featured = TRUE AND status = $1 ORDER BY created_at DESC LIMIT $2', ['published', limit]);
    res.json(rows);
  } catch (e) { next(e); }
});

// 搜索
router.get('/search', async (req, res, next) => {
  try {
    const keyword = req.query.keyword;
    if (!keyword) return res.json({ total: 0, items: [] });
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const pageSize = 20;
    const { rows } = await pool.query(
      'SELECT id, title, author, cover_url, description, status, word_count, chapter_count, view_count, rating FROM novels WHERE (title ILIKE $1 OR author ILIKE $1) AND status = $2 ORDER BY view_count DESC LIMIT $3 OFFSET $4',
      [`%${keyword}%`, 'published', pageSize, (page - 1) * pageSize]
    );
    res.json({ total: rows.length, page, page_size: pageSize, items: rows });
  } catch (e) { next(e); }
});

// 小说详情
router.get('/:id', async (req, res, next) => {
  try {
    const { rows } = await pool.query('SELECT * FROM novels WHERE id = $1', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ message: '小说不存在' });
    await pool.query('UPDATE novels SET view_count = view_count + 1 WHERE id = $1', [req.params.id]);
    res.json(rows[0]);
  } catch (e) { next(e); }
});

// 创建小说
router.post('/', authMiddleware, async (req, res, next) => {
  try {
    const { title, author, description, tags, category_id, status } = req.body;
    if (!title || !author) return res.status(400).json({ message: '请填写书名和作者' });
    
    const creatorRow = await pool.query('SELECT id FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
    if (creatorRow.rows.length === 0) return res.status(403).json({ message: '请先成为创作者' });
    
    const id = crypto.randomUUID();
    await pool.query(
      'INSERT INTO novels (id, title, author, creator_id, category_id, description, tags, status) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
      [id, title, author, creatorRow.rows[0].id, category_id || null, description || '', JSON.stringify(tags || []), status || 'ongoing']
    );
    const novel = (await pool.query('SELECT * FROM novels WHERE id = $1', [id])).rows[0];
    res.status(201).json(novel);
  } catch (e) { next(e); }
});

// 收藏
router.post('/:id/bookmark', authMiddleware, async (req, res, next) => {
  try {
    const exists = await pool.query('SELECT id FROM bookmarks WHERE user_id = $1 AND novel_id = $2', [req.userId, req.params.id]);
    if (exists.rows.length > 0) {
      await pool.query('DELETE FROM bookmarks WHERE user_id = $1 AND novel_id = $2', [req.userId, req.params.id]);
      res.json({ message: '已取消收藏', success: true });
    } else {
      await pool.query('INSERT INTO bookmarks (id, user_id, novel_id) VALUES ($1, $2, $3)', [crypto.randomUUID(), req.userId, req.params.id]);
      await pool.query('UPDATE novels SET collect_count = collect_count + 1 WHERE id = $1', [req.params.id]);
      res.json({ message: '已收藏', success: true });
    }
  } catch (e) { next(e); }
});

// 书架
router.get('/bookmarks/list', authMiddleware, async (req, res, next) => {
  try {
    const { rows } = await pool.query(
      'SELECT n.* FROM novels n INNER JOIN bookmarks b ON n.id = b.novel_id WHERE b.user_id = $1 ORDER BY b.created_at DESC',
      [req.userId]
    );
    res.json(rows);
  } catch (e) { next(e); }
});

// 章节子路由
router.use('/:novelId/chapters', require('./chapters'));

module.exports = router;
