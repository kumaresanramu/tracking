/**
 * App Update Manager
 * Handles app update detection, notifications, and automatic cache updates
 * Requirements: 3.4, 6.5
 */
class AppUpdateManager {
    constructor() {
        this.serviceWorkerRegistration = null;
        this.newWorkerWaiting = null;
        this.refreshing = false;
        this.updateAvailable = false;
        this.updateCheckInterval = 60000; // Check every minute
        this.lastUpdateCheck = 0;
        this.updateCheckTimer = null;
        
        this.init();
    }

    async init() {
        if ('serviceWorker' in navigator) {
            try {
                // Wait for service worker to be ready
                this.serviceWorkerRegistration = await navigator.serviceWorker.ready;
                this.setupServiceWorkerListeners();
                this.createUpdateUI();
                this.startUpdateChecking();
                
                console.log('App Update Manager initialized');
            } catch (error) {
                console.error('Failed to initialize App Update Manager:', error);
            }
        } else {
            console.warn('Service Worker not supported, update detection disabled');
        }
    }

    setupServiceWorkerListeners() {
        // Listen for service worker updates
        navigator.serviceWorker.addEventListener('controllerchange', () => {
            if (this.refreshing) return;
            console.log('App Update: New service worker took control, reloading page');
            this.refreshing = true;
            window.location.reload();
        });

        // Check for waiting service worker
        if (this.serviceWorkerRegistration.waiting) {
            console.log('App Update: Service worker waiting');
            this.newWorkerWaiting = this.serviceWorkerRegistration.waiting;
            this.showUpdateNotification();
        }

        // Listen for new service worker installing
        this.serviceWorkerRegistration.addEventListener('updatefound', () => {
            console.log('App Update: New service worker installing');
            const newWorker = this.serviceWorkerRegistration.installing;
            
            newWorker.addEventListener('statechange', () => {
                console.log('App Update: Service worker state changed to:', newWorker.state);
                
                if (newWorker.state === 'installed') {
                    if (navigator.serviceWorker.controller) {
                        // New service worker installed, update available
                        console.log('App Update: New content available');
                        this.newWorkerWaiting = newWorker;
                        this.updateAvailable = true;
                        this.showUpdateNotification();
                    } else {
                        // First time install
                        console.log('App Update: Content cached for offline use');
                        this.showCacheReadyNotification();
                    }
                }
            });
        });

        // Listen for messages from service worker
        navigator.serviceWorker.addEventListener('message', (event) => {
            if (event.data && event.data.type === 'UPDATE_AVAILABLE') {
                console.log('App Update: Update message received from service worker');
                this.updateAvailable = true;
                this.showUpdateNotification();
            }
        });
    }

