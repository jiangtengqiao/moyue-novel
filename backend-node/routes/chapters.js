const express = require('express');
const router = express.Router({ mergeParams: true });
const { pool } = require('../db');
const { authMiddleware, crypto } = require('../auth');

// 章节列表
router.get('/', async (req, res, next) => {
  try {
    const { novelId } = req.params;
    const { rows } = await pool.query(
      'SELECT id, novel_id, title, word_count, sort_order, is_free, status, level, volume, created_at FROM chapters WHERE novel_id = $1 AND status = $2 ORDER BY sort_order',
      [novelId, 'published']
    );
    res.json(rows);
  } catch (e) { next(e); }
});

// 创建章节（手动码字）
router.post('/', authMiddleware, async (req, res, next) => {
  try {
    const { novelId } = req.params;
    const { title, content, level, volume } = req.body;
    if (!title) return res.status(400).json({ message: '请填写章节标题' });
    
    const creator = await pool.query('SELECT id FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
    if (creator.rows.length === 0) return res.status(403).json({ message: '不是创作者' });
    
    const novel = await pool.query('SELECT * FROM novels WHERE id = $1 AND creator_id = $2', [novelId, creator.rows[0].id]);
    if (novel.rows.length === 0) return res.status(404).json({ message: '小说不存在' });
    
    const wc = (content || '').replace(/\s/g, '').length;
    const sortOrder = novel.rows[0].chapter_count || 0;
    const id = crypto.randomUUID();
    await pool.query(
      'INSERT INTO chapters (id, novel_id, title, content, word_count, sort_order, level, volume) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
      [id, novelId, title, content || '', wc, sortOrder + 1, level || 2, volume || '']
    );
    await pool.query('UPDATE novels SET chapter_count = chapter_count + 1, word_count = word_count + $1 WHERE id = $2', [wc, novelId]);
    
    const chapter = (await pool.query('SELECT * FROM chapters WHERE id = $1', [id])).rows[0];
    res.status(201).json(chapter);
  } catch (e) { next(e); }
});

// 更新章节（手动码字编辑）
router.put('/:chapterId', authMiddleware, async (req, res, next) => {
  try {
    const { novelId, chapterId } = req.params;
    const { title, content, level, volume } = req.body;
    
    const creator = await pool.query('SELECT id FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
    if (creator.rows.length === 0) return res.status(403).json({ message: '不是创作者' });
    
    const novel = await pool.query('SELECT id FROM novels WHERE id = $1 AND creator_id = $2', [novelId, creator.rows[0].id]);
    if (novel.rows.length === 0) return res.status(404).json({ message: '小说不存在' });
    
    const wc = (content || '').replace(/\s/g, '').length;
    await pool.query(
      'UPDATE chapters SET title = $1, content = $2, word_count = $3, level = $4, volume = $5, updated_at = NOW() WHERE id = $6',
      [title, content || '', wc, level || 2, volume || '', chapterId]
    );
    
    const chapter = (await pool.query('SELECT * FROM chapters WHERE id = $1', [chapterId])).rows[0];
    res.json(chapter);
  } catch (e) { next(e); }
});

// 删除章节
router.delete('/:chapterId', authMiddleware, async (req, res, next) => {
  try {
    const { novelId, chapterId } = req.params;
    
    const creator = await pool.query('SELECT id FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
    if (creator.rows.length === 0) return res.status(403).json({ message: '不是创作者' });
    
    const novel = await pool.query('SELECT id FROM novels WHERE id = $1 AND creator_id = $2', [novelId, creator.rows[0].id]);
    if (novel.rows.length === 0) return res.status(404).json({ message: '小说不存在' });
    
    const chapter = await pool.query('SELECT word_count FROM chapters WHERE id = $1', [chapterId]);
    if (chapter.rows.length === 0) return res.status(404).json({ message: '章节不存在' });
    
    await pool.query('DELETE FROM chapters WHERE id = $1', [chapterId]);
    await pool.query('UPDATE novels SET chapter_count = GREATEST(chapter_count - 1, 0), word_count = GREATEST(word_count - $1, 0) WHERE id = $2', [chapter.rows[0].word_count, novelId]);
    
    res.json({ success: true, message: '已删除' });
  } catch (e) { next(e); }
});

// 按序号读章节
router.get('/index/:index', async (req, res, next) => {
  try {
    const { novelId, index } = req.params;
    const { rows } = await pool.query(
      'SELECT * FROM chapters WHERE novel_id = $1 AND sort_order = $2 AND status = $3',
      [novelId, parseInt(index), 'published']
    );
    if (rows.length === 0) return res.status(404).json({ message: '章节不存在' });
    res.json(rows[0]);
  } catch (e) { next(e); }
});

module.exports = router;
