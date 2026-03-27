'use strict';

const { sequelize } = require('../config/database');
const UserModel = require('./User');
const NoteModel = require('./Note');
const AttachmentModel = require('./Attachment');
const RatingModel = require('./Rating');
const ShareLinkModel = require('./ShareLink');
const AuditLogModel = require('./AuditLog');

// Initialize models
const User = UserModel(sequelize);
const Note = NoteModel(sequelize);
const Attachment = AttachmentModel(sequelize);
const Rating = RatingModel(sequelize);
const ShareLink = ShareLinkModel(sequelize);
const AuditLog = AuditLogModel(sequelize);

const models = { User, Note, Attachment, Rating, ShareLink, AuditLog };

// Register associations after all models are initialized
Object.values(models)
  .filter((m) => typeof m.associate === 'function')
  .forEach((m) => m.associate(models));

module.exports = { sequelize, ...models };
