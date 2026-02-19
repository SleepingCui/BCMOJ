/**
 * Initializes dynamic content on the page after the DOM has loaded.
 * This function serves as a central point to call other initialization functions
 * that need to run when the page loads.
 */
function initializeDynamicContent() {
    updateCurrentYear();

}


function updateCurrentYear() {
    const yearElement = document.getElementById('currentYear');
    if (yearElement) {
        yearElement.textContent = new Date().getFullYear();
    }
}



// --- Page Load Event Handling ---
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeDynamicContent);
} else {
    initializeDynamicContent();
}