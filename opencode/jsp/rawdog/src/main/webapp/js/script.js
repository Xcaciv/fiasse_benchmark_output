document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            alert.style.transition = 'opacity 0.5s';
            alert.style.opacity = '0';
            setTimeout(function() {
                alert.remove();
            }, 500);
        }, 5000);
    });

    const deleteLinks = document.querySelectorAll('a[href*="action=delete"]');
    deleteLinks.forEach(function(link) {
        link.addEventListener('click', function(e) {
            if (!confirm('Are you sure you want to delete this item?')) {
                e.preventDefault();
            }
        });
    });

    const forms = document.querySelectorAll('form');
    forms.forEach(function(form) {
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.addEventListener('click', function() {
                this.disabled = true;
                this.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Processing...';
                setTimeout(() => {
                    this.disabled = false;
                }, 3000);
            });
        }
    });

    const passwordInputs = document.querySelectorAll('input[type="password"]');
    passwordInputs.forEach(function(input) {
        const toggleBtn = document.createElement('button');
        toggleBtn.type = 'button';
        toggleBtn.className = 'btn btn-sm btn-outline-secondary mt-2';
        toggleBtn.innerHTML = '<i class="fas fa-eye"></i> Show Password';
        toggleBtn.addEventListener('click', function() {
            if (input.type === 'password') {
                input.type = 'text';
                this.innerHTML = '<i class="fas fa-eye-slash"></i> Hide Password';
            } else {
                input.type = 'password';
                this.innerHTML = '<i class="fas fa-eye"></i> Show Password';
            }
        });
        input.parentNode.appendChild(toggleBtn);
    });
});

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(function() {
        alert('Copied to clipboard!');
    }, function(err) {
        console.error('Could not copy text: ', err);
    });
}
