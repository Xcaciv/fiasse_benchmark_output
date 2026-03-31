'use strict';

const { DataTypes } = require('sequelize');

/**
 * User model.
 * passwordHash stores bcrypt output only — plaintext never persisted.
 * Least-privilege: password hash excluded from default query attributes
 * via a dedicated scope (Confidentiality).
 */
module.exports = (sequelize) => {
  const User = sequelize.define(
    'User',
    {
      id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true,
      },
      username: {
        type: DataTypes.STRING(64),
        allowNull: false,
        unique: true,
        validate: {
          len: [3, 64],
          is: /^[a-z0-9_.-]+$/i,
        },
      },
      email: {
        type: DataTypes.STRING(254),
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
        allowNull: false,
        defaultValue: 'user',
      },
      isActive: {
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: true,
      },
    },
    {
      tableName: 'users',
      defaultScope: {
        attributes: { exclude: ['passwordHash'] },
      },
      scopes: {
        withPassword: { attributes: {} },
      },
      indexes: [
        { fields: ['username'] },
        { fields: ['email'] },
      ],
    }
  );

  User.associate = (models) => {
    User.hasMany(models.Note, { foreignKey: 'userId', as: 'notes' });
    User.hasMany(models.Rating, { foreignKey: 'userId', as: 'ratings' });
    User.hasMany(models.AuditLog, { foreignKey: 'userId', as: 'auditLogs' });
    User.hasMany(models.PasswordResetToken, {
      foreignKey: 'userId',
      as: 'passwordResetTokens',
    });
  };

  return User;
};
