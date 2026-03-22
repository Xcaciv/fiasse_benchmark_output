'use strict';

const path = require('node:path');

require('dotenv').config();

const express = require('express');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');
const helmet = require('helmet');

const { initializeDatabase, getDb } = require('./src/database');
const { attachUser, attachFlash, consumeFlash, requireAuth, requireAdmin } = require('./src/middleware/auth');
const { attachCsrfToken } = require('./src/middleware/csrf');
const { SQLiteSessionStore } = require('./src/session-store');
const { buildBaseLocals, formatDateTime, formatVisibility } = require('./src/utils/view-helpers');
const { createLogger } = require('./src/logger');
const { createAppRouter } = require('./src/routes');

initializeDatabase();

const logger = createLogger();
const app = express();

app.disable('x-powered-by');
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.set('layout', 'layout');

app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'"],
        styleSrc: ["'self'"],
        imgSrc: ["'self'", 'data:'],
        fontSrc: ["'self'"],
        objectSrc: ["'none'"],
        baseUri: ["'self'"],
        formAction: ["'self'"],
        frameAncestors: ["'none'"]
      }
    },
    crossOriginEmbedderPolicy: false
  })
);

app.use('/assets', express.static(path.join(__dirname, 'public')));
app.use('/vendor/bootstrap', express.static(path.join(__dirname, 'node_modules', 'bootstrap', 'dist')));
app.use(
  session({
    name: 'loose-notes.sid',
    secret: process.env.SESSION_SECRET || 'development-session-secret-change-me',
    resave: false,
    saveUninitialized: false,
    rolling: true,
    cookie: {
      httpOnly: true,
      sameSite: 'lax',
      maxAge: Number(process.env.SESSION_MAX_AGE_MS || 1000 * 60 * 60 * 8)
    },
    store: new SQLiteSessionStore({ session })
  })
);
app.use(express.urlencoded({ extended: false }));
app.use(expressLayouts);
app.use(attachFlash);
app.use(attachUser);
app.use(attachCsrfToken);

app.use((req, res, next) => {
  res.locals.csrfToken = req.csrfToken();
  res.locals.currentUser = req.currentUser;
  res.locals.flash = consumeFlash(req);
  res.locals.helpers = {
    ...buildBaseLocals(req),
    formatDateTime,
    formatVisibility
  };
  next();
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.use(
  createAppRouter({
    db: getDb(),
    logger,
    requireAuth,
    requireAdmin
  })
);

app.use((req, res) => {
  res.status(404).render('error', {
    title: 'Page not found',
    heading: 'Page not found',
    message: 'The requested page could not be found.'
  });
});

app.use((error, req, res, next) => {
  if (error.code === 'EBADCSRFTOKEN') {
    return res.status(403).render('error', {
      title: 'Invalid form token',
      heading: 'Invalid form token',
      message: 'Your form session expired. Please retry the action.'
    });
  }

  if (error.code === 'LIMIT_FILE_SIZE' || error.message === 'Unsupported file type.') {
    return res.status(400).render('error', {
      title: 'Invalid attachment',
      heading: 'Invalid attachment',
      message: error.code === 'LIMIT_FILE_SIZE'
        ? 'One of the uploaded files exceeded the allowed size limit.'
        : error.message
    });
  }

  logger.error('Unhandled application error', {
    message: error.message,
    stack: error.stack,
    path: req.originalUrl
  });

  return res.status(500).render('error', {
    title: 'Unexpected error',
    heading: 'Unexpected error',
    message: 'Something went wrong while processing your request.'
  });
});

if (require.main === module) {
  const port = Number(process.env.PORT || 3000);
  app.listen(port, () => {
    logger.info('Loose Notes started', { port });
  });
}

module.exports = { app };
