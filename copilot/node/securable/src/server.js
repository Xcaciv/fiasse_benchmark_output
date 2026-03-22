'use strict';

// Resilience: Graceful startup with error handling
try {
  require('dotenv').config();
} catch (e) {
  // dotenv not available; env vars must be set externally
}

const fs = require('fs');
const path = require('path');
const { createApp } = require('./app');
const { syncDatabase } = require('./config/database');
const logger = require('./utils/logger');

// Ensure required directories exist at startup
['uploads', 'data', 'logs'].forEach(dir => {
  const dirPath = path.resolve(dir);
  if (!fs.existsSync(dirPath)) fs.mkdirSync(dirPath, { recursive: true });
});

const PORT = process.env.PORT || 3000;

async function startServer() {
  await syncDatabase();
  const app = createApp();
  const server = app.listen(PORT, () => {
    logger.info('SERVER_START', { port: PORT, env: process.env.NODE_ENV || 'development' });
  });
  return server;
}

// Resilience: Handle uncaught errors without crashing silently
process.on('uncaughtException', (err) => {
  logger.error('UNCAUGHT_EXCEPTION', { message: err.message, stack: err.stack });
  process.exit(1);
});

process.on('unhandledRejection', (reason) => {
  logger.error('UNHANDLED_REJECTION', { reason: String(reason) });
  process.exit(1);
});

startServer().catch((err) => {
  logger.error('STARTUP_FAILURE', { message: err.message });
  process.exit(1);
});
