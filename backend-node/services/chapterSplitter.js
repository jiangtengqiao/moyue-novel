const { parseFile, scanFolderFiles } = require('./fileParser');

function splitChapters(text, defaultTitle) {
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
  const patterns = [
    /^第[一二三四五六七八九十百千零\d]+[章节回卷集部篇].*$/gm,
    /^Chapter\s+\d+/gim,
    /^第\d+章/gm,
    /^\d+[、.．].+$/gm,
    /^【.+】$/gm,
    /^\[.+\]$/gm,
  ];
  
  let positions = [];
  for (const p of patterns) {
    let m;
    const re = new RegExp(p.source, p.flags);
    while ((m = re.exec(text)) !== null) {
      const lineStart = text.lastIndexOf('\n', m.index) + 1;
      const lineEnd = text.indexOf('\n', m.index);
      positions.push({ start: lineStart, end: lineEnd === -1 ? text.length : lineEnd, title: text.substring(lineStart, lineEnd === -1 ? text.length : lineEnd).trim() });
    }
  }
  
  if (positions.length === 0) {
    const wc = text.replace(/\s/g, '').length;
    return [{ title: defaultTitle, content: text, word_count: wc }];
  }
  
  positions.sort((a, b) => a.start - b.start);
  const deduped = [];
  let lastStart = -1;
  for (const p of positions) {
    if (p.start > lastStart) { deduped.push(p); lastStart = p.start; }
  }
  
  const chapters = [];
  if (deduped[0].start > 0) {
    const preface = text.substring(0, deduped[0].start).trim();
    if (preface.length > 50) chapters.push({ title: '引言', content: preface, word_count: preface.replace(/\s/g, '').length });
  }
  
  for (let i = 0; i < deduped.length; i++) {
    const contentStart = deduped[i].end;
    const contentEnd = i + 1 < deduped.length ? deduped[i + 1].start : text.length;
    const content = text.substring(contentStart, contentEnd).trim();
    const wc = content.replace(/\s/g, '').length;
    if (wc > 0 || deduped[i].title) {
      chapters.push({ title: deduped[i].title.substring(0, 200), content, word_count: wc });
    }
  }
  
  return chapters.length > 0 ? chapters : [{ title: defaultTitle, content: text, word_count: text.replace(/\s/g, '').length }];
}

module.exports = { splitChapters, parseFile, scanFolderFiles };
