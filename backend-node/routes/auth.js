const express = require('express');
const router = express.Router();
const { pool } = require('../db');
const { hashPassword, verifyPassword, createToken, authMiddleware, getUser, crypto } = require('../auth');

router.post('/register', async (req, res, next) => {
  try {
    const { username, password, nickname } = req.body;
    if (!username || !password || username.length < 3) return res.status(400).json({ message: '用户名至少3个字符' });
    if (password.length < 6) return res.status(400).json({ message: '密码至少6个字符' });
    
    const exists = await pool.query('SELECT id FROM users WHERE username = $1', [username]);
    if (exists.rows.length > 0) return res.status(400).json({ message: '用户名已存在' });
    
    const id = crypto.randomUUID();
    const hash = hashPassword(password);
    await pool.query('INSERT INTO users (id, username, password_hash, nickname) VALUES ($1, $2, $3, $4)',
      [id, username, hash, nickname || username]);
    
    const user = (await pool.query('SELECT id, username, nickname, avatar, bio, is_creator, is_admin FROM users WHERE id = $1', [id])).rows[0];
    res.json({ access_token: createToken(id), token_type: 'bearer', user });
  } catch (e) { next(e); }
});

router.post('/login', async (req, res, next) => {
  try {
    const { username, password } = req.body;
    const { rows } = await pool.query('SELECT * FROM users WHERE username = $1', [username]);
    if (rows.length === 0) return res.status(401).json({ message: '用户名或密码错误' });
    
    const user = rows[0];
    if (!verifyPassword(password, user.password_hash)) return res.status(401).json({ message: '用户名或密码错误' });
    
    await pool.query('UPDATE users SET last_login_at = NOW() WHERE id = $1', [user.id]);
    const safe = { id: user.id, username: user.username, nickname: user.nickname, avatar: user.avatar, bio: user.bio, is_creator: user.is_creator, is_admin: user.is_admin };
    res.json({ access_token: createToken(user.id), token_type: 'bearer', user: safe });
  } catch (e) { next(e); }
});

router.get('/me', authMiddleware, async (req, res, next) => {
  try {
    const user = await getUser(req, pool);
    if (!user) return res.status(404).json({ message: '用户不存在' });
    delete user.password_hash;
    res.json(user);
  } catch (e) { next(e); }
});

module.exports = router;
