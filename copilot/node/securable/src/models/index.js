'use strict';
const { sequelize } = require('../config/database');
const { defineUser } = require('./User');
const { defineNote } = require('./Note');
const { defineAttachment } = require('./Attachment');
const { defineRating } = require('./Rating');
const { defineShareLink } = require('./ShareLink');

const User = defineUser(sequelize);
const Note = defineNote(sequelize);
const Attachment = defineAttachment(sequelize);
const Rating = defineRating(sequelize);
const ShareLink = defineShareLink(sequelize);

// Associations
User.hasMany(Note, { foreignKey: 'userId', onDelete: 'CASCADE' });
Note.belongsTo(User, { foreignKey: 'userId' });

Note.hasMany(Attachment, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Attachment.belongsTo(Note, { foreignKey: 'noteId' });

Note.hasMany(Rating, { foreignKey: 'noteId', onDelete: 'CASCADE' });
Rating.belongsTo(Note, { foreignKey: 'noteId' });
Rating.belongsTo(User, { foreignKey: 'userId' });
User.hasMany(Rating, { foreignKey: 'userId' });

Note.hasMany(ShareLink, { foreignKey: 'noteId', onDelete: 'CASCADE' });
ShareLink.belongsTo(Note, { foreignKey: 'noteId' });

module.exports = { sequelize, User, Note, Attachment, Rating, ShareLink };
