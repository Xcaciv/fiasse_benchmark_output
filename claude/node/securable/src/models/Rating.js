'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const Rating = sequelize.define(
    'Rating',
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
      userId: {
        type: DataTypes.UUID,
        allowNull: false,
        references: { model: 'users', key: 'id' },
      },
      value: {
        type: DataTypes.INTEGER,
        allowNull: false,
        validate: { min: 1, max: 5 },
      },
      comment: {
        type: DataTypes.TEXT,
        allowNull: true,
      },
    },
    {
      tableName: 'ratings',
      indexes: [
        { fields: ['note_id'] },
        { fields: ['user_id'] },
        { unique: true, fields: ['note_id', 'user_id'] },
      ],
    }
  );

  Rating.associate = (models) => {
    Rating.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
    Rating.belongsTo(models.User, { foreignKey: 'userId', as: 'rater' });
  };

  return Rating;
};
