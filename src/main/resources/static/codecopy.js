document.addEventListener('DOMContentLoaded', () => {
        // --- Copy to Clipboard Functionality ---
        const copyButton = document.getElementById('copy-room-code-btn');
        const roomCodeTextElement = document.getElementById('room-code-text');

        if (copyButton && roomCodeTextElement) {
            copyButton.addEventListener('click', () => {
                const textToCopy = roomCodeTextElement.textContent;
                if (!textToCopy) return;

                navigator.clipboard.writeText(textToCopy).then(() => {
                    // Success feedback
                    const icon = copyButton.querySelector('i');
                    const originalIconClass = 'ri-file-copy-line';
                    const successIconClass = 'ri-check-line';

                    // Prevent multiple clicks while in "copied" state
                    copyButton.disabled = true;

                    icon.classList.remove(originalIconClass);
                    icon.classList.add(successIconClass);
                    copyButton.title = 'Copied!';

                    // Revert back after 2 seconds
                    setTimeout(() => {
                        icon.classList.remove(successIconClass);
                        icon.classList.add(originalIconClass);
                        copyButton.title = 'Copy room code';
                        copyButton.disabled = false;
                    }, 1000);

                }).catch(err => {
                    console.error('Failed to copy room code: ', err);
                    alert('Failed to copy room code.'); // Fallback for user
                });
            });
        }
    });