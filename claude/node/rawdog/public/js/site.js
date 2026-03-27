// Loose Notes - Client-side JS

document.addEventListener('DOMContentLoaded', function () {

  // Auto-dismiss flash messages after 5 seconds
  var alerts = document.querySelectorAll('.alert.alert-success, .alert.alert-danger');
  alerts.forEach(function (alert) {
    setTimeout(function () {
      try {
        var bsAlert = new bootstrap.Alert(alert);
        if (bsAlert) bsAlert.close();
      } catch (e) {
        alert.style.display = 'none';
      }
    }, 5000);
  });

  // Confirm delete for any delete forms with data-confirm attribute
  var deleteForms = document.querySelectorAll('form[data-confirm]');
  deleteForms.forEach(function (form) {
    form.addEventListener('submit', function (e) {
      var message = form.getAttribute('data-confirm') || 'Are you sure?';
      if (!confirm(message)) {
        e.preventDefault();
      }
    });
  });

  // Character counter for note title
  var titleInput = document.getElementById('title');
  if (titleInput) {
    var maxLength = parseInt(titleInput.getAttribute('maxlength') || '255');
    var counter = document.createElement('div');
    counter.className = 'form-text text-end';
    titleInput.parentNode.appendChild(counter);

    titleInput.addEventListener('input', function () {
      var len = titleInput.value.length;
      counter.textContent = len + ' / ' + maxLength;
      counter.className = len > maxLength * 0.9
        ? 'form-text text-end text-warning'
        : 'form-text text-end';
    });
    counter.textContent = titleInput.value.length + ' / ' + maxLength;
  }

  // File size validation for uploads
  var fileInputs = document.querySelectorAll('input[type="file"]');
  fileInputs.forEach(function (input) {
    input.addEventListener('change', function () {
      var maxSize = 10 * 1024 * 1024; // 10MB
      var hasError = false;
      Array.from(input.files).forEach(function (file) {
        if (file.size > maxSize) {
          alert('File "' + file.name + '" is too large. Maximum size is 10MB.');
          hasError = true;
        }
      });
      if (hasError) {
        input.value = '';
      }
    });
  });

  // Password strength indicator
  var passwordInput = document.getElementById('newPassword') || document.getElementById('password');
  if (passwordInput) {
    var isRegister = window.location.pathname.indexOf('register') !== -1;
    var isReset = window.location.pathname.indexOf('reset-password') !== -1;
    if (passwordInput.id === 'newPassword' || isRegister || isReset) {
      addPasswordStrength(passwordInput);
    }
  }

  function addPasswordStrength(input) {
    var indicator = document.createElement('div');
    indicator.className = 'mt-1';
    input.parentNode.appendChild(indicator);

    input.addEventListener('input', function () {
      var val = input.value;
      var strength = 0;

      if (val.length >= 6) strength++;
      if (val.length >= 10) strength++;
      if (/[A-Z]/.test(val)) strength++;
      if (/[0-9]/.test(val)) strength++;
      if (/[^A-Za-z0-9]/.test(val)) strength++;

      if (val.length === 0) {
        indicator.innerHTML = '';
        return;
      }

      var label, color;
      if (strength <= 1) {
        label = 'Weak';
        color = 'danger';
      } else if (strength <= 3) {
        label = 'Fair';
        color = 'warning';
      } else {
        label = 'Strong';
        color = 'success';
      }

      indicator.innerHTML = '<small class="text-' + color + '"><i class="bi bi-shield me-1"></i>Password strength: <strong>' + label + '</strong></small>';
    });
  }

  // Highlight active nav link
  var currentPath = window.location.pathname;
  var navLinks = document.querySelectorAll('.navbar-nav .nav-link');
  navLinks.forEach(function (link) {
    if (link.getAttribute('href') === currentPath) {
      link.classList.add('active');
    }
  });

});
