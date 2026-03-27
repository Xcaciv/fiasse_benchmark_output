'use strict';

const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Attachment = sequelize.define('Attachment', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  noteId: {
    type: DataTypes.UUID,
    allowNull: false,
  },
  originalName: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  storedName: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  mimeType: {
    type: DataTypes.STRING(100),
    allowNull: false,
  },
  size: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
}, {
  tableName: 'attachments',
  updatedAt: false,
});

module.exports = Attachment;
