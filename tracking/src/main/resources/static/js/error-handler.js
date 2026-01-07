// Error Handler Service - Centralized error handling and user feedback
class ErrorHandler {
    constructor() {
        this.ui = null; // Will be set by the main app
        this.retryQueue = new Map();
        this.errorCounts = new Map();
        this.maxRetries = 3;
        this.setupGlobalErrorHandlers();
    }

    setUI(uiInstance) {
        this.ui = uiInstance;
    }

    setupGlobalErrorHandlers() {
        // Handle unhandled promise rejections
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
            this.handleError(event.reason, 'Unhandled Promise Rejection');
            event.preventDefault(); // Prevent default browser error handling
        });

        // Handle JavaScript errors
        window.addEventListener('error', (event) => {
            console.error('JavaScript error:', event.error);
            this.handleError(event.error, 'JavaScript Error');
        });

        // Handle fetch errors globally
        const originalFetch = window.fetch;
        window.fetch = async (...args) => {
            try {
                const response = await originalFetch(...args);
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response;
            } catch (error) {
                this.handleApiError(error, args[0]);
                throw error;
            }
        };
    }

    /**
     * Main error handling method
     */
    handleError(error, context = 'Unknown', options = {}) {
        const errorInfo = this.analyzeError(error, context);
        
        // Log error for debugging
        console.error(`[${context}] Error:`, error);
        
        // Track error frequency
        this.trackError(errorInfo.type);
        
        // Show user-friendly message
        if (this.ui && !options.silent) {
            this.showUserFriendlyError(errorInfo, options);
        }
        
        // Handle specific error types
        this.handleSpecificError(errorInfo, options);
        
        return errorInfo;
    }

    /**
     * Handle API-specific errors
     */
    async handleApiError(error, url, options = {}) {
        const errorInfo = this.analyzeApiError(error, url);
        
        console.error(`API Error [${url}]:`, error);
        
        // Handle network errors
        if (errorInfo.isNetworkError) {
            this.handleNetworkError(errorInfo, options);
            return errorInfo;
        }
        
        // Handle HTTP errors
        if (errorInfo.isHttpError) {
            this.handleHttpError(errorInfo, options);
            return errorInfo;
        }
        
        // Handle timeout errors
        if (errorInfo.isTimeout) {
            this.handleTimeoutError(errorInfo, options);
            return errorInfo;
        }
        
        // Generic API error handling
        if (this.ui && !options.silent) {
            this.ui.showToast(errorInfo.userMessage, 'error');
        }
        
        return errorInfo;
    }

    /**
     * Analyze error to determine type and appropriate response
     */
    analyzeError(error, context) {
        const errorInfo = {
            originalError: error,
            context,
            type: 'unknown',
            severity: 'medium',
            userMessage: 'An unexpected error occurred',
            isRetryable: false,
            suggestedAction: null
        };

        if (!error) {
            return errorInfo;
        }

        const message = error.message || error.toString();
        const lowerMessage = message.toLowerCase();

        // Network errors
        if (lowerMessage.includes('fetch') || 
            lowerMessage.includes('network') || 
            lowerMessage.includes('connection') ||
            error.name === 'TypeError' && lowerMessage.includes('failed to fetch')) {
            errorInfo.type = 'network';
            errorInfo.isRetryable = true;
            errorInfo.userMessage = 'Network connection error. Please check your internet connection.';
            errorInfo.suggestedAction = 'retry';
        }
        // Timeout errors
        else if (lowerMessage.includes('timeout') || lowerMessage.includes('aborted')) {
            errorInfo.type = 'timeout';
            errorInfo.isRetryable = true;
            errorInfo.userMessage = 'Request timed out. Please try again.';
            errorInfo.suggestedAction = 'retry';
        }
        // Validation errors
        else if (lowerMessage.includes('validation') || lowerMessage.includes('invalid')) {
            errorInfo.type = 'validation';
            errorInfo.severity = 'low';
            errorInfo.userMessage = 'Please check your input and try again.';
            errorInfo.suggestedAction = 'fix_input';
        }
        // Permission errors
        else if (lowerMessage.includes('unauthorized') || lowerMessage.includes('forbidden')) {
            errorInfo.type = 'permission';
            errorInfo.severity = 'high';
            errorInfo.userMessage = 'You do not have permission to perform this action.';
            errorInfo.suggestedAction = 'contact_support';
        }
        // Server errors
        else if (lowerMessage.includes('server') || lowerMessage.includes('500')) {
            errorInfo.type = 'server';
            errorInfo.severity = 'high';
            errorInfo.isRetryable = true;
            errorInfo.userMessage = 'Server error. Please try again later.';
            errorInfo.suggestedAction = 'retry_later';
        }

        return errorInfo;
    }

    /**
     * Analyze API-specific errors
     */
    analyzeApiError(error, url) {
        const errorInfo = this.analyzeError(error, `API: ${url}`);
        
        // Additional API-specific analysis
        if (error.message) {
            const message = error.message;
            
            // HTTP status code analysis
            if (message.includes('HTTP 400')) {
                errorInfo.type = 'validation';
                errorInfo.userMessage = 'Invalid request data. Please check your input.';
                errorInfo.isHttpError = true;
                errorInfo.statusCode = 400;
            } else if (message.includes('HTTP 401')) {
                errorInfo.type = 'authentication';
                errorInfo.userMessage = 'Authentication required. Please log in.';
                errorInfo.isHttpError = true;
                errorInfo.statusCode = 401;
            } else if (message.includes('HTTP 403')) {
                errorInfo.type = 'permission';
                errorInfo.userMessage = 'Access denied.';
                errorInfo.isHttpError = true;
                errorInfo.statusCode = 403;
            } else if (message.includes('HTTP 404')) {
                errorInfo.type = 'not_found';
                errorInfo.userMessage = 'Requested resource not found.';
                errorInfo.isHttpError = true;
                errorInfo.statusCode = 404;
            } else if (message.includes('HTTP 409')) {
                errorInfo.type = 'conflict';
                errorInfo.userMessage = 'Data conflict. The resource may have been modified.';
                errorInfo.isHttpError = true;
                errorInfo.statusCode = 409;
            } else if (message.includes('HTTP 429')) {
                errorInfo.type = 'rate_limit';
                errorInfo.userMessage = 'Too many requests. Please wait a moment and try again.';
                errorInfo.isRetryable = true;
                errorInfo.isHttpError = true;
                errorInfo.statusCode = 429;
            } else if (message.includes('HTTP 5')) {
                errorInfo.type = 'server';
                errorInfo.userMessage = 'Server error. Please try again later.';
                errorInfo.isRetryable = true;
                errorInfo.isHttpError = true;
                errorInfo.statusCode = parseInt(message.match(/HTTP (\d+)/)?.[1]) || 500;
            }
        }

        // Network error detection
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            errorInfo.isNetworkError = true;
            errorInfo.isRetryable = true;
        }

        // Timeout detection
        if (error.name === 'AbortError' || error.message.includes('timeout')) {
            errorInfo.isTimeout = true;
            errorInfo.isRetryable = true;
        }

        return errorInfo;
    }

    /**
     * Handle network errors
     */
    handleNetworkError(errorInfo, options = {}) {
        if (this.ui && !options.silent) {
            this.ui.showToast('Connection lost. Working offline.', 'warning', 3000);
        }

        // Trigger offline mode if available
        if (window.app && window.app.syncService) {
            window.app.syncService.handleConnectivityChange(false);
        }

        // Queue for retry when connection is restored
        if (options.retryable !== false) {
            this.queueForRetry(errorInfo, options);
        }
    }

    /**
     * Handle HTTP errors
     */
    handleHttpError(errorInfo, options = {}) {
        const { statusCode } = errorInfo;
        
        switch (statusCode) {
            case 400:
                if (this.ui && !options.silent) {
                    this.ui.showToast('Invalid data. Please check your input.', 'error');
                }
                break;
            case 401:
                if (this.ui && !options.silent) {
                    this.ui.showToast('Authentication required.', 'error');
                }
                // Could trigger login flow here
                break;
            case 403:
                if (this.ui && !options.silent) {
                    this.ui.showToast('Access denied.', 'error');
                }
                break;
            case 404:
                if (this.ui && !options.silent) {
                    this.ui.showToast('Resource not found.', 'error');
                }
                break;
            case 409:
                if (this.ui && !options.silent) {
                    this.ui.showToast('Data conflict detected.', 'warning');
                }
                break;
            case 429:
                if (this.ui && !options.silent) {
                    this.ui.showToast('Too many requests. Please wait.', 'warning');
                }
                this.queueForRetry(errorInfo, { ...options, delay: 5000 });
                break;
            default:
                if (statusCode >= 500) {
                    if (this.ui && !options.silent) {
                        this.ui.showToast('Server error. Please try again.', 'error');
                    }
                    if (errorInfo.isRetryable) {
                        this.queueForRetry(errorInfo, options);
                    }
                }
        }
    }

    /**
     * Handle timeout errors
     */
    handleTimeoutError(errorInfo, options = {}) {
        if (this.ui && !options.silent) {
            this.ui.showToast('Request timed out. Please try again.', 'warning');
        }
        
        if (options.retryable !== false) {
            this.queueForRetry(errorInfo, options);
        }
    }

    /**
     * Handle specific error types
     */
    handleSpecificError(errorInfo, options = {}) {
        switch (errorInfo.type) {
            case 'validation':
                // Could highlight specific form fields
                break;
            case 'permission':
                // Could redirect to login or show permission request
                break;
            case 'server':
                // Could show server status or maintenance message
                break;
        }
    }

    /**
     * Show user-friendly error messages
     */
    showUserFriendlyError(errorInfo, options = {}) {
        if (!this.ui) return;

        const { type, userMessage, suggestedAction } = errorInfo;
        
        let toastType = 'error';
        if (type === 'network' || type === 'timeout') {
            toastType = 'warning';
        } else if (type === 'validation') {
            toastType = 'info';
        }

        // Show toast with action button if applicable
        if (suggestedAction === 'retry' && options.onRetry) {
            this.showRetryableError(userMessage, options.onRetry);
        } else {
            this.ui.showToast(userMessage, toastType);
        }
    }

    /**
     * Show error with retry option
     */
    showRetryableError(message, onRetry) {
        if (!this.ui) return;

        const toast = this.ui.showToast(
            `${message} <button class="toast-retry-btn">Retry</button>`, 
            'warning', 
            10000
        );

        const retryBtn = toast.querySelector('.toast-retry-btn');
        if (retryBtn) {
            retryBtn.addEventListener('click', () => {
                this.ui.removeToast(toast);
                onRetry();
            });
        }
    }

    /**
     * Queue operation for retry
     */
    queueForRetry(errorInfo, options = {}) {
        const retryId = Date.now() + Math.random();
        const retryInfo = {
            errorInfo,
            options,
            attempts: 0,
            maxAttempts: options.maxRetries || this.maxRetries,
            delay: options.delay || 1000,
            exponentialBackoff: options.exponentialBackoff !== false
        };

        this.retryQueue.set(retryId, retryInfo);

        // Schedule retry
        this.scheduleRetry(retryId);
    }

    /**
     * Schedule retry with exponential backoff
     */
    scheduleRetry(retryId) {
        const retryInfo = this.retryQueue.get(retryId);
        if (!retryInfo) return;

        const { attempts, maxAttempts, delay, exponentialBackoff } = retryInfo;
        
        if (attempts >= maxAttempts) {
            this.retryQueue.delete(retryId);
            if (this.ui) {
                this.ui.showToast('Maximum retry attempts reached.', 'error');
            }
            return;
        }

        const actualDelay = exponentialBackoff ? 
            delay * Math.pow(2, attempts) : delay;

        setTimeout(() => {
            this.executeRetry(retryId);
        }, actualDelay);
    }

    /**
     * Execute retry
     */
    async executeRetry(retryId) {
        const retryInfo = this.retryQueue.get(retryId);
        if (!retryInfo) return;

        retryInfo.attempts++;

        try {
            if (retryInfo.options.onRetry) {
                await retryInfo.options.onRetry();
                this.retryQueue.delete(retryId);
                if (this.ui) {
                    this.ui.showToast('Operation completed successfully.', 'success');
                }
            }
        } catch (error) {
            console.error('Retry failed:', error);
            this.scheduleRetry(retryId);
        }
    }

    /**
     * Track error frequency for monitoring
     */
    trackError(errorType) {
        const count = this.errorCounts.get(errorType) || 0;
        this.errorCounts.set(errorType, count + 1);

        // Log frequent errors
        if (count > 5) {
            console.warn(`Frequent ${errorType} errors detected:`, count);
        }
    }

    /**
     * Get error statistics
     */
    getErrorStats() {
        return {
            errorCounts: Object.fromEntries(this.errorCounts),
            activeRetries: this.retryQueue.size
        };
    }

    /**
     * Clear error tracking
     */
    clearErrorStats() {
        this.errorCounts.clear();
        this.retryQueue.clear();
    }

    /**
     * Handle form validation errors
     */
    handleFormValidationError(form, errors) {
        if (!this.ui) return;

        // Clear previous errors
        form.querySelectorAll('.form-group').forEach(group => {
            group.classList.remove('has-error');
            const errorMsg = group.querySelector('.error-message');
            if (errorMsg) errorMsg.remove();
        });

        // Show new errors
        errors.forEach(error => {
            const field = form.querySelector(`[name="${error.field}"]`);
            if (field) {
                const formGroup = field.closest('.form-group');
                if (formGroup) {
                    formGroup.classList.add('has-error');
                    
                    const errorDiv = document.createElement('div');
                    errorDiv.className = 'error-message';
                    errorDiv.textContent = error.message;
                    formGroup.appendChild(errorDiv);
                }
                this.ui.addFieldError(field);
            }
        });

        // Show summary toast
        this.ui.showToast(`Please fix ${errors.length} validation error(s)`, 'error');
    }

    /**
     * Create error boundary for components
     */
    createErrorBoundary(component, fallbackContent = 'An error occurred') {
        try {
            return component();
        } catch (error) {
            console.error('Component error:', error);
            this.handleError(error, 'Component Error');
            return fallbackContent;
        }
    }
}