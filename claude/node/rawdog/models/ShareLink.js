const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const ShareLink = sequelize.define('ShareLink', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true
  },
  noteId: {
    type: DataTypes.INTEGER,
    allowNull: false
  },
  token: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true
  },
  isActive: {
    type: DataTypes.BOOLEAN,
    defaultValue: true
  }
}, {
  tableName: 'share_links'
});

module.exports = ShareLink;
