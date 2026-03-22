'use strict';
const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

function defineRating(sequelize) {
  return sequelize.define('Rating', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false
    },
    userId: {
      type: DataTypes.UUID,
      allowNull: false
    },
    stars: {
      type: DataTypes.INTEGER,
      allowNull: false,
      validate: { min: 1, max: 5 }
    },
    comment: {
      type: DataTypes.TEXT,
      allowNull: true
    }
  }, {
    tableName: 'ratings',
    timestamps: true
  });
}

module.exports = { defineRating };
