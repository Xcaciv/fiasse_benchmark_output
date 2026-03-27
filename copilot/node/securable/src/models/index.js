'use strict';

const { sequelize } = require('../config/database');
const { User } = require('./User');
const { Note } = require('./Note');
const { Attachment } = require('./Attachment');
const { Rating } = require('./Rating');
const { ShareLink } = require('./ShareLink');
const { AuditLog } = require('./AuditLog');
const { logger } = require('../config/logger');

// --- Associations ---

// User ↔ Note
User.hasMany(Note, { foreignKey: 'userId', onDelete: 'CASCADE' });
Note.belongsTo(User, { foreignKey: 'userId' });

// User ↔ Rating
User.hasMany(Rating, { foreignKey: 'userId', onDelete: 'CASCADE' });
Rating.belongsTo(User, { foreignKey: 'userId' });

// Note ↔ Attachment (cascade delete when note deleted)
Note.hasMany(Attachment, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Attachment.belongsTo(Note, { foreignKey: 'noteId' });

// Note ↔ Rating (cascade delete when note deleted)
Note.hasMany(Rating, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Rating.belongsTo(Note, { foreignKey: 'noteId' });

// Note ↔ ShareLink (one-to-one, cascade delete)
Note.hasOne(ShareLink, { foreignKey: 'noteId', onDelete: 'CASCADE' });
ShareLink.belongsTo(Note, { foreignKey: 'noteId' });

// AuditLog optional FK to User (nullable actorId)
AuditLog.belongsTo(User, { foreignKey: 'actorId', as: 'actor', constraints: false });

/**
 * Synchronize all models with the database schema.
 * Uses alter:true to apply non-destructive schema changes.
 */
const syncDatabase = async () => {
  try {
    await sequelize.sync({ alter: true });
    logger.info('Database synchronized successfully');
  } catch (err) {
    logger.error('Database synchronization failed', { error: err.message });
    throw err;
  }
};

module.exports = {
  sequelize,
  User,
  Note,
  Attachment,
  Rating,
  ShareLink,
  AuditLog,
  syncDatabase,
};
