'use strict';

const path = require('path');
const { Sequelize } = require('sequelize');

// Trust boundary: database path is validated from config, not user input
const dbPath = process.env.DATABASE_PATH
  ? path.resolve(process.env.DATABASE_PATH)
  : path.join(__dirname, '..', 'data', 'loose-notes.db');

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: dbPath,
  logging: false,
  define: {
    freezeTableName: true,
    underscored: false,
    timestamps: true
  },
  pool: {
    max: 5,
    min: 0,
    acquire: 30000,
    idle: 10000
  }
});

/**
 * Test database connection - called at startup only
 */
async function connectDatabase() {
  await sequelize.authenticate();
}

module.exports = { sequelize, connectDatabase };
