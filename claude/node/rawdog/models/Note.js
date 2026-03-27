const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Note = sequelize.define('Note', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true
  },
  title: {
    type: DataTypes.STRING,
    allowNull: false,
    validate: {
      len: [1, 255],
      notEmpty: true
    }
  },
  content: {
    type: DataTypes.TEXT,
    allowNull: false
  },
  isPublic: {
    type: DataTypes.BOOLEAN,
    defaultValue: false
  },
  userId: {
    type: DataTypes.INTEGER,
    allowNull: false
  }
}, {
  tableName: 'notes'
});

module.exports = Note;
