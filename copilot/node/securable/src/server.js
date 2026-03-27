'use strict';

require('dotenv').config();

const app = require('./app');
const { syncDatabase } = require('./models');
const logger = require('./utils/logger');

const PORT = parseInt(process.env.PORT, 10) || 3000;

async function start() {
  try {
    await syncDatabase();
    logger.info('Database synchronized');
    app.listen(PORT, () => {
      logger.info(`Loose Notes server running on port ${PORT}`, {
        env: process.env.NODE_ENV || 'development',
        port: PORT,
      });
    });
  } catch (err) {
    logger.error('Failed to start server', { error: err.message, stack: err.stack });
    process.exit(1);
  }
}

start();
