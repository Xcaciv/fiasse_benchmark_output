'use strict';
const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

function defineShareLink(sequelize) {
  return sequelize.define('ShareLink', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false
    },
    token: {
      type: DataTypes.STRING(64),
      allowNull: false,
      unique: true
    }
  }, {
    tableName: 'share_links',
    timestamps: true,
    updatedAt: false
  });
}

module.exports = { defineShareLink };
