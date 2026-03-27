'use strict';

const { Sequelize } = require('sequelize');
const path = require('path');

// DB_PATH resolved from env; defaults to project root database.sqlite
const dbPath = path.resolve(process.env.DB_PATH || './database.sqlite');

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: dbPath,
  // Only log SQL in development to avoid leaking query data in production logs
  logging: process.env.NODE_ENV === 'development'
    ? (msg) => require('./logger').logger.debug(msg)
    : false,
  define: {
    underscored: false,
    freezeTableName: false,
    charset: 'utf8',
  },
});

module.exports = { sequelize };
