// Sync Service - Handles offline data synchronization
class SyncService {
    constructor() {
        this.isOnline = navigator.onLine;
        this.autoSyncEnabled = true;
        this.syncInProgress = false;
        this.syncQueue = [];
        this.lastSyncTime = null;
        this.retryCount = 0;
        this.maxRetries = 5;
        this.baseRetryDelay = 1000; // 1 second
        this.connectivityCheckInterval = null;
        this.periodicSyncInterval = null;
        
        this.setupEventListeners();
        this.startConnectivityMonitoring();
        this.startPeriodicSync();
    }

    setupEventListeners() {
        // Listen for online/offline events
        window.addEventListener('online', () => {
            console.log('Connection restored - triggering automatic sync');
            this.handleConnectivityChange(true);
        });

        window.addEventListener('offline', () => {
            console.log('Connection lost - switching to offline mode');
            this.handleConnectivityChange(false);
        });

        // Register for background sync if supported
        if ('serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype) {
            navigator.serviceWorker.ready.then(registration => {
                this.swRegistration = registration;
            });
        }

        // Listen for visibility change to sync when app becomes visible
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && this.isOnline && this.autoSyncEnabled) {
                this.syncOfflineData();
            }
        });
    }

    handleConnectivityChange(online) {
        const wasOnline = this.isOnline;
        this.isOnline = online;
        
        if (online && !wasOnline) {
            // Just came online - reset retry count and trigger sync
            this.retryCount = 0;
            if (this.autoSyncEnabled) {
                // Small delay to ensure connection is stable
                setTimeout(() => {
                    this.syncOfflineData();
                }, 1000);
            }
        }
        
        // Update UI status
        if (window.app) {
            window.app.updateSyncStatus(online ? 'online' : 'offline');
        }
        
        // Notify listeners
        this.notifyConnectivityChange(online);
    }

    notifyConnectivityChange(online) {
        // Dispatch custom event for other components to listen
        const event = new CustomEvent('connectivitychange', {
            detail: { online, timestamp: Date.now() }
        });
        window.dispatchEvent(event);
    }

    startConnectivityMonitoring() {
        // Enhanced connectivity detection beyond just navigator.onLine
        this.connectivityCheckInterval = setInterval(async () => {
            const actuallyOnline = await this.checkActualConnectivity();
            if (actuallyOnline !== this.isOnline) {
                this.handleConnectivityChange(actuallyOnline);
            }
        }, 10000); // Check every 10 seconds
    }

    async checkActualConnectivity() {
        try {
            // Try to fetch a small resource from our API
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 5000);
            
            const response = await fetch('/api/health', {
                method: 'HEAD',
                signal: controller.signal,
                cache: 'no-cache'
            });
            
            clearTimeout(timeoutId);
            return response.ok;
        } catch (error) {
            return false;
        }
    }

    setAutoSync(enabled) {
        this.autoSyncEnabled = enabled;
        console.log(`Auto sync ${enabled ? 'enabled' : 'disabled'}`);
    }

    async syncOfflineData() {
        if (this.syncInProgress || !this.isOnline) {
            console.log('Sync skipped - already in progress or offline');
            return;
        }

        this.syncInProgress = true;
        console.log('Starting offline data sync...');

        try {
            // Update sync status in UI
            if (window.app) {
                window.app.updateSyncStatus('syncing');
            }

            // Get offline expenses from IndexedDB
            const offlineExpenses = await this.getOfflineExpenses();
            
            if (offlineExpenses.length === 0) {
                console.log('No offline data to sync');
                this.lastSyncTime = new Date();
                this.retryCount = 0;
                return;
            }

            console.log(`Syncing ${offlineExpenses.length} offline expenses...`);

            // Validate all expenses before syncing
            const validExpenses = [];
            const invalidExpenses = [];
            
            for (const expense of offlineExpenses) {
                try {
                    this.validateExpenseData(expense);
                    validExpenses.push(expense);
                } catch (error) {
                    console.warn(`Invalid expense data: ${error.message}`, expense);
                    invalidExpenses.push({ expense, error: error.message });
                }
            }

            // Remove invalid expenses from offline queue
            for (const invalid of invalidExpenses) {
                await this.removeOfflineExpense(invalid.expense.tempId);
            }

            if (validExpenses.length === 0) {
                console.log('No valid expenses to sync');
                this.lastSyncTime = new Date();
                this.retryCount = 0;
                return;
            }

            // Sync expenses in batches with exponential backoff retry
            const batchSize = 5; // Smaller batches for better error handling
            const batches = this.createBatches(validExpenses, batchSize);
            
            let syncedCount = 0;
            for (let i = 0; i < batches.length; i++) {
                const batch = batches[i];
                try {
                    await this.syncExpenseBatchWithRetry(batch);
                    syncedCount += batch.length;
                    console.log(`Synced batch ${i + 1}/${batches.length} (${batch.length} expenses)`);
                } catch (error) {
                    console.error(`Failed to sync batch ${i + 1}:`, error);
                    throw error; // Stop processing remaining batches
                }
            }

            // Clear successfully synced offline data
            for (const expense of validExpenses) {
                await this.removeOfflineExpense(expense.tempId);
            }
            
            this.lastSyncTime = new Date();
            this.retryCount = 0;
            console.log(`Offline data sync completed successfully - ${syncedCount} expenses synced`);

            // Update sync metadata
            if (window.app) {
                await window.app.updateSyncMetadata({
                    syncedCount,
                    lastSyncTime: this.lastSyncTime.toISOString()
                });
            }

            // Show success notification
            if (window.app && window.app.ui) {
                window.app.ui.showToast(`${syncedCount} offline expenses synced successfully!`, 'success');
            }

            // Refresh current view to show synced data
            if (window.app) {
                await window.app.loadMonthlyExpenses();
            }

        } catch (error) {
            console.error('Sync failed:', error);
            this.retryCount++;
            
            // Show error notification
            if (window.app && window.app.ui) {
                const message = this.retryCount < this.maxRetries 
                    ? `Sync failed. Retrying... (${this.retryCount}/${this.maxRetries})`
                    : 'Sync failed after maximum retries. Will try again later.';
                window.app.ui.showToast(message, 'error');
            }
            
            // Schedule retry with exponential backoff if not exceeded max retries
            if (this.retryCount < this.maxRetries) {
                this.scheduleRetry();
            }
            
        } finally {
            this.syncInProgress = false;
            
            // Update sync status in UI
            if (window.app) {
                window.app.updateSyncStatus(this.isOnline ? 'online' : 'offline');
            }
        }
    }

    async syncExpenseBatchWithRetry(expenses) {
        const expenseService = new ExpenseService();
        let lastError;
        
        for (let attempt = 0; attempt < 3; attempt++) {
            try {
                await this.syncExpenseBatch(expenses, expenseService);
                return; // Success
            } catch (error) {
                lastError = error;
                if (attempt < 2) {
                    const delay = 1000 * Math.pow(2, attempt);
                    console.log(`Batch sync attempt ${attempt + 1} failed, retrying in ${delay}ms...`);
                    await new Promise(resolve => setTimeout(resolve, delay));
                }
            }
        }
        
        throw lastError;
    }

    async syncExpenseBatch(expenses) {
        const expenseService = new ExpenseService();
        
        for (const expense of expenses) {
            try {
                // Remove temporary offline properties
                const cleanExpense = { ...expense };
                delete cleanExpense.offline;
                delete cleanExpense.tempId;
                delete cleanExpense.timestamp;
                
                // Create expense on server
                const savedExpense = await expenseService.createExpense(cleanExpense);
                
                // Update local IndexedDB with server ID and mark as synced
                if (window.app && window.app.db) {
                    savedExpense.synced = true;
                    await this.updateExpenseInIndexedDB(expense.tempId, savedExpense);
                }
                
                console.log(`Synced expense: ${expense.description} (${expense.amount})`);
                
            } catch (error) {
                console.error(`Failed to sync expense ${expense.description}:`, error);
                
                // If it's a validation error (400), remove from offline queue
                if (error.message.includes('400') || error.message.includes('Invalid')) {
                    console.warn(`Removing invalid expense from queue: ${expense.description}`);
                    await this.removeOfflineExpense(expense.tempId);
                    continue; // Don't throw, continue with next expense
                }
                
                throw error; // Re-throw for other errors
            }
        }
    }

    async getOfflineExpenses() {
        return new Promise((resolve, reject) => {
            if (!window.app || !window.app.db) {
                resolve([]);
                return;
            }

            const transaction = window.app.db.transaction(['offlineExpenses'], 'readonly');
            const store = transaction.objectStore('offlineExpenses');
            const request = store.getAll();

            request.onsuccess = () => {
                resolve(request.result || []);
            };

            request.onerror = () => {
                console.error('Failed to get offline expenses:', request.error);
                reject(request.error);
            };
        });
    }

    async clearOfflineExpenses() {
        return new Promise((resolve, reject) => {
            if (!window.app || !window.app.db) {
                resolve();
                return;
            }

            const transaction = window.app.db.transaction(['offlineExpenses'], 'readwrite');
            const store = transaction.objectStore('offlineExpenses');
            const request = store.clear();

            request.onsuccess = () => {
                console.log('Offline expenses cleared');
                resolve();
            };

            request.onerror = () => {
                console.error('Failed to clear offline expenses:', request.error);
                reject(request.error);
            };
        });
    }

    async removeOfflineExpense(id) {
        return new Promise((resolve, reject) => {
            if (!window.app || !window.app.db) {
                resolve();
                return;
            }

            const transaction = window.app.db.transaction(['offlineExpenses'], 'readwrite');
            const store = transaction.objectStore('offlineExpenses');
            const request = store.delete(id);

            request.onsuccess = () => {
                resolve();
            };

            request.onerror = () => {
                console.error('Failed to remove offline expense:', request.error);
                reject(request.error);
            };
        });
    }

    async updateExpenseInIndexedDB(oldId, newExpense) {
        return new Promise((resolve, reject) => {
            if (!window.app || !window.app.db) {
                resolve();
                return;
            }

            const transaction = window.app.db.transaction(['expenses'], 'readwrite');
            const store = transaction.objectStore('expenses');
            
            // Remove old expense with temporary ID
            const deleteRequest = store.delete(oldId);
            
            deleteRequest.onsuccess = () => {
                // Add new expense with server ID
                const addRequest = store.put(newExpense);
                
                addRequest.onsuccess = () => {
                    resolve();
                };
                
                addRequest.onerror = () => {
                    reject(addRequest.error);
                };
            };
            
            deleteRequest.onerror = () => {
                reject(deleteRequest.error);
            };
        });
    }

    createBatches(array, batchSize) {
        const batches = [];
        for (let i = 0; i < array.length; i += batchSize) {
            batches.push(array.slice(i, i + batchSize));
        }
        return batches;
    }

    scheduleRetry() {
        // Exponential backoff retry with jitter
        const baseDelay = this.baseRetryDelay * Math.pow(2, this.retryCount - 1);
        const jitter = Math.random() * 1000; // Add up to 1 second of jitter
        const retryDelay = Math.min(30000, baseDelay + jitter); // Cap at 30 seconds
        
        console.log(`Scheduling retry in ${Math.round(retryDelay)}ms (attempt ${this.retryCount}/${this.maxRetries})`);
        
        setTimeout(() => {
            if (this.isOnline && this.autoSyncEnabled && this.retryCount < this.maxRetries) {
                this.syncOfflineData();
            }
        }, retryDelay);
    }

    // Background sync registration
    async registerBackgroundSync() {
        if (this.swRegistration) {
            try {
                await this.swRegistration.sync.register('expense-sync');
                console.log('Background sync registered');
            } catch (error) {
                console.error('Failed to register background sync:', error);
            }
        }
    }

    // Manual sync trigger
    async triggerManualSync() {
        if (!this.isOnline) {
            if (window.app && window.app.ui) {
                window.app.ui.showToast('Cannot sync while offline', 'warning');
            }
            return;
        }

        await this.syncOfflineData();
    }

    // Sync status information
    getSyncStatus() {
        return {
            isOnline: this.isOnline,
            syncInProgress: this.syncInProgress,
            autoSyncEnabled: this.autoSyncEnabled,
            lastSyncTime: this.lastSyncTime,
            retryCount: this.retryCount,
            maxRetries: this.maxRetries,
            hasOfflineData: this.hasOfflineData()
        };
    }

    async hasOfflineData() {
        try {
            const offlineExpenses = await this.getOfflineExpenses();
            return offlineExpenses.length > 0;
        } catch (error) {
            console.error('Failed to check offline data:', error);
            return false;
        }
    }

    // Conflict resolution
    async resolveConflicts(localExpense, serverExpense) {
        // Simple timestamp-based resolution
        // The expense with the later updatedAt timestamp wins
        const localTime = new Date(localExpense.updatedAt || localExpense.createdAt);
        const serverTime = new Date(serverExpense.updatedAt || serverExpense.createdAt);
        
        if (localTime > serverTime) {
            console.log('Local version is newer, keeping local changes');
            return localExpense;
        } else {
            console.log('Server version is newer, using server version');
            return serverExpense;
        }
    }

    // Data validation before sync
    validateExpenseData(expense) {
        const required = ['description', 'amount', 'date', 'categoryId'];
        
        for (const field of required) {
            if (!expense[field]) {
                throw new Error(`Missing required field: ${field}`);
            }
        }
        
        if (expense.amount <= 0) {
            throw new Error('Amount must be greater than 0');
        }
        
        if (isNaN(new Date(expense.date).getTime())) {
            throw new Error('Invalid date format');
        }
        
        return true;
    }

    // Periodic sync check
    startPeriodicSync(intervalMinutes = 5) {
        this.periodicSyncInterval = setInterval(() => {
            if (this.isOnline && this.autoSyncEnabled && !this.syncInProgress) {
                // Only sync if there's offline data to sync
                this.hasOfflineData().then(hasData => {
                    if (hasData) {
                        console.log('Periodic sync triggered - offline data detected');
                        this.syncOfflineData();
                    }
                });
            }
        }, intervalMinutes * 60 * 1000);
    }

    // Enhanced queue management
    async queueExpenseForSync(expense) {
        try {
            // Add timestamp and temporary ID for offline tracking
            const queuedExpense = {
                ...expense,
                tempId: Date.now() + Math.random(), // Unique temporary ID
                timestamp: Date.now(),
                offline: true,
                queuedAt: new Date().toISOString()
            };

            // Save to offline queue
            if (window.app && window.app.db) {
                await window.app.saveToIndexedDB('offlineExpenses', queuedExpense);
                console.log('Expense queued for sync:', queuedExpense.description);
                
                // Register for background sync if supported
                await this.registerBackgroundSync();
                
                return queuedExpense;
            }
        } catch (error) {
            console.error('Failed to queue expense for sync:', error);
            throw error;
        }
    }

    // API Communication Layer
    async makeApiRequest(url, options = {}) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10 second timeout
        
        try {
            const response = await fetch(url, {
                ...options,
                signal: controller.signal,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });
            
            clearTimeout(timeoutId);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return response;
        } catch (error) {
            clearTimeout(timeoutId);
            
            if (error.name === 'AbortError') {
                throw new Error('Request timeout');
            }
            
            throw error;
        }
    }

    // Enhanced connectivity-based operations
    async performOnlineOperation(operation, fallbackOperation = null) {
        if (this.isOnline) {
            try {
                return await operation();
            } catch (error) {
                console.error('Online operation failed:', error);
                
                // If network error, switch to offline mode
                if (error.message.includes('fetch') || error.message.includes('timeout')) {
                    this.handleConnectivityChange(false);
                }
                
                // Try fallback if provided
                if (fallbackOperation) {
                    console.log('Attempting fallback operation...');
                    return await fallbackOperation();
                }
                
                throw error;
            }
        } else if (fallbackOperation) {
            console.log('Offline - using fallback operation');
            return await fallbackOperation();
        } else {
            throw new Error('Operation requires internet connection');
        }
    }

    // Cleanup method
    destroy() {
        if (this.connectivityCheckInterval) {
            clearInterval(this.connectivityCheckInterval);
        }
        
        if (this.periodicSyncInterval) {
            clearInterval(this.periodicSyncInterval);
        }
        
        // Remove event listeners
        window.removeEventListener('online', this.handleConnectivityChange);
        window.removeEventListener('offline', this.handleConnectivityChange);
        document.removeEventListener('visibilitychange', this.handleVisibilityChange);
    }
}