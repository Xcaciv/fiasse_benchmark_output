function isAuthenticated(req, res, next) {
  if (req.session && req.session.user) {
    return next();
  }
  req.flash('error', 'Please log in to access this page');
  res.redirect('/auth/login');
}

function isGuest(req, res, next) {
  if (req.session && req.session.user) {
    return res.redirect('/');
  }
  return next();
}

function isAdmin(req, res, next) {
  if (req.session && req.session.user && req.session.user.role === 'admin') {
    return next();
  }
  req.flash('error', 'Access denied. Administrator privileges required.');
  res.redirect('/');
}

function isNoteOwner(req, res, next) {
  const noteId = req.params.id || req.body.noteId;
  if (!noteId) {
    return next();
  }

  const { db } = require('../db/database');
  const note = db.prepare('SELECT user_id FROM notes WHERE id = ?').get(noteId);

  if (!note) {
    return res.status(404).render('error', { 
      message: 'Note not found',
      user: req.session?.user 
    });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).render('error', { 
      message: 'You do not have permission to modify this note',
      user: req.session?.user 
    });
  }

  next();
}

module.exports = {
  isAuthenticated,
  isGuest,
  isAdmin,
  isNoteOwner
};
