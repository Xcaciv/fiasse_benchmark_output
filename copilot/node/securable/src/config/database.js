'use strict';
const { Sequelize } = require('sequelize');
const path = require('path');

// Trust boundary: DB connection config from env only
function createSequelizeInstance(dbPath) {
  const resolvedPath = path.resolve(dbPath || process.env.DB_PATH || './data/loose-notes.db');
  return new Sequelize({
    dialect: 'sqlite',
    storage: resolvedPath,
    logging: false,
    define: {
      underscored: false,
      freezeTableName: false
    }
  });
}

const sequelize = createSequelizeInstance();

async function syncDatabase() {
  await sequelize.sync({ alter: true });
}

module.exports = { sequelize, syncDatabase, createSequelizeInstance };
