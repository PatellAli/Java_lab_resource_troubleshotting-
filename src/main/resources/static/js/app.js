(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        setupLoadingStates();
        setupConfirmations();
        setupFilters();
        setupAlertAutoDismiss();
    });

    /* Disable the submit button and show progress text while a form posts. */
    function setupLoadingStates() {
        document.querySelectorAll("form[data-loading]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                var button = event.submitter || form.querySelector('button[type="submit"]');
                if (!button || button.dataset.loading === "true") {
                    return;
                }
                var original = button.dataset.originalText || button.textContent;
                button.dataset.originalText = original;
                button.dataset.loading = "true";
                button.disabled = true;
                if (button.dataset.loadingText) {
                    button.textContent = button.dataset.loadingText;
                }
            });
        });
    }

    /* Ask for confirmation on potentially destructive actions. */
    function setupConfirmations() {
        document.querySelectorAll("[data-confirm]").forEach(function (element) {
            element.addEventListener("click", function (event) {
                var message = element.getAttribute("data-confirm");
                if (message && !window.confirm(message)) {
                    event.preventDefault();
                    event.stopPropagation();
                }
            });
        });
    }

    /* Client-side quick filter for tables and card grids. */
    function setupFilters() {
        document.querySelectorAll("[data-filter-input]").forEach(function (input) {
            var target = document.getElementById(input.getAttribute("data-filter-input"));
            if (!target) {
                return;
            }
            input.addEventListener("input", function () {
                filterTarget(target, input.value);
            });
        });
    }

    function filterTarget(target, term) {
        var needle = term.trim().toLowerCase();
        var rows = target.tagName === "TABLE"
            ? target.querySelectorAll("tbody tr")
            : target.querySelectorAll(":scope > *");
        rows.forEach(function (row) {
            var matches = needle === "" || row.textContent.toLowerCase().indexOf(needle) !== -1;
            row.style.display = matches ? "" : "none";
        });
    }

    /* Fade out non-critical alerts after a short delay. */
    function setupAlertAutoDismiss() {
        document.querySelectorAll(".alert-success, .alert-info").forEach(function (alert) {
            window.setTimeout(function () {
                alert.style.transition = "opacity .4s ease";
                alert.style.opacity = "0";
                window.setTimeout(function () { alert.remove(); }, 400);
            }, 6000);
        });
    }
})();
