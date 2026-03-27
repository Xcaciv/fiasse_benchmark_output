require('dotenv').config();

const express = require('express');
const session = require('express-session');
const flash = require('express-flash');
const passport = require('passport');
const path = require('path');
const fs = require('fs');
const bcrypt = require('bcryptjs');
const csurf = require('csurf');
const methodOverride = require('method-override');
const winston = require('winston');

const db = require('./models');

// Initialize passport config
require('./config/passport')(passport);

// Logger setup
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.printf(({ timestamp, level, message }) => `${timestamp} [${level.toUpperCase()}] ${message}`)
  ),
  transports: [
    new winston.transports.Console()
  ]
});

const app = express();

// Ensure required directories exist
const uploadDir = path.resolve(process.env.UPLOAD_DIR || './uploads');
const dataDir = path.resolve('./data');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });
if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, { recursive: true });

// View engine setup
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Body parsing
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Method override (for PUT/DELETE in forms)
app.use(methodOverride('_method'));

// Static files
app.use(express.static(path.join(__dirname, 'public')));
app.use('/uploads', express.static(uploadDir));

// Session
const SQLiteStore = require('connect-sqlite3')(session);
app.use(session({
  store: new SQLiteStore({ db: 'sessions.db', dir: './data' }),
  secret: process.env.SESSION_SECRET || 'loose-notes-dev-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    maxAge: 1000 * 60 * 60 * 24 * 7, // 7 days
    httpOnly: true,
    sameSite: 'lax'
  }
}));

// Passport
app.use(passport.initialize());
app.use(passport.session());

// Flash messages
app.use(flash());

// CSRF protection (after session & passport)
const csrfProtection = csurf();
app.use(csrfProtection);

// CSRF error handler
app.use((err, req, res, next) => {
  if (err.code === 'EBADCSRFTOKEN') {
    req.flash('error', 'Form expired or invalid. Please try again.');
    return res.redirect('back');
  }
  next(err);
});

// Global locals for all views
app.use((req, res, next) => {
  res.locals.user = req.user || null;
  res.locals.csrfToken = req.csrfToken();
  res.locals.success_msg = req.flash('success_msg');
  res.locals.error_msg = req.flash('error_msg');
  res.locals.error = req.flash('error');
  next();
});

// Routes
app.use('/', require('./routes/index'));
app.use('/auth', require('./routes/auth'));
app.use('/notes', require('./routes/notes'));
app.use('/profile', require('./routes/profile'));
app.use('/admin', require('./routes/admin'));
app.use('/share', require('./routes/share'));

// 404 handler
app.use((req, res) => {
  res.status(404).render('error', {
    title: 'Not Found',
    statusCode: 404,
    message: 'The page you requested could not be found.'
  });
});

// 500 handler
app.use((err, req, res, next) => {
  logger.error(`${err.status || 500} - ${err.message} - ${req.originalUrl} - ${req.method} - ${req.ip}`);
  const statusCode = err.status || 500;
  res.status(statusCode).render('error', {
    title: 'Error',
    statusCode,
    message: process.env.NODE_ENV === 'production' ? 'An internal server error occurred.' : (err.message || 'An internal server error occurred.')
  });
});

// Start server
const PORT = process.env.PORT || 3000;

async function startServer() {
  try {
    // Sync database
    await db.sequelize.sync({ alter: false });
    logger.info('Database synchronized.');

    // Create default admin if none exists
    const adminExists = await db.User.findOne({ where: { isAdmin: true } });
    if (!adminExists) {
      const hashedPassword = await bcrypt.hash('Admin@123', 12);
      await db.User.create({
        username: 'admin',
        email: 'admin@example.com',
        password: hashedPassword,
        isAdmin: true
      });
      logger.info('Default admin user created: admin@example.com / Admin@123');
    }

    app.listen(PORT, () => {
      logger.info(`Loose Notes server running on http://localhost:${PORT}`);
    });
  } catch (error) {
    logger.error('Failed to start server: ' + error.message);
    process.exit(1);
  }
}

startServer();

module.exports = app;
