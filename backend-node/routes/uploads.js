const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const { pool } = require('../db');
const { authMiddleware, crypto } = require('../auth');
const { decodeBuffer } = require('../services/fileParser');

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 50 * 1024 * 1024 }
});

async function getCreator(req) {
  const { rows } = await pool.query('SELECT * FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
  return rows[0];
}

const SUPPORTED = ['.txt', '.md', '.markdown', '.docx'];

function extractContent(buf, filename) {
  const ext = path.extname(filename).toLowerCase();
  if (ext === '.docx') {
    try {
      const AdmZip = require('adm-zip');
      const zip = new AdmZip(buf);
      const docXml = zip.getEntry('word/document.xml');
      if (!docXml) return { title: path.basename(filename, ext), content: '' };
      const xml = docXml.getData().toString('utf8');
      const paragraphs = [];
      const pRegex = /<w:p[^>]*>([\s\S]*?)<\/w:p>/g;
      let match;
      while ((match = pRegex.exec(xml)) !== null) {
        const texts = [];
        const tRegex = /<w:t[^>]*>(.*?)<\/w:t>/g;
        let tm;
        while ((tm = tRegex.exec(match[1])) !== null) texts.push(tm[1]);
        const text = texts.join('');
        if (text.trim()) paragraphs.push(text);
      }
      return { title: path.basename(filename, ext), content: paragraphs.join('\n\n') };
    } catch (e) {
      return { title: path.basename(filename, ext), content: '' };
    }
  }
  // TXT / MD：智能解码后直接保存
  const text = decodeBuffer(buf);
  let title = path.basename(filename, ext);
  // Markdown: 尝试取第一个 # 标题作为章节名
  if (ext === '.md' || ext === '.markdown') {
    const m = text.match(/^#\s+(.+)$/m);
    if (m) title = m[1].trim();
  }
  return { title, content: text };
}

// 单文件上传（一个文件 = 一个章节，不自动切分）
router.post('/single/:novelId', authMiddleware, upload.single('file'), async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '不是创作者' });

    const novel = (await pool.query('SELECT * FROM novels WHERE id = $1 AND creator_id = $2', [req.params.novelId, creator.id])).rows[0];
    if (!novel) return res.status(404).json({ message: '小说不存在' });

    const ext = path.extname(req.file.originalname).toLowerCase();
    if (!SUPPORTED.includes(ext)) return res.status(400).json({ message: '仅支持 TXT / MD / DOCX 格式' });

    const { title, content } = extractContent(req.file.buffer, req.file.originalname);
    const wc = content.replace(/\s/g, '').length;
    const sortOrder = (novel.chapter_count || 0) + 1;

    await pool.query(
      'INSERT INTO chapters (id, novel_id, title, content, word_count, sort_order, level, volume) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
      [crypto.randomUUID(), novel.id, title, content, wc, sortOrder, 2, '']
    );
    await pool.query('UPDATE novels SET chapter_count = $1, word_count = word_count + $2 WHERE id = $3', [sortOrder, wc, novel.id]);

    res.json({ success: true, filename: req.file.originalname, chapters_added: 1, words_added: wc, message: `上传成功：${title}（${wc}字）` });
  } catch (e) { next(e); }
});

// 文件夹批量上传（每个文件 = 一个章节）
router.post('/folder/:novelId', authMiddleware, upload.array('files', 50), async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '不是创作者' });

    const novel = (await pool.query('SELECT * FROM novels WHERE id = $1 AND creator_id = $2', [req.params.novelId, creator.id])).rows[0];
    if (!novel) return res.status(404).json({ message: '小说不存在' });

    const validFiles = req.files.filter(f => SUPPORTED.includes(path.extname(f.originalname).toLowerCase()));
    if (validFiles.length === 0) return res.status(400).json({ message: '没有支持的文件格式（仅 TXT / MD / DOCX）' });

    const taskId = crypto.randomUUID();
    const fileList = validFiles.map(f => ({ filename: f.originalname, status: 'pending', chapters: 0, words: 0 }));
    await pool.query(
      'INSERT INTO upload_tasks (id, creator_id, novel_id, status, total_files, processed_files, failed_files, progress, message, file_list) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)',
      [taskId, creator.id, novel.id, 'processing', validFiles.length, 0, 0, 0, '开始处理...', JSON.stringify(fileList)]
    );

    let chapterSort = novel.chapter_count || 0;
    let processed = 0, failed = 0, totalWords = 0;

    for (let i = 0; i < validFiles.length; i++) {
      const file = validFiles[i];
      try {
        const { title, content } = extractContent(file.buffer, file.originalname);
        const wc = content.replace(/\s/g, '').length;
        chapterSort++;
        await pool.query(
          'INSERT INTO chapters (id, novel_id, title, content, word_count, sort_order, level, volume) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
          [crypto.randomUUID(), novel.id, title, content, wc, chapterSort, 2, '']
        );
        totalWords += wc;
        processed++;
        fileList[i].status = 'completed';
        fileList[i].chapters = 1;
        fileList[i].words = wc;
      } catch (err) {
        failed++;
        fileList[i].status = 'failed';
        fileList[i].error = err.message;
      }
      const progress = Math.round(((processed + failed) / validFiles.length) * 100);
      await pool.query('UPDATE upload_tasks SET processed_files = $1, failed_files = $2, progress = $3, current_file = $4, file_list = $5, message = $6 WHERE id = $7',
        [processed, failed, progress, file.originalname, JSON.stringify(fileList), `处理中: ${file.originalname} (${i + 1}/${validFiles.length})`, taskId]);
    }

    await pool.query('UPDATE novels SET chapter_count = $1, word_count = word_count + $2 WHERE id = $3', [chapterSort, totalWords, novel.id]);
    const status = failed === 0 ? 'completed' : (processed > 0 ? 'completed' : 'failed');
    const message = failed === 0 ? `完成: ${processed}个文件` : `部分完成: 成功${processed}, 失败${failed}`;
    await pool.query('UPDATE upload_tasks SET status = $1, progress = $2, message = $3, current_file = NULL WHERE id = $4', [status, 100, message, taskId]);

    const task = (await pool.query('SELECT * FROM upload_tasks WHERE id = $1', [taskId])).rows[0];
    res.status(201).json(task);
  } catch (e) { next(e); }
});

// 查询任务进度
router.get('/task/:taskId', authMiddleware, async (req, res, next) => {
  try {
    const { rows } = await pool.query('SELECT * FROM upload_tasks WHERE id = $1 AND creator_id = (SELECT id FROM creators WHERE user_id = $2)', [req.params.taskId, req.userId]);
    if (rows.length === 0) return res.status(404).json({ message: '任务不存在' });
    res.json(rows[0]);
  } catch (e) { next(e); }
});

module.exports = router;
