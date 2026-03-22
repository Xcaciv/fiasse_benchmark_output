function flashMiddleware(req, res, next) {
  const flash = req.session?.flash || {};

  req.flash = function flash(type, message) {
    req.session = req.session || {};
    req.session.flash = req.session.flash || {};
    req.session.flash[type] = req.session.flash[type] || [];
    req.session.flash[type].push(message);
  };

  res.locals.flash = flash;

  if (req.session) {
    delete req.session.flash;
  }
  next();
}

module.exports = {
  flashMiddleware
};
