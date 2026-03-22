'use strict';

function escapeLikeToken(value) {
  return value.replace(/[%_]/g, '\\$&');
}

function formatDateTime(value) {
  if (!value) {
    return '';
  }

  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function formatVisibility(value) {
  return value === 'public' ? 'Public' : 'Private';
}

function buildBaseLocals(req) {
  return {
    activePath: req.path
  };
}

function makeExcerpt(value, limit = 200) {
  if (!value) {
    return '';
  }

  if (value.length <= limit) {
    return value;
  }

  return `${value.slice(0, limit).trimEnd()}...`;
}

module.exports = {
  buildBaseLocals,
  escapeLikeToken,
  formatDateTime,
  formatVisibility,
  makeExcerpt
};
