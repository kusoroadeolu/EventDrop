// EventDrop PWA Installation Handler
class EventDropPWA {
    constructor() {
        this.deferredPrompt = null;
        this.installButton = null;
        this.isInstalled = false;

        this.init();
    }

    init() {
        this.registerServiceWorker();
        this.setupInstallPrompt();
        this.checkInstallStatus();
        this.createInstallButton();
        this.handleAppUpdates();
    }

    async registerServiceWorker() {
        if ('serviceWorker' in navigator) {
            try {
                const registration = await navigator.serviceWorker.register('/sw.js', {
                    scope: '/'
                });

                console.log('EventDrop Service Worker registered:', registration);

                registration.addEventListener('updatefound', () => {
                    const newWorker = registration.installing;

                    newWorker.addEventListener('statechange', () => {
                        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                            this.showUpdateNotification();
                        }
                    });
                });

            } catch (error) {
                console.error('Service Worker registration failed:', error);
            }
        }
    }

    setupInstallPrompt() {
        window.addEventListener('beforeinstallprompt', (e) => {
            console.log('PWA install prompt available');
            e.preventDefault();
            this.deferredPrompt = e;
            this.showInstallButton();
        });

        window.addEventListener('appinstalled', () => {
            console.log('EventDrop PWA installed successfully');
            this.isInstalled = true;
            this.hideInstallButton();
            this.showInstallSuccess();
            this.deferredPrompt = null;
        });
    }

    checkInstallStatus() {
        if (window.matchMedia('(display-mode: standalone)').matches ||
            window.navigator.standalone === true) {
            this.isInstalled = true;
            console.log('EventDrop running in installed mode');
        }
    }

    createInstallButton() {
        // Only show install toast on landing page
        if (!document.querySelector('.hero')) return;

        this.addInstallButtonStyles();
        // Don't create button immediately, wait for install prompt
    }

    addInstallButtonStyles() {
        if (document.getElementById('pwa-install-styles')) return;

        const style = document.createElement('style');
        style.id = 'pwa-install-styles';
        style.textContent = `
            .install-toast {
                position: fixed;
                top: 0;
                left: 50%;
                transform: translateX(-50%) translateY(-100%);
                background: rgba(255, 255, 255, 0.95);
                backdrop-filter: blur(20px);
                border: 1px solid rgba(52, 152, 219, 0.2);
                border-radius: 0 0 16px 16px;
                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
                z-index: 10000;
                animation: slideDownIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
                max-width: 420px;
                width: calc(100% - 40px);
                font-family: 'MiSans', sans-serif;
            }

            .toast-content {
                display: flex;
                align-items: center;
                padding: 20px;
                gap: 16px;
            }

            .toast-icon {
                font-size: 24px;
                color: #3498db;
                background: rgba(52, 152, 219, 0.1);
                width: 48px;
                height: 48px;
                border-radius: 12px;
                display: flex;
                align-items: center;
                justify-content: center;
                flex-shrink: 0;
            }

            .toast-text {
                flex: 1;
            }

            .toast-title {
                font-weight: 600;
                color: #2c3e50;
                font-size: 16px;
                margin-bottom: 4px;
            }

            .toast-subtitle {
                color: #7f8c8d;
                font-size: 14px;
                line-height: 1.4;
            }

            .toast-actions {
                display: flex;
                gap: 8px;
                flex-shrink: 0;
            }

            .toast-btn {
                border: none;
                border-radius: 8px;
                font-family: 'MiSans', sans-serif;
                font-weight: 500;
                font-size: 14px;
                cursor: pointer;
                transition: all 0.2s ease;
                height: 36px;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            .toast-btn-primary {
                background: #3498db;
                color: white;
                padding: 0 16px;
            }

            .toast-btn-primary:hover {
                background: #2980b9;
                transform: translateY(-1px);
            }

            .toast-btn-secondary {
                background: rgba(127, 140, 141, 0.1);
                color: #7f8c8d;
                padding: 0 8px;
                width: 36px;
            }

            .toast-btn-secondary:hover {
                background: rgba(127, 140, 141, 0.2);
                color: #5a6c7d;
            }

            @keyframes slideDownIn {
                0% {
                    transform: translateX(-50%) translateY(-100%);
                    opacity: 0;
                }
                100% {
                    transform: translateX(-50%) translateY(0);
                    opacity: 1;
                }
            }

            @keyframes slideOutUp {
                0% {
                    transform: translateX(-50%) translateY(0);
                    opacity: 1;
                }
                100% {
                    transform: translateX(-50%) translateY(-100%);
                    opacity: 0;
                }
            }

            .notification {
                position: fixed;
                top: 20px;
                right: 20px;
                padding: 16px 20px;
                border-radius: 10px;
                color: white;
                font-family: 'MiSans', sans-serif;
                font-weight: 500;
                z-index: 10000;
                animation: slideInRight 0.3s ease;
                max-width: 350px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            }

            .notification-success {
                background: #2ecc71;
            }

            .notification-update {
                background: #3498db;
            }

            .notification-content {
                display: flex;
                align-items: center;
                gap: 10px;
            }

            .notification-close {
                background: none;
                border: none;
                color: white;
                font-size: 18px;
                cursor: pointer;
                margin-left: auto;
                opacity: 0.8;
            }

            .notification-close:hover {
                opacity: 1;
            }

            .update-btn {
                background: rgba(255, 255, 255, 0.2);
                border: 1px solid rgba(255, 255, 255, 0.3);
                color: white;
                padding: 6px 12px;
                border-radius: 20px;
                cursor: pointer;
                font-size: 12px;
                font-weight: 500;
                transition: all 0.2s ease;
            }

            .update-btn:hover {
                background: rgba(255, 255, 255, 0.3);
            }

            @keyframes slideInRight {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }

            @media (max-width: 768px) {
                .install-toast {
                    max-width: none;
                    width: calc(100% - 20px);
                    border-radius: 0 0 12px 12px;
                }

                .toast-content {
                    padding: 16px;
                    gap: 12px;
                }

                .toast-icon {
                    width: 40px;
                    height: 40px;
                    font-size: 20px;
                }

                .toast-title {
                    font-size: 15px;
                }

                .toast-subtitle {
                    font-size: 13px;
                }

                .notification {
                    top: 10px;
                    left: 10px;
                    right: 10px;
                    max-width: none;
                }
            }
        `;
        document.head.appendChild(style);
    }

    showInstallButton() {
        if (!this.isInstalled && document.querySelector('.hero')) {
            this.showInstallToast();
        }
    }

    hideInstallButton() {
        const toast = document.getElementById('pwa-install-toast');
        if (toast) {
            toast.style.animation = 'slideOutUp 0.4s ease-in-out forwards';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.remove();
                }
            }, 400);
        }
    }

    showInstallToast() {
        // Don't show if already exists or installed
        if (document.getElementById('pwa-install-toast') || this.isInstalled) return;

        const toast = document.createElement('div');
        toast.id = 'pwa-install-toast';
        toast.className = 'install-toast';
        toast.innerHTML = `
            <div class="toast-content">
                <div class="toast-icon">
                    <i class="ri-cloud-line"></i>
                </div>
                <div class="toast-text">
                    <div class="toast-title">Install EventDrop</div>
                    <div class="toast-subtitle">Get faster access and offline features</div>
                </div>
                <div class="toast-actions">
                    <button class="toast-btn toast-btn-primary" onclick="eventDropPWAInstance.promptInstall()">
                        Install
                    </button>
                    <button class="toast-btn toast-btn-secondary" onclick="eventDropPWAInstance.dismissInstallToast()">
                        <i class="ri-close-line"></i>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(toast);

        // Store reference for global access
        window.eventDropPWAInstance = this;

        // Auto-dismiss after 8 seconds
        setTimeout(() => {
            this.dismissInstallToast();
        }, 8000);
    }

    dismissInstallToast() {
        this.hideInstallButton();
    }

    async promptInstall() {
        if (!this.deferredPrompt) {
            console.log('No install prompt available');
            return;
        }

        this.deferredPrompt.prompt();
        const { outcome } = await this.deferredPrompt.userChoice;

        console.log(`Install prompt result: ${outcome}`);

        this.deferredPrompt = null;
        this.hideInstallButton();
    }

    showInstallSuccess() {
        this.showNotification(
            'notification-success',
            '<i class="ri-check-line"></i><span>EventDrop installed successfully!</span>'
        );
    }

    showUpdateNotification() {
        this.showNotification(
            'notification-update',
            `<i class="ri-refresh-line"></i>
             <span>New version available!</span>
             <button class="update-btn" onclick="window.location.reload()">Update</button>`
        );
    }

    showNotification(className, content) {
        const notification = document.createElement('div');
        notification.className = `notification ${className}`;
        notification.innerHTML = `
            <div class="notification-content">
                ${content}
                <button class="notification-close">&times;</button>
            </div>
        `;

        document.body.appendChild(notification);

        notification.querySelector('.notification-close').addEventListener('click', () => {
            notification.remove();
        });

        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    }

    handleAppUpdates() {
        let updateCheckInterval;

        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                updateCheckInterval = setInterval(async () => {
                    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
                        const registration = await navigator.serviceWorker.getRegistration();
                        if (registration) {
                            await registration.update();
                        }
                    }
                }, 30000);
            } else {
                clearInterval(updateCheckInterval);
            }
        });
    }
}

// Initialize when DOM loads
document.addEventListener('DOMContentLoaded', () => {
    new EventDropPWA();
});

window.EventDropPWA = EventDropPWA;