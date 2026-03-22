require('dotenv').config();
const express = require('express');
const session = require('express-session');
const passport = require('passport');
const flash = require('connect-flash');
const methodOverride = require('method-override');
const morgan = require('morgan');
const path = require('path');
const bcrypt = require('bcrypt');

const logger = require('./config/logger');
module.exports.logger = logger;

const { sequelize, User } = require('./models');
const configurePassport = require('./config/passport');

const app = express();

// View engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Static files
app.use(express.static(path.join(__dirname, 'public')));

// Body parsing
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Method override (for PUT/DELETE from forms)
app.use(methodOverride('_method'));

// Morgan HTTP logging
app.use(morgan('combined', {
  stream: { write: (msg) => logger.info(msg.trim()) },
}));

// Session
app.use(session({
  secret: process.env.SESSION_SECRET || 'loose-notes-default-secret',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 24 * 60 * 60 * 1000 }, // 1 day
}));

// Flash messages
app.use(flash());

// Passport
configurePassport(passport);
app.use(passport.initialize());
app.use(passport.session());

// Global template locals
app.use((req, res, next) => {
  res.locals.currentUser = req.user || null;
  next();
});

// Routes
app.use('/', require('./routes/index'));
app.use('/auth', require('./routes/auth'));
app.use('/notes', require('./routes/notes'));
app.use('/admin', require('./routes/admin'));
app.use('/profile', require('./routes/profile'));
app.use('/share', require('./routes/share'));

// 404 handler
app.use((req, res) => {
  res.status(404).render('error', { title: 'Not Found', message: 'The page you are looking for does not exist.', user: req.user || null });
});

// Error handler
app.use((err, req, res, next) => {
  logger.error(`Unhandled error: ${err.message}\n${err.stack}`);
  const status = err.status || 500;
  res.status(status).render('error', {
    title: 'Error',
    message: process.env.NODE_ENV === 'production' ? 'An unexpected error occurred.' : err.message,
    user: req.user || null,
  });
});

// Start
const PORT = process.env.PORT || 3000;

async function start() {
  await sequelize.sync({ alter: false });
  logger.info('Database synced.');

  // Seed default admin
  const adminEmail = 'admin@example.com';
  const existing = await User.findOne({ where: { email: adminEmail } });
  if (!existing) {
    const passwordHash = await bcrypt.hash('admin123', 12);
    await User.create({
      username: 'admin',
      email: adminEmail,
      passwordHash,
      role: 'admin',
    });
    logger.info('Default admin user created: admin@example.com / admin123');
  }

  app.listen(PORT, () => {
    logger.info(`Loose Notes listening on http://localhost:${PORT}`);
  });
}

start().catch((err) => {
  logger.error(`Startup error: ${err.message}`);
  process.exit(1);
});

module.exports = app;
