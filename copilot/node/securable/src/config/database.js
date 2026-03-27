'use strict';

const { Sequelize } = require('sequelize');
const logger = require('../utils/logger');

const DATABASE_URL = process.env.DATABASE_URL;

let sequelize;

if (DATABASE_URL && DATABASE_URL.startsWith('postgres')) {
  sequelize = new Sequelize(DATABASE_URL, {
    dialect: 'postgres',
    logging: (msg) => logger.debug(msg),
    pool: { max: 10, min: 0, acquire: 30000, idle: 10000 },
  });
} else {
  const dbPath = DATABASE_URL || './data/database.sqlite';
  sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: dbPath,
    logging: (msg) => logger.debug(msg),
  });
}

module.exports = sequelize;
