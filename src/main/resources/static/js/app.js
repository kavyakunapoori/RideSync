/**
 * Smart Carpool and Ride-Sharing System
 * Core UI interactions and dynamic calculations
 */

document.addEventListener("DOMContentLoaded", function () {
    // Auto-dismiss alerts after 6 seconds
    setTimeout(function () {
        const alerts = document.querySelectorAll(".alert");
        alerts.forEach(function (alert) {
            alert.style.transition = "opacity 0.5s ease";
            alert.style.opacity = "0";
            setTimeout(function () {
                if (alert.parentNode) {
                    alert.parentNode.removeChild(alert);
                }
            }, 500);
        });
    }, 6000);

    // Dynamic Fare Calculator for Ride Creation
    initFareCalculator();
});

/**
 * Helper to auto-fill demo accounts on the login page for rapid testing
 */
function fillLogin(role) {
    const emailField = document.getElementById("email");
    const passwordField = document.getElementById("password");
    if (!emailField || !passwordField) return;

    if (role === "driver") {
        emailField.value = "driver@carpool.com";
        passwordField.value = "Driver123!";
    } else if (role === "passenger") {
        emailField.value = "passenger@carpool.com";
        passwordField.value = "Passenger123!";
    } else if (role === "admin") {
        emailField.value = "admin@carpool.com";
        passwordField.value = "Admin123!";
    }
}

/**
 * Real-time dynamic recalculation of passenger fare split
 */
function initFareCalculator() {
    const totalCostInput = document.getElementById("totalTripCost");
    const driverPercentInput = document.getElementById("driverContributionPercent");
    const seatsInput = document.getElementById("totalSeats");
    const costPreview = document.getElementById("calculatedSeatCost");
    const driverPayPreview = document.getElementById("calculatedDriverContribution");

    if (!totalCostInput || !driverPercentInput || !seatsInput || !costPreview) {
        return;
    }

    function recalculate() {
        const total = parseFloat(totalCostInput.value) || 0;
        const driverPercent = parseFloat(driverPercentInput.value) || 0;
        const seats = parseInt(seatsInput.value, 10) || 1;

        const driverShare = (total * (driverPercent / 100.0));
        const passengerPool = total - driverShare;
        const perSeat = seats > 0 ? (passengerPool / seats) : passengerPool;

        costPreview.textContent = "$" + perSeat.toFixed(2);
        if (driverPayPreview) {
            driverPayPreview.textContent = "$" + driverShare.toFixed(2);
        }
    }

    totalCostInput.addEventListener("input", recalculate);
    driverPercentInput.addEventListener("input", recalculate);
    seatsInput.addEventListener("input", recalculate);
    recalculate();
}
