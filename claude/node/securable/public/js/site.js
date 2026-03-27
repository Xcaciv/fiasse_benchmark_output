'use strict';

// Confirm destructive actions before form submission
document.querySelectorAll('[data-confirm]').forEach((el) => {
  el.addEventListener('click', (e) => {
    if (!window.confirm(el.dataset.confirm)) {
      e.preventDefault();
    }
  });
});

// Auto-dismiss flash alerts after 5 seconds
setTimeout(() => {
  document.querySelectorAll('.alert.alert-success, .alert.alert-danger').forEach((el) => {
    const bsAlert = bootstrap.Alert.getOrCreateInstance(el);
    if (bsAlert) bsAlert.close();
  });
}, 5000);
