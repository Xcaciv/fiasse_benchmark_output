'use strict';

const { DataTypes, Model } = require('sequelize');
const { sequelize } = require('../config/database');

class Rating extends Model {}

Rating.init({
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
  stars: {
    type: DataTypes.INTEGER,
    allowNull: false,
    validate: { min: 1, max: 5, isInt: true },
  },
  comment: {
    type: DataTypes.TEXT,
    allowNull: true,
    validate: { len: [0, 1000] },
  },
}, {
  sequelize,
  modelName: 'Rating',
  tableName: 'ratings',
  timestamps: true,
  indexes: [
    { unique: true, fields: ['noteId', 'userId'] },
  ],
});

module.exports = { Rating };
