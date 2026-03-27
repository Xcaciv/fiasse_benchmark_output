'use strict';
const express = require('express');
const path = require('path');
const session = require('express-session');
const ConnectSQLite3 = require('connect-sqlite3')(session);
const config = require('./config');
const { applySecurityHeaders } = require('./middleware/security');
const csrfMiddleware = require('./middleware/csrf');
const { notFoundHandler, errorHandler } = require('./middleware/errorHandler');
const { generalLimiter } = require('./middleware/rateLimit');
const registerRoutes = require('./routes/index');
const db = require('./models/database');
const auditService = require('./services/auditService');
const logger = require('./utils/logger');

const app = express();

// Initialize audit service with db
auditService.init(db);

// Security headers
applySecurityHeaders(app);

// Body parser - limit to 1mb
app.use(express.urlencoded({ extended: false, limit: '1mb' }));
app.use(express.json({ limit: '1mb' }));

// Static files from /public
app.use(express.static(path.join(__dirname, '../public')));

// Session configuration
app.use(session({
  store: new ConnectSQLite3({
    db: 'loose-notes.db',
    dir: path.dirname(config.dbPath),
  }),
  secret: config.sessionSecret,
  resave: false,
  saveUninitialized: false,
  name: 'ln.sid',
  cookie: {
    httpOnly: true,
    secure: config.nodeEnv === 'production',
    sameSite: 'strict',
    maxAge: config.sessionMaxAge,
  },
}));

// CSRF protection
app.use(csrfMiddleware);

// View engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, '../views'));

// Make session user info available to all templates
app.use((req, res, next) => {
  res.locals.user = req.session.userId ? {
    id: req.session.userId,
    username: req.session.username,
    role: req.session.role,
  } : null;
  res.locals.flash = req.session.flash || {};
  delete req.session.flash;
  next();
});

// General rate limiter
app.use(generalLimiter);

// Register all routes
registerRoutes(app, db);

// Error handlers (must be last)
app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
