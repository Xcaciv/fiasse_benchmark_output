'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const ShareLink = sequelize.define('ShareLink', {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false,
    },
    // token: cryptographically random, stored as-is (not sensitive enough for hash,
    // but is a capability token — treat as opaque string, not user-visible secret)
    token: {
      type: DataTypes.STRING(64),
      allowNull: false,
      unique: true,
    },
    isRevoked: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
    },
  }, {
    tableName: 'share_links',
    indexes: [
      { fields: ['noteId'] },
      { unique: true, fields: ['token'] },
    ],
  });

  ShareLink.associate = (models) => {
    ShareLink.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
  };

  return ShareLink;
};
