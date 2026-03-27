'use strict';

const { DataTypes, Model } = require('sequelize');
const { sequelize } = require('../config/database');

class ShareLink extends Model {}

ShareLink.init({
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  noteId: {
    type: DataTypes.UUID,
    allowNull: false,
    unique: true,
    references: { model: 'notes', key: 'id' },
  },
  // token is a server-generated UUID — never derived from client input
  token: {
    type: DataTypes.UUID,
    allowNull: false,
    unique: true,
  },
}, {
  sequelize,
  modelName: 'ShareLink',
  tableName: 'share_links',
  timestamps: true,
});

module.exports = { ShareLink };
