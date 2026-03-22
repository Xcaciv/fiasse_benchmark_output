'use strict';
const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

function defineAttachment(sequelize) {
  return sequelize.define('Attachment', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false
    },
    originalFilename: {
      type: DataTypes.STRING(255),
      allowNull: false
    },
    storedFilename: {
      type: DataTypes.STRING(255),
      allowNull: false
    },
    mimeType: {
      type: DataTypes.STRING(100),
      allowNull: false
    },
    fileSizeBytes: {
      type: DataTypes.INTEGER,
      allowNull: false
    }
  }, {
    tableName: 'attachments',
    timestamps: true,
    updatedAt: false
  });
}

module.exports = { defineAttachment };
