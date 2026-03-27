'use strict';

require('dotenv').config();

const path = require('path');
const fs = require('fs');
const http = require('http');
const { createApp } = require('./app');
const { initializeDatabase } = require('./models');
const { connectDatabase } = require('./config/database');
const { logger } = require('./config/logger');

const PORT = parseInt(process.env.PORT || '3000', 10);

/**
 * Ensure required directories exist before startup.
 */
function ensureDirectories() {
  const dirs = [
    process.env.UPLOAD_PATH || path.join(__dirname, 'uploads'),
    path.dirname(path.resolve(process.env.DATABASE_PATH || path.join(__dirname, 'data', 'loose-notes.db')))
  ];

  for (const dir of dirs) {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
      logger.info('Created directory', { event: 'app.dir_created', dir });
    }
  }
}

/**
 * Start the HTTP server.
 * Resilience: startup errors are logged and cause a non-zero exit.
 */
async function start() {
  try {
    ensureDirectories();

    await connectDatabase();
    logger.info('Database connection established', { event: 'app.db_connected' });

    await initializeDatabase();
    logger.info('Database schema initialized', { event: 'app.db_initialized' });

    const app = createApp();
    const server = http.createServer(app);

    // Resilience: set server timeout to prevent connection starvation
    server.timeout = 30000;
    server.keepAliveTimeout = 65000;

    server.listen(PORT, () => {
      logger.info('Server started', {
        event: 'app.started',
        port: PORT,
        env: process.env.NODE_ENV || 'development'
      });
      console.log(`Loose Notes running on http://localhost:${PORT}`);
    });

    // Graceful shutdown on SIGTERM/SIGINT
    const shutdown = (signal) => {
      logger.info('Shutdown signal received', { event: 'app.shutdown', signal });
      server.close(() => {
        logger.info('Server closed', { event: 'app.stopped' });
        process.exit(0);
      });
      // Force exit after 10s if connections don't close
      setTimeout(() => process.exit(1), 10000);
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));

    // Resilience: log unhandled promise rejections without crashing silently
    process.on('unhandledRejection', (reason) => {
      logger.error('Unhandled promise rejection', {
        event: 'app.unhandled_rejection',
        error: String(reason)
      });
    });

  } catch (err) {
    logger.error('Startup failed', {
      event: 'app.startup_failed',
      error: err.message,
      stack: err.stack
    });
    process.exit(1);
  }
}

start();
