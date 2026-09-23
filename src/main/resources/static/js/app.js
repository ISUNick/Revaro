// Small behaviors shared by every page

// Buttons with data-confirm ask before submitting their form
document.addEventListener('click', function (e) {
    const button = e.target.closest('[data-confirm]');
    if (button && !window.confirm(button.dataset.confirm)) {
        e.preventDefault();
    }
});

// Forms with data-submit-once can't be double submitted. A double click on sign in used to
// send a stale CSRF token on the second request.
document.addEventListener('submit', function (e) {
    const form = e.target;
    if (!form.hasAttribute('data-submit-once')) {
        return;
    }
    if (form.dataset.submitted) {
        e.preventDefault();
        return;
    }
    form.dataset.submitted = 'true';
    // Disable after the browser has collected the form data
    setTimeout(function () {
        form.querySelectorAll('button[type="submit"]').forEach(function (button) {
            button.disabled = true;
        });
    });
});
