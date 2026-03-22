function formatDate(value) {
  if (!value) {
    return 'N/A';
  }

  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function excerpt(text, length = 200) {
  if (!text) {
    return '';
  }

  if (text.length <= length) {
    return text;
  }

  return `${text.slice(0, length).trimEnd()}...`;
}

module.exports = {
  formatDate,
  excerpt
};
