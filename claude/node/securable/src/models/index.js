'use strict';

/**
 * Model registry — loads all models, runs associations.
 * Centralised to avoid circular requires and to make the dependency graph explicit.
 */

const sequelize = require('../config/database');

const User = require('./User')(sequelize);
const Note = require('./Note')(sequelize);
const Attachment = require('./Attachment')(sequelize);
const Rating = require('./Rating')(sequelize);
const ShareLink = require('./ShareLink')(sequelize);
const AuditLog = require('./AuditLog')(sequelize);
const PasswordResetToken = require('./PasswordResetToken')(sequelize);

const models = { User, Note, Attachment, Rating, ShareLink, AuditLog, PasswordResetToken };

// Wire associations
Object.values(models).forEach((model) => {
  if (typeof model.associate === 'function') {
    model.associate(models);
  }
});

module.exports = { sequelize, ...models };
