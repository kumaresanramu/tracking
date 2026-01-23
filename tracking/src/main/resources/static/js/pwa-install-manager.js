/**
 * PWA Install Manager
 * Handles PWA installation prompts, timing logic, and custom install UI
 * Requirements: 3.1, 3.2
 */
class PWAInstallManager {
    constructor() {
        this.deferredPrompt = null;
        this.isInstalled = false;
        this.installPromptShown = false;
        this.installPromptDismissed = false;
        this.installPromptDismissedAt = null;
        this.sessionStartTime = Date.now();
        this.minSessionTimeBeforePrompt = 30000; // 30 seconds
        this.dismissCooldownPeriod = 7 * 24 * 60 * 60 * 1000; // 7 days
        
        this.init();
    }

    async init() {
        this.loadInstallState();
        this.setupEventListeners();
        await this.checkInstallability();
        this.createCustomInstallUI();
        
        // Add fallback mechanism - show install prompt after delay even if beforeinstallprompt doesn't fire
        setTimeout(() => {
            this.evaluateInstallPromptTimingFallback();
        }, this.minSessionTimeBeforePrompt + 1000); // Wait a bit longer than minimum session time
    }

    loadInstallState() {
        try {
            const installState = localStorage.getItem('pwa-install-state');
            if (installState) {
                const state = JSON.parse(installState);
                this.installPromptShown = state.promptShown || false;
                this.installPromptDismissed = state.promptDismissed || false;
                this.installPromptDismissedAt = state.dismissedAt || null;
            }
            
            // Check if app is already installed
            this.isInstalled = this.checkIfInstalled();
        } catch (error) {
            console.error('Failed to load install state:', error);
        }
    }

    saveInstallState() {
        try {
            const state = {
                promptShown: this.installPromptShown,
                promptDismissed: this.installPromptDismissed,
                dismissedAt: this.installPromptDismissedAt
            };
            localStorage.setItem('pwa-install-state', JSON.stringify(state));
        } catch (error) {
            console.error('Failed to save install state:', error);
        }
    }

    setupEventListeners() {
        // Listen for beforeinstallprompt event
        window.addEventListener('beforeinstallprompt', (e) => {
            console.log('PWA Install: beforeinstallprompt event fired');
            
            // Prevent the mini-infobar from appearing on mobile
            e.preventDefault();
            
            // Store the event so it can be triggered later
            this.deferredPrompt = e;
            
            // Check if we should show the install prompt
            this.evaluateInstallPromptTiming();
        });

        // Listen for appinstalled event
        window.addEventListener('appinstalled', (e) => {
            console.log('PWA Install: App was installed');
            this.isInstalled = true;
            this.hideCustomInstallUI();
            this.showInstallSuccessMessage();
            this.logInstallEvent('installed');
        });

        // Listen for app launch detection
        if (window.matchMedia('(display-mode: standalone)').matches || 
            window.navigator.standalone === true) {
            this.isInstalled = true;
            console.log('PWA Install: App launched in standalone mode');
        }
    }

    async checkInstallability() {
        // Check PWA criteria
        const criteria = {
            hasManifest: this.checkManifest(),
            hasServiceWorker: 'serviceWorker' in navigator,
            isHTTPS: location.protocol === 'https:' || location.hostname === 'localhost',
            hasIcons: await this.checkIcons(),
            hasStartUrl: await this.checkStartUrl()
        };

        const isInstallable = Object.values(criteria).every(criterion => criterion);
        
        console.log('PWA Install: Installability check:', criteria, 'Installable:', isInstallable);
        
        if (!isInstallable) {
            console.warn('PWA Install: App does not meet PWA criteria');
        }

        return isInstallable;
    }

    checkManifest() {
        const manifestLink = document.querySelector('link[rel="manifest"]');
        return manifestLink !== null;
    }

