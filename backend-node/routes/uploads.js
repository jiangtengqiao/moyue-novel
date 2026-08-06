const express = require('express');
const router = express.Router();
const multer = require('multer');
const { pool } = require('../db');
const { authMiddleware, crypto } = require('../auth');
const { parseFile, scanFolderFiles } = require('../services/fileParser');

const upload = multer({ 
  storage: multer.memoryStorage(),
  limits: { fileSize: 50 * 1024 * 1024 } // 50MB
});

async function getCreator(req) {
  const { rows } = await pool.query('SELECT * FROM creators WHERE user_id = $1 AND status = $2', [req.userId, 'active']);
  return rows[0];
}

// 单文件上传
router.post('/single/:novelId', authMiddleware, upload.single('file'), async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '不是创作者' });
    
    const novel = (await pool.query('SELECT * FROM novels WHERE id = $1 AND creator_id = $2', [req.params.novelId, creator.id])).rows[0];
    if (!novel) return res.status(404).json({ message: '小说不存在' });
    
    const result = parseFile(req.file.buffer, req.file.originalname);
    if (!result.success) return res.status(400).json({ message: result.error });
    
    let chapterSort = novel.chapter_count || 0;
    let totalWords = 0;
    for (const ch of result.chapters) {
      chapterSort++;
      await pool.query('INSERT INTO chapters (id, novel_id, title, content, word_count, sort_order) VALUES ($1, $2, $3, $4, $5, $6)',
        [crypto.randomUUID(), novel.id, ch.title, ch.content, ch.word_count, chapterSort]);
      totalWords += ch.word_count;
    }
    await pool.query('UPDATE novels SET chapter_count = $1, word_count = word_count + $2 WHERE id = $3', [chapterSort, totalWords, novel.id]);
    
    res.json({ success: true, filename: req.file.originalname, chapters_added: result.chapters.length, words_added: totalWords, message: `成功解析 ${result.chapters.length} 章, 新增 ${totalWords} 字` });
  } catch (e) { next(e); }
});

// 文件夹批量上传
router.post('/folder/:novelId', authMiddleware, upload.array('files', 50), async (req, res, next) => {
  try {
    const creator = await getCreator(req);
    if (!creator) return res.status(403).json({ message: '不是创作者' });
    
    const novel = (await pool.query('SELECT * FROM novels WHERE id = $1 AND creator_id = $2', [req.params.novelId, creator.id])).rows[0];
    if (!novel) return res.status(404).json({ message: '小说不存在' });
    
    const filenames = req.files.map(f => f.originalname);
    const supported = scanFolderFiles(filenames);
    if (supported.length === 0) return res.status(400).json({ message: '没有支持的文件格式' });
    
    // 创建任务
    const taskId = crypto.randomUUID();
    const fileList = supported.map(f => ({ filename: f, status: 'pending', chapters: 0, words: 0 }));
    await pool.query(
      'INSERT INTO upload_tasks (id, creator_id, novel_id, status, total_files, processed_files, failed_files, progress, message, file_list) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)',
      [taskId, creator.id, novel.id, 'processing', supported.length, 0, 0, 0, '开始处理...', JSON.stringify(fileList)]
    );
    
    // 同步处理所有文件
    let chapterSort = novel.chapter_count || 0;
    let processed = 0, failed = 0, totalWords = 0;
    
    for (let i = 0; i < supported.length; i++) {
      const filename = supported[i];
      const file = req.files.find(f => f.originalname === filename);
      if (!file) continue;
      
      const result = parseFile(file.buffer, filename);
      
      if (!result.success) {
        failed++;
        fileList[i].status = 'failed';
        fileList[i].error = result.error;
      } else {
        for (const ch of result.chapters) {
          chapterSort++;
          await pool.query('INSERT INTO chapters (id, novel_id, title, content, word_count, sort_order) VALUES ($1, $2, $3, $4, $5, $6)',
            [crypto.randomUUID(), novel.id, ch.title, ch.content, ch.word_count, chapterSort]);
          totalWords += ch.word_count;
        }
        processed++;
        fileList[i].status = 'completed';
        fileList[i].chapters = result.chapters.length;
        fileList[i].words = result.total_words;
      }
      
      const progress = Math.round(((processed + failed) / supported.length) * 100);
      await pool.query('UPDATE upload_tasks SET processed_files = $1, failed_files = $2, progress = $3, current_file = $4, file_list = $5, message = $6 WHERE id = $7',
        [processed, failed, progress, filename, JSON.stringify(fileList), `处理中: ${filename} (${i + 1}/${supported.length})`, taskId]);
    }
    
    await pool.query('UPDATE novels SET chapter_count = $1, word_count = word_count + $2 WHERE id = $3', [chapterSort, totalWords, novel.id]);
    
    const status = failed === 0 ? 'completed' : (processed > 0 ? 'completed' : 'failed');
    const message = failed === 0 ? `完成: ${processed}个文件` : `部分完成: 成功${processed}, 失败${failed}`;
    await pool.query('UPDATE upload_tasks SET status = $1, progress = $2, message = $3, current_file = NULL WHERE id = $4',
      [status, 100, message, taskId]);
    
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
