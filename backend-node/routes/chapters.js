const express = require('express');
const router = express.Router({ mergeParams: true });
const { pool } = require('../db');
const { crypto } = require('../auth');

// 章节列表
router.get('/', async (req, res, next) => {
  try {
    const { novelId } = req.params;
    const { rows } = await pool.query(
      'SELECT id, novel_id, title, word_count, sort_order, is_free, status, created_at FROM chapters WHERE novel_id = $1 AND status = $2 ORDER BY sort_order',
      [novelId, 'published']
    );
    res.json(rows);
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