    checkIcons() {
        // Check if manifest has required icons
        return fetch('/manifest.json')
            .then(response => response.json())
            .then(manifest => {
                const hasRequiredIcons = manifest.icons && 
                    manifest.icons.some(icon => 
                        icon.sizes.includes('192x192') || 
                        icon.sizes.includes('512x512')
                    );
                return hasRequiredIcons;
            })
            .catch(() => false);
    }

    checkStartUrl() {
        return fetch('/manifest.json')
            .then(response => response.json())
            .then(manifest => manifest.start_url !== undefined)
            .catch(() => false);
    }

    checkIfInstalled() {
        // Check various indicators that the app is installed
        return window.matchMedia('(display-mode: standalone)').matches ||
               window.navigator.standalone === true ||
               document.referrer.includes('android-app://');
    }

    evaluateInstallPromptTiming() {
        if (this.isInstalled) {
            console.log('PWA Install: App already installed, skipping prompt');
            return false;
        }

        if (this.installPromptShown) {
            console.log('PWA Install: Prompt already shown this session');
            return false;
        }

        if (this.installPromptDismissed && this.installPromptDismissedAt) {
            const timeSinceDismissal = Date.now() - this.installPromptDismissedAt;
            if (timeSinceDismissal < this.dismissCooldownPeriod) {
                console.log('PWA Install: Still in cooldown period after dismissal');
                return false;
            }
        }

        const sessionTime = Date.now() - this.sessionStartTime;
        if (sessionTime < this.minSessionTimeBeforePrompt) {
            console.log('PWA Install: Waiting for minimum session time');
            setTimeout(() => this.evaluateInstallPromptTiming(), 
                this.minSessionTimeBeforePrompt - sessionTime);
            return false;
        }

        // Show the install prompt
        this.showInstallPrompt();
        return true;
    }

    async evaluateInstallPromptTimingFallback() {
        // Fallback method for when beforeinstallprompt event doesn't fire
        console.log('PWA Install: Evaluating fallback install prompt timing');
        
        if (this.isInstalled) {
            console.log('PWA Install: App already installed, skipping fallback prompt');
            return false;
        }

        if (this.installPromptShown) {
            console.log('PWA Install: Prompt already shown, skipping fallback');
            return false;
        }

        if (this.installPromptDismissed && this.installPromptDismissedAt) {
            const timeSinceDismissal = Date.now() - this.installPromptDismissedAt;
            if (timeSinceDismissal < this.dismissCooldownPeriod) {
                console.log('PWA Install: Still in cooldown period, skipping fallback');
                return false;
            }
        }

        // Check if PWA criteria are met
        const isInstallable = await this.checkInstallability();
        if (!isInstallable) {
            console.log('PWA Install: App does not meet PWA criteria, skipping fallback');
            return false;
        }

        console.log('PWA Install: Showing fallback install prompt');
        this.showInstallPrompt();
        return true;
    }

    showInstallPrompt() {
        console.log('PWA Install: Showing install prompt');
        this.installPromptShown = true;
        this.saveInstallState();

        if (!this.deferredPrompt) {
            console.log('PWA Install: No deferred prompt available, showing custom UI with instructions');
            this.showCustomInstallUI();
            return;
        }

        // Show the custom install UI first
        this.showCustomInstallUI();
    }

