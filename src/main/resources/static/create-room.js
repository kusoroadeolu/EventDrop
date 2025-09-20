/**
 * Handles form submissions for creating and joining rooms, including
 * input validation, API requests, error handling, and cookie management.
 */
document.addEventListener('DOMContentLoaded', () => {
    // Dynamically get the base URL for the API.
    const API_BASE_URL = window.location.origin;

    // --- Element Selectors ---
    // Create Room Form Elements
    const createForm = document.getElementById('create-form');
    const createRoomNameInput = document.getElementById('create-room-name');
    const roomTtlInput = document.getElementById('room-ttl');
    const createUsernameInput = document.getElementById('create-username');
    const createSubmitButton = document.getElementById('create-submit');

    // Create Room Error Spans
    const createRoomNameError = document.getElementById('create-room-name-error');
    const roomTtlError = document.getElementById('room-ttl-error');
    const createUsernameError = document.getElementById('create-username-error');

    // Join Room Form Elements
    const joinForm = document.getElementById('join-form');
    const joinUsernameInput = document.getElementById('join-username');
    const roomCodeInput = document.getElementById('room-code');
    const joinSubmitButton = document.getElementById('join-submit');

    // Join Room Error Spans
    const joinUsernameError = document.getElementById('join-username-error');
    const roomCodeError = document.getElementById('room-code-error');

    // --- Utility Functions ---

    /**
     * Sets a cookie with a specified name, value, and expiration days.
     * @param {string} name - The name of the cookie.
     * @param {string} value - The value of the cookie.
     * @param {number} days - The number of days until the cookie expires.
     */
    const setCookie = (name, value, days = 7) => {
        let expires = "";
        if (days) {
            const date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = "; expires=" + date.toUTCString();
        }
        // Set path to '/' to make the cookie available site-wide.
        document.cookie = name + "=" + (value || "") + expires + "; path=/";
        console.log(`Cookie set: ${name}=${value}`);
    };

    /**
     * Displays an error message for a specific form field.
     * @param {HTMLElement} errorElement - The span element to display the error.
     * @param {string} message - The error message to show.
     */
    const showError = (errorElement, message) => {
        errorElement.textContent = message;
        errorElement.classList.add('visible');
    };

    /**
     * Hides all error messages in a given form.
     * @param {HTMLElement} form - The form element.
     */
    const hideAllErrors = (form) => {
        form.querySelectorAll('.error-message').forEach(el => el.classList.remove('visible'));
    };

    // --- Validation Logic ---
    const validateCreateForm = () => {
        const isRoomNameValid = createRoomNameInput.checkValidity();
        const isTtlValid = roomTtlInput.checkValidity();
        const isUsernameValid = createUsernameInput.checkValidity();
        createSubmitButton.disabled = !(isRoomNameValid && isTtlValid && isUsernameValid);
    };

    const validateJoinForm = () => {
        const isUsernameValid = joinUsernameInput.checkValidity();
        const isRoomCodeValid = roomCodeInput.checkValidity();
        joinSubmitButton.disabled = !(isUsernameValid && isRoomCodeValid);
    };

    // --- Event Listeners ---

    // Add real-time validation listeners
    createForm.addEventListener('input', validateCreateForm);
    joinForm.addEventListener('input', validateJoinForm);

    /**
     * Handles the 'Create Room' form submission.
     */
    createForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        hideAllErrors(createForm);
        createSubmitButton.disabled = true;
        createSubmitButton.textContent = 'Creating...';

        const roomData = {
            roomName: createRoomNameInput.value,
            ttl: parseInt(roomTtlInput.value, 10),
            username: createUsernameInput.value
        };

        try {
            const response = await fetch(`${API_BASE_URL}/rooms/create`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(roomData)
            });

            const responseData = await response.json();
            console.log('Create Room Response:', responseData);

            if (!response.ok) {
                // Assuming the backend returns a specific error message format
                showError(createRoomNameError, responseData.message || 'An unexpected error occurred.');
                throw new Error(`Server error: ${response.status}`);
            }

            // On success (201 CREATED)
            window.location.href = 'rooms.html'; // Redirect to the room page

        } catch (error) {
            console.error('Failed to create room:', error);
            showError(createRoomNameError, 'Could not connect to the server. Please try again.');
        } finally {
            createSubmitButton.disabled = false;
            createSubmitButton.textContent = 'Create Room';
        }
    });

    /**
     * Handles the 'Join Room' form submission.
     */
    joinForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        hideAllErrors(joinForm);
        joinSubmitButton.disabled = true;
        joinSubmitButton.textContent = 'Joining...';

        const joinData = {
            username: joinUsernameInput.value,
            roomCode: roomCodeInput.value
        };

        try {
            const response = await fetch(`${API_BASE_URL}/rooms/join`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(joinData)
            });

            const responseData = await response.json();
            console.log('Join Room Response:', responseData);

            if (!response.ok) {
                 // Handle specific backend errors
                if (response.status === 404) {
                    showError(roomCodeError, 'Room not found. Please check the code.');
                } else if (response.status === 409) {
                    showError(roomCodeError, 'Room is full. Cannot join.');
                } else {
                    showError(roomCodeError, responseData.message || 'An unexpected error occurred.');
                }

                throw new Error(`Server error: ${response.status}`);
            }

            // On success (200 OK)
            window.location.href = 'rooms.html'; // Redirect to the room page

        } catch (error) {
            console.error('Failed to join room:', error);
            if (!roomCodeError.classList.contains('visible')) {
               showError(roomCodeError, 'Could not connect to the server. Please try again.');
            }
        } finally {
            joinSubmitButton.disabled = false;
            joinSubmitButton.textContent = 'Join Room';
        }
    });
});