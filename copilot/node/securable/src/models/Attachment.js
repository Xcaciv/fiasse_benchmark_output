'use strict';

const { DataTypes, Model } = require('sequelize');
const { sequelize } = require('../config/database');

class Attachment extends Model {}

Attachment.init({
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  noteId: {
    type: DataTypes.UUID,
    allowNull: false,
    references: { model: 'notes', key: 'id' },
  },
  originalName: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  // storedName is server-generated UUID-based filename; never client-supplied
  storedName: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  mimeType: {
    type: DataTypes.STRING(100),
    allowNull: false,
  },
  sizeBytes: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
}, {
  sequelize,
  modelName: 'Attachment',
  tableName: 'attachments',
  timestamps: true,
  updatedAt: false,
});

module.exports = { Attachment };
