'use strict';

const { DataTypes } = require('sequelize');

/**
 * Password reset tokens.
 * Token value stored as bcrypt hash — raw token sent by email only (Confidentiality).
 * Expiry enforced at query time, not by database TTL.
 */
module.exports = (sequelize) => {
  const PasswordResetToken = sequelize.define(
    'PasswordResetToken',
    {
      id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true,
      },
      userId: {
        type: DataTypes.UUID,
        allowNull: false,
        references: { model: 'users', key: 'id' },
      },
      tokenHash: {
        type: DataTypes.STRING(255),
        allowNull: false,
      },
      expiresAt: {
        type: DataTypes.DATE,
        allowNull: false,
      },
      usedAt: {
        type: DataTypes.DATE,
        allowNull: true,
      },
    },
    {
      tableName: 'password_reset_tokens',
      indexes: [{ fields: ['user_id'] }],
    }
  );

  PasswordResetToken.associate = (models) => {
    PasswordResetToken.belongsTo(models.User, {
      foreignKey: 'userId',
      as: 'user',
    });
  };

  return PasswordResetToken;
};
