'use strict';

const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

module.exports = (sequelize) => {
  const Note = sequelize.define('Note', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true,
      allowNull: false
    },
    title: {
      type: DataTypes.STRING(200),
      allowNull: false,
      validate: {
        len: [1, 200],
        notEmpty: true
      }
    },
    content: {
      type: DataTypes.TEXT,
      allowNull: false,
      validate: {
        len: [1, 50000]
      }
    },
    // Visibility is server-controlled; clients cannot escalate
    visibility: {
      type: DataTypes.ENUM('private', 'public'),
      allowNull: false,
      defaultValue: 'private'
    },
    ownerId: {
      type: DataTypes.UUID,
      allowNull: false,
      references: {
        model: 'users',
        key: 'id'
      }
    }
  }, {
    tableName: 'notes',
    timestamps: true
  });

  Note.associate = (models) => {
    Note.belongsTo(models.User, { foreignKey: 'ownerId', as: 'owner' });
    Note.hasMany(models.Attachment, { foreignKey: 'noteId', as: 'attachments' });
    Note.hasMany(models.Rating, { foreignKey: 'noteId', as: 'ratings' });
    Note.hasMany(models.ShareLink, { foreignKey: 'noteId', as: 'shareLinks' });
  };

  return Note;
};
