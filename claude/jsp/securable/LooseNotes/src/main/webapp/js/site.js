/**
 * Loose Notes - Site JavaScript
 * SSEM: Integrity - minimal JavaScript, no inline eval.
 * SSEM: Authenticity - CSRF token injected into AJAX requests via meta tag.
 */
'use strict';

/**
 * Copies the share URL to the clipboard.
 */
function copyShareUrl() {
    const input = document.getElementById('shareUrl');
    if (!input) return;
    navigator.clipboard.writeText(input.value).then(() => {
        showToast('Share link copied to clipboard!');
    }).catch(() => {
        input.select();
        document.execCommand('copy');
        showToast('Share link copied!');
    });
}

/**
 * Shows a brief toast notification.
 */
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'position-fixed bottom-0 end-0 p-3';
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <div class="toast show" role="alert">
            <div class="toast-body bg-dark text-white rounded">
                ${escapeHtml(message)}
            </div>
        </div>`;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

/**
 * Escapes HTML special characters to prevent XSS in dynamically created elements.
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

/**
 * Reads the CSRF token from the meta tag for AJAX requests.
 */
function getCsrfToken() {
    const meta = document.querySelector('meta[name="csrf-token"]');
    return meta ? meta.getAttribute('content') : '';
}

/**
 * Confirm delete with native dialog (accessible, no custom JS needed).
 * Forms that need confirmation should call this from onsubmit.
 */
function confirmDelete(event) {
    if (!confirm('Are you sure you want to delete this? This cannot be undone.')) {
        event.preventDefault();
        return false;
    }
    return true;
}

// Password confirmation validation
document.addEventListener('DOMContentLoaded', function () {
    const confirmPwd = document.getElementById('confirmPassword');
    const newPwd = document.getElementById('newPassword') || document.getElementById('password');
    if (confirmPwd && newPwd) {
        confirmPwd.addEventListener('input', function () {
            if (confirmPwd.value !== newPwd.value) {
                confirmPwd.setCustomValidity('Passwords do not match');
            } else {
                confirmPwd.setCustomValidity('');
            }
        });
    }
});
