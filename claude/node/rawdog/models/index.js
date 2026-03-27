const sequelize = require('../config/database');
const User = require('./User');
const Note = require('./Note');
const Attachment = require('./Attachment');
const Rating = require('./Rating');
const ShareLink = require('./ShareLink');
const AuditLog = require('./AuditLog');

// User hasMany Note (onDelete CASCADE)
User.hasMany(Note, { foreignKey: 'userId', onDelete: 'CASCADE' });
Note.belongsTo(User, { foreignKey: 'userId', as: 'author' });

// Note hasMany Attachment (onDelete CASCADE)
Note.hasMany(Attachment, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Attachment.belongsTo(Note, { foreignKey: 'noteId' });

// Note hasMany Rating (onDelete CASCADE)
Note.hasMany(Rating, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Rating.belongsTo(Note, { foreignKey: 'noteId' });

// Note hasOne ShareLink (onDelete CASCADE)
Note.hasOne(ShareLink, { foreignKey: 'noteId', onDelete: 'CASCADE' });
ShareLink.belongsTo(Note, { foreignKey: 'noteId' });

// Rating belongsTo User (as 'rater')
User.hasMany(Rating, { foreignKey: 'userId' });
Rating.belongsTo(User, { foreignKey: 'userId', as: 'rater' });

// AuditLog belongsTo User
AuditLog.belongsTo(User, { foreignKey: 'userId' });
User.hasMany(AuditLog, { foreignKey: 'userId' });

module.exports = {
  sequelize,
  User,
  Note,
  Attachment,
  Rating,
  ShareLink,
  AuditLog
};
