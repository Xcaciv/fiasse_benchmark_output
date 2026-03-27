// Loose Notes - Site JavaScript

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function () {
    const alerts = document.querySelectorAll('.alert.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        }, 5000);
    });
});

// Confirm delete forms
document.addEventListener('DOMContentLoaded', function () {
    const deleteForms = document.querySelectorAll('form[data-confirm]');
    deleteForms.forEach(function (form) {
        form.addEventListener('submit', function (e) {
            const msg = form.getAttribute('data-confirm') || 'Are you sure?';
            if (!confirm(msg)) {
                e.preventDefault();
            }
        });
    });
});

// Password confirmation validation
document.addEventListener('DOMContentLoaded', function () {
    const newPass = document.getElementById('newPassword');
    const confirmPass = document.getElementById('confirmPassword');

    if (newPass && confirmPass) {
        function checkMatch() {
            if (newPass.value && confirmPass.value && newPass.value !== confirmPass.value) {
                confirmPass.setCustomValidity('Passwords do not match');
            } else {
                confirmPass.setCustomValidity('');
            }
        }
        newPass.addEventListener('input', checkMatch);
        confirmPass.addEventListener('input', checkMatch);
    }
});
