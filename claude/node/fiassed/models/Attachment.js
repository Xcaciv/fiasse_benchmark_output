'use strict';

const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

module.exports = (sequelize) => {
  const Attachment = sequelize.define('Attachment', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true,
      allowNull: false
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false,
      references: {
        model: 'notes',
        key: 'id'
      }
    },
    // Original name for display only - never used in file system paths
    originalFilename: {
      type: DataTypes.STRING(255),
      allowNull: false
    },
    // UUID-based name used for actual storage - prevents path traversal
    storedFilename: {
      type: DataTypes.STRING(255),
      allowNull: false,
      unique: true
    },
    mimeType: {
      type: DataTypes.STRING(100),
      allowNull: false
    },
    size: {
      type: DataTypes.INTEGER,
      allowNull: false,
      validate: {
        min: 1
      }
    }
  }, {
    tableName: 'attachments',
    timestamps: true,
    updatedAt: false
  });

  Attachment.associate = (models) => {
    Attachment.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
  };

  return Attachment;
};
