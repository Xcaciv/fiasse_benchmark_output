// Loose Notes - Application JavaScript

// Delete confirmation
function confirmDelete() {
    return confirm('Are you sure you want to delete this item? This action cannot be undone.');
}

// CSRF token helper for fetch requests
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : null;
}

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert-success');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            alert.style.opacity = '0';
            alert.style.transition = 'opacity 0.5s';
            setTimeout(function() { alert.remove(); }, 500);
        }, 5000);
    });

    // Character counter for note content
    const contentArea = document.getElementById('content');
    if (contentArea) {
        const counter = document.createElement('small');
        counter.style.color = '#888';
        contentArea.parentNode.appendChild(counter);
        function updateCounter() {
            const remaining = 50000 - contentArea.value.length;
            counter.textContent = remaining + ' characters remaining';
            counter.style.color = remaining < 500 ? '#e74c3c' : '#888';
        }
        contentArea.addEventListener('input', updateCounter);
        updateCounter();
    }

    // Character counter for title
    const titleInput = document.getElementById('title');
    if (titleInput) {
        const counter = document.createElement('small');
        counter.style.color = '#888';
        titleInput.parentNode.appendChild(counter);
        function updateTitleCounter() {
            const remaining = 200 - titleInput.value.length;
            counter.textContent = remaining + ' characters remaining';
        }
        titleInput.addEventListener('input', updateTitleCounter);
        updateTitleCounter();
    }
});
