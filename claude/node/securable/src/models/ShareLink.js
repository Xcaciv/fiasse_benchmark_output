'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const ShareLink = sequelize.define(
    'ShareLink',
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
      token: {
        type: DataTypes.STRING(128),
        allowNull: false,
        unique: true,
      },
      isActive: {
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: true,
      },
    },
    {
      tableName: 'share_links',
      indexes: [{ fields: ['token'] }, { fields: ['note_id'] }],
    }
  );

  ShareLink.associate = (models) => {
    ShareLink.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
  };

  return ShareLink;
};
