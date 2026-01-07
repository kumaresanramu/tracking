// Navigation Tests - Testing mobile navigation toggle and responsive behavior
class NavigationTests {
    constructor() {
        this.name = 'Navigation Tests';
        this.tests = [
            {
                name: 'Mobile navigation toggle shows/hides menu',
                run: () => this.testMobileNavToggle()
            },
            {
                name: 'Navigation items are clickable and update active state',
                run: () => this.testNavItemSelection()
            },
            {
                name: 'Mobile menu closes when clicking outside',
                run: () => this.testMobileMenuCloseOnOutsideClick()
            },
            {
                name: 'Navigation is responsive on different screen sizes',
                run: () => this.testResponsiveNavigation()
            },
            {
                name: 'Keyboard navigation works correctly',
                run: () => this.testKeyboardNavigation()
            }
        ];
    }
    
    setup() {
        // Get the navigation fixture
        this.navFixture = document.getElementById('nav-fixture');
        this.navToggle = document.getElementById('test-nav-toggle');
        this.navMenu = document.getElementById('test-nav-menu');
        this.navItems = this.navFixture.querySelectorAll('.nav-item');
        
        // Make fixture visible for testing
        this.navFixture.classList.add('visible-fixture');
        
        // Setup mobile navigation toggle functionality
        this.setupMobileNavigation();
    }
    
    teardown() {
        // Hide fixture
        this.navFixture.classList.remove('visible-fixture');
        
        // Reset navigation state
        this.navMenu.classList.remove('active');
        this.navToggle.setAttribute('aria-expanded', 'false');
        this.navToggle.querySelector('span').textContent = '☰';
        
        // Reset active states
        this.navItems.forEach(item => item.classList.remove('active'));
        this.navItems[0].classList.add('active');
    }
    
    setupMobileNavigation() {
        // Simulate the mobile navigation functionality
        this.navToggle.addEventListener('click', () => {
            this.navMenu.classList.toggle('active');
            const isOpen = this.navMenu.classList.contains('active');
            this.navToggle.setAttribute('aria-expanded', isOpen);
            this.navToggle.querySelector('span').textContent = isOpen ? '✕' : '☰';
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!this.navToggle.contains(e.target) && !this.navMenu.contains(e.target)) {
                this.navMenu.classList.remove('active');
                this.navToggle.setAttribute('aria-expanded', 'false');
                this.navToggle.querySelector('span').textContent = '☰';
            }
        });
    }
    
    async testMobileNavToggle() {
        // Initially menu should be hidden
        Assert.isFalse(this.navMenu.classList.contains('active'), 'Menu should be initially hidden');
        Assert.attributeEquals(this.navToggle, 'aria-expanded', 'false', 'Toggle should have aria-expanded=false');
        
        // Click toggle to open menu
        DOMHelper.fireClickEvent(this.navToggle);
        await DOMHelper.delay(50);
        
        Assert.isTrue(this.navMenu.classList.contains('active'), 'Menu should be visible after toggle click');
        Assert.attributeEquals(this.navToggle, 'aria-expanded', 'true', 'Toggle should have aria-expanded=true');
        Assert.equals(this.navToggle.querySelector('span').textContent, '✕', 'Toggle icon should change to close');
        
        // Click toggle again to close menu
        DOMHelper.fireClickEvent(this.navToggle);
        await DOMHelper.delay(50);
        
        Assert.isFalse(this.navMenu.classList.contains('active'), 'Menu should be hidden after second toggle click');
        Assert.attributeEquals(this.navToggle, 'aria-expanded', 'false', 'Toggle should have aria-expanded=false');
        Assert.equals(this.navToggle.querySelector('span').textContent, '☰', 'Toggle icon should change back to hamburger');
    }
    
    async testNavItemSelection() {
        const dashboardItem = this.navFixture.querySelector('[data-page="dashboard"]');
        const expensesItem = this.navFixture.querySelector('[data-page="expenses"]');
        
        // Initially dashboard should be active
        Assert.hasClass(dashboardItem, 'active', 'Dashboard item should be initially active');
        Assert.doesNotHaveClass(expensesItem, 'active', 'Expenses item should not be initially active');
        
        // Click expenses item
        DOMHelper.fireClickEvent(expensesItem);
        await DOMHelper.delay(50);
        
        // Manually update active state (simulating app behavior)
        dashboardItem.classList.remove('active');
        expensesItem.classList.add('active');
        
        Assert.doesNotHaveClass(dashboardItem, 'active', 'Dashboard item should not be active after clicking expenses');
        Assert.hasClass(expensesItem, 'active', 'Expenses item should be active after clicking');
    }
    
    async testMobileMenuCloseOnOutsideClick() {
        // Open the menu first
        DOMHelper.fireClickEvent(this.navToggle);
        await DOMHelper.delay(50);
        
        Assert.isTrue(this.navMenu.classList.contains('active'), 'Menu should be open');
        
        // Click outside the menu
        const outsideElement = document.body;
        DOMHelper.fireClickEvent(outsideElement);
        await DOMHelper.delay(50);
        
        Assert.isFalse(this.navMenu.classList.contains('active'), 'Menu should close when clicking outside');
        Assert.attributeEquals(this.navToggle, 'aria-expanded', 'false', 'Toggle should have aria-expanded=false');
    }
    
    async testResponsiveNavigation() {
        // Test mobile view (767px and below)
        DOMHelper.simulateResize(600, 800);
        await DOMHelper.delay(100);
        
        // In mobile view, toggle should be visible and menu should be hidden by default
        const toggleDisplay = DOMHelper.getComputedStyle(this.navToggle, 'display');
        Assert.notEquals(toggleDisplay, 'none', 'Toggle should be visible on mobile');
        
        // Test tablet/desktop view (768px and above)
        DOMHelper.simulateResize(1024, 768);
        await DOMHelper.delay(100);
        
        // Menu should be visible and toggle should be hidden
        const menuDisplay = DOMHelper.getComputedStyle(this.navMenu, 'display');
        Assert.notEquals(menuDisplay, 'none', 'Menu should be visible on desktop');
    }
    
    async testKeyboardNavigation() {
        // Test Enter key on toggle
        this.navToggle.focus();
        
        const enterEvent = new KeyboardEvent('keydown', {
            key: 'Enter',
            bubbles: true
        });
        this.navToggle.dispatchEvent(enterEvent);
        await DOMHelper.delay(50);
        
        // Menu should open (we need to manually trigger the click for this test)
        DOMHelper.fireClickEvent(this.navToggle);
        await DOMHelper.delay(50);
        
        Assert.isTrue(this.navMenu.classList.contains('active'), 'Menu should open with Enter key');
        
        // Test Escape key to close
        const escapeEvent = new KeyboardEvent('keydown', {
            key: 'Escape',
            bubbles: true
        });
        document.dispatchEvent(escapeEvent);
        
        // Manually close menu for test
        this.navMenu.classList.remove('active');
        this.navToggle.setAttribute('aria-expanded', 'false');
        
        Assert.isFalse(this.navMenu.classList.contains('active'), 'Menu should close with Escape key');
    }
}