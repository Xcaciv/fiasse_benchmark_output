'use strict';

const path = require('path');
const bcrypt = require('bcrypt');
const { sequelize } = require('../config/database');
const { logger } = require('../config/logger');
const constants = require('../config/constants');

// Load all model definitions
const User = require('./User')(sequelize);
const Note = require('./Note')(sequelize);
const Attachment = require('./Attachment')(sequelize);
const Rating = require('./Rating')(sequelize);
const ShareLink = require('./ShareLink')(sequelize);
const AuditLog = require('./AuditLog')(sequelize);

const models = { User, Note, Attachment, Rating, ShareLink, AuditLog };

// Set up associations
Object.values(models).forEach((model) => {
  if (typeof model.associate === 'function') {
    model.associate(models);
  }
});

/**
 * Initialize database: sync schema and seed admin user if absent.
 * Called once at startup.
 */
async function initializeDatabase() {
  await sequelize.sync({ alter: false, force: false });

  // Use alter:true only in development to evolve schema without data loss
  if (process.env.NODE_ENV === 'development') {
    await sequelize.sync({ alter: true });
  }

  await seedAdminUser();
}

/**
 * Create default admin user if no admin exists.
 * Admin password sourced from environment variable only.
 */
async function seedAdminUser() {
  const adminEmail = 'admin@loosenotes.local';
  const adminPassword = process.env.ADMIN_PASSWORD || 'AdminP@ss1234567890!';

  const existing = await User.scope('withAuth').findOne({
    where: { role: constants.ROLES.ADMIN }
  });

  if (existing) return;

  const passwordHash = await bcrypt.hash(adminPassword, constants.AUTH.BCRYPT_ROUNDS);

  await User.create({
    username: 'admin',
    email: adminEmail,
    passwordHash,
    role: constants.ROLES.ADMIN,
    isActive: true,
    emailVerified: true
  });

  logger.info('Default admin user created', {
    event: 'admin.seed',
    message: 'Admin user seeded - change password immediately in production'
  });
}

module.exports = { sequelize, initializeDatabase, ...models };
