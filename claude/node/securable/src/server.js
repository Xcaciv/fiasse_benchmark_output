'use strict';

/**
 * Server entry point.
 * Loads environment, synchronises database, then starts the HTTP server.
 * Graceful shutdown closes the DB connection pool (Availability / Resilience).
 */

// Load .env only in non-production environments
if (process.env.NODE_ENV !== 'production') {
  try {
    require('fs').accessSync('.env');
    // Only load if file exists — avoids crashing when running with real env vars
    const envContent = require('fs').readFileSync('.env', 'utf8');
    envContent.split('\n').forEach((line) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) return;
      const eqIdx = trimmed.indexOf('=');
      if (eqIdx < 0) return;
      const key = trimmed.slice(0, eqIdx).trim();
      const value = trimmed.slice(eqIdx + 1).trim();
      if (!process.env[key]) process.env[key] = value;
    });
  } catch (_) {
    // .env not present — rely on real environment variables
  }
}

const logger = require('./config/logger');
const { sequelize } = require('./models');
const createApp = require('./app');

const PORT = parseInt(process.env.PORT || '3000', 10);

async function start() {
  try {
    await sequelize.authenticate();
    logger.info({ event: 'DB_CONNECTED' });

    await sequelize.sync({ alter: false });
    logger.info({ event: 'DB_SYNCED' });

    const app = createApp();

    const server = app.listen(PORT, () => {
      logger.info({ event: 'SERVER_STARTED', port: PORT, env: process.env.NODE_ENV || 'development' });
    });

    // Graceful shutdown
    const shutdown = async (signal) => {
      logger.info({ event: 'SHUTDOWN_INITIATED', signal });
      server.close(async () => {
        await sequelize.close();
        logger.info({ event: 'SHUTDOWN_COMPLETE' });
        process.exit(0);
      });
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
  } catch (err) {
    logger.error({ event: 'STARTUP_FAILURE', message: err.message, stack: err.stack });
    process.exit(1);
  }
}

start();
