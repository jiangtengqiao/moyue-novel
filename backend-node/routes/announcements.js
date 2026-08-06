const express = require('express');
const router = express.Router();
const { pool } = require('../db');

router.get('/', async (req, res, next) => {
  try {
    const limit = Math.min(100, parseInt(req.query.limit) || 20);
    const { rows } = await pool.query(
      'SELECT * FROM announcements WHERE is_active = TRUE ORDER BY is_pinned DESC, sort_order DESC, created_at DESC LIMIT $1',
      [limit]
    );
    res.json(rows);
  } catch (e) { next(e); }
});

router.get('/latest', async (req, res, next) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM announcements WHERE is_active = TRUE ORDER BY is_pinned DESC, created_at DESC LIMIT 3'
    );
    res.json(rows);
  } catch (e) { next(e); }
});

module.exports = router;
