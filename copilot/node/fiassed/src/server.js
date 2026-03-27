'use strict';
const http = require('http');
const app = require('./app');
const config = require('./config');
const logger = require('./utils/logger');

const server = http.createServer(app);

server.setTimeout(30000);

server.listen(config.port, () => {
  logger.info('Server started', { port: config.port, env: config.nodeEnv });
  console.log(`Loose Notes listening on port ${config.port}`);
});

server.on('error', (err) => {
  logger.error('Server error', { error: err.message });
  process.exit(1);
});

process.on('SIGTERM', () => {
  logger.info('SIGTERM received, shutting down gracefully');
  server.close(() => {
    logger.info('Server closed');
    process.exit(0);
  });
});

module.exports = server;
