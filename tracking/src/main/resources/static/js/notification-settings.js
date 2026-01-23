// Notification Settings Manager
class NotificationSettingsManager {
    constructor() {
        this.settingsUrl = '/api/notification-settings';
        this.pushSubscriptionUrl = '/api/push-subscriptions';
        this.permissionUrl = '/api/notification-permissions';
        this.currentSettings = null;
        this.pushSubscription = null;
        this.permissionStatus = null;
        
        this.initializeEventListeners();
        this.loadSettings(); // This will now handle the full initialization flow
    }

    initializeEventListeners() {
        // Channel toggles
        document.getElementById('enable-push-notifications')?.addEventListener('change', (e) => {
            this.handlePushNotificationToggle(e.target.checked);
        });
        
        document.getElementById('enable-email-notifications')?.addEventListener('change', (e) => {
            this.handleEmailNotificationToggle(e.target.checked);
        });
        
        // Notification type toggles
        document.getElementById('enable-daily-reminder')?.addEventListener('change', (e) => {
            this.toggleTimeSetting('daily-reminder-time-setting', e.target.checked);
        });
        
        document.getElementById('enable-budget-alerts')?.addEventListener('change', (e) => {
            this.toggleTimeSetting('budget-threshold-setting', e.target.checked);
        });
        
        document.getElementById('enable-weekly-summary')?.addEventListener('change', (e) => {
            this.toggleTimeSetting('weekly-summary-setting', e.target.checked);
        });
        
        // Budget threshold slider
        document.getElementById('budget-warning-threshold')?.addEventListener('input', (e) => {
            document.getElementById('threshold-value').textContent = e.target.value + '%';
        });
        
        // Action buttons
        document.getElementById('save-notification-settings')?.addEventListener('click', () => {
            this.saveSettings();
        });
        
        document.getElementById('reset-notification-settings')?.addEventListener('click', () => {
            this.resetSettings();
        });
        
        document.getElementById('test-notifications')?.addEventListener('click', () => {
            this.testNotifications();
        });
        
        document.getElementById('test-email-btn')?.addEventListener('click', () => {
            this.testEmailNotification();
        });
        
        // Add click handler for push permission status to refresh
        document.getElementById('push-permission-status')?.addEventListener('click', () => {
            this.refreshPushPermissionStatus();
        });
        
        // Update quiet hours status
        document.getElementById('quiet-hours-start')?.addEventListener('change', () => {
            this.updateQuietHoursStatus();
        });
        
        document.getElementById('quiet-hours-end')?.addEventListener('change', () => {
            this.updateQuietHoursStatus();
        });
    }

    async loadSettings() {
        try {
            const response = await fetch(this.settingsUrl);
            if (response.ok) {
                this.currentSettings = await response.json();
                this.populateSettingsForm();
                this.updateQuietHoursStatus();
                
                // Load permission status after settings are loaded
                await this.loadPermissionStatus();
                
                // Check browser push permission after everything else is loaded
                await this.checkPushPermission();
            } else {
                console.error('Failed to load notification settings');
                this.showToast('Failed to load settings', 'error');
            }
        } catch (error) {
            console.error('Error loading notification settings:', error);
            this.showToast('Error loading settings', 'error');
        }
    }

    async loadPermissionStatus() {
        try {
            const response = await fetch(`${this.permissionUrl}/status`);
            if (response.ok) {
                this.permissionStatus = await response.json();
                this.updatePermissionStatusUI();
            } else {
                console.error('Failed to load permission status');
            }
        } catch (error) {
            console.error('Error loading permission status:', error);
        }
    }

