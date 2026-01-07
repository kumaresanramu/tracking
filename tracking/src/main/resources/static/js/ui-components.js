// UI Components - Handles UI interactions and components
class UIComponents {
    constructor() {
        this.toastContainer = null;
        this.modalOverlay = null;
        this.init();
    }

    init() {
        this.createToastContainer();
        this.createModalOverlay();
    }

    createToastContainer() {
        this.toastContainer = document.createElement('div');
        this.toastContainer.className = 'toast-container';
        document.body.appendChild(this.toastContainer);
    }

    createModalOverlay() {
        this.modalOverlay = document.createElement('div');
        this.modalOverlay.className = 'modal-overlay';
        this.modalOverlay.innerHTML = `
            <div class="modal">
                <div class="modal-header">
                    <h2 class="modal-title"></h2>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body"></div>
            </div>
        `;
        document.body.appendChild(this.modalOverlay);

        // Close modal on overlay click
        this.modalOverlay.addEventListener('click', (e) => {
            if (e.target === this.modalOverlay) {
                this.closeModal();
            }
        });

        // Close modal on close button click
        const closeBtn = this.modalOverlay.querySelector('.modal-close');
        closeBtn.addEventListener('click', () => this.closeModal());

        // Close modal on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.modalOverlay.classList.contains('active')) {
                this.closeModal();
            }
        });
    }

    // Toast notifications
    showToast(message, type = 'info', duration = 5000) {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div class="toast-message">${message}</div>
        `;

        this.toastContainer.appendChild(toast);

        // Auto remove after duration
        setTimeout(() => {
            this.removeToast(toast);
        }, duration);

        // Allow manual close
        toast.addEventListener('click', () => {
            this.removeToast(toast);
        });

        return toast;
    }

    removeToast(toast) {
        if (toast && toast.parentNode) {
            toast.style.animation = 'slideOut 0.3s ease-in';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }
    }

    // Modal dialogs
    showModal(title, content, options = {}) {
        const titleElement = this.modalOverlay.querySelector('.modal-title');
        const bodyElement = this.modalOverlay.querySelector('.modal-body');

        titleElement.textContent = title;
        bodyElement.innerHTML = content;

        this.modalOverlay.classList.add('active');

        // Handle options
        if (options.onClose) {
            this.modalOverlay.dataset.onClose = options.onClose;
        }

        return this.modalOverlay;
    }

    closeModal() {
        this.modalOverlay.classList.remove('active');
        
        // Call onClose callback if provided
        const onClose = this.modalOverlay.dataset.onClose;
        if (onClose && typeof window[onClose] === 'function') {
            window[onClose]();
        }
        
        delete this.modalOverlay.dataset.onClose;
    }

    // Confirmation dialog
    showConfirmDialog(message, onConfirm, onCancel) {
        const content = `
            <p>${message}</p>
            <div class="modal-actions">
                <button class="btn btn-primary confirm-btn">Confirm</button>
                <button class="btn btn-secondary cancel-btn">Cancel</button>
            </div>
        `;

        this.showModal('Confirm Action', content);

        const confirmBtn = this.modalOverlay.querySelector('.confirm-btn');
        const cancelBtn = this.modalOverlay.querySelector('.cancel-btn');

        confirmBtn.addEventListener('click', () => {
            this.closeModal();
            if (onConfirm) onConfirm();
        });

        cancelBtn.addEventListener('click', () => {
            this.closeModal();
            if (onCancel) onCancel();
        });
    }

    // Loading spinner
    showLoading(element, message = 'Loading...') {
        if (!element) return;

        const spinner = document.createElement('div');
        spinner.className = 'loading-overlay';
        spinner.innerHTML = `
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <div class="loading-message">${message}</div>
            </div>
        `;

        element.style.position = 'relative';
        element.appendChild(spinner);

        return spinner;
    }

    hideLoading(element) {
        if (!element) return;

        const spinner = element.querySelector('.loading-overlay');
        if (spinner) {
            spinner.remove();
        }
    }

    // Form validation
    validateForm(form) {
        const errors = [];
        const requiredFields = form.querySelectorAll('[required]');

        requiredFields.forEach(field => {
            if (!field.value.trim()) {
                errors.push(`${this.getFieldLabel(field)} is required`);
                this.addFieldError(field);
            } else {
                this.removeFieldError(field);
            }
        });

        // Validate specific field types
        const emailFields = form.querySelectorAll('input[type="email"]');
        emailFields.forEach(field => {
            if (field.value && !this.isValidEmail(field.value)) {
                errors.push(`${this.getFieldLabel(field)} must be a valid email address`);
                this.addFieldError(field);
            }
        });

        const numberFields = form.querySelectorAll('input[type="number"]');
        numberFields.forEach(field => {
            if (field.value && isNaN(field.value)) {
                errors.push(`${this.getFieldLabel(field)} must be a valid number`);
                this.addFieldError(field);
            }
        });

        return errors;
    }

    getFieldLabel(field) {
        const label = field.closest('.form-group')?.querySelector('label');
        return label ? label.textContent : field.name || field.id || 'Field';
    }

    addFieldError(field) {
        field.classList.add('error');
        field.style.borderColor = '#F44336';
    }

    removeFieldError(field) {
        field.classList.remove('error');
        field.style.borderColor = '';
    }

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    // Enhanced Category tree component with mobile-first design
    renderCategoryTree(categories, container, options = {}) {
        if (!container) return;

        const tree = this.buildCategoryTree(categories);
        container.innerHTML = this.renderCategoryTreeHTML(tree, options);

        // Add event listeners for expand/collapse
        container.addEventListener('click', (e) => {
            if (e.target.classList.contains('category-toggle')) {
                e.preventDefault();
                e.stopPropagation();
                
                const toggle = e.target;
                const categoryItem = toggle.closest('.category-item');
                const children = categoryItem.parentElement.querySelector('.category-children');
                
                if (children) {
                    const isExpanded = children.classList.contains('expanded');
                    children.classList.toggle('expanded');
                    toggle.classList.toggle('expanded');
                    toggle.textContent = isExpanded ? '▶' : '▼';
                    toggle.setAttribute('aria-expanded', !isExpanded);
                }
            }
        });

        // Add event listeners for selection
        if (options.selectable) {
            container.addEventListener('click', (e) => {
                if (e.target.classList.contains('category-name')) {
                    // Remove previous selection
                    container.querySelectorAll('.category-item').forEach(item => {
                        item.classList.remove('selected');
                    });
                    
                    // Add selection to clicked item
                    const categoryItem = e.target.closest('.category-item');
                    categoryItem.classList.add('selected');
                    
                    // Call selection callback
                    if (options.onSelect) {
                        const categoryId = e.target.dataset.categoryId;
                        options.onSelect(categoryId);
                    }
                }
            });
        }

        // Add keyboard navigation
        container.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                if (e.target.classList.contains('category-toggle')) {
                    e.target.click();
                } else if (e.target.classList.contains('category-name') && options.selectable) {
                    e.target.click();
                }
            }
        });
    }

    buildCategoryTree(categories) {
        const tree = [];
        const categoryMap = {};

        // Create category map
        categories.forEach(category => {
            categoryMap[category.id] = { ...category, children: [] };
        });

        // Build tree structure
        categories.forEach(category => {
            if (category.parentId && categoryMap[category.parentId]) {
                categoryMap[category.parentId].children.push(categoryMap[category.id]);
            } else {
                tree.push(categoryMap[category.id]);
            }
        });

        return tree;
    }

    renderCategoryTreeHTML(tree, options = {}, level = 0) {
        let html = '<ul class="category-tree">';

        tree.forEach(category => {
            const hasChildren = category.children && category.children.length > 0;
            const categoryId = category.id;
            const isSelectable = options.selectable ? 'tabindex="0"' : '';

            html += `
                <li>
                    <div class="category-item" data-level="${level}" ${isSelectable}>
                        ${hasChildren ? 
                            `<button class="category-toggle" aria-expanded="false" tabindex="0">▶</button>` : 
                            '<span class="category-spacer"></span>'
                        }
                        <span class="category-name" data-category-id="${categoryId}" ${isSelectable}>${category.name}</span>
                    </div>
                    ${hasChildren ? 
                        `<div class="category-children">${this.renderCategoryTreeHTML(category.children, options, level + 1)}</div>` : 
                        ''
                    }
                </li>
            `;
        });

        html += '</ul>';
        return html;
    }

    // Enhanced Category Dropdown Component
    renderCategoryDropdown(categories, container, options = {}) {
        if (!container) return;

        const selectedCategory = options.selectedId ? 
            categories.find(c => c.id == options.selectedId) : null;
        const selectedText = selectedCategory ? selectedCategory.name : (options.placeholder || 'Select a category');

        const dropdownHTML = `
            <div class="category-dropdown">
                <button class="category-dropdown-toggle" type="button" aria-haspopup="listbox" aria-expanded="false">
                    <span class="selected-text">${selectedText}</span>
                    <span class="dropdown-arrow">▼</span>
                </button>
                <div class="category-dropdown-menu" role="listbox">
                    ${this.renderCategoryDropdownItems(categories)}
                </div>
            </div>
        `;

        container.innerHTML = dropdownHTML;

        const toggle = container.querySelector('.category-dropdown-toggle');
        const menu = container.querySelector('.category-dropdown-menu');
        const selectedTextEl = container.querySelector('.selected-text');
        const arrow = container.querySelector('.dropdown-arrow');

        // Toggle dropdown
        toggle.addEventListener('click', (e) => {
            e.preventDefault();
            const isOpen = menu.classList.contains('active');
            
            // Close all other dropdowns
            document.querySelectorAll('.category-dropdown-menu.active').forEach(otherMenu => {
                if (otherMenu !== menu) {
                    otherMenu.classList.remove('active');
                    otherMenu.parentElement.querySelector('.category-dropdown-toggle').setAttribute('aria-expanded', 'false');
                    otherMenu.parentElement.querySelector('.dropdown-arrow').textContent = '▼';
                }
            });
            
            menu.classList.toggle('active');
            toggle.setAttribute('aria-expanded', !isOpen);
            arrow.textContent = isOpen ? '▼' : '▲';
        });

        // Handle item selection
        menu.addEventListener('click', (e) => {
            if (e.target.classList.contains('category-dropdown-item')) {
                const categoryId = e.target.dataset.categoryId;
                const categoryName = e.target.textContent.trim();
                
                // Update selected text
                selectedTextEl.textContent = categoryName;
                
                // Update selection state
                menu.querySelectorAll('.category-dropdown-item').forEach(item => {
                    item.classList.remove('selected');
                });
                e.target.classList.add('selected');
                
                // Close dropdown
                menu.classList.remove('active');
                toggle.setAttribute('aria-expanded', 'false');
                arrow.textContent = '▼';
                
                // Call selection callback
                if (options.onSelect) {
                    options.onSelect(categoryId, categoryName);
                }
            }
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!container.contains(e.target)) {
                menu.classList.remove('active');
                toggle.setAttribute('aria-expanded', 'false');
                arrow.textContent = '▼';
            }
        });

        // Keyboard navigation
        toggle.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
                e.preventDefault();
                toggle.click();
                if (e.key === 'ArrowDown') {
                    const firstItem = menu.querySelector('.category-dropdown-item');
                    if (firstItem) firstItem.focus();
                }
            }
        });

        menu.addEventListener('keydown', (e) => {
            const items = Array.from(menu.querySelectorAll('.category-dropdown-item'));
            const currentIndex = items.indexOf(document.activeElement);
            
            switch (e.key) {
                case 'ArrowDown':
                    e.preventDefault();
                    const nextIndex = (currentIndex + 1) % items.length;
                    items[nextIndex].focus();
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    const prevIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1;
                    items[prevIndex].focus();
                    break;
                case 'Enter':
                case ' ':
                    e.preventDefault();
                    if (document.activeElement.classList.contains('category-dropdown-item')) {
                        document.activeElement.click();
                    }
                    break;
                case 'Escape':
                    menu.classList.remove('active');
                    toggle.setAttribute('aria-expanded', 'false');
                    arrow.textContent = '▼';
                    toggle.focus();
                    break;
            }
        });
    }

    renderCategoryDropdownItems(categories, level = 0) {
        const tree = this.buildCategoryTree(categories);
        return this.renderCategoryDropdownItemsHTML(tree, level);
    }

    renderCategoryDropdownItemsHTML(tree, level = 0) {
        let html = '';

        tree.forEach(category => {
            html += `
                <div class="category-dropdown-item" data-category-id="${category.id}" data-level="${level}" tabindex="0">
                    ${category.name}
                </div>
            `;

            if (category.children && category.children.length > 0) {
                html += this.renderCategoryDropdownItemsHTML(category.children, level + 1);
            }
        });

        return html;
    }

    // Expense item component
    renderExpenseItem(expense, category) {
        return `
            <div class="expense-item" data-expense-id="${expense.id}">
                <div class="expense-details">
                    <div class="expense-description">${expense.description}</div>
                    <div class="expense-meta">
                        ${category ? category.name : 'Unknown'} • 
                        ${new Date(expense.date).toLocaleDateString()}
                        ${expense.offline ? ' • <span class="text-warning">Offline</span>' : ''}
                    </div>
                </div>
                <div class="expense-amount">$${expense.amount.toFixed(2)}</div>
                <div class="expense-item-actions">
                    <button class="edit-btn" title="Edit expense">✏️</button>
                    <button class="delete-btn" title="Delete expense">🗑️</button>
                </div>
            </div>
        `;
    }

    // Empty state component
    renderEmptyState(icon, title, description, actionButton = null) {
        let html = `
            <div class="empty-state">
                <div class="empty-state-icon">${icon}</div>
                <div class="empty-state-title">${title}</div>
                <div class="empty-state-description">${description}</div>
        `;

        if (actionButton) {
            html += `<div class="empty-state-action">${actionButton}</div>`;
        }

        html += '</div>';
        return html;
    }

    // Utility methods
    formatCurrency(amount, currency = 'USD') {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: currency
        }).format(amount);
    }

    formatDate(date, options = {}) {
        const defaultOptions = {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        };

        return new Date(date).toLocaleDateString('en-US', { ...defaultOptions, ...options });
    }

    formatRelativeTime(date) {
        const now = new Date();
        const diffInSeconds = Math.floor((now - new Date(date)) / 1000);

        if (diffInSeconds < 60) {
            return 'Just now';
        } else if (diffInSeconds < 3600) {
            const minutes = Math.floor(diffInSeconds / 60);
            return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
        } else if (diffInSeconds < 86400) {
            const hours = Math.floor(diffInSeconds / 3600);
            return `${hours} hour${hours > 1 ? 's' : ''} ago`;
        } else if (diffInSeconds < 604800) {
            const days = Math.floor(diffInSeconds / 86400);
            return `${days} day${days > 1 ? 's' : ''} ago`;
        } else {
            return this.formatDate(date);
        }
    }

    // Accessibility helpers
    announceToScreenReader(message) {
        const announcement = document.createElement('div');
        announcement.setAttribute('aria-live', 'polite');
        announcement.setAttribute('aria-atomic', 'true');
        announcement.className = 'sr-only';
        announcement.textContent = message;

        document.body.appendChild(announcement);

        setTimeout(() => {
            document.body.removeChild(announcement);
        }, 1000);
    }

    // Sync Status Display Component
    renderSyncStatus(status, container) {
        if (!container) return;

        const statusInfo = this.getSyncStatusInfo(status);
        
        container.innerHTML = `
            <div class="sync-status ${statusInfo.className}">
                <div class="sync-indicator ${statusInfo.className}">
                    <span class="sync-icon">${statusInfo.icon}</span>
                </div>
                <div class="sync-details">
                    <div class="sync-text">${statusInfo.text}</div>
                    <div class="sync-meta">${statusInfo.meta}</div>
                </div>
                ${statusInfo.showRetry ? '<button class="sync-retry-btn" title="Retry sync">↻</button>' : ''}
            </div>
        `;

        // Add retry functionality
        const retryBtn = container.querySelector('.sync-retry-btn');
        if (retryBtn && window.app && window.app.syncService) {
            retryBtn.addEventListener('click', () => {
                window.app.syncService.triggerManualSync();
            });
        }
    }

    getSyncStatusInfo(status) {
        const now = new Date();
        
        switch (status.state) {
            case 'online':
                return {
                    className: 'online',
                    icon: '●',
                    text: 'Online',
                    meta: status.lastSync ? `Last sync: ${this.formatRelativeTime(status.lastSync)}` : 'Ready to sync',
                    showRetry: false
                };
            case 'offline':
                return {
                    className: 'offline',
                    icon: '●',
                    text: 'Offline',
                    meta: status.pendingCount > 0 ? `${status.pendingCount} changes pending` : 'No pending changes',
                    showRetry: false
                };
            case 'syncing':
                return {
                    className: 'syncing',
                    icon: '↻',
                    text: 'Syncing...',
                    meta: status.progress ? `${status.progress}% complete` : 'Synchronizing data',
                    showRetry: false
                };
            case 'error':
                return {
                    className: 'error',
                    icon: '⚠',
                    text: 'Sync Error',
                    meta: status.errorMessage || 'Sync failed',
                    showRetry: true
                };
            case 'success':
                return {
                    className: 'success',
                    icon: '✓',
                    text: 'Synced',
                    meta: `${status.syncedCount || 0} items synced`,
                    showRetry: false
                };
            default:
                return {
                    className: 'unknown',
                    icon: '?',
                    text: 'Unknown',
                    meta: 'Status unknown',
                    showRetry: false
                };
        }
    }

    // Enhanced Error Display Component
    renderErrorMessage(error, container, options = {}) {
        if (!container) return;

        const errorInfo = this.analyzeErrorForDisplay(error);
        
        container.innerHTML = `
            <div class="error-message ${errorInfo.severity}">
                <div class="error-icon">${errorInfo.icon}</div>
                <div class="error-content">
                    <div class="error-title">${errorInfo.title}</div>
                    <div class="error-description">${errorInfo.description}</div>
                    ${errorInfo.suggestion ? `<div class="error-suggestion">${errorInfo.suggestion}</div>` : ''}
                </div>
                <div class="error-actions">
                    ${options.showRetry ? '<button class="error-retry-btn">Retry</button>' : ''}
                    ${options.showDismiss ? '<button class="error-dismiss-btn">×</button>' : ''}
                </div>
            </div>
        `;

        // Add event listeners
        const retryBtn = container.querySelector('.error-retry-btn');
        const dismissBtn = container.querySelector('.error-dismiss-btn');

        if (retryBtn && options.onRetry) {
            retryBtn.addEventListener('click', options.onRetry);
        }

        if (dismissBtn && options.onDismiss) {
            dismissBtn.addEventListener('click', options.onDismiss);
        }
    }

    analyzeErrorForDisplay(error) {
        if (!error) {
            return {
                severity: 'info',
                icon: 'ℹ',
                title: 'Information',
                description: 'No error details available',
                suggestion: null
            };
        }

        const message = error.message || error.toString();
        const lowerMessage = message.toLowerCase();

        // Network errors
        if (lowerMessage.includes('network') || lowerMessage.includes('fetch') || lowerMessage.includes('connection')) {
            return {
                severity: 'warning',
                icon: '📡',
                title: 'Connection Error',
                description: 'Unable to connect to the server',
                suggestion: 'Check your internet connection and try again'
            };
        }

        // Server errors
        if (lowerMessage.includes('server') || lowerMessage.includes('500')) {
            return {
                severity: 'error',
                icon: '🔧',
                title: 'Server Error',
                description: 'The server encountered an error',
                suggestion: 'Please try again later or contact support if the problem persists'
            };
        }

        // Validation errors
        if (lowerMessage.includes('validation') || lowerMessage.includes('invalid')) {
            return {
                severity: 'warning',
                icon: '⚠',
                title: 'Validation Error',
                description: message,
                suggestion: 'Please check your input and try again'
            };
        }

        // Permission errors
        if (lowerMessage.includes('unauthorized') || lowerMessage.includes('forbidden')) {
            return {
                severity: 'error',
                icon: '🔒',
                title: 'Access Denied',
                description: 'You do not have permission to perform this action',
                suggestion: 'Please contact an administrator if you need access'
            };
        }

        // Generic error
        return {
            severity: 'error',
            icon: '❌',
            title: 'Error',
            description: message,
            suggestion: 'Please try again or contact support if the problem persists'
        };
    }

    // Enhanced Toast with better error handling
    showToast(message, type = 'info', duration = 5000, options = {}) {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const iconMap = {
            success: '✓',
            error: '❌',
            warning: '⚠',
            info: 'ℹ'
        };

        toast.innerHTML = `
            <div class="toast-icon">${iconMap[type] || iconMap.info}</div>
            <div class="toast-content">
                <div class="toast-message">${message}</div>
                ${options.action ? `<button class="toast-action-btn">${options.action.text}</button>` : ''}
            </div>
            <button class="toast-close-btn">×</button>
        `;

        this.toastContainer.appendChild(toast);

        // Add event listeners
        const closeBtn = toast.querySelector('.toast-close-btn');
        const actionBtn = toast.querySelector('.toast-action-btn');

        closeBtn.addEventListener('click', () => {
            this.removeToast(toast);
        });

        if (actionBtn && options.action && options.action.callback) {
            actionBtn.addEventListener('click', () => {
                options.action.callback();
                this.removeToast(toast);
            });
        }

        // Auto remove after duration
        if (duration > 0) {
            setTimeout(() => {
                this.removeToast(toast);
            }, duration);
        }

        // Allow manual close by clicking toast
        toast.addEventListener('click', (e) => {
            if (e.target === toast || e.target.classList.contains('toast-message')) {
                this.removeToast(toast);
            }
        });

        return toast;
    }

    // Connection Status Indicator
    renderConnectionStatus(isOnline, container) {
        if (!container) return;

        const statusClass = isOnline ? 'online' : 'offline';
        const statusText = isOnline ? 'Online' : 'Offline';
        const statusIcon = isOnline ? '●' : '●';

        container.innerHTML = `
            <div class="connection-status ${statusClass}">
                <span class="connection-icon">${statusIcon}</span>
                <span class="connection-text">${statusText}</span>
            </div>
        `;
    }

    // Progress Indicator for Sync Operations
    renderProgressIndicator(progress, container, options = {}) {
        if (!container) return;

        const percentage = Math.min(100, Math.max(0, progress));
        
        container.innerHTML = `
            <div class="progress-indicator">
                ${options.label ? `<div class="progress-label">${options.label}</div>` : ''}
                <div class="progress-bar">
                    <div class="progress-fill" style="width: ${percentage}%"></div>
                </div>
                <div class="progress-text">${percentage}%</div>
            </div>
        `;
    }

    // Theme management
    setTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('expense-tracker-theme', theme);
    }

    getTheme() {
        return localStorage.getItem('expense-tracker-theme') || 'light';
    }

    initTheme() {
        const savedTheme = this.getTheme();
        this.setTheme(savedTheme);

        // Listen for system theme changes
        if (window.matchMedia) {
            const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
            mediaQuery.addListener((e) => {
                if (!localStorage.getItem('expense-tracker-theme')) {
                    this.setTheme(e.matches ? 'dark' : 'light');
                }
            });
        }
    }

    // Responsive utilities
    isMobile() {
        return window.innerWidth <= 768;
    }

    isTablet() {
        return window.innerWidth > 768 && window.innerWidth <= 1024;
    }

    isDesktop() {
        return window.innerWidth > 1024;
    }

    // Animation utilities
    fadeIn(element, duration = 300) {
        element.style.opacity = '0';
        element.style.display = 'block';

        let start = null;
        const animate = (timestamp) => {
            if (!start) start = timestamp;
            const progress = timestamp - start;
            const opacity = Math.min(progress / duration, 1);

            element.style.opacity = opacity;

            if (progress < duration) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }

    fadeOut(element, duration = 300) {
        let start = null;
        const animate = (timestamp) => {
            if (!start) start = timestamp;
            const progress = timestamp - start;
            const opacity = Math.max(1 - (progress / duration), 0);

            element.style.opacity = opacity;

            if (progress < duration) {
                requestAnimationFrame(animate);
            } else {
                element.style.display = 'none';
            }
        };

        requestAnimationFrame(animate);
    }
}