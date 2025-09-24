/**
 * Mobile-specific functionality for rooms page
 * Handles mobile UI optimizations
 */
class RoomsMobileManager {
    constructor() {
        this.init();
    }

    init() {
        this.addMobileStyles();
        this.attachEventListeners();
        this.updateMobileRoomInfo(); // Run once on load
        console.log('Mobile manager initialized for UI optimizations');
    }

    attachEventListeners() {
        // Handle window resize for UI adjustments
        window.addEventListener('resize', () => {
            this.updateMobileRoomInfo();
        });
    }

    /**
     * Add mobile-specific styles programmatically.
     * These are general improvements, not card-specific.
     */
    addMobileStyles() {
        const style = document.createElement('style');
        style.textContent = `
            /* Improve touch targets on mobile */
            @media (max-width: 480px) {
                .btn-icon {
                    min-width: 44px;
                    min-height: 44px;
                }

                .copy-btn {
                    min-width: 44px;
                    min-height: 44px;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                }

                /* Better mobile notifications */
                .notification {
                    margin-bottom: 8px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.15);
                }

                /* Prevent horizontal scroll on very small screens */
                .room-detail {
                    min-width: 0;
                    overflow: hidden;
                }

                #room-code-text {
                    max-width: 100px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }
        `;
        document.head.appendChild(style);
    }

    /**
     * Handle mobile-specific room info updates like text truncation.
     */
    updateMobileRoomInfo() {
        const roomCodeText = document.querySelector('#room-code-text');
        if (roomCodeText && window.innerWidth <= 480) {
            const text = roomCodeText.textContent;
            if (text.length > 8) {
                roomCodeText.title = text; // Show full text on hover
            }
        }
    }
}

// Initialize mobile manager when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.roomsMobileManager = new RoomsMobileManager();
});