/**
 * This script handles the functionality for the EventDrop landing page.
 * It fetches metrics from the API, animates the stat counters,
 * and handles the navigation to the room creation page.
 */
document.addEventListener('DOMContentLoaded', () => {
    // Dynamically get the base URL for the API from the current window location.
    // This works for both local development (http://localhost:8080) and deployed domains.
    const API_BASE_URL = window.location.origin;

    const createButton = document.querySelector('.btn-primary');
    const roomsCounter = document.querySelector('.stat-box:nth-child(1) .stat-number');
    const uploadedCounter = document.querySelector('.stat-box:nth-child(2) .stat-number');
    const downloadedCounter = document.querySelector('.stat-box:nth-child(3) .stat-number');

    /**
     * Fetches the latest metrics from the backend API.
     */
    async function fetchMetrics() {
        try {
            const response = await fetch(`${API_BASE_URL}/metrics`);
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            const metrics = await response.json();
            updateMetricsUI(metrics);
        } catch (error) {
            console.error("Failed to fetch metrics:", error);
            // Keep default values if API fails
            animateCounters();
        }
    }

    /**
     * Updates the data attributes in the HTML with the fetched metric values.
     * @param {object} metrics - The metrics object from the API.
     */
    function updateMetricsUI(metrics) {
        if (roomsCounter) {
            roomsCounter.setAttribute('data-count', metrics.totalRoomsCreated);
        }
        if (uploadedCounter) {
            uploadedCounter.setAttribute('data-count', metrics.totalFilesUploaded);
        }
        if (downloadedCounter) {
            downloadedCounter.setAttribute('data-count', metrics.totalFilesDownloaded);
        }
        // After updating the data, trigger the animation.
        animateCounters();
    }

    /**
     * Animates the stat counters when they scroll into view.
     */
    function animateCounters() {
        const counters = document.querySelectorAll('.stat-number');
        if (counters.length === 0) return;

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const counter = entry.target;
                    const countTo = parseInt(counter.getAttribute('data-count'), 10) || 0;
                    const span = counter.querySelector('span');

                    counter.classList.add('animated'); // Make it visible

                    let currentCount = 0;
                    const updateCount = () => {
                        const increment = Math.ceil(countTo / 100); // Animation speed
                        if (currentCount < countTo) {
                            currentCount = Math.min(currentCount + increment, countTo);
                            span.textContent = currentCount.toLocaleString();
                            requestAnimationFrame(updateCount);
                        } else {
                            span.textContent = countTo.toLocaleString();
                        }
                    };
                    updateCount();
                    observer.unobserve(counter); // Stop observing once animated
                }
            });
        }, { threshold: 0.5 });

        counters.forEach(counter => {
            observer.observe(counter);
        });
    }

    // Add event listener for the redirect button.
    if (createButton) {
        createButton.addEventListener('click', (event) => {
            event.preventDefault(); // Prevent default link behavior
            window.location.href = 'create.html';
        });
    }

    // Initial fetch of metrics when the page loads.
    fetchMetrics();
});