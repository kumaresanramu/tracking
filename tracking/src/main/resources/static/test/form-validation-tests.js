// Form Validation Tests - Testing form validation and submission
class FormValidationTests {
    constructor() {
        this.name = 'Form Validation Tests';
        this.tests = [
            {
                name: 'Required fields show validation errors when empty',
                run: () => this.testRequiredFieldValidation()
            },
            {
                name: 'Amount field validates numeric input',
                run: () => this.testAmountValidation()
            },
            {
                name: 'Form submission prevents invalid data',
                run: () => this.testFormSubmissionValidation()
            },
            {
                name: 'Real-time validation provides immediate feedback',
                run: () => this.testRealTimeValidation()
            },
            {
                name: 'Form reset clears validation errors',
                run: () => this.testFormReset()
            }
        ];
    }
    
    setup() {
        // Get the form fixture
        this.formFixture = document.getElementById('form-fixture');
        this.form = document.getElementById('test-expense-form');
        this.descriptionInput = document.getElementById('test-description');
        this.amountInput = document.getElementById('test-amount');
        this.categorySelect = document.getElementById('test-category');
        
        // Make fixture visible for testing
        this.formFixture.classList.add('visible-fixture');
        
        // Initialize UI components for validation
        this.ui = new UIComponents();
        
        // Setup form validation
        this.setupFormValidation();
    }
    
    teardown() {
        // Hide fixture
        this.formFixture.classList.remove('visible-fixture');
        
        // Reset form
        this.form.reset();
        
        // Clear validation errors
        this.clearValidationErrors();
    }
    
    setupFormValidation() {
        // Add real-time validation listeners
        this.amountInput.addEventListener('input', (e) => {
            const value = parseFloat(e.target.value);
            if (e.target.value && (isNaN(value) || value <= 0)) {
                this.ui.addFieldError(e.target);
            } else {
                this.ui.removeFieldError(e.target);
            }
        });
        
        this.descriptionInput.addEventListener('input', (e) => {
            if (e.target.value.trim().length === 0) {
                this.ui.addFieldError(e.target);
            } else {
                this.ui.removeFieldError(e.target);
            }
        });
        
        this.categorySelect.addEventListener('change', (e) => {
            if (!e.target.value) {
                this.ui.addFieldError(e.target);
            } else {
                this.ui.removeFieldError(e.target);
            }
        });
        
        // Prevent actual form submission for testing
        this.form.addEventListener('submit', (e) => {
            e.preventDefault();
        });
    }
    
    clearValidationErrors() {
        const fields = this.form.querySelectorAll('input, select');
        fields.forEach(field => {
            this.ui.removeFieldError(field);
        });
    }
    
    async testRequiredFieldValidation() {
        // Test empty description
        this.descriptionInput.value = '';
        DOMHelper.fireInputEvent(this.descriptionInput, '');
        await DOMHelper.delay(50);
        
        Assert.hasClass(this.descriptionInput, 'error', 'Description field should show error when empty');
        
        // Test valid description
        DOMHelper.fireInputEvent(this.descriptionInput, 'Test expense');
        await DOMHelper.delay(50);
        
        Assert.doesNotHaveClass(this.descriptionInput, 'error', 'Description field should not show error when filled');
        
        // Test empty category
        this.categorySelect.value = '';
        DOMHelper.fireChangeEvent(this.categorySelect, '');
        await DOMHelper.delay(50);
        
        Assert.hasClass(this.categorySelect, 'error', 'Category field should show error when empty');
        
        // Test valid category
        DOMHelper.fireChangeEvent(this.categorySelect, '1');
        await DOMHelper.delay(50);
        
        Assert.doesNotHaveClass(this.categorySelect, 'error', 'Category field should not show error when selected');
    }
    
