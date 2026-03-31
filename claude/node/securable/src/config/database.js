'use strict';

/**
 * Sequelize database connection factory.
 * SQLite for development/testing; swap dialect + options for production RDBMS.
 * Parameterised queries are enforced by Sequelize — direct interpolation is prohibited.
 */

const path = require('path');
const { Sequelize } = require('sequelize');
const logger = require('./logger');

const dbPath = path.resolve(process.env.DB_PATH || './data/loosenotes.sqlite');

// Ensure data directory exists
const fs = require('fs');
const dataDir = path.dirname(dbPath);
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: dbPath,
  logging: (sql) => logger.debug({ event: 'DB_QUERY', sql }),
  define: {
    underscored: true,
    timestamps: true,
    paranoid: false,
  },
  pool: {
    max: 5,
    min: 0,
    acquire: 30000,
    idle: 10000,
  },
});

module.exports = sequelize;
