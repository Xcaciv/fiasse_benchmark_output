'use strict';

function addFlash(req, type, message, extras = {}) {
  if (!req.session.flashMessages) {
    req.session.flashMessages = [];
  }

  req.session.flashMessages.push({
    type,
    message,
    ...extras
  });
}

module.exports = { addFlash };