    async testAmountValidation() {
        // Test negative amount
        DOMHelper.fireInputEvent(this.amountInput, '-10');
        await DOMHelper.delay(50);
        
        Assert.hasClass(this.amountInput, 'error', 'Amount field should show error for negative values');
        
        // Test zero amount
        DOMHelper.fireInputEvent(this.amountInput, '0');
        await DOMHelper.delay(50);
        
        Assert.hasClass(this.amountInput, 'error', 'Amount field should show error for zero values');
        
        // Test non-numeric amount
        DOMHelper.fireInputEvent(this.amountInput, 'abc');
        await DOMHelper.delay(50);
        
        Assert.hasClass(this.amountInput, 'error', 'Amount field should show error for non-numeric values');
        
        // Test valid amount
        DOMHelper.fireInputEvent(this.amountInput, '25.50');
        await DOMHelper.delay(50);
        
        Assert.doesNotHaveClass(this.amountInput, 'error', 'Amount field should not show error for valid positive values');
    }
    
    async testFormSubmissionValidation() {
        // Clear form
        this.form.reset();
        this.clearValidationErrors();
        
        // Try to submit empty form
        const errors = this.ui.validateForm(this.form);
        
        Assert.isTrue(errors.length > 0, 'Form validation should return errors for empty form');
        Assert.isTrue(errors.some(error => error.includes('Description')), 'Should have description error');
        Assert.isTrue(errors.some(error => error.includes('Amount')), 'Should have amount error');
        Assert.isTrue(errors.some(error => error.includes('Category')), 'Should have category error');
        
        // Fill form with valid data
        this.descriptionInput.value = 'Test expense';
        this.amountInput.value = '25.50';
        this.categorySelect.value = '1';
        
        const validErrors = this.ui.validateForm(this.form);
        Assert.equals(validErrors.length, 0, 'Form validation should return no errors for valid form');
    }
    
    async testRealTimeValidation() {
        // Test that validation happens immediately on input
        this.descriptionInput.value = '';
        
        // Simulate typing
        DOMHelper.fireInputEvent(this.descriptionInput, '');
        await DOMHelper.delay(10);
        
        Assert.hasClass(this.descriptionInput, 'error', 'Validation should happen immediately on input');
        
        // Simulate typing valid text
        DOMHelper.fireInputEvent(this.descriptionInput, 'Valid expense');
        await DOMHelper.delay(10);
        
        Assert.doesNotHaveClass(this.descriptionInput, 'error', 'Validation should clear immediately when input becomes valid');
        
        // Test amount real-time validation
        DOMHelper.fireInputEvent(this.amountInput, '-5');
        await DOMHelper.delay(10);
        
        Assert.hasClass(this.amountInput, 'error', 'Amount validation should happen immediately');
        
        DOMHelper.fireInputEvent(this.amountInput, '15.75');
        await DOMHelper.delay(10);
        
        Assert.doesNotHaveClass(this.amountInput, 'error', 'Amount validation should clear immediately when valid');
    }
    
    async testFormReset() {
        // Add some validation errors
        DOMHelper.fireInputEvent(this.descriptionInput, '');
        DOMHelper.fireInputEvent(this.amountInput, '-10');
        DOMHelper.fireChangeEvent(this.categorySelect, '');
        await DOMHelper.delay(50);
        
        // Verify errors are present
        Assert.hasClass(this.descriptionInput, 'error', 'Description should have error before reset');
        Assert.hasClass(this.amountInput, 'error', 'Amount should have error before reset');
        Assert.hasClass(this.categorySelect, 'error', 'Category should have error before reset');
        
        // Reset form
        this.form.reset();
        this.clearValidationErrors();
        await DOMHelper.delay(50);
        
        // Verify errors are cleared
        Assert.doesNotHaveClass(this.descriptionInput, 'error', 'Description error should be cleared after reset');
        Assert.doesNotHaveClass(this.amountInput, 'error', 'Amount error should be cleared after reset');
        Assert.doesNotHaveClass(this.categorySelect, 'error', 'Category error should be cleared after reset');
        
        // Verify form values are cleared
        Assert.equals(this.descriptionInput.value, '', 'Description value should be cleared');
        Assert.equals(this.amountInput.value, '', 'Amount value should be cleared');
        Assert.equals(this.categorySelect.value, '', 'Category value should be cleared');
    }
}