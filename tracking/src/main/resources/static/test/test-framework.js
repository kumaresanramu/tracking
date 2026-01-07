// Simple Test Framework for UI Components
class TestRunner {
    constructor() {
        this.suites = [];
        this.results = {
            total: 0,
            passed: 0,
            failed: 0,
            pending: 0
        };
    }
    
    addSuite(suite) {
        this.suites.push(suite);
    }
    
    displayTestSuites() {
        const container = document.getElementById('test-results');
        let html = '';
        
        this.suites.forEach(suite => {
            html += `
                <div class="test-suite">
                    <div class="test-suite-header">${suite.name}</div>
                    <div id="suite-${suite.name.replace(/\s+/g, '-').toLowerCase()}">
                        ${suite.tests.map(test => `
                            <div class="test-case" id="test-${test.name.replace(/\s+/g, '-').toLowerCase()}">
                                <div class="test-name">${test.name}</div>
                                <div class="test-status pending">Pending</div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        });
        
        container.innerHTML = html;
    }
    
    async runAllTests() {
        const button = document.getElementById('run-tests-btn');
        button.disabled = true;
        button.textContent = 'Running Tests...';
        
        this.results = { total: 0, passed: 0, failed: 0, pending: 0 };
        
        for (const suite of this.suites) {
            await this.runSuite(suite);
        }
        
        this.displayResults();
        
        button.disabled = false;
        button.textContent = 'Run All Tests';
    }
    
    async runSuite(suite) {
        console.log(`Running test suite: ${suite.name}`);
        
        // Setup suite if needed
        if (suite.setup) {
            await suite.setup();
        }
        
        for (const test of suite.tests) {
            await this.runTest(suite, test);
        }
        
        // Teardown suite if needed
        if (suite.teardown) {
            await suite.teardown();
        }
    }
    
    async runTest(suite, test) {
        const testId = `test-${test.name.replace(/\s+/g, '-').toLowerCase()}`;
        const testElement = document.getElementById(testId);
        const statusElement = testElement.querySelector('.test-status');
        
        this.results.total++;
        
        try {
            statusElement.textContent = 'Running';
            statusElement.className = 'test-status pending';
            
            // Run the test
            await test.run();
            
            // Test passed
            statusElement.textContent = 'Pass';
            statusElement.className = 'test-status pass';
            this.results.passed++;
            
            console.log(`✓ ${test.name}`);
            
        } catch (error) {
            // Test failed
            statusElement.textContent = 'Fail';
            statusElement.className = 'test-status fail';
            this.results.failed++;
            
            console.error(`✗ ${test.name}:`, error.message);
            
            // Add error details
            const errorDiv = document.createElement('div');
            errorDiv.className = 'test-error';
            errorDiv.textContent = error.message;
            testElement.appendChild(errorDiv);
        }
        
        // Small delay to make tests visible
        await this.delay(100);
    }
    
    displayResults() {
        const container = document.getElementById('test-results');
        const resultsDiv = document.createElement('div');
        resultsDiv.className = 'test-results';
        
        const passRate = this.results.total > 0 ? 
            Math.round((this.results.passed / this.results.total) * 100) : 0;
        
        resultsDiv.innerHTML = `
            <h3>Test Results</h3>
            <p><strong>Total:</strong> ${this.results.total}</p>
            <p><strong>Passed:</strong> ${this.results.passed}</p>
            <p><strong>Failed:</strong> ${this.results.failed}</p>
            <p><strong>Pass Rate:</strong> ${passRate}%</p>
        `;
        
        container.appendChild(resultsDiv);
    }
    
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Test Assertion Helpers
class Assert {
    static isTrue(condition, message = 'Expected condition to be true') {
        if (!condition) {
            throw new Error(message);
        }
    }
    
    static isFalse(condition, message = 'Expected condition to be false') {
        if (condition) {
            throw new Error(message);
        }
    }
    
    static equals(actual, expected, message = `Expected ${expected}, got ${actual}`) {
        if (actual !== expected) {
            throw new Error(message);
        }
    }
    
