'use strict';

const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

module.exports = (sequelize) => {
  const ShareLink = sequelize.define('ShareLink', {
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
    // Authenticity: cryptographically random UUID v4 token
    token: {
      type: DataTypes.UUID,
      allowNull: false,
      unique: true,
      defaultValue: () => uuidv4()
    },
    expiresAt: {
      type: DataTypes.DATE,
      allowNull: true,
      defaultValue: null
    },
    revokedAt: {
      type: DataTypes.DATE,
      allowNull: true,
      defaultValue: null
    }
  }, {
    tableName: 'share_links',
    timestamps: true,
    updatedAt: false
  });

  ShareLink.associate = (models) => {
    ShareLink.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
  };

  return ShareLink;
};