    startUpdateChecking() {
        // Check for updates immediately
        this.checkForUpdates();
        
        // Set up periodic update checking
        this.updateCheckTimer = setInterval(() => {
            this.checkForUpdates();
        }, this.updateCheckInterval);

        // Check for updates when page becomes visible
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                const timeSinceLastCheck = Date.now() - this.lastUpdateCheck;
                if (timeSinceLastCheck > this.updateCheckInterval) {
                    this.checkForUpdates();
                }
            }
        });

        // Check for updates on network reconnection
        window.addEventListener('online', () => {
            setTimeout(() => this.checkForUpdates(), 1000);
        });
    }

    async checkForUpdates() {
        if (!this.serviceWorkerRegistration) return;

        try {
            console.log('App Update: Checking for updates...');
            this.lastUpdateCheck = Date.now();
            
            // Trigger service worker update check
            await this.serviceWorkerRegistration.update();
            
            // Also check version from server
            await this.checkVersionFromServer();
            
        } catch (error) {
            console.error('App Update: Failed to check for updates:', error);
        }
    }

    async checkVersionFromServer() {
        try {
            // Check if there's a version endpoint or use cache-busting
            const response = await fetch('/manifest.json?' + Date.now());
            const manifest = await response.json();
            
            // You could add a version field to manifest.json or check other indicators
            const serverVersion = manifest.version || manifest.short_name;
            const currentVersion = localStorage.getItem('app-version');
            
            if (currentVersion && serverVersion !== currentVersion) {
                console.log('App Update: Version mismatch detected', {
                    current: currentVersion,
                    server: serverVersion
                });
                this.updateAvailable = true;
                this.showUpdateNotification();
            }
            
            localStorage.setItem('app-version', serverVersion);
            
        } catch (error) {
            console.error('App Update: Failed to check server version:', error);
        }
    }

    createUpdateUI() {
        // Create update notification banner
        const updateBanner = document.createElement('div');
        updateBanner.id = 'app-update-banner';
        updateBanner.className = 'app-update-banner hidden';
        updateBanner.innerHTML = `
            <div class="update-banner-content">
                <div class="update-banner-icon">🔄</div>
                <div class="update-banner-text">
                    <h3>Update Available</h3>
                    <p>A new version of the app is ready</p>
                </div>
                <div class="update-banner-actions">
                    <button id="app-update-btn" class="btn btn-primary btn-small">Update Now</button>
                    <button id="app-update-dismiss-btn" class="btn btn-secondary btn-small">Later</button>
                </div>
            </div>
        `;

        // Create cache ready notification
        const cacheReadyBanner = document.createElement('div');
        cacheReadyBanner.id = 'cache-ready-banner';
        cacheReadyBanner.className = 'cache-ready-banner hidden';
        cacheReadyBanner.innerHTML = `
            <div class="cache-banner-content">
                <div class="cache-banner-icon">✅</div>
                <div class="cache-banner-text">
                    <h3>Ready for Offline</h3>
                    <p>App cached and ready to work offline</p>
                </div>
                <div class="cache-banner-actions">
                    <button id="cache-ready-dismiss-btn" class="btn btn-primary btn-small">Got it</button>
                </div>
            </div>
        `;

        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .app-update-banner,
            .cache-ready-banner {
                position: fixed;
                top: 20px;
                left: 20px;
                right: 20px;
                background: var(--card-background, #ffffff);
                border: 1px solid var(--border-color, #e0e0e0);
                border-radius: 12px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
                z-index: 1000;
                transform: translateY(-100px);
                opacity: 0;
                transition: all 0.3s ease;
                max-width: 500px;
                margin: 0 auto;
            }

            .app-update-banner.show,
            .cache-ready-banner.show {
                transform: translateY(0);
                opacity: 1;
            }

            .app-update-banner.hidden,
            .cache-ready-banner.hidden {
                display: none;
            }

            .update-banner-content,
            .cache-banner-content {
                display: flex;
                align-items: center;
                padding: 16px;
                gap: 12px;
            }

            .update-banner-icon,
            .cache-banner-icon {
                font-size: 24px;
                flex-shrink: 0;
            }

            .update-banner-text,
            .cache-banner-text {
                flex: 1;
                min-width: 0;
            }

            .update-banner-text h3,
            .cache-banner-text h3 {
                margin: 0 0 4px 0;
                font-size: 16px;
                font-weight: 600;
                color: var(--text-primary, #333);
            }

            .update-banner-text p,
            .cache-banner-text p {
                margin: 0;
                font-size: 14px;
                color: var(--text-secondary, #666);
            }

            .update-banner-actions,
            .cache-banner-actions {
                display: flex;
                gap: 8px;
                flex-shrink: 0;
            }

            .update-banner-actions .btn,
            .cache-banner-actions .btn {
                padding: 8px 16px;
                font-size: 14px;
                border-radius: 6px;
                border: none;
                cursor: pointer;
                font-weight: 500;
                transition: all 0.2s ease;
            }

            .update-banner-actions .btn-primary,
            .cache-banner-actions .btn-primary {
                background: var(--primary-color, #2196F3);
                color: white;
            }

            .update-banner-actions .btn-primary:hover,
            .cache-banner-actions .btn-primary:hover {
                background: var(--primary-hover, #1976D2);
            }

            .update-banner-actions .btn-secondary {
                background: transparent;
                color: var(--text-secondary, #666);
                border: 1px solid var(--border-color, #e0e0e0);
            }

            .update-banner-actions .btn-secondary:hover {
                background: var(--hover-background, #f5f5f5);
            }

            /* Update progress indicator */
            .update-progress {
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                height: 3px;
                background: var(--primary-color, #2196F3);
                transform: scaleX(0);
                transform-origin: left;
                transition: transform 0.3s ease;
                z-index: 1001;
            }

            .update-progress.show {
                animation: updateProgress 2s ease-in-out;
            }

            @keyframes updateProgress {
                0% { transform: scaleX(0); }
                50% { transform: scaleX(0.7); }
                100% { transform: scaleX(1); }
            }

            @media (max-width: 480px) {
                .app-update-banner,
                .cache-ready-banner {
                    left: 10px;
                    right: 10px;
                    top: 10px;
                }

                .update-banner-content,
                .cache-banner-content {
                    padding: 12px;
                }

                .update-banner-actions,
                .cache-banner-actions {
                    flex-direction: column;
                    width: 80px;
                }

                .update-banner-actions .btn,
                .cache-banner-actions .btn {
                    padding: 6px 12px;
                    font-size: 12px;
                }
            }
        `;

        // Add to document
        document.head.appendChild(style);
        document.body.appendChild(updateBanner);
        document.body.appendChild(cacheReadyBanner);

        // Create progress indicator
        const progressBar = document.createElement('div');
        progressBar.id = 'update-progress';
        progressBar.className = 'update-progress';
        document.body.appendChild(progressBar);

        // Set up event listeners
        document.getElementById('app-update-btn').addEventListener('click', () => {
            this.handleUpdateClick();
        });

        document.getElementById('app-update-dismiss-btn').addEventListener('click', () => {
            this.handleUpdateDismiss();
        });

        document.getElementById('cache-ready-dismiss-btn').addEventListener('click', () => {
            this.hideCacheReadyNotification();
        });
    }

    showUpdateNotification() {
        const banner = document.getElementById('app-update-banner');
        if (banner) {
            banner.classList.remove('hidden');
            setTimeout(() => {
                banner.classList.add('show');
            }, 10);

            // Auto-hide after 10 seconds if not interacted with
            setTimeout(() => {
                if (banner.classList.contains('show')) {
                    this.handleUpdateDismiss();
                }
            }, 10000);

            this.logUpdateEvent('update_notification_shown');
        }
    }

    hideUpdateNotification() {
        const banner = document.getElementById('app-update-banner');
        if (banner) {
            banner.classList.remove('show');
            setTimeout(() => {
                banner.classList.add('hidden');
            }, 300);
        }
    }

    showCacheReadyNotification() {
        const banner = document.getElementById('cache-ready-banner');
        if (banner) {
            banner.classList.remove('hidden');
            setTimeout(() => {
                banner.classList.add('show');
            }, 10);

            // Auto-hide after 5 seconds
            setTimeout(() => {
                this.hideCacheReadyNotification();
            }, 5000);

            this.logUpdateEvent('cache_ready_shown');
        }
    }

    hideCacheReadyNotification() {
        const banner = document.getElementById('cache-ready-banner');
        if (banner) {
            banner.classList.remove('show');
            setTimeout(() => {
                banner.classList.add('hidden');
            }, 300);
        }
    }

    showUpdateProgress() {
        const progressBar = document.getElementById('update-progress');
        if (progressBar) {
            progressBar.classList.add('show');
            
            setTimeout(() => {
                progressBar.classList.remove('show');
            }, 2000);
        }
    }

    async handleUpdateClick() {
        console.log('App Update: User clicked update');
        this.hideUpdateNotification();
        this.showUpdateProgress();
        
        try {
            if (this.newWorkerWaiting) {
                // Tell the waiting service worker to skip waiting
                this.newWorkerWaiting.postMessage({ type: 'SKIP_WAITING' });
            } else {
                // Force update by clearing caches and reloading
                await this.forceUpdate();
            }
            
            this.logUpdateEvent('update_accepted');
            
        } catch (error) {
            console.error('App Update: Failed to apply update:', error);
            this.showUpdateError();
        }
    }

    handleUpdateDismiss() {
        console.log('App Update: User dismissed update');
        this.hideUpdateNotification();
        this.logUpdateEvent('update_dismissed');
        
        // Show again in 1 hour
        setTimeout(() => {
            if (this.updateAvailable) {
                this.showUpdateNotification();
            }
        }, 60 * 60 * 1000);
    }

    async forceUpdate() {
        try {
            // Clear all caches
            if ('caches' in window) {
                const cacheNames = await caches.keys();
                await Promise.all(
                    cacheNames.map(cacheName => caches.delete(cacheName))
                );
                console.log('App Update: All caches cleared');
            }

            // Clear storage
            localStorage.removeItem('app-version');
            
            // Unregister service worker and reload
            if (this.serviceWorkerRegistration) {
                await this.serviceWorkerRegistration.unregister();
                console.log('App Update: Service worker unregistered');
            }

            // Reload with cache busting
            const url = new URL(window.location);
            url.searchParams.set('v', Date.now());
            window.location.replace(url.href);
            
        } catch (error) {
            console.error('App Update: Force update failed:', error);
            // Fallback to simple reload
            window.location.reload(true);
        }
    }

    showUpdateError() {
        const errorMessage = document.createElement('div');
        errorMessage.className = 'update-error-message';
        errorMessage.innerHTML = `
            <div class="error-content">
                <span class="error-icon">⚠️</span>
                <span class="error-text">Update failed. Please refresh manually.</span>
                <button class="error-close" onclick="this.parentElement.parentElement.remove()">×</button>
            </div>
        `;

        // Add error styles
        const style = document.createElement('style');
        style.textContent = `
            .update-error-message {
                position: fixed;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                background: #f44336;
                color: white;
                padding: 12px 20px;
                border-radius: 8px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                z-index: 1001;
                font-weight: 500;
                animation: slideInDown 0.3s ease;
            }

            .update-error-message .error-content {
                display: flex;
                align-items: center;
                gap: 8px;
            }

            .update-error-message .error-close {
                background: none;
                border: none;
                color: white;
                font-size: 18px;
                cursor: pointer;
                padding: 0;
                margin-left: 8px;
            }
        `;

        document.head.appendChild(style);
        document.body.appendChild(errorMessage);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (errorMessage.parentNode) {
                errorMessage.parentNode.removeChild(errorMessage);
            }
        }, 5000);
    }

    logUpdateEvent(event) {
        try {
            // Log to analytics if available
            if (window.gtag) {
                gtag('event', 'app_update', {
                    event_category: 'PWA',
                    event_label: event,
                    value: 1
                });
            }

            // Log to console for debugging
            console.log('App Update Event:', event, {
                timestamp: new Date().toISOString(),
                updateAvailable: this.updateAvailable,
                hasWaitingWorker: !!this.newWorkerWaiting
            });

            // Store in local analytics
            const updateEvents = JSON.parse(localStorage.getItem('app-update-events') || '[]');
            updateEvents.push({
                event,
                timestamp: Date.now(),
                updateAvailable: this.updateAvailable
            });
            
            // Keep only last 50 events
            if (updateEvents.length > 50) {
                updateEvents.splice(0, updateEvents.length - 50);
            }
            
            localStorage.setItem('app-update-events', JSON.stringify(updateEvents));
        } catch (error) {
            console.error('Failed to log update event:', error);
        }
    }

    // Public API methods
    isUpdateAvailable() {
        return this.updateAvailable;
    }

    async manualUpdateCheck() {
        await this.checkForUpdates();
    }

    forceShowUpdateNotification() {
        if (this.updateAvailable) {
            this.showUpdateNotification();
        }
    }

    destroy() {
        if (this.updateCheckTimer) {
            clearInterval(this.updateCheckTimer);
            this.updateCheckTimer = null;
        }
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AppUpdateManager;
}