'use strict';

const { DataTypes, Model } = require('sequelize');
const bcrypt = require('bcryptjs');
const { sequelize } = require('../config/database');

const BCRYPT_COST = 12;

class User extends Model {
  /**
   * Timing-safe password verification using bcrypt.
   * @param {string} plaintext
   * @returns {Promise<boolean>}
   */
  async verifyPassword(plaintext) {
    return bcrypt.compare(plaintext, this.passwordHash);
  }

  /**
   * Hash a plaintext password with bcrypt cost factor 12.
   * @param {string} plaintext
   * @returns {Promise<string>}
   */
  static async hashPassword(plaintext) {
    return bcrypt.hash(plaintext, BCRYPT_COST);
  }

  /**
   * Confidentiality: strip sensitive fields before serialization.
   * passwordHash and resetToken must never leave the server layer.
   */
  toJSON() {
    const values = { ...this.get() };
    delete values.passwordHash;
    delete values.resetToken;
    return values;
  }
}

User.init({
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  username: {
    type: DataTypes.STRING(30),
    allowNull: false,
    unique: true,
    validate: { len: [3, 30], isAlphanumeric: true },
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
    allowNull: false,
    defaultValue: 'user',
  },
  resetToken: {
    type: DataTypes.STRING(255),
    allowNull: true,
  },
  resetTokenExpiresAt: {
    type: DataTypes.DATE,
    allowNull: true,
  },
}, {
  sequelize,
  modelName: 'User',
  tableName: 'users',
  timestamps: true,
});

module.exports = { User };
