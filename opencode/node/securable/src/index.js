require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const morgan = require('morgan');
const session = require('express-session');
const flash = require('connect-flash');
const methodOverride = require('method-override');
const path = require('path');
const fs = require('fs');
const { initDatabase } = require('./models/database');
const logger = require('./utils/logger');

const logsDir = path.join(__dirname, '../logs');
if (!fs.existsSync(logsDir)) {
  fs.mkdirSync(logsDir, { recursive: true });
}

const app = express();

app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "blob:"],
    },
  },
  hsts: false,
}));

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(methodOverride('_method'));

const sessionConfig = {
  secret: process.env.SESSION_SECRET || 'default-secret-change-me',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 3600000,
  },
};
app.use(session(sessionConfig));

app.use(flash());
app.use(morgan('combined', { stream: { write: (msg) => logger.info(msg.trim()) } }));

app.use(express.static(path.join(__dirname, '../public')));

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

const authMiddleware = require('./middleware/auth');
const errorHandler = require('./middleware/errorHandler');

app.use(authMiddleware.loadUser);

const authRoutes = require('./routes/auth');
const noteRoutes = require('./routes/notes');
const userRoutes = require('./routes/users');
const adminRoutes = require('./routes/admin');
const shareRoutes = require('./routes/share');

app.use('/', authRoutes);
app.use('/notes', noteRoutes);
app.use('/users', userRoutes);
app.use('/admin', adminRoutes);
app.use('/share', shareRoutes);

app.get('/', (req, res) => {
  if (req.user) {
    return res.redirect('/notes');
  }
  res.render('home', { user: req.user });
});

app.get('/top-rated', async (req, res) => {
  const { Note, Rating } = require('./models');
  try {
    const notes = await Note.getTopRated(3);
    res.render('top-rated', { user: req.user, notes });
  } catch (error) {
    logger.error('Error fetching top rated notes', { error: error.message });
    res.status(500).render('error', { user: req.user, message: 'Failed to load top rated notes' });
  }
});

app.use(errorHandler);

const PORT = process.env.PORT || 3000;

async function startServer() {
  try {
    await initDatabase();
    logger.info('Database initialized successfully');

    app.listen(PORT, () => {
      logger.info(`Loose Notes application started on port ${PORT}`);
      console.log(`Loose Notes app running at http://localhost:${PORT}`);
    });
  } catch (error) {
    logger.error('Failed to start server', { error: error.message, stack: error.stack });
    process.exit(1);
  }
}

startServer();
