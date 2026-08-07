const path = require('path');
const { splitChapters } = require('./chapterSplitter');

const SUPPORTED = ['.txt', '.md', '.markdown', '.docx'];

function scanFolderFiles(filenames) {
  return filenames
    .filter(f => SUPPORTED.includes(path.extname(f).toLowerCase()))
    .sort((a, b) => {
      const na = a.match(/\d+/g) || [];
      const nb = b.match(/\d+/g) || [];
      for (let i = 0; i < Math.max(na.length, nb.length); i++) {
        const x = parseInt(na[i] || -1), y = parseInt(nb[i] || -1);
        if (x !== y) return x - y;
      }
      return a.localeCompare(b);
    });
}

/**
 * 智能编码检测：先检测BOM，再用统计学方法判断UTF-8/GBK/Big5
 */
function detectEncoding(buf) {
  // 1. BOM 检测
  if (buf.length >= 3 && buf[0] === 0xEF && buf[1] === 0xBB && buf[2] === 0xBF) return 'utf8';
  if (buf.length >= 2 && buf[0] === 0xFF && buf[1] === 0xFE) return 'utf16le';
  if (buf.length >= 2 && buf[0] === 0xFE && buf[1] === 0xFF) return 'utf16be';

  // 2. 尝试 UTF-8 严格解码
  let isUtf8 = true;
  let i = 0;
  while (i < buf.length) {
    const b = buf[i];
    if (b < 0x80) { i++; continue; }
    if (b < 0xC2) { isUtf8 = false; break; }
    if (b < 0xE0) {
      if (i + 1 >= buf.length || (buf[i + 1] & 0xC0) !== 0x80) { isUtf8 = false; break; }
      i += 2; continue;
    }
    if (b < 0xF0) {
      if (i + 2 >= buf.length || (buf[i + 1] & 0xC0) !== 0x80 || (buf[i + 2] & 0xC0) !== 0x80) { isUtf8 = false; break; }
      i += 3; continue;
    }
    isUtf8 = false; break;
  }
  if (isUtf8) return 'utf8';

  // 3. GBK 检测：双字节字符高位在 0x81-0xFE，低位在 0x40-0xFE
  let gbkScore = 0, totalBytes = 0;
  for (let j = 0; j < Math.min(buf.length, 8192); j++) {
    const b = buf[j];
    if (b >= 0x81 && b <= 0xFE && j + 1 < buf.length) {
      const b2 = buf[j + 1];
      if (b2 >= 0x40 && b2 <= 0xFE && b2 !== 0x7F) { gbkScore++; j++; totalBytes += 2; continue; }
    }
    totalBytes++;
  }
  if (gbkScore > 0 && gbkScore / (totalBytes / 2) > 0.3) return 'gbk';

  // 4. 默认 UTF-8（兼容）
  return 'utf8';
}

function decodeBuffer(buf) {
  const enc = detectEncoding(buf);
  if (enc === 'utf8') {
    // 去 BOM
    if (buf.length >= 3 && buf[0] === 0xEF && buf[1] === 0xBB && buf[2] === 0xBF) {
      return buf.slice(3).toString('utf8');
    }
    return buf.toString('utf8');
  }
  if (enc === 'utf16le') return buf.slice(2).toString('utf16le');
  if (enc === 'utf16be') {
    const swapped = Buffer.allocUnsafe(buf.length - 2);
    for (let i = 2; i < buf.length; i += 2) {
      swapped[i - 2] = buf[i + 1]; swapped[i - 1] = buf[i];
    }
    return swapped.toString('utf16le');
  }
  if (enc === 'gbk') {
    try {
      const iconv = require('iconv-lite');
      return iconv.decode(buf, 'gbk');
    } catch (e) {
      return buf.toString('utf8');
    }
  }
  return buf.toString('utf8');
}

function parseFile(content, filename) {
  const ext = path.extname(filename).toLowerCase();
  const title = path.basename(filename, ext);
  
  if (ext === '.txt') return parseTxt(content, filename, title);
  if (ext === '.md' || ext === '.markdown') return parseMd(content, filename, title);
  if (ext === '.docx') return parseDocx(content, filename, title);
  
  return { filename, title: '', chapters: [], total_words: 0, success: false, error: `不支持的格式: ${ext}` };
}

function parseTxt(content, filename, title) {
  const buf = Buffer.isBuffer(content) ? content : Buffer.from(content);
  const text = decodeBuffer(buf);
  const chapters = splitChapters(text, title);
  const total = chapters.reduce((s, c) => s + c.word_count, 0);
  return { filename, title, chapters, total_words: total, success: true };
}

function parseMd(content, filename, title) {
  const buf = Buffer.isBuffer(content) ? content : Buffer.from(content);
  let text = decodeBuffer(buf);
  
  for (const line of text.split('\n')) {
    if (line.startsWith('# ')) { title = line.replace(/^#+\s*/, '').trim(); break; }
  }
  
  // Markdown 转纯文本
  text = text
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`(.+?)`/g, '$1')
    .replace(/\[(.+?)\]\(.+?\)/g, '$1')
    .replace(/^>\s+/gm, '')
    .replace(/^[-*+]\s+/gm, '');
  
  const chapters = splitChapters(text, title);
  const total = chapters.reduce((s, c) => s + c.word_count, 0);
  return { filename, title, chapters, total_words: total, success: true };
}

function parseDocx(content, filename, title) {
  try {
    const AdmZip = require('adm-zip');
    const buf = Buffer.isBuffer(content) ? content : Buffer.from(content);
    const zip = new AdmZip(buf);
    const docXml = zip.getEntry('word/document.xml');
    if (!docXml) return { filename, title, chapters: [], total_words: 0, success: false, error: '无法读取DOCX' };
    
    const xml = docXml.getData().toString('utf8');
    const paragraphs = [];
    const pRegex = /<w:p[^>]*>([\s\S]*?)<\/w:p>/g;
    let match;
    while ((match = pRegex.exec(xml)) !== null) {
      const texts = [];
      const tRegex = /<w:t[^>]*>(.*?)<\/w:t>/g;
      let tm;
      while ((tm = tRegex.exec(match[1])) !== null) {
        texts.push(tm[1]);
      }
      const text = texts.join('');
      if (text.trim()) {
        // 检测标题样式
        const isHeading = /<w:pStyle[^>]*w:val="[^"]*[Hh]eading[^"]*"/.test(match[1]) || /<w:pStyle[^>]*w:val="[^"]*[Tt]itle[^"]*"/.test(match[1]);
        paragraphs.push(isHeading ? `\n${text}\n` : text);
      }
    }
    
    const fullText = paragraphs.join('\n');
    const chapters = splitChapters(fullText, title);
    const total = chapters.reduce((s, c) => s + c.word_count, 0);
    return { filename, title, chapters, total_words: total, success: true };
  } catch (e) {
    return { filename, title, chapters: [], total_words: 0, success: false, error: 'DOCX解析失败: ' + e.message };
  }
}

module.exports = { parseFile, scanFolderFiles, decodeBuffer, detectEncoding };
