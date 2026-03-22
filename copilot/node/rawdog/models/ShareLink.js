const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const ShareLink = sequelize.define('ShareLink', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  noteId: {
    type: DataTypes.UUID,
    allowNull: false,
  },
  token: {
    type: DataTypes.UUID,
    allowNull: false,
    unique: true,
  },
}, {
  tableName: 'share_links',
  updatedAt: false,
});

module.exports = ShareLink;
