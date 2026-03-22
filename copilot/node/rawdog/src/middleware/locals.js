const helpers = require('../lib/view-helpers');

function populateLocals(req, res, next) {
  res.locals.currentUser = req.currentUser;
  res.locals.helpers = helpers;
  res.locals.currentPath = req.path;
  next();
}

module.exports = {
  populateLocals
};
