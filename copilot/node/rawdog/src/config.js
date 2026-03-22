const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

dotenv.config();

const rootDir = process.cwd();

function resolvePath(value, fallback) {
  return path.resolve(rootDir, value || fallback);
}

const config = {
  rootDir,
  port: Number(process.env.PORT || 3000),
  baseUrl: process.env.BASE_URL || `http://localhost:${process.env.PORT || 3000}`,
  sessionSecret: process.env.SESSION_SECRET || 'change-me-in-production',
  dbPath: resolvePath(process.env.DB_PATH, '.\\data\\loose-notes.sqlite'),
  uploadDir: resolvePath(process.env.UPLOAD_DIR, '.\\uploads'),
  outboxDir: resolvePath(process.env.OUTBOX_DIR, '.\\data\\outbox'),
  maxFileSizeMb: Number(process.env.MAX_FILE_SIZE_MB || 10),
  allowedExtensions: ['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg'],
  defaultAdmin: {
    username: process.env.DEFAULT_ADMIN_USERNAME || 'admin',
    email: process.env.DEFAULT_ADMIN_EMAIL || 'admin@example.com',
    password: process.env.DEFAULT_ADMIN_PASSWORD || 'Admin123!'
  }
};

function ensureDirectories() {
  fs.mkdirSync(path.dirname(config.dbPath), { recursive: true });
  fs.mkdirSync(config.uploadDir, { recursive: true });
  fs.mkdirSync(config.outboxDir, { recursive: true });
}

module.exports = {
  config,
  ensureDirectories
};
