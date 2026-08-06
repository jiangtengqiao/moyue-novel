const app = require('./app');
const { initDB, seedData } = require('./db');

const PORT = process.env.PORT || 9000;

async function start() {
  try {
    await initDB();
    await seedData();
    console.log('[墨阅小说] 数据库初始化完成');
  } catch (e) {
    console.error('[墨阅小说] 数据库初始化失败:', e.message);
  }
  
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[墨阅小说] v1.0.0 启动成功，端口 ${PORT}`);
  });
}

start();
