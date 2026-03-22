// Loose Notes – minimal client JS
// No inline event handlers; all interactions are form-based (server-driven) to avoid XSS surface.

(function () {
    'use strict';
    // Auto-dismiss success alerts after 5 seconds
    document.querySelectorAll('.alert-success').forEach(function (el) {
        setTimeout(function () {
            var bsAlert = bootstrap.Alert.getOrCreateInstance(el);
            bsAlert.close();
        }, 5000);
    });
}());
