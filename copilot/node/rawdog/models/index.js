const sequelize = require('../config/database');
const User = require('./user');
const Note = require('./note');
const Attachment = require('./attachment');
const Rating = require('./rating');
const ShareLink = require('./shareLink');
const ActivityLog = require('./activityLog');

// Associations
User.hasMany(Note, { foreignKey: 'userId', onDelete: 'CASCADE' });
Note.belongsTo(User, { foreignKey: 'userId' });

Note.hasMany(Attachment, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Attachment.belongsTo(Note, { foreignKey: 'noteId' });

Note.hasMany(Rating, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Rating.belongsTo(Note, { foreignKey: 'noteId' });
Rating.belongsTo(User, { foreignKey: 'userId' });
User.hasMany(Rating, { foreignKey: 'userId' });

Note.hasOne(ShareLink, { foreignKey: 'noteId', onDelete: 'CASCADE' });
ShareLink.belongsTo(Note, { foreignKey: 'noteId' });

User.hasMany(ActivityLog, { foreignKey: 'userId' });
ActivityLog.belongsTo(User, { foreignKey: 'userId' });

module.exports = { sequelize, User, Note, Attachment, Rating, ShareLink, ActivityLog };
