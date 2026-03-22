'use strict';
const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

function defineUser(sequelize) {
  return sequelize.define('User', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true
    },
    username: {
      type: DataTypes.STRING(30),
      allowNull: false,
      unique: true
    },
    email: {
      type: DataTypes.STRING(255),
      allowNull: false,
      unique: true
    },
    passwordHash: {
      type: DataTypes.STRING,
      allowNull: false
    },
    isAdmin: {
      type: DataTypes.BOOLEAN,
      defaultValue: false
    },
    passwordResetTokenHash: {
      type: DataTypes.STRING,
      allowNull: true
    },
    passwordResetExpiry: {
      type: DataTypes.DATE,
      allowNull: true
    }
  }, {
    tableName: 'users',
    timestamps: true
  });
}

module.exports = { defineUser };
