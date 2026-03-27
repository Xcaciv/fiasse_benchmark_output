/* Loose Notes — Main client JS */
'use strict';

// Confirm dialog for delete operations
document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.confirm-delete').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!confirm('Are you sure you want to delete this? This action cannot be undone.')) {
        e.preventDefault();
      }
    });
  });

  // Auto-dismiss flash messages after 5 seconds
  document.querySelectorAll('.flash-auto-dismiss').forEach(function (el) {
    setTimeout(function () {
      const alert = bootstrap.Alert.getOrCreateInstance(el);
      if (alert) alert.close();
    }, 5000);
  });

  // Star rating visual feedback
  const starContainer = document.getElementById('starRating');
  if (starContainer) {
    const labels = starContainer.querySelectorAll('label');
    labels.forEach(function (label, index) {
      label.addEventListener('mouseenter', function () {
        labels.forEach(function (l, i) {
          l.querySelector('.star').style.color = i <= index ? '#f0c040' : '#ccc';
        });
      });
      label.addEventListener('mouseleave', function () {
        labels.forEach(function (l) {
          l.querySelector('.star').style.color = '';
        });
      });
    });
  }

  // Support _method override for PUT/DELETE via hidden input
  document.querySelectorAll('form[action*="?_method="]').forEach(function (form) {
    const url = new URL(form.action, window.location.origin);
    const method = url.searchParams.get('_method');
    if (method) {
      let input = form.querySelector('input[name="_method"]');
      if (!input) {
        input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_method';
        form.appendChild(input);
      }
      input.value = method;
    }
  });
});
