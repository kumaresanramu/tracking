// Responsive Tests - Testing responsive behavior across different screen sizes
class ResponsiveTests {
    constructor() {
        this.name = 'Responsive Behavior Tests';
        this.tests = [
            {
                name: 'Layout adapts to mobile screen sizes',
                run: () => this.testMobileLayout()
            },
            {
                name: 'Layout adapts to tablet screen sizes',
                run: () => this.testTabletLayout()
            },
            {
                name: 'Layout adapts to desktop screen sizes',
                run: () => this.testDesktopLayout()
            },
            {
                name: 'Grid layouts respond to screen size changes',
                run: () => this.testGridResponsiveness()
            },
            {
                name: 'Touch targets are appropriately sized on mobile',
                run: () => this.testTouchTargets()
            }
        ];
        
        // Store original window dimensions
        this.originalWidth = window.innerWidth;
        this.originalHeight = window.innerHeight;
    }
    
    setup() {
        // Get the responsive fixture
        this.responsiveFixture = document.getElementById('responsive-fixture');
        this.quickStats = this.responsiveFixture.querySelector('.quick-stats');
        this.statCards = this.responsiveFixture.querySelectorAll('.stat-card');
        
        // Make fixture visible for testing
        this.responsiveFixture.classList.add('visible-fixture');
        
        // Add some test buttons for touch target testing
        this.createTestButtons();
    }
    
    teardown() {
        // Hide fixture
        this.responsiveFixture.classList.remove('visible-fixture');
        
        // Restore original window dimensions
        DOMHelper.simulateResize(this.originalWidth, this.originalHeight);
        
        // Remove test buttons
        this.removeTestButtons();
    }
    
    createTestButtons() {
        const buttonContainer = DOMHelper.createElement('div', { className: 'test-buttons' });
        
        const smallButton = DOMHelper.createElement('button', { 
            className: 'btn btn-primary test-small-btn',
            style: 'padding: 0.25rem 0.5rem; font-size: 0.8rem;'
        }, 'Small Button');
        
        const normalButton = DOMHelper.createElement('button', { 
            className: 'btn btn-primary test-normal-btn'
        }, 'Normal Button');
        
        buttonContainer.appendChild(smallButton);
        buttonContainer.appendChild(normalButton);
        this.responsiveFixture.appendChild(buttonContainer);
        
        this.testButtons = buttonContainer;
        this.smallButton = smallButton;
        this.normalButton = normalButton;
    }
    
    removeTestButtons() {
        if (this.testButtons) {
            this.testButtons.remove();
        }
    }
    
    async testMobileLayout() {
        // Simulate mobile screen size (320px - 767px)
        DOMHelper.simulateResize(375, 667);
        await DOMHelper.delay(100);
        
        // Test grid layout changes to single column
        const gridColumns = DOMHelper.getComputedStyle(this.quickStats, 'grid-template-columns');
        
        // On mobile, should be single column (1fr or similar)
        Assert.isTrue(
            gridColumns.includes('1fr') || gridColumns === 'none',
            'Quick stats should use single column layout on mobile'
        );
        
        // Test that stat cards stack vertically
        if (this.statCards.length > 1) {
            const firstCard = this.statCards[0];
            const secondCard = this.statCards[1];
            
            const firstRect = firstCard.getBoundingClientRect();
            const secondRect = secondCard.getBoundingClientRect();
            
            Assert.isTrue(
                secondRect.top >= firstRect.bottom - 5, // Allow for small margin
                'Stat cards should stack vertically on mobile'
            );
        }
        
        // Test mobile-specific padding
        const mainContent = document.querySelector('.main-content');
        if (mainContent) {
            const padding = DOMHelper.getComputedStyle(mainContent, 'padding');
            // Should have smaller padding on mobile
            Assert.isTrue(
                padding.includes('16px') || padding.includes('1rem'),
                'Main content should have mobile-appropriate padding'
            );
        }
    }
    
