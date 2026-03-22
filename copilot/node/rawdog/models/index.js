const sequelize = require('../config/database');
const User = require('./User');
const Note = require('./Note');
const Attachment = require('./Attachment');
const Rating = require('./Rating');
const ShareLink = require('./ShareLink');

// Associations
User.hasMany(Note, { foreignKey: 'userId', onDelete: 'CASCADE' });
Note.belongsTo(User, { foreignKey: 'userId', as: 'author' });

Note.hasMany(Attachment, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Attachment.belongsTo(Note, { foreignKey: 'noteId' });

Note.hasMany(Rating, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Rating.belongsTo(Note, { foreignKey: 'noteId' });
Rating.belongsTo(User, { foreignKey: 'userId', as: 'rater' });
User.hasMany(Rating, { foreignKey: 'userId', onDelete: 'CASCADE' });

Note.hasMany(ShareLink, { foreignKey: 'noteId', onDelete: 'CASCADE' });
ShareLink.belongsTo(Note, { foreignKey: 'noteId' });

module.exports = { sequelize, User, Note, Attachment, Rating, ShareLink };
