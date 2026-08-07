-- 墨阅小说数据库表结构
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  nickname TEXT,
  avatar TEXT,
  bio TEXT DEFAULT '',
  is_creator BOOLEAN DEFAULT FALSE,
  is_admin BOOLEAN DEFAULT FALSE,
  last_login_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS creators (
  id TEXT PRIMARY KEY,
  user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
  pen_name TEXT NOT NULL,
  real_name TEXT,
  introduction TEXT DEFAULT '',
  contact_email TEXT,
  contact_phone TEXT,
  social_accounts JSONB DEFAULT '{}',
  status TEXT DEFAULT 'active',
  total_words INTEGER DEFAULT 0,
  total_novels INTEGER DEFAULT 0,
  total_readers INTEGER DEFAULT 0,
  verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS categories (
  id TEXT PRIMARY KEY,
  name TEXT UNIQUE NOT NULL,
  display_name TEXT NOT NULL,
  sort_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS novels (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  author TEXT NOT NULL,
  creator_id TEXT REFERENCES creators(id) ON DELETE CASCADE,
  category_id TEXT REFERENCES categories(id),
  cover_url TEXT,
  description TEXT DEFAULT '',
  tags JSONB DEFAULT '[]',
  status TEXT DEFAULT 'ongoing',
  word_count INTEGER DEFAULT 0,
  chapter_count INTEGER DEFAULT 0,
  view_count INTEGER DEFAULT 0,
  like_count INTEGER DEFAULT 0,
  collect_count INTEGER DEFAULT 0,
  rating REAL DEFAULT 0,
  rating_count INTEGER DEFAULT 0,
  is_original BOOLEAN DEFAULT TRUE,
  source TEXT DEFAULT 'upload',
  featured BOOLEAN DEFAULT FALSE,
  sort_order INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chapters (
  id TEXT PRIMARY KEY,
  novel_id TEXT REFERENCES novels(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  content TEXT DEFAULT '',
  word_count INTEGER DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0,
  is_free BOOLEAN DEFAULT TRUE,
  price INTEGER DEFAULT 0,
  status TEXT DEFAULT 'published',
  level INTEGER DEFAULT 2,
  volume TEXT DEFAULT '',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS bookmarks (
  id TEXT PRIMARY KEY,
  user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
  novel_id TEXT REFERENCES novels(id) ON DELETE CASCADE,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reading_histories (
  id TEXT PRIMARY KEY,
  user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
  novel_id TEXT REFERENCES novels(id) ON DELETE CASCADE,
  chapter_id TEXT,
  chapter_index INTEGER DEFAULT 0,
  scroll_position REAL DEFAULT 0,
  read_percent REAL DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS announcements (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  content TEXT DEFAULT '',
  type TEXT DEFAULT 'info',
  is_pinned BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  sort_order INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_versions (
  id TEXT PRIMARY KEY,
  version_name TEXT NOT NULL,
  version_code INTEGER NOT NULL,
  download_url TEXT NOT NULL,
  file_size BIGINT DEFAULT 0,
  md5 TEXT,
  update_title TEXT,
  update_log TEXT DEFAULT '',
  force_update BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  min_supported_version TEXT DEFAULT '1.0.0',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS upload_tasks (
  id TEXT PRIMARY KEY,
  creator_id TEXT REFERENCES creators(id) ON DELETE CASCADE,
  novel_id TEXT REFERENCES novels(id),
  status TEXT DEFAULT 'pending',
  total_files INTEGER DEFAULT 0,
  processed_files INTEGER DEFAULT 0,
  failed_files INTEGER DEFAULT 0,
  current_file TEXT,
  progress REAL DEFAULT 0,
  message TEXT DEFAULT '',
  file_list JSONB DEFAULT '[]',
  error_log TEXT DEFAULT '',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_novels_status ON novels(status);
CREATE INDEX IF NOT EXISTS idx_novels_creator ON novels(creator_id);
CREATE INDEX IF NOT EXISTS idx_chapters_novel ON chapters(novel_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX IF NOT EXISTS idx_reading_user ON reading_histories(user_id);
CREATE INDEX IF NOT EXISTS idx_upload_tasks_creator ON upload_tasks(creator_id);
