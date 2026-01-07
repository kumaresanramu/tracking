// Main Application Controller
class ExpenseTrackerApp {
    constructor() {
        this.currentPage = 'dashboard';
        this.currentMonth = new Date().getMonth();
        this.currentYear = new Date().getFullYear();
        this.expenses = [];
        this.categories = [];
        
        this.init();
    }

    async init() {
        // Initialize services
        this.expenseService = new ExpenseService();
        this.syncService = new SyncService();
        this.analytics = new Analytics();
        this.ui = new UIComponents();
        this.errorHandler = new ErrorHandler();
        
        // Connect error handler to UI
        this.errorHandler.setUI(this.ui);
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Initialize IndexedDB for offline storage
        await this.initIndexedDB();
        
        // Load initial data
        await this.loadInitialData();
        
        // Set up connectivity monitoring
        this.setupConnectivityMonitoring();
        
        // Update UI
        this.updateUI();
        
        console.log('Expense Tracker App initialized');
    }

    setupEventListeners() {
        // Mobile navigation toggle
        const navToggle = document.getElementById('nav-menu-toggle');
        const navMenu = document.getElementById('nav-menu');
        
        if (navToggle && navMenu) {
            navToggle.addEventListener('click', () => {
                navMenu.classList.toggle('active');
                const isOpen = navMenu.classList.contains('active');
                navToggle.setAttribute('aria-expanded', isOpen);
                navToggle.querySelector('span').textContent = isOpen ? '✕' : '☰';
            });
            
            // Close mobile menu when clicking outside
            document.addEventListener('click', (e) => {
                if (!navToggle.contains(e.target) && !navMenu.contains(e.target)) {
                    navMenu.classList.remove('active');
                    navToggle.setAttribute('aria-expanded', 'false');
                    navToggle.querySelector('span').textContent = '☰';
                }
            });
            
            // Close mobile menu on window resize to desktop
            window.addEventListener('resize', () => {
                if (window.innerWidth >= 768) {
                    navMenu.classList.remove('active');
                    navToggle.setAttribute('aria-expanded', 'false');
                    navToggle.querySelector('span').textContent = '☰';
                }
            });
        }

        // Navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const page = e.target.dataset.page;
                this.navigateToPage(page);
                
                // Close mobile menu after navigation
                if (navMenu && navMenu.classList.contains('active')) {
                    navMenu.classList.remove('active');
                    if (navToggle) {
                        navToggle.setAttribute('aria-expanded', 'false');
                        navToggle.querySelector('span').textContent = '☰';
                    }
                }
            });
        });

        // Expense form
        const expenseForm = document.getElementById('expense-form');
        if (expenseForm) {
            expenseForm.addEventListener('submit', (e) => this.handleExpenseSubmit(e));
            
            // Real-time validation
            const amountInput = document.getElementById('expense-amount');
            const descriptionInput = document.getElementById('expense-description');
            const categorySelect = document.getElementById('expense-category');
            
            if (amountInput) {
                amountInput.addEventListener('input', (e) => {
                    const value = parseFloat(e.target.value);
                    if (e.target.value && (isNaN(value) || value <= 0)) {
                        this.ui.addFieldError(e.target);
                        this.ui.showToast('Amount must be a positive number', 'error', 2000);
                    } else {
                        this.ui.removeFieldError(e.target);
                    }
                });
            }
            
            if (descriptionInput) {
                descriptionInput.addEventListener('input', (e) => {
                    if (e.target.value.trim().length === 0) {
                        this.ui.addFieldError(e.target);
                    } else {
                        this.ui.removeFieldError(e.target);
                    }
                });
            }
            
            if (categorySelect) {
                categorySelect.addEventListener('change', (e) => {
                    if (!e.target.value) {
                        this.ui.addFieldError(e.target);
                    } else {
                        this.ui.removeFieldError(e.target);
                    }
                });
            }
        }

        // Month navigation
        const prevMonthBtn = document.getElementById('prev-month');
        const nextMonthBtn = document.getElementById('next-month');
        
        if (prevMonthBtn) {
            prevMonthBtn.addEventListener('click', () => this.navigateMonth(-1));
        }
        
        if (nextMonthBtn) {
            nextMonthBtn.addEventListener('click', () => this.navigateMonth(1));
        }

        // Quick actions
        const quickAddBtn = document.getElementById('quick-add-expense');
        const viewAnalyticsBtn = document.getElementById('view-analytics');
        
        if (quickAddBtn) {
            quickAddBtn.addEventListener('click', () => this.navigateToPage('expenses'));
        }
        
        if (viewAnalyticsBtn) {
            viewAnalyticsBtn.addEventListener('click', () => this.navigateToPage('analytics'));
        }

        // Settings
        const autoSyncToggle = document.getElementById('auto-sync-enabled');
        if (autoSyncToggle) {
            autoSyncToggle.addEventListener('change', (e) => {
                this.syncService.setAutoSync(e.target.checked);
            });
        }
    }

    async initIndexedDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open('ExpenseTrackerDB', 2);
            
            request.onerror = () => {
                console.error('Failed to open IndexedDB');
                reject(request.error);
            };
            
            request.onsuccess = (event) => {
                this.db = event.target.result;
                console.log('IndexedDB initialized');
                resolve();
            };
            
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                
                // Create expenses object store
                if (!db.objectStoreNames.contains('expenses')) {
                    const expenseStore = db.createObjectStore('expenses', { keyPath: 'id', autoIncrement: true });
                    expenseStore.createIndex('date', 'date', { unique: false });
                    expenseStore.createIndex('categoryId', 'categoryId', { unique: false });
                    expenseStore.createIndex('month', ['year', 'month'], { unique: false });
                    expenseStore.createIndex('synced', 'synced', { unique: false });
                }
                
                // Create categories object store
                if (!db.objectStoreNames.contains('categories')) {
                    const categoryStore = db.createObjectStore('categories', { keyPath: 'id', autoIncrement: true });
                    categoryStore.createIndex('name', 'name', { unique: false });
                    categoryStore.createIndex('parentId', 'parentId', { unique: false });
                }
                
                // Create offline expenses queue
                if (!db.objectStoreNames.contains('offlineExpenses')) {
                    const offlineStore = db.createObjectStore('offlineExpenses', { keyPath: 'tempId', autoIncrement: true });
                    offlineStore.createIndex('timestamp', 'timestamp', { unique: false });
                }
                
                // Create app settings store
                if (!db.objectStoreNames.contains('settings')) {
                    db.createObjectStore('settings', { keyPath: 'key' });
                }
                
                // Create sync metadata store
                if (!db.objectStoreNames.contains('syncMetadata')) {
                    const syncStore = db.createObjectStore('syncMetadata', { keyPath: 'id' });
                    syncStore.createIndex('lastSync', 'lastSync', { unique: false });
                }
                
                console.log('IndexedDB schema created/updated');
            };
        });
    }

    async loadInitialData() {
        try {
            // Load categories first
            await this.loadCategories();
            
            // Load expenses for current month
            await this.loadMonthlyExpenses();
            
            // Update dashboard stats
            this.updateDashboardStats();
            
        } catch (error) {
            console.error('Failed to load initial data:', error);
            
            // Use error handler for better user experience
            this.errorHandler.handleError(error, 'Initial Data Load', {
                onRetry: () => this.loadInitialData(),
                silent: false
            });
            
            // Try to load from local storage
            await this.loadOfflineData();
        }
    }

    async loadCategories() {
        try {
            this.categories = await this.expenseService.getCategories();
            this.populateCategoryDropdown();
        } catch (error) {
            console.error('Failed to load categories:', error);
            
            // Handle error with retry option
            this.errorHandler.handleApiError(error, '/api/categories', {
                onRetry: () => this.loadCategories(),
                silent: true // Don't show toast since this is called from loadInitialData
            });
            
            // Load from IndexedDB if available
            this.categories = await this.getFromIndexedDB('categories') || [];
            this.populateCategoryDropdown();
        }
    }

    async loadMonthlyExpenses() {
        try {
            this.expenses = await this.expenseService.getMonthlyExpenses(this.currentYear, this.currentMonth + 1);
            this.displayMonthlyExpenses();
        } catch (error) {
            console.error('Failed to load monthly expenses:', error);
            
            // Handle error with retry option
            this.errorHandler.handleApiError(error, '/api/expenses/month', {
                onRetry: () => this.loadMonthlyExpenses(),
                silent: true // Don't show toast since this is called from loadInitialData
            });
            
            // Load from IndexedDB if available
            this.expenses = await this.getMonthlyExpensesFromIndexedDB() || [];
            this.displayMonthlyExpenses();
        }
    }

    async loadOfflineData() {
        try {
            this.categories = await this.getFromIndexedDB('categories') || [];
            this.expenses = await this.getMonthlyExpensesFromIndexedDB() || [];
            
            this.populateCategoryDropdown();
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
        } catch (error) {
            console.error('Failed to load offline data:', error);
        }
    }

    setupConnectivityMonitoring() {
        // Monitor online/offline status
        window.addEventListener('online', () => {
            this.updateSyncStatus('online');
            // Sync service will handle automatic sync on connectivity restoration
        });
        
        window.addEventListener('offline', () => {
            this.updateSyncStatus('offline');
        });
        
        // Listen for custom connectivity change events from sync service
        window.addEventListener('connectivitychange', (event) => {
            const { online } = event.detail;
            this.updateSyncStatus(online ? 'online' : 'offline');
        });
        
        // Initial status
        this.updateSyncStatus(navigator.onLine ? 'online' : 'offline');
    }

    updateSyncStatus(status, details = {}) {
        const indicator = document.getElementById('sync-indicator');
        const statusContainer = document.getElementById('sync-status-container');
        
        if (statusContainer) {
            const statusInfo = {
                state: status,
                lastSync: details.lastSync || this.syncService?.lastSyncTime,
                pendingCount: details.pendingCount || 0,
                progress: details.progress,
                errorMessage: details.errorMessage,
                syncedCount: details.syncedCount
            };
            
            this.ui.renderSyncStatus(statusInfo, statusContainer);
        }
        
        // Legacy support for existing sync indicator
        if (indicator) {
            const text = document.getElementById('sync-text');
            indicator.className = `sync-indicator ${status}`;
            
            if (text) {
                switch (status) {
                    case 'online':
                        text.textContent = 'Online';
                        break;
                    case 'offline':
                        text.textContent = 'Offline';
                        break;
                    case 'syncing':
                        text.textContent = 'Syncing...';
                        break;
                    case 'error':
                        text.textContent = 'Sync Error';
                        break;
                    case 'success':
                        text.textContent = 'Synced';
                        break;
                }
            }
        }
        
        // Update connection status indicator
        const connectionStatus = document.getElementById('connection-status');
        if (connectionStatus) {
            this.ui.renderConnectionStatus(status === 'online', connectionStatus);
        }
    }

    navigateToPage(page) {
        // Update navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        
        document.querySelector(`[data-page="${page}"]`).classList.add('active');
        
        // Show/hide pages
        document.querySelectorAll('.page').forEach(pageEl => {
            pageEl.classList.remove('active');
        });
        
        document.getElementById(`${page}-page`).classList.add('active');
        
        this.currentPage = page;
        
        // Load page-specific data
        this.loadPageData(page);
    }

    async loadPageData(page) {
        switch (page) {
            case 'dashboard':
                this.updateDashboardStats();
                this.displayRecentExpenses();
                break;
            case 'expenses':
                this.displayMonthlyExpenses();
                break;
            case 'analytics':
                await this.analytics.loadCharts();
                break;
            case 'reminders':
                await this.loadReminders();
                break;
            case 'settings':
                // Load settings
                break;
        }
    }

    async handleExpenseSubmit(e) {
        e.preventDefault();
        
        // Validate form
        const validationErrors = this.ui.validateForm(e.target);
        if (validationErrors.length > 0) {
            this.errorHandler.handleFormValidationError(e.target, 
                validationErrors.map(msg => ({ field: 'general', message: msg }))
            );
            return;
        }
        
        const formData = new FormData(e.target);
        const expense = {
            description: formData.get('description').trim(),
            amount: parseFloat(formData.get('amount')),
            date: formData.get('date'),
            categoryId: parseInt(formData.get('categoryId')),
            createdAt: new Date().toISOString(),
            synced: false
        };
        
        // Additional validation
        if (expense.amount <= 0) {
            this.ui.showToast('Amount must be greater than 0', 'error');
            return;
        }
        
        if (!expense.description) {
            this.ui.showToast('Description is required', 'error');
            return;
        }
        
        if (!expense.categoryId) {
            this.ui.showToast('Please select a category', 'error');
            return;
        }
        
        // Add month/year for indexing
        const expenseDate = new Date(expense.date);
        expense.year = expenseDate.getFullYear();
        expense.month = expenseDate.getMonth() + 1;
        
        try {
            this.updateSyncStatus('syncing', { progress: 0 });
            
            if (navigator.onLine) {
                // Online - save to server
                const savedExpense = await this.expenseService.createExpense(expense);
                savedExpense.synced = true;
                this.expenses.unshift(savedExpense);
                
                // Also save to IndexedDB for offline access
                await this.saveToIndexedDB('expenses', savedExpense);
                
                this.ui.showToast('Expense added successfully!', 'success');
                this.updateSyncStatus('success', { syncedCount: 1 });
            } else {
                // Offline - save to IndexedDB and queue for sync
                const queuedExpense = await this.syncService.queueExpenseForSync(expense);
                
                // Also add to main expenses for immediate UI update
                queuedExpense.id = queuedExpense.tempId;
                this.expenses.unshift(queuedExpense);
                await this.saveToIndexedDB('expenses', queuedExpense);
                
                this.ui.showToast('Expense saved offline. Will sync when online.', 'warning');
                this.updateSyncStatus('offline', { pendingCount: 1 });
            }
            
            // Update UI
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
            
            // Reset form
            e.target.reset();
            
            // Set today's date as default
            document.getElementById('expense-date').value = new Date().toISOString().split('T')[0];
            
            // Clear any validation errors
            e.target.querySelectorAll('.form-group input, .form-group select').forEach(field => {
                this.ui.removeFieldError(field);
            });
            
        } catch (error) {
            console.error('Failed to save expense:', error);
            
            // Use error handler for better error reporting
            this.errorHandler.handleApiError(error, '/api/expenses', {
                onRetry: () => this.handleExpenseSubmit(e),
                retryable: true
            });
            
            this.updateSyncStatus('error', { errorMessage: error.message });
        } finally {
            // Reset sync status after a delay
            setTimeout(() => {
                this.updateSyncStatus(navigator.onLine ? 'online' : 'offline');
            }, 2000);
        }
    }

    navigateMonth(direction) {
        this.currentMonth += direction;
        
        if (this.currentMonth < 0) {
            this.currentMonth = 11;
            this.currentYear--;
        } else if (this.currentMonth > 11) {
            this.currentMonth = 0;
            this.currentYear++;
        }
        
        this.updateMonthDisplay();
        this.loadMonthlyExpenses();
    }

    updateMonthDisplay() {
        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];
        
        const monthYearElement = document.getElementById('current-month-year');
        if (monthYearElement) {
            monthYearElement.textContent = `${monthNames[this.currentMonth]} ${this.currentYear}`;
        }
    }

    populateCategoryDropdown() {
        const categorySelect = document.getElementById('expense-category');
        if (!categorySelect) return;
        
        // Clear existing options (except the first one)
        while (categorySelect.children.length > 1) {
            categorySelect.removeChild(categorySelect.lastChild);
        }
        
        // Build hierarchical category options
        const hierarchicalCategories = this.buildCategoryHierarchy(this.categories);
        this.addCategoryOptionsToSelect(categorySelect, hierarchicalCategories, 0);
    }

    buildCategoryHierarchy(categories) {
        const categoryMap = {};
        const rootCategories = [];
        
        // Create category map
        categories.forEach(category => {
            categoryMap[category.id] = { ...category, children: [] };
        });
        
        // Build hierarchy
        categories.forEach(category => {
            if (category.parentId && categoryMap[category.parentId]) {
                categoryMap[category.parentId].children.push(categoryMap[category.id]);
            } else {
                rootCategories.push(categoryMap[category.id]);
            }
        });
        
        return rootCategories;
    }

    addCategoryOptionsToSelect(selectElement, categories, level) {
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            
            // Add indentation for subcategories
            const indent = '  '.repeat(level);
            option.textContent = `${indent}${category.name}`;
            
            selectElement.appendChild(option);
            
            // Add subcategories recursively
            if (category.children && category.children.length > 0) {
                this.addCategoryOptionsToSelect(selectElement, category.children, level + 1);
            }
        });
    }

    displayMonthlyExpenses() {
        const container = document.getElementById('monthly-expenses-list');
        if (!container) return;
        
        if (this.expenses.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">💰</div>
                    <div class="empty-state-title">No expenses yet</div>
                    <div class="empty-state-description">Add your first expense to get started!</div>
                </div>
            `;
            return;
        }
        
        container.innerHTML = this.expenses.map(expense => {
            const category = this.categories.find(c => c.id === expense.categoryId);
            const categoryName = category ? category.name : 'Unknown';
            
            return `
                <div class="expense-item">
                    <div class="expense-details">
                        <div class="expense-description">${expense.description}</div>
                        <div class="expense-meta">${categoryName} • ${new Date(expense.date).toLocaleDateString()}</div>
                    </div>
                    <div class="expense-amount">$${expense.amount.toFixed(2)}</div>
                    <div class="expense-item-actions">
                        <button class="edit-btn" onclick="app.editExpense(${expense.id})">✏️</button>
                        <button class="delete-btn" onclick="app.deleteExpense(${expense.id})">🗑️</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    displayRecentExpenses() {
        const container = document.getElementById('recent-expenses-list');
        if (!container) return;
        
        const recentExpenses = this.expenses.slice(0, 5);
        
        if (recentExpenses.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">No recent expenses</div>
                    <button class="btn btn-primary" onclick="app.navigateToPage('expenses')">Add your first expense</button>
                </div>
            `;
            return;
        }
        
        container.innerHTML = recentExpenses.map(expense => {
            const category = this.categories.find(c => c.id === expense.categoryId);
            const categoryName = category ? category.name : 'Unknown';
            
            return `
                <div class="expense-item">
                    <div class="expense-details">
                        <div class="expense-description">${expense.description}</div>
                        <div class="expense-meta">${categoryName} • ${new Date(expense.date).toLocaleDateString()}</div>
                    </div>
                    <div class="expense-amount">$${expense.amount.toFixed(2)}</div>
                </div>
            `;
        }).join('');
    }

    updateDashboardStats() {
        const now = new Date();
        const thisMonth = this.expenses.filter(e => {
            const expenseDate = new Date(e.date);
            return expenseDate.getMonth() === now.getMonth() && 
                   expenseDate.getFullYear() === now.getFullYear();
        });
        
        const last7Days = this.expenses.filter(e => {
            const expenseDate = new Date(e.date);
            const daysDiff = (now - expenseDate) / (1000 * 60 * 60 * 24);
            return daysDiff <= 7;
        });
        
        const monthlyTotal = thisMonth.reduce((sum, e) => sum + e.amount, 0);
        const weeklyTotal = last7Days.reduce((sum, e) => sum + e.amount, 0);
        
        // Update UI
        const monthlyElement = document.getElementById('monthly-total');
        const weeklyElement = document.getElementById('weekly-total');
        const countElement = document.getElementById('total-count');
        
        if (monthlyElement) monthlyElement.textContent = `$${monthlyTotal.toFixed(2)}`;
        if (weeklyElement) weeklyElement.textContent = `$${weeklyTotal.toFixed(2)}`;
        if (countElement) countElement.textContent = this.expenses.length.toString();
    }

    async editExpense(id) {
        // TODO: Implement expense editing
        console.log('Edit expense:', id);
    }

    async deleteExpense(id) {
        if (!confirm('Are you sure you want to delete this expense?')) {
            return;
        }
        
        try {
            if (navigator.onLine) {
                await this.expenseService.deleteExpense(id);
            }
            
            // Remove from local arrays
            this.expenses = this.expenses.filter(e => e.id !== id);
            
            // Remove from IndexedDB
            await this.deleteFromIndexedDB('expenses', id);
            
            // Update UI
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
            
            this.ui.showToast('Expense deleted successfully!', 'success');
            
        } catch (error) {
            console.error('Failed to delete expense:', error);
            
            // Use error handler for better error reporting
            this.errorHandler.handleApiError(error, `/api/expenses/${id}`, {
                onRetry: () => this.deleteExpense(id),
                retryable: true
            });
        }
    }

    updateUI() {
        // Set today's date as default in expense form
        const dateInput = document.getElementById('expense-date');
        if (dateInput && !dateInput.value) {
            dateInput.value = new Date().toISOString().split('T')[0];
        }
        
        // Update month display
        this.updateMonthDisplay();
        
        // Focus on description field for better UX
        const descriptionInput = document.getElementById('expense-description');
        if (descriptionInput && this.currentPage === 'expenses') {
            setTimeout(() => descriptionInput.focus(), 100);
        }
    }

    // IndexedDB helper methods
    async saveToIndexedDB(storeName, data) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.put(data);
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async getFromIndexedDB(storeName, key = null) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readonly');
            const store = transaction.objectStore(storeName);
            const request = key ? store.get(key) : store.getAll();
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async getMonthlyExpensesFromIndexedDB() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['expenses'], 'readonly');
            const store = transaction.objectStore('expenses');
            const index = store.index('month');
            const request = index.getAll([this.currentYear, this.currentMonth + 1]);
            
            request.onsuccess = () => {
                const expenses = request.result || [];
                // Sort by date descending
                expenses.sort((a, b) => new Date(b.date) - new Date(a.date));
                resolve(expenses);
            };
            
            request.onerror = () => reject(request.error);
        });
    }

    async deleteFromIndexedDB(storeName, id) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.delete(id);
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async clearIndexedDBStore(storeName) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.clear();
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async getOfflineExpenseCount() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['offlineExpenses'], 'readonly');
            const store = transaction.objectStore('offlineExpenses');
            const request = store.count();
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async saveSetting(key, value) {
        return this.saveToIndexedDB('settings', { key, value, updatedAt: new Date().toISOString() });
    }

    async getSetting(key, defaultValue = null) {
        try {
            const setting = await this.getFromIndexedDB('settings', key);
            return setting ? setting.value : defaultValue;
        } catch (error) {
            console.error('Failed to get setting:', error);
            return defaultValue;
        }
    }

    async loadReminders() {
        try {
            const reminders = await this.expenseService.getPaymentReminders();
            this.displayReminders(reminders);
        } catch (error) {
            console.error('Failed to load reminders:', error);
            
            // Handle error with retry option
            this.errorHandler.handleApiError(error, '/api/reminders', {
                onRetry: () => this.loadReminders(),
                silent: false
            });
        }
    }

    displayReminders(reminders) {
        const container = document.getElementById('reminders-list');
        if (!container) return;
        
        if (reminders.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🔔</div>
                    <div class="empty-state-title">No payment reminders</div>
                    <div class="empty-state-description">Set up reminders for recurring payments</div>
                    <button class="btn btn-primary" onclick="app.showAddReminderForm()">Add Reminder</button>
                </div>
            `;
            return;
        }
        
        container.innerHTML = reminders.map(reminder => {
            const dueDate = new Date(reminder.dueDate);
            const isOverdue = dueDate < new Date();
            const statusClass = isOverdue ? 'overdue' : 'upcoming';
            
            return `
                <div class="reminder-item ${statusClass}">
                    <div class="reminder-details">
                        <div class="reminder-name">${reminder.name}</div>
                        <div class="reminder-meta">
                            ${reminder.amount.toFixed(2)} • Due: ${dueDate.toLocaleDateString()}
                            ${reminder.frequency ? ` • ${reminder.frequency}` : ''}
                        </div>
                    </div>
                    <div class="reminder-actions">
                        <button class="btn btn-small btn-primary" onclick="app.markReminderAsPaid(${reminder.id})">
                            Mark Paid
                        </button>
                        <button class="btn btn-small btn-secondary" onclick="app.editReminder(${reminder.id})">
                            Edit
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    async markReminderAsPaid(reminderId) {
        try {
            await this.expenseService.markReminderAsPaid(reminderId);
            this.ui.showToast('Reminder marked as paid!', 'success');
            await this.loadReminders(); // Refresh the list
        } catch (error) {
            console.error('Failed to mark reminder as paid:', error);
            this.errorHandler.handleApiError(error, `/api/reminders/${reminderId}/mark-paid`, {
                onRetry: () => this.markReminderAsPaid(reminderId),
                retryable: true
            });
        }
    }

    showAddReminderForm() {
        // TODO: Implement add reminder form
        this.ui.showToast('Add reminder form coming soon!', 'info');
    }

    editReminder(reminderId) {
        // TODO: Implement edit reminder functionality
        this.ui.showToast('Edit reminder coming soon!', 'info');
    }

    async updateSyncMetadata(data) {
        const metadata = {
            id: 'sync',
            lastSync: new Date().toISOString(),
            ...data
        };
        return this.saveToIndexedDB('syncMetadata', metadata);
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new ExpenseTrackerApp();
});