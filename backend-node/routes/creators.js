const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { authMiddleware, crypto } = require('../auth');

async function getCreator(req) {
  const { rows } = await pool.query('SELECT * FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
  return rows[0];
}

router.post('/register', authMiddleware, async (req, res, next) => {
  try {
    const { pen_name, real_name, introduction, contact_email, contact_phone } = req.body;
    if (!pen_name) return res.status(400).json({ message: '请输入笔名' });
    
    const existing = await getCreator(req);
    if (existing) return res.status(400).json({ message: '您已是创作者' });
    
    const id = crypto.randomUUID();
    const social = {};
    if (contact_phone) social.phone = contact_phone;
    await pool.query(
      'INSERT INTO creators (id, user_id, pen_name, real_name, introduction, contact_email, contact_phone, social_accounts) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
      [id, req.userId, pen_name, real_name || null, introduction || '', contact_email || null, contact_phone || null, JSON.stringify(social)]
    );
    await pool.query('UPDATE users SET is_creator = TRUE WHERE id = $1', [req.userId]);
    const creator = (await pool.query('SELECT * FROM creators WHERE id = $1', [id])).rows[0];
    res.status(201).json(creator);
  } catch (e) { next(e); }
});

router.get('/profile', authMiddleware, async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '还不是创作者' });
    res.json(creator);
  } catch (e) { next(e); }
});

router.get('/novels', authMiddleware, async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '还不是创作者' });
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const pageSize = 20;
    const { rows } = await pool.query(
      'SELECT id, title, author, cover_url, description, status, word_count, chapter_count, view_count, rating FROM novels WHERE creator_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3',
      [creator.id, pageSize, (page - 1) * pageSize]
    );
    const countRow = await pool.query('SELECT COUNT(*) as count FROM novels WHERE creator_id = $1', [creator.id]);
    res.json({ total: parseInt(countRow.rows[0].count), page, page_size: pageSize, items: rows });
  } catch (e) { next(e); }
});

router.get('/dashboard', authMiddleware, async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '还不是创作者' });
    const stats = await pool.query(
      'SELECT COUNT(*) as total_novels, COALESCE(SUM(word_count), 0) as total_words, COALESCE(SUM(view_count), 0) as total_views, COALESCE(SUM(like_count), 0) as total_likes, COALESCE(SUM(collect_count), 0) as total_collects, COALESCE(SUM(chapter_count), 0) as total_chapters FROM novels WHERE creator_id = $1',
      [creator.id]
    );
    const recent = await pool.query('SELECT id, title, author, word_count, chapter_count, view_count FROM novels WHERE creator_id = $1 ORDER BY created_at DESC LIMIT 5', [creator.id]);
    await pool.query('UPDATE creators SET total_words = $1, total_novels = $2, total_readers = $3 WHERE id = $4',
      [stats.rows[0].total_words, stats.rows[0].total_novels, stats.rows[0].total_views, creator.id]);
    res.json({ ...stats.rows[0], recent_novels: recent.rows });
  } catch (e) { next(e); }
});

module.exports = router;
