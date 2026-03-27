'use strict';

require('dotenv').config();

const fs = require('fs');
const path = require('path');
const app = require('./app');
const { syncDatabase } = require('./models/index');
const { logger } = require('./config/logger');

const PORT = parseInt(process.env.PORT || '3000', 10);

/** Ensure required runtime directories exist before accepting requests. */
const ensureRuntimeDirectories = () => {
  const dirs = [
    path.resolve(process.env.UPLOAD_DIR || './uploads'),
    path.resolve('./logs'),
  ];

  for (const dir of dirs) {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
      logger.info('Created runtime directory', { dir });
    }
  }
};

const start = async () => {
  ensureRuntimeDirectories();

  // Sync database schema (non-destructive alter)
  await syncDatabase();

  app.listen(PORT, () => {
    logger.info('Loose Notes started', {
      port: PORT,
      env: process.env.NODE_ENV || 'development',
    });
  });
};

start().catch((err) => {
  logger.error('Server startup failed', { error: err.message, stack: err.stack });
  process.exit(1);
});
