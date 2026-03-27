const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');
const sequelize = require('../config/database');

const Note = sequelize.define('Note', {
  id: {
    type: DataTypes.UUID,
    defaultValue: () => uuidv4(),
    primaryKey: true,
  },
  title: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  content: {
    type: DataTypes.TEXT,
    allowNull: false,
  },
  visibility: {
    type: DataTypes.ENUM('public', 'private'),
    defaultValue: 'private',
  },
  userId: {
    type: DataTypes.UUID,
    allowNull: false,
  },
});

module.exports = Note;