    static notEquals(actual, expected, message = `Expected not ${expected}, got ${actual}`) {
        if (actual === expected) {
            throw new Error(message);
        }
    }
    
    static exists(element, message = 'Expected element to exist') {
        if (!element) {
            throw new Error(message);
        }
    }
    
    static hasClass(element, className, message = `Expected element to have class ${className}`) {
        if (!element || !element.classList.contains(className)) {
            throw new Error(message);
        }
    }
    
    static doesNotHaveClass(element, className, message = `Expected element not to have class ${className}`) {
        if (element && element.classList.contains(className)) {
            throw new Error(message);
        }
    }
    
    static isVisible(element, message = 'Expected element to be visible') {
        if (!element || element.style.display === 'none' || 
            element.offsetParent === null) {
            throw new Error(message);
        }
    }
    
    static isHidden(element, message = 'Expected element to be hidden') {
        if (element && element.style.display !== 'none' && 
            element.offsetParent !== null) {
            throw new Error(message);
        }
    }
    
    static contains(container, text, message = `Expected to contain text: ${text}`) {
        if (!container || !container.textContent.includes(text)) {
            throw new Error(message);
        }
    }
    
    static hasAttribute(element, attribute, message = `Expected element to have attribute ${attribute}`) {
        if (!element || !element.hasAttribute(attribute)) {
            throw new Error(message);
        }
    }
    
    static attributeEquals(element, attribute, value, message = `Expected ${attribute} to equal ${value}`) {
        if (!element || element.getAttribute(attribute) !== value) {
            throw new Error(message);
        }
    }
}

// DOM Helper Utilities
class DOMHelper {
    static createElement(tag, attributes = {}, textContent = '') {
        const element = document.createElement(tag);
        
        Object.keys(attributes).forEach(key => {
            if (key === 'className') {
                element.className = attributes[key];
            } else {
                element.setAttribute(key, attributes[key]);
            }
        });
        
        if (textContent) {
            element.textContent = textContent;
        }
        
        return element;
    }
    
    static fireEvent(element, eventType, eventData = {}) {
        const event = new Event(eventType, { bubbles: true, cancelable: true });
        Object.assign(event, eventData);
        element.dispatchEvent(event);
    }
    
    static fireClickEvent(element) {
        const event = new MouseEvent('click', {
            bubbles: true,
            cancelable: true,
            view: window
        });
        element.dispatchEvent(event);
    }
    
    static fireInputEvent(element, value) {
        element.value = value;
        const event = new Event('input', { bubbles: true });
        element.dispatchEvent(event);
    }
    
    static fireChangeEvent(element, value) {
        element.value = value;
        const event = new Event('change', { bubbles: true });
        element.dispatchEvent(event);
    }
    
    static fireSubmitEvent(form) {
        const event = new Event('submit', { bubbles: true, cancelable: true });
        form.dispatchEvent(event);
    }
    
    static simulateResize(width, height) {
        // Mock window resize for responsive tests
        Object.defineProperty(window, 'innerWidth', {
            writable: true,
            configurable: true,
            value: width,
        });
        Object.defineProperty(window, 'innerHeight', {
            writable: true,
            configurable: true,
            value: height,
        });
        
        window.dispatchEvent(new Event('resize'));
    }
    
    static getComputedStyle(element, property) {
        return window.getComputedStyle(element).getPropertyValue(property);
    }
    
    static isElementInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }
    
    static waitForElement(selector, timeout = 5000) {
        return new Promise((resolve, reject) => {
            const element = document.querySelector(selector);
            if (element) {
                resolve(element);
                return;
            }
            
            const observer = new MutationObserver((mutations, obs) => {
                const element = document.querySelector(selector);
                if (element) {
                    obs.disconnect();
                    resolve(element);
                }
            });
            
            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
            
            setTimeout(() => {
                observer.disconnect();
                reject(new Error(`Element ${selector} not found within ${timeout}ms`));
            }, timeout);
        });
    }
    
    static delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}