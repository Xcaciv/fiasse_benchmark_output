'use strict';

const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

module.exports = (sequelize) => {
  const User = sequelize.define('User', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true,
      allowNull: false
    },
    username: {
      type: DataTypes.STRING(50),
      allowNull: false,
      unique: true,
      validate: {
        len: [3, 50],
        is: /^[a-zA-Z0-9_-]+$/i
      }
    },
    email: {
      type: DataTypes.STRING(255),
      allowNull: false,
      unique: true,
      validate: {
        isEmail: true
      }
    },
    // Confidentiality: store only hash, never plaintext password
    passwordHash: {
      type: DataTypes.STRING(255),
      allowNull: false
    },
    role: {
      type: DataTypes.ENUM('user', 'admin'),
      allowNull: false,
      defaultValue: 'user'
    },
    isActive: {
      type: DataTypes.BOOLEAN,
      allowNull: false,
      defaultValue: true
    },
    emailVerified: {
      type: DataTypes.BOOLEAN,
      allowNull: false,
      defaultValue: false
    },
    failedLoginAttempts: {
      type: DataTypes.INTEGER,
      allowNull: false,
      defaultValue: 0
    },
    lockoutUntil: {
      type: DataTypes.DATE,
      allowNull: true,
      defaultValue: null
    },
    // Password reset fields - token hash only, never raw token
    passwordResetHash: {
      type: DataTypes.STRING(255),
      allowNull: true,
      defaultValue: null
    },
    passwordResetExpiry: {
      type: DataTypes.DATE,
      allowNull: true,
      defaultValue: null
    }
  }, {
    tableName: 'users',
    timestamps: true,
    // Prevent accidental exposure of sensitive fields
    defaultScope: {
      attributes: {
        exclude: ['passwordHash', 'passwordResetHash', 'passwordResetExpiry']
      }
    },
    scopes: {
      withAuth: {
        attributes: {}
      }
    }
  });

  User.associate = (models) => {
    User.hasMany(models.Note, { foreignKey: 'ownerId', as: 'notes' });
    User.hasMany(models.Rating, { foreignKey: 'userId', as: 'ratings' });
    User.hasMany(models.AuditLog, { foreignKey: 'actorId', as: 'auditLogs' });
  };

  return User;
};
