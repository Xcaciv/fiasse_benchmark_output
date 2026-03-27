// site.js — Minimal progressive enhancement. No sensitive operations.

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function () {
    setTimeout(function () {
        document.querySelectorAll('.alert.alert-success').forEach(function (el) {
            var bsAlert = bootstrap.Alert.getOrCreateInstance(el);
            bsAlert.close();
        });
    }, 5000);
});
