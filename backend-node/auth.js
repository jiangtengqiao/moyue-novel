const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
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
