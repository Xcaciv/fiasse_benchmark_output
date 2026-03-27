'use strict';

const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

module.exports = (sequelize) => {
  const Rating = sequelize.define('Rating', {
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
    userId: {
      type: DataTypes.UUID,
      allowNull: false,
      references: {
        model: 'users',
        key: 'id'
      }
    },
    // Integrity: value validated at model level and service level
    value: {
      type: DataTypes.INTEGER,
      allowNull: false,
      validate: {
        min: 1,
        max: 5,
        isInt: true
      }
    },
    comment: {
      type: DataTypes.TEXT,
      allowNull: true,
      validate: {
        len: [0, 1000]
      }
    }
  }, {
    tableName: 'ratings',
    timestamps: true,
    indexes: [
      {
        // Enforce one rating per user per note
        unique: true,
        fields: ['noteId', 'userId']
      }
    ]
  });

  Rating.associate = (models) => {
    Rating.belongsTo(models.Note, { foreignKey: 'noteId', as: 'note' });
    Rating.belongsTo(models.User, { foreignKey: 'userId', as: 'rater' });
  };

  return Rating;
};