    updatePermissionStatusUI() {
        if (!this.permissionStatus) return;
        
        // Update push notification status - prioritize browser permission over server status
        const pushStatus = document.getElementById('push-permission-status');
        if (pushStatus) {
            // Check browser permission first
            const browserPermission = 'Notification' in window ? Notification.permission : 'not-supported';
            
            if (browserPermission === 'not-supported') {
                pushStatus.className = 'permission-status not-supported';
                pushStatus.textContent = '❌ Push notifications not supported in this browser';
            } else if (browserPermission === 'denied') {
                pushStatus.className = 'permission-status denied';
                pushStatus.textContent = '❌ Push notifications blocked. Please enable in browser settings.';
            } else if (this.permissionStatus.pushEnabled && this.permissionStatus.pushAvailable) {
                pushStatus.className = 'permission-status granted';
                pushStatus.textContent = '✅ Push notifications enabled and working';
            } else if (browserPermission === 'granted') {
                pushStatus.className = 'permission-status default';
                pushStatus.textContent = '⏳ Click to enable push notifications';
            } else {
                pushStatus.className = 'permission-status default';
                pushStatus.textContent = '⏳ Click to enable push notifications';
            }
        }
        
        // Update email notification status
        const emailSettings = document.getElementById('email-settings');
        if (emailSettings) {
            emailSettings.style.display = this.permissionStatus.emailEnabled ? 'flex' : 'none';
        }
        
        // Update quiet hours status
        if (this.permissionStatus.inQuietHours) {
            this.updateQuietHoursStatus();
        }
    }

    populateSettingsForm() {
        if (!this.currentSettings) return;
        
        const settings = this.currentSettings;
        
        // Channel preferences
        const pushEnabled = settings.preferredChannels?.includes('PUSH') || false;
        const emailEnabled = settings.preferredChannels?.includes('EMAIL') || false;
        const inAppEnabled = settings.preferredChannels?.includes('IN_APP') || true;
        
        this.setCheckboxValue('enable-push-notifications', pushEnabled);
        this.setCheckboxValue('enable-email-notifications', emailEnabled);
        this.setCheckboxValue('enable-in-app-notifications', inAppEnabled);
        
        // Email settings
        if (settings.emailAddress) {
            document.getElementById('notification-email').value = settings.emailAddress;
        }
        this.handleEmailNotificationToggle(emailEnabled);
        
        // Notification types
        this.setCheckboxValue('enable-daily-reminder', settings.enableDailyReminder || false);
        this.setCheckboxValue('enable-budget-alerts', settings.enableBudgetAlerts || false);
        this.setCheckboxValue('enable-weekly-summary', settings.enableWeeklySummary || false);
        this.setCheckboxValue('enable-streak-rewards', settings.enableStreakRewards || false);
        this.setCheckboxValue('enable-badges', settings.enableBadges || false);
        
        // Time settings
        if (settings.dailyReminderTime) {
            document.getElementById('daily-reminder-time').value = settings.dailyReminderTime;
        }
        
        if (settings.budgetWarningThreshold) {
            const threshold = document.getElementById('budget-warning-threshold');
            threshold.value = settings.budgetWarningThreshold;
            document.getElementById('threshold-value').textContent = settings.budgetWarningThreshold + '%';
        }
        
        if (settings.weeklySummaryDay) {
            document.getElementById('weekly-summary-day').value = settings.weeklySummaryDay;
        }
        
        if (settings.weeklySummaryTime) {
            document.getElementById('weekly-summary-time').value = settings.weeklySummaryTime;
        }
        
        // Quiet hours
        if (settings.quietHoursStart) {
            document.getElementById('quiet-hours-start').value = settings.quietHoursStart;
        }
        
        if (settings.quietHoursEnd) {
            document.getElementById('quiet-hours-end').value = settings.quietHoursEnd;
        }
        
        // Show/hide time settings based on enabled state
        this.toggleTimeSetting('daily-reminder-time-setting', settings.enableDailyReminder);
        this.toggleTimeSetting('budget-threshold-setting', settings.enableBudgetAlerts);
        this.toggleTimeSetting('weekly-summary-setting', settings.enableWeeklySummary);
    }

    setCheckboxValue(id, value) {
        const checkbox = document.getElementById(id);
        if (checkbox) {
            checkbox.checked = value;
        }
    }

    toggleTimeSetting(settingId, show) {
        const setting = document.getElementById(settingId);
        if (setting) {
            setting.style.display = show ? 'block' : 'none';
        }
    }

