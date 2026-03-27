'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const Rating = sequelize.define('Rating', {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    noteId: {
      type: DataTypes.UUID,
      allowNull: false,
    },
    userId: {
      type: DataTypes.UUID,
      allowNull: false,
    },
    // Integrity: constrained to valid range at DB and application layer
    value: {
      type: DataTypes.INTEGER,
      allowNull: false,
      validate: { min: 1, max: 5 },
    },
    comment: {
      type: DataTypes.STRING(1000),
      allowNull: true,
    },
  }, {
    tableName: 'ratings',
    indexes: [
      { fields: ['noteId'] },
      { unique: true, fields: ['noteId', 'userId'] }, // one rating per user per note
    ],
  });

  Rating.associate = (models) => {
    Rating.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
    Rating.belongsTo(models.User, { foreignKey: 'userId', as: 'rater' });
  };

  return Rating;
};
