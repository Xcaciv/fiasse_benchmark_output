'use strict';

const { Sequelize } = require('sequelize');
const path = require('path');
const fs = require('fs');
const logger = require('./logger');

// Ensure data directory exists before initializing SQLite
const dbPath = process.env.DB_PATH || './data/loosenotes.sqlite';
const dbDir = path.dirname(path.resolve(dbPath));
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

// [TRUST BOUNDARY] Database connection — parameterized queries enforced via Sequelize ORM
const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: dbPath,
  logging: (msg) => logger.debug(msg),
  define: {
    timestamps: true,
    underscored: false,
  },
  pool: {
    max: 10,
    min: 0,
    acquire: 30000, // 30s timeout on connection acquire — Availability
    idle: 10000,
  },
});

async function connectDatabase() {
  await sequelize.authenticate();
  logger.info('Database connection established');
}

module.exports = { sequelize, connectDatabase };
