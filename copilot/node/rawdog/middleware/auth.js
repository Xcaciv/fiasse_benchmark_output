function ensureAuthenticated(req, res, next) {
  if (req.isAuthenticated()) return next();
  req.flash('error', 'Please log in to access that page.');
  res.redirect('/auth/login');
}

function ensureAdmin(req, res, next) {
  if (req.isAuthenticated() && req.user.role === 'admin') return next();
  res.status(403).render('error', {
    title: 'Forbidden',
    message: 'You do not have permission to access this page.',
    user: req.user || null,
  });
}

function ensureNoteOwner(req, res, next) {
  const { note } = res.locals;
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.user || null });
  if (note.userId === req.user.id || req.user.role === 'admin') return next();
  res.status(403).render('error', {
    title: 'Forbidden',
    message: 'You do not own this note.',
    user: req.user || null,
  });
}

module.exports = { ensureAuthenticated, ensureAdmin, ensureNoteOwner };
