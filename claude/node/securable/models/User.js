'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const User = sequelize.define('User', {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    // Stored lowercase — canonicalization enforced at model level
    username: {
      type: DataTypes.STRING(32),
      allowNull: false,
      unique: true,
      set(value) {
        this.setDataValue('username', value.trim().toLowerCase());
      },
    },
    email: {
      type: DataTypes.STRING(254),
      allowNull: false,
      unique: true,
      set(value) {
        this.setDataValue('email', value.trim().toLowerCase());
      },
    },
    // Confidentiality: field named to make clear it is a hash, never plaintext
    passwordHash: {
      type: DataTypes.STRING(60),
      allowNull: false,
    },
    role: {
      type: DataTypes.ENUM('user', 'admin'),
      defaultValue: 'user',
      allowNull: false,
    },
    isLocked: {
      type: DataTypes.BOOLEAN,
      defaultValue: false,
    },
    // Password reset — token stored as hash, expiry enforced at service layer
    resetTokenHash: {
      type: DataTypes.STRING(64),
      allowNull: true,
    },
    resetTokenExpiry: {
      type: DataTypes.DATE,
      allowNull: true,
    },
  }, {
    tableName: 'users',
    indexes: [
      { unique: true, fields: ['username'] },
      { unique: true, fields: ['email'] },
    ],
  });

  User.associate = (models) => {
    User.hasMany(models.Note, { foreignKey: 'userId', as: 'notes', onDelete: 'CASCADE' });
    User.hasMany(models.Rating, { foreignKey: 'userId', as: 'ratings' });
    User.hasMany(models.AuditLog, { foreignKey: 'userId', as: 'auditLogs' });
  };

  return User;
};
