// Authentication Pages JavaScript - Register Form Validation

/**
 * Initialize register form validation
 * This function sets up password validation and matching checks
 */
function initializeRegisterValidation() {
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const lengthRequirement = document.getElementById('lengthRequirement');
    const passwordMatch = document.getElementById('passwordMatch');
    const submitBtn = document.getElementById('submitBtn');
    const form = document.getElementById('registrationForm');

    // Check if we're on the register page
    if (!passwordInput || !confirmPasswordInput || !form) {
        return; // Exit if elements don't exist (not on register page)
    }

    /**
     * Validate password length requirement
     */
    passwordInput.addEventListener('input', function() {
        const password = this.value;

        // Check length requirement
        if (password.length >= 6) {
            lengthRequirement.classList.add('valid');
            lengthRequirement.innerHTML = '<i class="fa fa-check-circle"></i> At least 6 characters';
        } else {
            lengthRequirement.classList.remove('valid');
            lengthRequirement.innerHTML = '<i class="fa fa-times-circle"></i> At least 6 characters';
        }

        // Check if passwords match
        checkPasswordMatch();
    });

    /**
     * Check password match on confirm password input
     */
    confirmPasswordInput.addEventListener('input', checkPasswordMatch);

    /**
     * Check if password and confirm password match
     */
    function checkPasswordMatch() {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        if (confirmPassword.length > 0) {
            if (password !== confirmPassword) {
                passwordMatch.style.display = 'block';
                passwordMatch.classList.remove('valid');
                passwordMatch.innerHTML = '<i class="fa fa-times-circle"></i> Passwords do not match';
            } else {
                passwordMatch.style.display = 'block';
                passwordMatch.classList.add('valid');
                passwordMatch.innerHTML = '<i class="fa fa-check-circle"></i> Passwords match';
            }
        } else {
            passwordMatch.style.display = 'none';
        }
    }

    /**
     * Validate form on submit
     */
    form.addEventListener('submit', function(e) {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        // Check password length
        if (password.length < 6) {
            e.preventDefault();
            alert('Password must be at least 6 characters long.');
            return false;
        }

        // Check if passwords match
        if (password !== confirmPassword) {
            e.preventDefault();
            alert('Passwords do not match. Please try again.');
            return false;
        }
    });
}

/**
 * Initialize authentication pages functionality
 */
function initializeAuthPages() {
    // Initialize register form validation if on register page
    initializeRegisterValidation();

    // Add body class for register page if needed
    const isRegisterPage = document.getElementById('registrationForm');
    if (isRegisterPage) {
        document.body.classList.add('register-page');
    }

    // Auto-hide success/error messages after 5 seconds
    const alerts = document.querySelectorAll('.text-success, .text-danger');
    alerts.forEach(alert => {
        if (alert.textContent.trim()) {
            setTimeout(() => {
                alert.style.transition = 'opacity 0.5s ease';
                alert.style.opacity = '0';
                setTimeout(() => {
                    alert.style.display = 'none';
                }, 500);
            }, 5000);
        }
    });
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', initializeAuthPages);