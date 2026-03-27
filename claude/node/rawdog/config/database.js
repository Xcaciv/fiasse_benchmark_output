const { Sequelize } = require('sequelize');
const path = require('path');
const fs = require('fs');

const dbPath = process.env.DB_PATH || './data/loose-notes.db';
const resolvedPath = path.resolve(dbPath);

// Ensure the data directory exists
const dbDir = path.dirname(resolvedPath);
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: resolvedPath,
  logging: process.env.NODE_ENV === 'development' ? (msg) => console.log('[SQL]', msg) : false,
  define: {
    timestamps: true
  }
});

module.exports = sequelize;
