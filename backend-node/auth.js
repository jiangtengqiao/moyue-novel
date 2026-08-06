const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');

const SECRET = process.env.SECRET_KEY || 'mo-yue-novel-secret-key-2024';
const TOKEN_EXPIRE = '7d';

function hashPassword(pwd) {
  return bcrypt.hashSync(pwd, 10);
}

function verifyPassword(pwd, hash) {
  return bcrypt.compareSync(pwd, hash);
}

function createToken(userId) {
  return jwt.sign({ sub: userId }, SECRET, { expiresIn: TOKEN_EXPIRE });
}

function authMiddleware(req, res, next) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ message: '请先登录' });
  }
  try {
    const token = header.split(' ')[1];
    const decoded = jwt.verify(token, SECRET);
    req.userId = decoded.sub;
    next();
  } catch (e) {
    return res.status(401).json({ message: '登录已过期，请重新登录' });
  }
}

async function getUser(req, db) {
  const { rows } = await db.query('SELECT * FROM users WHERE id = $1', [req.userId]);
  return rows[0];
}

module.exports = { hashPassword, verifyPassword, createToken, authMiddleware, getUser, crypto };
