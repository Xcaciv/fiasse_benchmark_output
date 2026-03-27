'use strict';

const sequelize = require('../config/database');
const User = require('./User');
const Note = require('./Note');
const Attachment = require('./Attachment');
const Rating = require('./Rating');
const ShareLink = require('./ShareLink');
const ActivityLog = require('./ActivityLog');

// User associations
User.hasMany(Note, { foreignKey: 'userId', onDelete: 'CASCADE' });
User.hasMany(Rating, { foreignKey: 'userId', onDelete: 'CASCADE' });
User.hasMany(ActivityLog, { foreignKey: 'userId', onDelete: 'SET NULL' });

// Note associations
Note.belongsTo(User, { foreignKey: 'userId', as: 'author' });
Note.hasMany(Attachment, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Note.hasMany(Rating, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Note.hasMany(ShareLink, { foreignKey: 'noteId', onDelete: 'CASCADE' });

// Rating associations
Rating.belongsTo(User, { foreignKey: 'userId', as: 'rater' });
Rating.belongsTo(Note, { foreignKey: 'noteId' });

// Attachment associations
Attachment.belongsTo(Note, { foreignKey: 'noteId' });

// ShareLink associations
ShareLink.belongsTo(Note, { foreignKey: 'noteId' });

// ActivityLog associations
ActivityLog.belongsTo(User, { foreignKey: 'userId', as: 'actor' });

async function syncDatabase() {
  await sequelize.sync({ force: false, alter: false });
}

module.exports = {
  sequelize,
  syncDatabase,
  User,
  Note,
  Attachment,
  Rating,
  ShareLink,
  ActivityLog,
};