    async handlePushNotificationToggle(enabled) {
        if (enabled) {
            try {
                // Request permission from the permission service
                const permissionResponse = await fetch(`${this.permissionUrl}/request`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        channel: 'PUSH',
                        reason: 'User enabled push notifications in settings'
                    })
                });
                
                if (!permissionResponse.ok) {
                    throw new Error('Failed to request push permission from server');
                }
                
                const permission = await this.requestPushPermission();
                if (permission === 'granted') {
                    await this.subscribeToPush();
                    this.updatePushPermissionStatus('granted');
                    await this.loadPermissionStatus();
                } else {
                    // Revert checkbox if permission denied
                    document.getElementById('enable-push-notifications').checked = false;
                    this.updatePushPermissionStatus('denied');
                    
                    // Revoke permission on server side
                    await this.revokePermission('PUSH');
                }
            } catch (error) {
                console.error('Error enabling push notifications:', error);
                document.getElementById('enable-push-notifications').checked = false;
                this.updatePushPermissionStatus('error');
                this.showToast('Failed to enable push notifications', 'error');
            }
        } else {
            await this.unsubscribeFromPush();
            await this.revokePermission('PUSH');
            this.updatePushPermissionStatus('disabled');
            await this.loadPermissionStatus();
        }
    }

    async revokePermission(channel) {
        try {
            const response = await fetch(`${this.permissionUrl}/revoke`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    channel: channel
                })
            });
            
            if (response.ok) {
                console.log(`Permission revoked for channel: ${channel}`);
            } else {
                console.error(`Failed to revoke permission for channel: ${channel}`);
            }
        } catch (error) {
            console.error(`Error revoking permission for channel ${channel}:`, error);
        }
    }

    handleEmailNotificationToggle(enabled) {
        const emailSettings = document.getElementById('email-settings');
        if (emailSettings) {
            emailSettings.style.display = enabled ? 'flex' : 'none';
        }
    }

    async refreshPushPermissionStatus() {
        // Re-check browser permission and update UI accordingly
        await this.checkPushPermission();
        
        // Also refresh server-side permission status
        await this.loadPermissionStatus();
    }

    async requestPushPermission() {
        if (!('Notification' in window)) {
            throw new Error('This browser does not support notifications');
        }
        
        if (Notification.permission === 'granted') {
            return 'granted';
        }
        
        if (Notification.permission === 'denied') {
            return 'denied';
        }
        
        const permission = await Notification.requestPermission();
        return permission;
    }

    async subscribeToPush() {
        if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
            throw new Error('Push messaging is not supported');
        }
        
        try {
            const registration = await navigator.serviceWorker.ready;
            
            // Get VAPID public key from server
            const vapidResponse = await fetch('/api/push-subscriptions/vapid-public-key');
            const vapidPublicKey = await vapidResponse.text();
            
            const subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: this.urlBase64ToUint8Array(vapidPublicKey)
            });
            
            // Send subscription to server
            const response = await fetch(this.pushSubscriptionUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    endpoint: subscription.endpoint,
                    p256dhKey: btoa(String.fromCharCode(...new Uint8Array(subscription.getKey('p256dh')))),
                    authKey: btoa(String.fromCharCode(...new Uint8Array(subscription.getKey('auth')))),
                    userAgent: navigator.userAgent
                })
            });
            
            if (!response.ok) {
                throw new Error('Failed to save push subscription');
            }
            
            this.pushSubscription = subscription;
            console.log('Push subscription successful');
            
        } catch (error) {
            console.error('Error subscribing to push:', error);
            throw error;
        }
    }

    async unsubscribeFromPush() {
        try {
            if (this.pushSubscription) {
                await this.pushSubscription.unsubscribe();
            }
            
            // Remove subscription from server
            const registration = await navigator.serviceWorker.ready;
            const subscription = await registration.pushManager.getSubscription();
            
            if (subscription) {
                await fetch(`${this.pushSubscriptionUrl}/unsubscribe`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        endpoint: subscription.endpoint
                    })
                });
                
                await subscription.unsubscribe();
            }
            
            this.pushSubscription = null;
            console.log('Push unsubscription successful');
            
        } catch (error) {
            console.error('Error unsubscribing from push:', error);
        }
    }

    async checkPushPermission() {
        if (!('Notification' in window)) {
            this.updatePushPermissionStatus('not-supported');
            return;
        }
        
        const permission = Notification.permission;
        
        // Check if already subscribed
        if (permission === 'granted' && 'serviceWorker' in navigator) {
            try {
                const registration = await navigator.serviceWorker.ready;
                const subscription = await registration.pushManager.getSubscription();
                if (subscription) {
                    this.pushSubscription = subscription;
                    document.getElementById('enable-push-notifications').checked = true;
                    this.updatePushPermissionStatus('granted');
                    return;
                }
            } catch (error) {
                console.error('Error checking push subscription:', error);
            }
        }
        
        // Update status based on browser permission
        if (permission === 'granted') {
            this.updatePushPermissionStatus('default'); // Granted but not subscribed
        } else {
            this.updatePushPermissionStatus(permission);
        }
    }

    updatePushPermissionStatus(status) {
        const statusElement = document.getElementById('push-permission-status');
        if (!statusElement) return;
        
        statusElement.className = 'permission-status ' + status;
        
        switch (status) {
            case 'granted':
                statusElement.innerHTML = '✅ Push notifications enabled';
                break;
            case 'denied':
                statusElement.innerHTML = `
                    ❌ Push notifications blocked. 
                    <div style="margin-top: 0.5rem;">
                        <strong>To enable:</strong>
                        <ol style="margin: 0.5rem 0; padding-left: 1.5rem; font-size: 0.85rem;">
                            <li>Click the 🔒 or ⓘ icon in your browser's address bar</li>
                            <li>Find "Notifications" and change it to "Allow"</li>
                            <li>Refresh this page and try again</li>
                        </ol>
                        <button onclick="window.notificationSettingsManager.showBrowserInstructions()" 
                                class="btn btn-small btn-secondary" style="margin-top: 0.5rem;">
                            📖 Detailed Instructions
                        </button>
                    </div>
                `;
                break;
            case 'default':
                statusElement.textContent = '⏳ Click to enable push notifications';
                break;
            case 'disabled':
                statusElement.textContent = '🔕 Push notifications disabled';
                break;
            case 'not-supported':
                statusElement.textContent = '❌ Push notifications not supported in this browser';
                break;
            case 'error':
                statusElement.textContent = '❌ Error enabling push notifications';
                break;
        }
    }

    updateQuietHoursStatus() {
        const startTime = document.getElementById('quiet-hours-start')?.value;
        const endTime = document.getElementById('quiet-hours-end')?.value;
        const statusElement = document.getElementById('quiet-hours-status');
        
        if (!startTime || !endTime || !statusElement) return;
        
        const now = new Date();
        const currentTime = now.getHours() * 60 + now.getMinutes();
        
        const [startHour, startMin] = startTime.split(':').map(Number);
        const [endHour, endMin] = endTime.split(':').map(Number);
        
        const startMinutes = startHour * 60 + startMin;
        const endMinutes = endHour * 60 + endMin;
        
        let isInQuietHours = false;
        
        if (startMinutes < endMinutes) {
            // Same day quiet hours
            isInQuietHours = currentTime >= startMinutes && currentTime < endMinutes;
        } else {
            // Overnight quiet hours
            isInQuietHours = currentTime >= startMinutes || currentTime < endMinutes;
        }
        
        statusElement.className = 'quiet-hours-status ' + (isInQuietHours ? 'active' : 'inactive');
        statusElement.textContent = isInQuietHours 
            ? '🌙 Currently in quiet hours - notifications are paused'
            : '🔔 Notifications are active';
    }

    async saveSettings() {
        try {
            const settings = this.collectSettingsFromForm();
            
            const response = await fetch(this.settingsUrl, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(settings)
            });
            
            if (response.ok) {
                this.currentSettings = await response.json();
                this.showToast('Settings saved successfully!', 'success');
                this.updateQuietHoursStatus();
                
                // Validate preferences after saving
                await this.validatePreferences();
                await this.loadPermissionStatus();
            } else {
                throw new Error('Failed to save settings');
            }
        } catch (error) {
            console.error('Error saving settings:', error);
            this.showToast('Failed to save settings', 'error');
        }
    }

    async validatePreferences() {
        try {
            const response = await fetch(`${this.permissionUrl}/validate`, {
                method: 'POST'
            });
            
            if (response.ok) {
                console.log('Preferences validated successfully');
            } else {
                console.error('Failed to validate preferences');
            }
        } catch (error) {
            console.error('Error validating preferences:', error);
        }
    }

    collectSettingsFromForm() {
        const preferredChannels = [];
        
        if (document.getElementById('enable-push-notifications')?.checked) {
            preferredChannels.push('PUSH');
        }
        if (document.getElementById('enable-email-notifications')?.checked) {
            preferredChannels.push('EMAIL');
        }
        if (document.getElementById('enable-in-app-notifications')?.checked) {
            preferredChannels.push('IN_APP');
        }
        
        return {
            enableDailyReminder: document.getElementById('enable-daily-reminder')?.checked || false,
            dailyReminderTime: document.getElementById('daily-reminder-time')?.value || '20:00',
            enableBudgetAlerts: document.getElementById('enable-budget-alerts')?.checked || false,
            budgetWarningThreshold: parseInt(document.getElementById('budget-warning-threshold')?.value) || 80,
            enableWeeklySummary: document.getElementById('enable-weekly-summary')?.checked || false,
            weeklySummaryDay: parseInt(document.getElementById('weekly-summary-day')?.value) || 7,
            weeklySummaryTime: document.getElementById('weekly-summary-time')?.value || '09:00',
            enableStreakRewards: document.getElementById('enable-streak-rewards')?.checked || false,
            enableBadges: document.getElementById('enable-badges')?.checked || false,
            quietHoursStart: document.getElementById('quiet-hours-start')?.value || '22:00',
            quietHoursEnd: document.getElementById('quiet-hours-end')?.value || '08:00',
            preferredChannels: preferredChannels,
            emailAddress: document.getElementById('notification-email')?.value || null,
            enableEmailNotifications: document.getElementById('enable-email-notifications')?.checked || false
        };
    }

    async resetSettings() {
        if (!confirm('Are you sure you want to reset all notification settings to defaults?')) {
            return;
        }
        
        try {
            const response = await fetch(`${this.settingsUrl}/reset`, {
                method: 'POST'
            });
            
            if (response.ok) {
                await this.loadSettings();
                this.showToast('Settings reset to defaults', 'success');
            } else {
                throw new Error('Failed to reset settings');
            }
        } catch (error) {
            console.error('Error resetting settings:', error);
            this.showToast('Failed to reset settings', 'error');
        }
    }

    async testNotifications() {
        try {
            // Test in-app notification
            this.showToast('🧪 Test notification sent!', 'success');
            
            // Test push notification if enabled
            if (document.getElementById('enable-push-notifications')?.checked && this.pushSubscription) {
                const response = await fetch('/api/notifications/test', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        type: 'TEST',
                        title: 'Test Notification',
                        message: 'This is a test notification from Expense Tracker!',
                        channels: ['PUSH']
                    })
                });
                
                if (response.ok) {
                    console.log('Test push notification sent');
                }
            }
            
        } catch (error) {
            console.error('Error sending test notification:', error);
            this.showToast('Failed to send test notification', 'error');
        }
    }

    async testEmailNotification() {
        const email = document.getElementById('notification-email')?.value;
        if (!email) {
            this.showToast('Please enter an email address first', 'warning');
            return;
        }
        
        try {
            const response = await fetch('/api/notifications/test-email', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email: email,
                    type: 'TEST'
                })
            });
            
            if (response.ok) {
                this.showToast('Test email sent successfully!', 'success');
            } else {
                throw new Error('Failed to send test email');
            }
        } catch (error) {
            console.error('Error sending test email:', error);
            this.showToast('Failed to send test email', 'error');
        }
    }

    urlBase64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - base64String.length % 4) % 4);
        const base64 = (base64String + padding)
            .replace(/-/g, '+')
            .replace(/_/g, '/');
        
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    showBrowserInstructions() {
        const userAgent = navigator.userAgent.toLowerCase();
        let instructions = '';
        let title = 'Enable Push Notifications';

        if (userAgent.includes('chrome') && !userAgent.includes('edg')) {
            instructions = `
                <strong>For Chrome:</strong>
                <ol style="margin: 0.5rem 0; padding-left: 1.5rem;">
                    <li>Click the 🔒 (lock) or ⓘ (info) icon in the address bar</li>
                    <li>Find "Notifications" in the permissions list</li>
                    <li>Change it from "Block" to "Allow"</li>
                    <li>Refresh this page</li>
                    <li>Click the push notifications toggle again</li>
                </ol>
                <p><strong>Alternative:</strong> Go to Chrome Settings → Privacy and Security → Site Settings → Notifications → Add this site to "Allowed to send notifications"</p>
            `;
        } else if (userAgent.includes('firefox')) {
            instructions = `
                <strong>For Firefox:</strong>
                <ol style="margin: 0.5rem 0; padding-left: 1.5rem;">
                    <li>Click the 🔒 (shield) icon in the address bar</li>
                    <li>Click "Connection is secure"</li>
                    <li>Click "More information"</li>
                    <li>Go to "Permissions" tab</li>
                    <li>Find "Receive Notifications" and select "Allow"</li>
                    <li>Refresh this page</li>
                </ol>
                <p><strong>Alternative:</strong> Go to Firefox Settings → Privacy & Security → Permissions → Notifications → Settings → Add this site</p>
            `;
        } else if (userAgent.includes('safari')) {
            instructions = `
                <strong>For Safari:</strong>
                <ol style="margin: 0.5rem 0; padding-left: 1.5rem;">
                    <li>Go to Safari → Preferences (or Settings)</li>
                    <li>Click "Websites" tab</li>
                    <li>Select "Notifications" from the left sidebar</li>
                    <li>Find this website and change to "Allow"</li>
                    <li>Refresh this page</li>
                </ol>
                <p><strong>Note:</strong> Safari may require you to interact with the page before showing notification permission.</p>
            `;
        } else if (userAgent.includes('edg')) {
            instructions = `
                <strong>For Microsoft Edge:</strong>
                <ol style="margin: 0.5rem 0; padding-left: 1.5rem;">
                    <li>Click the 🔒 (lock) or ⓘ (info) icon in the address bar</li>
                    <li>Find "Notifications" and change to "Allow"</li>
                    <li>Refresh this page</li>
                </ol>
                <p><strong>Alternative:</strong> Go to Edge Settings → Cookies and site permissions → Notifications → Add this site to allowed list</p>
            `;
        } else {
            instructions = `
                <strong>General Instructions:</strong>
                <ol style="margin: 0.5rem 0; padding-left: 1.5rem;">
                    <li>Look for a lock 🔒 or info ⓘ icon in your browser's address bar</li>
                    <li>Click it to open site permissions</li>
                    <li>Find "Notifications" and change it to "Allow"</li>
                    <li>Refresh this page and try again</li>
                </ol>
                <p>If you can't find these options, check your browser's settings under Privacy or Site Permissions.</p>
            `;
        }

        this.showModal(title, instructions);
    }

    showModal(title, content) {
        // Create modal HTML
        const modal = document.createElement('div');
        modal.className = 'notification-instructions-modal';
        modal.innerHTML = `
            <div class="modal-overlay" onclick="this.parentElement.remove()">
                <div class="modal-content" onclick="event.stopPropagation()">
                    <div class="modal-header">
                        <h3>${title}</h3>
                        <button class="modal-close" onclick="this.closest('.notification-instructions-modal').remove()">×</button>
                    </div>
                    <div class="modal-body">
                        ${content}
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-primary" onclick="this.closest('.notification-instructions-modal').remove()">Got it!</button>
                    </div>
                </div>
            </div>
        `;

        // Add modal styles if not already present
        if (!document.querySelector('#notification-modal-styles')) {
            const style = document.createElement('style');
            style.id = 'notification-modal-styles';
            style.textContent = `
                .notification-instructions-modal {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    z-index: 10000;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                }

                .notification-instructions-modal .modal-overlay {
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: rgba(0, 0, 0, 0.6);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 20px;
                    animation: fadeIn 0.2s ease;
                }

                .notification-instructions-modal .modal-content {
                    background: var(--card-background, white);
                    border-radius: 12px;
                    max-width: 500px;
                    width: 100%;
                    max-height: 80vh;
                    overflow-y: auto;
                    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                    animation: slideIn 0.3s ease;
                }

                .notification-instructions-modal .modal-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 20px 24px 0 24px;
                    border-bottom: 1px solid var(--border-color, #e0e0e0);
                    margin-bottom: 20px;
                }

                .notification-instructions-modal .modal-header h3 {
                    margin: 0;
                    color: var(--text-primary, #333);
                    font-size: 18px;
                    font-weight: 600;
                }

                .notification-instructions-modal .modal-close {
                    background: none;
                    border: none;
                    font-size: 24px;
                    cursor: pointer;
                    color: var(--text-secondary, #666);
                    padding: 0;
                    width: 30px;
                    height: 30px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 50%;
                    transition: background-color 0.2s ease;
                }

                .notification-instructions-modal .modal-close:hover {
                    background-color: var(--hover-background, #f5f5f5);
                }

                .notification-instructions-modal .modal-body {
                    padding: 0 24px 20px 24px;
                    color: var(--text-primary, #333);
                    line-height: 1.6;
                }

                .notification-instructions-modal .modal-body ol {
                    margin: 12px 0;
                    padding-left: 20px;
                }

                .notification-instructions-modal .modal-body li {
                    margin: 8px 0;
                }

                .notification-instructions-modal .modal-body p {
                    margin: 16px 0;
                    color: var(--text-secondary, #666);
                    font-size: 14px;
                }

                .notification-instructions-modal .modal-footer {
                    padding: 20px 24px;
                    border-top: 1px solid var(--border-color, #e0e0e0);
                    display: flex;
                    justify-content: flex-end;
                }

                .notification-instructions-modal .btn {
                    padding: 10px 20px;
                    border: none;
                    border-radius: 6px;
                    font-weight: 500;
                    cursor: pointer;
                    transition: all 0.2s ease;
                }

                .notification-instructions-modal .btn-primary {
                    background: var(--primary-color, #2196F3);
                    color: white;
                }

                .notification-instructions-modal .btn-primary:hover {
                    background: var(--primary-hover, #1976D2);
                }

                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }

                @keyframes slideIn {
                    from { 
                        opacity: 0;
                        transform: translateY(-20px) scale(0.95);
                    }
                    to { 
                        opacity: 1;
                        transform: translateY(0) scale(1);
                    }
                }

                @media (max-width: 480px) {
                    .notification-instructions-modal .modal-content {
                        margin: 10px;
                        max-height: 90vh;
                    }
                    
                    .notification-instructions-modal .modal-header,
                    .notification-instructions-modal .modal-body,
                    .notification-instructions-modal .modal-footer {
                        padding-left: 16px;
                        padding-right: 16px;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        // Add modal to page
        document.body.appendChild(modal);
    }

    showToast(message, type = 'info') {
        // Create toast element
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        
        // Add to container or create one
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        
        container.appendChild(toast);
        
        // Remove after 5 seconds
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 5000);
    }
}

// Initialize notification settings manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('settings-page')) {
        window.notificationSettingsManager = new NotificationSettingsManager();
    }
});