'use strict';

// Confirm delete actions to prevent accidental data loss
document.addEventListener('DOMContentLoaded', function () {
  const deleteForms = document.querySelectorAll('.delete-form');
  deleteForms.forEach(function (form) {
    form.addEventListener('submit', function (event) {
      if (!window.confirm('Are you sure you want to delete this item? This action cannot be undone.')) {
        event.preventDefault();
      }
    });
  });
});
