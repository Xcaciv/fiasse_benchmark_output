'use strict';

const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Note = sequelize.define('Note', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  title: {
    type: DataTypes.STRING(255),
    allowNull: false,
    validate: { len: [1, 255] },
  },
  content: {
    type: DataTypes.TEXT,
    allowNull: false,
    validate: { len: [1, 50000] },
  },
  visibility: {
    type: DataTypes.ENUM('public', 'private'),
    defaultValue: 'private',
    allowNull: false,
  },
  userId: {
    type: DataTypes.UUID,
    allowNull: false,
  },
}, {
  tableName: 'notes',
});

module.exports = Note;
