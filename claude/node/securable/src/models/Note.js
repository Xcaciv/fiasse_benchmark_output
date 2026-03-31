'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const Note = sequelize.define(
    'Note',
    {
      id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true,
      },
      userId: {
        type: DataTypes.UUID,
        allowNull: false,
        references: { model: 'users', key: 'id' },
      },
      title: {
        type: DataTypes.STRING(255),
        allowNull: false,
        validate: { len: [1, 255], notEmpty: true },
      },
      content: {
        type: DataTypes.TEXT,
        allowNull: false,
        validate: { notEmpty: true },
      },
      isPublic: {
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: false,
      },
    },
    {
      tableName: 'notes',
      indexes: [
        { fields: ['user_id'] },
        { fields: ['is_public'] },
      ],
    }
  );

  Note.associate = (models) => {
    Note.belongsTo(models.User, { foreignKey: 'userId', as: 'owner' });
    Note.hasMany(models.Attachment, { foreignKey: 'noteId', as: 'attachments' });
    Note.hasMany(models.Rating, { foreignKey: 'noteId', as: 'ratings' });
    Note.hasMany(models.ShareLink, { foreignKey: 'noteId', as: 'shareLinks' });
  };

  return Note;
};