    createCustomInstallUI() {
        // Create install banner HTML
        const installBanner = document.createElement('div');
        installBanner.id = 'pwa-install-banner';
        installBanner.className = 'pwa-install-banner hidden';
        installBanner.innerHTML = `
            <div class="install-banner-content">
                <div class="install-banner-icon">📱</div>
                <div class="install-banner-text">
                    <h3>Install Expense Tracker</h3>
                    <p>Get quick access and work offline</p>
                </div>
                <div class="install-banner-actions">
                    <button id="pwa-install-btn" class="btn btn-primary btn-small">Install</button>
                    <button id="pwa-dismiss-btn" class="btn btn-secondary btn-small">Not now</button>
                </div>
            </div>
        `;

        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .pwa-install-banner {
                position: fixed;
                bottom: 20px;
                left: 20px;
                right: 20px;
                background: var(--card-background, #ffffff);
                border: 1px solid var(--border-color, #e0e0e0);
                border-radius: 12px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
                z-index: 1000;
                transform: translateY(100px);
                opacity: 0;
                transition: all 0.3s ease;
                max-width: 400px;
                margin: 0 auto;
            }

            .pwa-install-banner.show {
                transform: translateY(0);
                opacity: 1;
            }

            .pwa-install-banner.hidden {
                display: none;
            }

            .install-banner-content {
                display: flex;
                align-items: center;
                padding: 16px;
                gap: 12px;
            }

            .install-banner-icon {
                font-size: 24px;
                flex-shrink: 0;
            }

            .install-banner-text {
                flex: 1;
                min-width: 0;
            }

            .install-banner-text h3 {
                margin: 0 0 4px 0;
                font-size: 16px;
                font-weight: 600;
                color: var(--text-primary, #333);
            }

            .install-banner-text p {
                margin: 0;
                font-size: 14px;
                color: var(--text-secondary, #666);
            }

            .install-banner-actions {
                display: flex;
                gap: 8px;
                flex-shrink: 0;
            }

            .install-banner-actions .btn {
                padding: 8px 16px;
                font-size: 14px;
                border-radius: 6px;
                border: none;
                cursor: pointer;
                font-weight: 500;
                transition: all 0.2s ease;
            }

            .install-banner-actions .btn-primary {
                background: var(--primary-color, #2196F3);
                color: white;
            }

            .install-banner-actions .btn-primary:hover {
                background: var(--primary-hover, #1976D2);
            }

            .install-banner-actions .btn-secondary {
                background: transparent;
                color: var(--text-secondary, #666);
                border: 1px solid var(--border-color, #e0e0e0);
            }

            .install-banner-actions .btn-secondary:hover {
                background: var(--hover-background, #f5f5f5);
            }

            @media (max-width: 480px) {
                .pwa-install-banner {
                    left: 10px;
                    right: 10px;
                    bottom: 10px;
                }

                .install-banner-content {
                    padding: 12px;
                }

                .install-banner-actions {
                    flex-direction: column;
                    width: 80px;
                }

                .install-banner-actions .btn {
                    padding: 6px 12px;
                    font-size: 12px;
                }
            }

            /* Success message styles */
            .pwa-install-success {
                position: fixed;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                background: #4CAF50;
                color: white;
                padding: 12px 24px;
                border-radius: 8px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                z-index: 1001;
                font-weight: 500;
                animation: slideInDown 0.3s ease;
            }

            @keyframes slideInDown {
                from {
                    transform: translateX(-50%) translateY(-20px);
                    opacity: 0;
                }
                to {
                    transform: translateX(-50%) translateY(0);
                    opacity: 1;
                }
            }
        `;

        // Add to document
        document.head.appendChild(style);
        document.body.appendChild(installBanner);

        // Set up event listeners
        document.getElementById('pwa-install-btn').addEventListener('click', () => {
            this.handleInstallClick();
        });

        document.getElementById('pwa-dismiss-btn').addEventListener('click', () => {
            this.handleDismissClick();
        });
    }

    showCustomInstallUI() {
        const banner = document.getElementById('pwa-install-banner');
        if (banner && !this.isInstalled) {
            banner.classList.remove('hidden');
            // Trigger animation
            setTimeout(() => {
                banner.classList.add('show');
            }, 10);

            this.logInstallEvent('prompt_shown');
        }
    }

    hideCustomInstallUI() {
        const banner = document.getElementById('pwa-install-banner');
        if (banner) {
            banner.classList.remove('show');
            setTimeout(() => {
                banner.classList.add('hidden');
            }, 300);
        }
    }

    async handleInstallClick() {
        if (!this.deferredPrompt) {
            console.log('PWA Install: No deferred prompt available, showing manual install instructions');
            this.showInstallInstructions();
            return;
        }

        try {
            // Show the install prompt
            this.deferredPrompt.prompt();

            // Wait for the user to respond to the prompt
            const { outcome } = await this.deferredPrompt.userChoice;
            
            console.log('PWA Install: User choice:', outcome);
            
            if (outcome === 'accepted') {
                console.log('PWA Install: User accepted the install prompt');
                this.logInstallEvent('accepted');
            } else {
                console.log('PWA Install: User dismissed the install prompt');
                this.logInstallEvent('dismissed');
            }

            // Clear the deferred prompt
            this.deferredPrompt = null;
            this.hideCustomInstallUI();

        } catch (error) {
            console.error('PWA Install: Error during install:', error);
            this.showInstallInstructions();
        }
    }

    handleDismissClick() {
        console.log('PWA Install: User dismissed install prompt');
        this.installPromptDismissed = true;
        this.installPromptDismissedAt = Date.now();
        this.saveInstallState();
        this.hideCustomInstallUI();
        this.logInstallEvent('dismissed');
    }

    showInstallInstructions() {
        // Show platform-specific install instructions
        const userAgent = navigator.userAgent.toLowerCase();
        let instructions = '';
        let title = 'Install Expense Tracker';

        if (userAgent.includes('chrome') && userAgent.includes('android')) {
            title = 'Install on Android Chrome';
            instructions = `
                <p>To install this app on your Android device:</p>
                <ol>
                    <li>Tap the menu button (⋮) in the top-right corner</li>
                    <li>Select "Add to Home screen" or "Install app"</li>
                    <li>Tap "Add" or "Install" to confirm</li>
                </ol>
                <p>The app will be added to your home screen and app drawer.</p>
            `;
        } else if (userAgent.includes('safari') && userAgent.includes('iphone')) {
            title = 'Install on iPhone Safari';
            instructions = `
                <p>To install this app on your iPhone:</p>
                <ol>
                    <li>Tap the share button (□↗) at the bottom of the screen</li>
                    <li>Scroll down and tap "Add to Home Screen"</li>
                    <li>Tap "Add" to confirm</li>
                </ol>
                <p>The app will appear on your home screen like a native app.</p>
            `;
        } else if (userAgent.includes('chrome')) {
            title = 'Install on Desktop Chrome';
            instructions = `
                <p>To install this app on your computer:</p>
                <ol>
                    <li>Look for an install icon (⊕) in the address bar</li>
                    <li>Click it and select "Install"</li>
                    <li>Or use Chrome menu → "Install Expense Tracker"</li>
                </ol>
                <p>The app will open in its own window and appear in your applications.</p>
            `;
        } else if (userAgent.includes('firefox')) {
            title = 'Install on Firefox';
            instructions = `
                <p>To install this app in Firefox:</p>
                <ol>
                    <li>Look for the install icon in the address bar</li>
                    <li>Click it to install the app</li>
                    <li>Or bookmark this page for quick access</li>
                </ol>
                <p>Firefox support for PWA installation may vary.</p>
            `;
        } else if (userAgent.includes('edg')) {
            title = 'Install on Microsoft Edge';
            instructions = `
                <p>To install this app in Edge:</p>
                <ol>
                    <li>Click the menu button (⋯) in the top-right</li>
                    <li>Select "Apps" → "Install this site as an app"</li>
                    <li>Click "Install" to confirm</li>
                </ol>
                <p>The app will be available in your Start menu and taskbar.</p>
            `;
        } else {
            instructions = `
                <p>To install this app:</p>
                <ol>
                    <li>Look for install options in your browser menu</li>
                    <li>Check the address bar for an install icon</li>
                    <li>Or bookmark this page for easy access</li>
                </ol>
                <p>Installation options may vary by browser.</p>
            `;
        }

        instructions += `
            <div style="margin-top: 1rem; padding: 1rem; background: #f0f8ff; border-radius: 8px; border-left: 4px solid #2196F3;">
                <strong>Benefits of installing:</strong>
                <ul style="margin: 0.5rem 0; padding-left: 1.5rem;">
                    <li>Works offline</li>
                    <li>Faster loading</li>
                    <li>Desktop/home screen access</li>
                    <li>Push notifications</li>
                </ul>
            </div>
        `;

        this.showMessage(title, instructions, 'info');
    }

    showInstallSuccessMessage() {
        const successMessage = document.createElement('div');
        successMessage.className = 'pwa-install-success';
        successMessage.textContent = '✅ App installed successfully!';
        
        document.body.appendChild(successMessage);
        
        setTimeout(() => {
            if (successMessage.parentNode) {
                successMessage.parentNode.removeChild(successMessage);
            }
        }, 4000);
    }

    showMessage(title, message, type = 'info') {
        // Create a simple modal or toast for messages
        const modal = document.createElement('div');
        modal.className = 'pwa-message-modal';
        modal.innerHTML = `
            <div class="modal-overlay">
                <div class="modal-content">
                    <h3>${title}</h3>
                    <p>${message}</p>
                    <button class="btn btn-primary" onclick="this.closest('.pwa-message-modal').remove()">OK</button>
                </div>
            </div>
        `;

        // Add modal styles
        const style = document.createElement('style');
        style.textContent = `
            .pwa-message-modal {
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                z-index: 2000;
            }

            .pwa-message-modal .modal-overlay {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
            }

            .pwa-message-modal .modal-content {
                background: var(--card-background, white);
                border-radius: 12px;
                padding: 24px;
                max-width: 400px;
                width: 100%;
                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
            }

            .pwa-message-modal h3 {
                margin: 0 0 12px 0;
                color: var(--text-primary, #333);
            }

            .pwa-message-modal p {
                margin: 0 0 20px 0;
                color: var(--text-secondary, #666);
                line-height: 1.5;
            }

            .pwa-message-modal .btn {
                width: 100%;
                padding: 12px;
                border: none;
                border-radius: 6px;
                background: var(--primary-color, #2196F3);
                color: white;
                font-weight: 500;
                cursor: pointer;
            }
        `;

        document.head.appendChild(style);
        document.body.appendChild(modal);
    }

    logInstallEvent(event) {
        try {
            // Log to analytics if available
            if (window.gtag) {
                gtag('event', 'pwa_install', {
                    event_category: 'PWA',
                    event_label: event,
                    value: 1
                });
            }

            // Log to console for debugging
            console.log('PWA Install Event:', event, {
                timestamp: new Date().toISOString(),
                userAgent: navigator.userAgent,
                isInstalled: this.isInstalled
            });

            // Store in local analytics
            const installEvents = JSON.parse(localStorage.getItem('pwa-install-events') || '[]');
            installEvents.push({
                event,
                timestamp: Date.now(),
                userAgent: navigator.userAgent
            });
            
            // Keep only last 50 events
            if (installEvents.length > 50) {
                installEvents.splice(0, installEvents.length - 50);
            }
            
            localStorage.setItem('pwa-install-events', JSON.stringify(installEvents));
        } catch (error) {
            console.error('Failed to log install event:', error);
        }
    }

    // Public API methods
    isAppInstalled() {
        return this.isInstalled;
    }

    canShowInstallPrompt() {
        return !this.isInstalled && 
               !this.installPromptShown && 
               (!this.installPromptDismissed || 
                (Date.now() - this.installPromptDismissedAt) > this.dismissCooldownPeriod);
    }

    forceShowInstallPrompt() {
        if (!this.isInstalled) {
            this.showInstallPrompt();
        }
    }

    resetInstallState() {
        this.installPromptShown = false;
        this.installPromptDismissed = false;
        this.installPromptDismissedAt = null;
        this.saveInstallState();
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PWAInstallManager;
}