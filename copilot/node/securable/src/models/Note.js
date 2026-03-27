'use strict';

const { DataTypes, Model } = require('sequelize');
const { sequelize } = require('../config/database');

class Note extends Model {}

Note.init({
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
  },
  // Derived Integrity Principle: server assigns default; only validated enum accepted from client
  visibility: {
    type: DataTypes.ENUM('public', 'private'),
    allowNull: false,
    defaultValue: 'private',
  },
  userId: {
    type: DataTypes.UUID,
    allowNull: false,
    references: { model: 'users', key: 'id' },
  },
}, {
  sequelize,
  modelName: 'Note',
  tableName: 'notes',
  timestamps: true,
  scopes: {
    public: { where: { visibility: 'public' } },
    owned: (userId) => ({ where: { userId } }),
  },
});

module.exports = { Note };
