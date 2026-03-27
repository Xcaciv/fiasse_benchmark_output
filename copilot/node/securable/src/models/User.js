'use strict';

const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const User = sequelize.define('User', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  username: {
    type: DataTypes.STRING(50),
    allowNull: false,
    unique: true,
    validate: {
      len: [3, 50],
      is: /^[a-zA-Z0-9_]+$/,
    },
  },
  email: {
    type: DataTypes.STRING(255),
    allowNull: false,
    unique: true,
    validate: { isEmail: true },
  },
  passwordHash: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  role: {
    type: DataTypes.ENUM('user', 'admin'),
    defaultValue: 'user',
    allowNull: false,
  },
  passwordResetToken: {
    type: DataTypes.STRING(255),
    allowNull: true,
  },
  passwordResetExpires: {
    type: DataTypes.DATE,
    allowNull: true,
  },
}, {
  tableName: 'users',
  defaultScope: {
    attributes: { exclude: ['passwordHash', 'passwordResetToken', 'passwordResetExpires'] },
  },
  scopes: {
    withAuth: {
      attributes: {},
    },
  },
});

module.exports = User;
