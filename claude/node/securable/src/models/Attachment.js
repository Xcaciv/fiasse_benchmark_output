'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const Attachment = sequelize.define(
    'Attachment',
    {
      id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true,
      },
      noteId: {
        type: DataTypes.UUID,
        allowNull: false,
        references: { model: 'notes', key: 'id' },
      },
      storedFilename: {
        type: DataTypes.STRING(255),
        allowNull: false,
      },
      originalFilename: {
        type: DataTypes.STRING(255),
        allowNull: false,
      },
      mimeType: {
        type: DataTypes.STRING(100),
        allowNull: false,
      },
      sizeBytes: {
        type: DataTypes.INTEGER,
        allowNull: false,
      },
    },
    {
      tableName: 'attachments',
      indexes: [{ fields: ['note_id'] }],
    }
  );

  Attachment.associate = (models) => {
    Attachment.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
  };

  return Attachment;
};