    async testTabletLayout() {
        // Simulate tablet screen size (768px - 1023px)
        DOMHelper.simulateResize(768, 1024);
        await DOMHelper.delay(100);
        
        // Test grid layout changes to two columns
        const gridColumns = DOMHelper.getComputedStyle(this.quickStats, 'grid-template-columns');
        
        // On tablet, should be two columns
        Assert.isTrue(
            gridColumns.includes('1fr 1fr') || gridColumns.split(' ').length >= 2,
            'Quick stats should use two-column layout on tablet'
        );
        
        // Test that navigation is visible (not mobile menu)
        const navMenu = document.getElementById('test-nav-menu');
        const navToggle = document.getElementById('test-nav-toggle');
        
        if (navMenu && navToggle) {
            const menuDisplay = DOMHelper.getComputedStyle(navMenu, 'display');
            const toggleDisplay = DOMHelper.getComputedStyle(navToggle, 'display');
            
            Assert.notEquals(menuDisplay, 'none', 'Navigation menu should be visible on tablet');
        }
    }
    
    async testDesktopLayout() {
        // Simulate desktop screen size (1024px+)
        DOMHelper.simulateResize(1200, 800);
        await DOMHelper.delay(100);
        
        // Test grid layout uses three columns
        const gridColumns = DOMHelper.getComputedStyle(this.quickStats, 'grid-template-columns');
        
        // On desktop, should be three columns
        Assert.isTrue(
            gridColumns.includes('1fr 1fr 1fr') || gridColumns.split(' ').length >= 3,
            'Quick stats should use three-column layout on desktop'
        );
        
        // Test that cards are arranged horizontally
        if (this.statCards.length >= 2) {
            const firstCard = this.statCards[0];
            const secondCard = this.statCards[1];
            
            const firstRect = firstCard.getBoundingClientRect();
            const secondRect = secondCard.getBoundingClientRect();
            
            Assert.isTrue(
                secondRect.left >= firstRect.right - 20, // Allow for gap
                'Stat cards should be arranged horizontally on desktop'
            );
        }
        
        // Test desktop-specific spacing
        const mainContent = document.querySelector('.main-content');
        if (mainContent) {
            const padding = DOMHelper.getComputedStyle(mainContent, 'padding');
            // Should have larger padding on desktop
            Assert.isTrue(
                padding.includes('32px') || padding.includes('2rem'),
                'Main content should have desktop-appropriate padding'
            );
        }
    }
    
    async testGridResponsiveness() {
        // Test that grid layouts respond to screen size changes
        const breakpoints = [
            { width: 320, expectedColumns: 1, name: 'small mobile' },
            { width: 480, expectedColumns: 1, name: 'large mobile' },
            { width: 768, expectedColumns: 2, name: 'tablet' },
            { width: 1024, expectedColumns: 3, name: 'desktop' },
            { width: 1200, expectedColumns: 3, name: 'large desktop' }
        ];
        
        for (const breakpoint of breakpoints) {
            DOMHelper.simulateResize(breakpoint.width, 800);
            await DOMHelper.delay(100);
            
            const gridColumns = DOMHelper.getComputedStyle(this.quickStats, 'grid-template-columns');
            const columnCount = gridColumns === 'none' ? 1 : gridColumns.split(' ').length;
            
            Assert.isTrue(
                columnCount >= breakpoint.expectedColumns || 
                (breakpoint.expectedColumns === 1 && columnCount === 1),
                `Grid should have appropriate columns for ${breakpoint.name} (${breakpoint.width}px)`
            );
        }
    }
    
    async testTouchTargets() {
        // Test mobile touch targets (should be at least 44px)
        DOMHelper.simulateResize(375, 667);
        await DOMHelper.delay(100);
        
        // Test button sizes
        const buttons = [this.smallButton, this.normalButton];
        
        for (const button of buttons) {
            const rect = button.getBoundingClientRect();
            const computedStyle = window.getComputedStyle(button);
            
            // Get actual dimensions including padding
            const minHeight = parseFloat(computedStyle.minHeight) || rect.height;
            const minWidth = rect.width;
            
            // On mobile, touch targets should be at least 44px
            if (window.innerWidth <= 767) {
                Assert.isTrue(
                    minHeight >= 44 || rect.height >= 44,
                    `Button should have minimum height of 44px on mobile (actual: ${rect.height}px)`
                );
                
                Assert.isTrue(
                    minWidth >= 44 || rect.width >= 44,
                    `Button should have minimum width of 44px on mobile (actual: ${rect.width}px)`
                );
            }
        }
        
        // Test navigation items on mobile
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach((item, index) => {
            const rect = item.getBoundingClientRect();
            
            if (window.innerWidth <= 767) {
                Assert.isTrue(
                    rect.height >= 44,
                    `Navigation item ${index} should have minimum height of 44px on mobile`
                );
            }
        });
    }
}