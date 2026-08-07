const { parseFile, scanFolderFiles } = require('./fileParser');

/**
 * 高级章节切分器 - 支持卷/章/节三级结构
 * 识别规则优先级：
 *   1. 卷：第X卷 / 卷X / [卷名] / 【卷名】
 *   2. 章：第X章 / Chapter X / X、标题 / 数字.标题
 *   3. 节：第X节 / Section X / §X
 */

// 卷级别匹配（最高层级）
const VOLUME_PATTERNS = [
  /^第[一二三四五六七八九十百千零\d]+[卷部篇]/m,
  /^卷[一二三四五六七八九十百千零\d]+/m,
  /^[【【].+?[】】]$/m,
  /^★.+★$/m,
];

// 章级别匹配（中间层级）
const CHAPTER_PATTERNS = [
  /^第[一二三四五六七八九十百千零\d]+[章回话]/m,
  /^Chapter\s+\d+/im,
  /^第\d+章/m,
  /^\d+[、.．]\s*\S+/m,
  /^[【\[][^】\]]+[】\]]$/m,
];

// 节级别匹配（最低层级）
const SECTION_PATTERNS = [
  /^第[一二三四五六七八九十百千零\d]+[节]/m,
  /^Section\s+\d+/im,
  /^§\s*\d+/m,
  /^小节\d+/m,
];

function buildTitleRegex(patterns) {
  return new RegExp('(' + patterns.map(p => p.source.replace(/^\^/, '').replace(/\$$/, '')).join(')|(') + ')', 'gm');
}

function findHeadings(text, patterns) {
  const regex = buildTitleRegex(patterns);
  const result = [];
  let m;
  while ((m = regex.exec(text)) !== null) {
    const lineStart = text.lastIndexOf('\n', m.index) + 1;
    const lineEnd = text.indexOf('\n', m.index);
    const fullLine = text.substring(lineStart, lineEnd === -1 ? text.length : lineEnd).trim();
    // 过滤过短或过长的非标题行
    if (fullLine.length === 0 || fullLine.length > 100) continue;
    result.push({
      start: lineStart,
      end: lineEnd === -1 ? text.length : lineEnd,
      title: fullLine,
      level: patterns === VOLUME_PATTERNS ? 1 : (patterns === CHAPTER_PATTERNS ? 2 : 3),
    });
  }
  return result;
}

function cleanTitle(title) {
  return title
    .replace(/^[【【\[★]+/, '')
    .replace(/[】】\]★]+$/, '')
    .replace(/^第[一二三四五六七八九十百千零\d]+[卷部篇章节回]/, '')
    .replace(/^卷[一二三四五六七八九十百千零\d]+/, '')
    .replace(/^Chapter\s+\d+/i, '')
    .replace(/^Section\s+\d+/i, '')
    .replace(/^§\s*\d+/, '')
    .replace(/^\d+[、.．]\s*/, '')
    .trim() || title;
}

function splitChapters(text, defaultTitle) {
  // 1. 规范化换行符
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  // 2. 去除多余空行（保留单个空行作为段落分隔）
  text = text.replace(/\n{3,}/g, '\n\n').trim();

  // 3. 收集所有标题位置
  const volumes = findHeadings(text, VOLUME_PATTERNS);
  const chapters = findHeadings(text, CHAPTER_PATTERNS);
  const sections = findHeadings(text, SECTION_PATTERNS);

  const hasVolume = volumes.length > 0;
  const hasChapter = chapters.length > 0;
  const hasSection = sections.length > 0;

  // 4. 如果没有任何标题，整篇作为一章
  if (!hasVolume && !hasChapter && !hasSection) {
    const wc = text.replace(/\s/g, '').length;
    return [{ title: defaultTitle, content: text, word_count: wc, level: 0, volume: '' }];
  }

  // 5. 构建章节列表
  const allHeadings = [...volumes, ...chapters, ...sections]
    .sort((a, b) => a.start - b.start);

  // 去重（同位置只保留最高级别）
  const deduped = [];
  let lastStart = -1;
  for (const h of allHeadings) {
    if (h.start > lastStart) {
      deduped.push(h);
      lastStart = h.start;
    }
  }

  // 6. 提取章节内容
  const result = [];
  let currentVolume = '';

  // 引言
  if (deduped[0].start > 0) {
    const preface = text.substring(0, deduped[0].start).trim();
    if (preface.length > 50) {
      result.push({
        title: '引言',
        content: preface,
        word_count: preface.replace(/\s/g, '').length,
        level: 2,
        volume: '',
      });
    }
  }

  // 各章节
  for (let i = 0; i < deduped.length; i++) {
    const h = deduped[i];
    if (h.level === 1) {
      currentVolume = cleanTitle(h.title);
      // 卷本身可能没有独立内容，跳过空卷
      const contentStart = h.end;
      const contentEnd = i + 1 < deduped.length ? deduped[i + 1].start : text.length;
      const content = text.substring(contentStart, contentEnd).trim();
      const wc = content.replace(/\s/g, '').length;
      if (wc > 100) {
        result.push({
          title: cleanTitle(h.title),
          content,
          word_count: wc,
          level: 1,
          volume: currentVolume,
        });
      }
    } else {
      const contentStart = h.end;
      const contentEnd = i + 1 < deduped.length ? deduped[i + 1].start : text.length;
      const content = text.substring(contentStart, contentEnd).trim();
      const wc = content.replace(/\s/g, '').length;
      if (wc > 0 || h.title) {
        result.push({
          title: cleanTitle(h.title).substring(0, 200),
          content,
          word_count: wc,
          level: h.level,
          volume: currentVolume,
        });
      }
    }
  }

  return result.length > 0 ? result : [{ title: defaultTitle, content: text, word_count: text.replace(/\s/g, '').length, level: 0, volume: '' }];
}

module.exports = { splitChapters, parseFile, scanFolderFiles };
