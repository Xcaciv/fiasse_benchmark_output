'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const Attachment = sequelize.define('Attachment', {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false,
    },
    // storedFilename: UUID-based name on disk — prevents path traversal and collisions
    storedFilename: {
      type: DataTypes.STRING(100),
      allowNull: false,
    },
    // originalFilename: display only, never used in filesystem operations
    originalFilename: {
      type: DataTypes.STRING(255),
      allowNull: false,
    },
    mimeType: {
      type: DataTypes.STRING(100),
      allowNull: false,
    },
    fileSizeBytes: {
      type: DataTypes.INTEGER,
      allowNull: false,
    },
  }, {
    tableName: 'attachments',
    indexes: [{ fields: ['noteId'] }],
  });

  Attachment.associate = (models) => {
    Attachment.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
  };

  return Attachment;
};
