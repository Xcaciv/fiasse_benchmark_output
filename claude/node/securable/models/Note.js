'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const Note = sequelize.define('Note', {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    title: {
      type: DataTypes.STRING(200),
      allowNull: false,
    },
    content: {
      type: DataTypes.TEXT,
      allowNull: false,
    },
    // Default private — Confidentiality: opt-in to public exposure
    isPublic: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
      allowNull: false,
    },
    userId: {
      type: DataTypes.UUID,
      allowNull: false,
    },
  }, {
    tableName: 'notes',
    indexes: [
      { fields: ['userId'] },
      { fields: ['isPublic'] },
    ],
  });

  Note.associate = (models) => {
    Note.belongsTo(models.User, { foreignKey: 'userId', as: 'owner' });
    Note.hasMany(models.Attachment, { foreignKey: 'noteId', as: 'attachments', onDelete: 'CASCADE' });
    Note.hasMany(models.Rating, { foreignKey: 'noteId', as: 'ratings', onDelete: 'CASCADE' });
    Note.hasMany(models.ShareLink, { foreignKey: 'noteId', as: 'shareLinks', onDelete: 'CASCADE' });
  };

  return Note;
};
