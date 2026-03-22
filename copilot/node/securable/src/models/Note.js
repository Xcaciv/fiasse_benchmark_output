'use strict';
const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

function defineNote(sequelize) {
  return sequelize.define('Note', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true
    },
    userId: {
      type: DataTypes.UUID,
      allowNull: false
    },
    title: {
      type: DataTypes.STRING(255),
      allowNull: false
    },
    content: {
      type: DataTypes.TEXT,
      allowNull: false
    },
    isPublic: {
      type: DataTypes.BOOLEAN,
      defaultValue: false
    }
  }, {
    tableName: 'notes',
    timestamps: true
  });
}

module.exports = { defineNote };
