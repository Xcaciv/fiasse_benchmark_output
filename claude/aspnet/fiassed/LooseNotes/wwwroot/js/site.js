// Loose Notes - Site JavaScript
// Show/hide password toggle (ASVS V6.2.6 - password show/hide toggle permitted)
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.btn-toggle-password').forEach(function (button) {
        button.addEventListener('click', function () {
            var field = this.previousElementSibling;
            if (!field) return;
            if (field.type === 'password') {
                field.type = 'text';
                this.textContent = 'Hide';
            } else {
                field.type = 'password';
                this.textContent = 'Show';
            }
        });
    });
});
// NOTE: paste is NOT prevented anywhere in this file.
// Blocking paste in password fields is explicitly prohibited (ASVS V6.2.7).
