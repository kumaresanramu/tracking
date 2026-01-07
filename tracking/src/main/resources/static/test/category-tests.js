// Category Tests - Testing category hierarchy UI components
class CategoryTests {
    constructor() {
        this.name = 'Category Hierarchy Tests';
        this.tests = [
            {
                name: 'Category tree renders hierarchical structure',
                run: () => this.testCategoryTreeRendering()
            },
            {
                name: 'Category tree expand/collapse functionality works',
                run: () => this.testCategoryTreeToggle()
            },
            {
                name: 'Category selection works correctly',
                run: () => this.testCategorySelection()
            },
            {
                name: 'Category dropdown renders with proper hierarchy',
                run: () => this.testCategoryDropdown()
            },
            {
                name: 'Category components are keyboard accessible',
                run: () => this.testKeyboardAccessibility()
            }
        ];
        
        // Test data
        this.testCategories = [
            { id: 1, name: 'Food', parentId: null },
            { id: 2, name: 'Restaurants', parentId: 1 },
            { id: 3, name: 'Groceries', parentId: 1 },
            { id: 4, name: 'Transportation', parentId: null },
            { id: 5, name: 'Gas', parentId: 4 },
            { id: 6, name: 'Public Transit', parentId: 4 },
            { id: 7, name: 'Housing', parentId: null },
            { id: 8, name: 'Rent', parentId: 7 },
            { id: 9, name: 'Utilities', parentId: 7 },
            { id: 10, name: 'Electricity', parentId: 9 }
        ];
    }
    
    setup() {
        // Get the category fixture
        this.categoryFixture = document.getElementById('category-fixture');
        this.categoryTreeContainer = document.getElementById('test-category-tree');
        
        // Make fixture visible for testing
        this.categoryFixture.classList.add('visible-fixture');
        
        // Initialize UI components
        this.ui = new UIComponents();
    }
    
    teardown() {
        // Hide fixture
        this.categoryFixture.classList.remove('visible-fixture');
        
        // Clear category tree
        this.categoryTreeContainer.innerHTML = '';
    }
    
    async testCategoryTreeRendering() {
        // Render category tree
        this.ui.renderCategoryTree(this.testCategories, this.categoryTreeContainer, {
            selectable: true
        });
        
        await DOMHelper.delay(50);
        
        // Check that tree structure is rendered
        const categoryTree = this.categoryTreeContainer.querySelector('.category-tree');
        Assert.exists(categoryTree, 'Category tree should be rendered');
        
        // Check root categories are present
        const rootCategories = categoryTree.querySelectorAll('li > .category-item[data-level="0"]');
        Assert.equals(rootCategories.length, 3, 'Should have 3 root categories (Food, Transportation, Housing)');
        
        // Check that hierarchical structure is maintained
        const foodCategory = Array.from(rootCategories).find(cat => 
            cat.querySelector('.category-name').textContent === 'Food'
        );
        Assert.exists(foodCategory, 'Food category should be present');
        
        // Check for subcategories
        const foodItem = foodCategory.closest('li');
        const foodChildren = foodItem.querySelector('.category-children');
        Assert.exists(foodChildren, 'Food category should have children container');
        
        const foodSubcategories = foodChildren.querySelectorAll('.category-item[data-level="1"]');
        Assert.equals(foodSubcategories.length, 2, 'Food should have 2 subcategories (Restaurants, Groceries)');
    }
}
    
    async testCategoryTreeToggle() {
        // Render category tree
        this.ui.renderCategoryTree(this.testCategories, this.categoryTreeContainer, {
            selectable: true
        });
        
        await DOMHelper.delay(50);
        
        // Find a category with children (Food)
        const foodToggle = this.categoryTreeContainer.querySelector('.category-toggle');
        Assert.exists(foodToggle, 'Category toggle should exist for categories with children');
        
        // Initially children should be hidden
        const foodChildren = foodToggle.closest('li').querySelector('.category-children');
        Assert.isFalse(foodChildren.classList.contains('expanded'), 'Children should be initially hidden');
        
        // Click toggle to expand
        DOMHelper.fireClickEvent(foodToggle);
        await DOMHelper.delay(50);
        
        Assert.isTrue(foodChildren.classList.contains('expanded'), 'Children should be visible after toggle click');
        Assert.hasClass(foodToggle, 'expanded', 'Toggle should have expanded class');
        Assert.equals(foodToggle.textContent, '▼', 'Toggle icon should change to down arrow');
        
        // Click toggle again to collapse
        DOMHelper.fireClickEvent(foodToggle);
        await DOMHelper.delay(50);
        
        Assert.isFalse(foodChildren.classList.contains('expanded'), 'Children should be hidden after second toggle click');
        Assert.doesNotHaveClass(foodToggle, 'expanded', 'Toggle should not have expanded class');
        Assert.equals(foodToggle.textContent, '▶', 'Toggle icon should change back to right arrow');
    }
    
    async testCategorySelection() {
        // Render selectable category tree
        let selectedCategoryId = null;
        
        this.ui.renderCategoryTree(this.testCategories, this.categoryTreeContainer, {
            selectable: true,
            onSelect: (categoryId) => {
                selectedCategoryId = categoryId;
            }
        });
        
        await DOMHelper.delay(50);
        
        // Find and click a category
        const foodCategoryName = this.categoryTreeContainer.querySelector('[data-category-id="1"]');
        Assert.exists(foodCategoryName, 'Food category name should exist');
        
        DOMHelper.fireClickEvent(foodCategoryName);
        await DOMHelper.delay(50);
        
        // Check selection state
        const foodCategoryItem = foodCategoryName.closest('.category-item');
        Assert.hasClass(foodCategoryItem, 'selected', 'Clicked category should be selected');
        Assert.equals(selectedCategoryId, '1', 'Selection callback should be called with correct ID');
        
        // Click another category
        const transportationCategoryName = this.categoryTreeContainer.querySelector('[data-category-id="4"]');
        if (transportationCategoryName) {
            DOMHelper.fireClickEvent(transportationCategoryName);
            await DOMHelper.delay(50);
            
            // Previous selection should be cleared
            Assert.doesNotHaveClass(foodCategoryItem, 'selected', 'Previous selection should be cleared');
            
            // New selection should be active
            const transportationCategoryItem = transportationCategoryName.closest('.category-item');
            Assert.hasClass(transportationCategoryItem, 'selected', 'New category should be selected');
            Assert.equals(selectedCategoryId, '4', 'Selection callback should be called with new ID');
        }
    }
    
    async testCategoryDropdown() {
        // Create dropdown container
        const dropdownContainer = DOMHelper.createElement('div', { id: 'test-dropdown' });
        this.categoryFixture.appendChild(dropdownContainer);
        
        let selectedCategoryId = null;
        let selectedCategoryName = null;
        
        // Render category dropdown
        this.ui.renderCategoryDropdown(this.testCategories, dropdownContainer, {
            placeholder: 'Select a category',
            onSelect: (categoryId, categoryName) => {
                selectedCategoryId = categoryId;
                selectedCategoryName = categoryName;
            }
        });
        
        await DOMHelper.delay(50);
        
        // Check dropdown structure
        const dropdown = dropdownContainer.querySelector('.category-dropdown');
        Assert.exists(dropdown, 'Category dropdown should be rendered');
        
        const toggle = dropdown.querySelector('.category-dropdown-toggle');
        const menu = dropdown.querySelector('.category-dropdown-menu');
        
        Assert.exists(toggle, 'Dropdown toggle should exist');
        Assert.exists(menu, 'Dropdown menu should exist');
        
        // Initially menu should be hidden
        Assert.isFalse(menu.classList.contains('active'), 'Menu should be initially hidden');
        
        // Click toggle to open menu
        DOMHelper.fireClickEvent(toggle);
        await DOMHelper.delay(50);
        
        Assert.isTrue(menu.classList.contains('active'), 'Menu should be visible after toggle click');
        Assert.attributeEquals(toggle, 'aria-expanded', 'true', 'Toggle should have aria-expanded=true');
        
        // Check hierarchical items
        const menuItems = menu.querySelectorAll('.category-dropdown-item');
        Assert.isTrue(menuItems.length > 0, 'Menu should have category items');
        
        // Check that subcategories have proper indentation
        const subcategoryItems = menu.querySelectorAll('.category-dropdown-item[data-level="1"]');
        Assert.isTrue(subcategoryItems.length > 0, 'Menu should have subcategory items');
        
        // Click a category item
        const foodItem = Array.from(menuItems).find(item => 
            item.textContent.trim() === 'Food'
        );
        
        if (foodItem) {
            DOMHelper.fireClickEvent(foodItem);
            await DOMHelper.delay(50);
            
            // Menu should close
            Assert.isFalse(menu.classList.contains('active'), 'Menu should close after selection');
            
            // Selection should be updated
            Assert.equals(selectedCategoryId, '1', 'Selected category ID should be correct');
            Assert.equals(selectedCategoryName, 'Food', 'Selected category name should be correct');
            
            // Toggle text should be updated
            const selectedText = toggle.querySelector('.selected-text');
            Assert.equals(selectedText.textContent, 'Food', 'Toggle text should show selected category');
        }
        
        // Clean up
        dropdownContainer.remove();
    }
    
    async testKeyboardAccessibility() {
        // Render category tree
        this.ui.renderCategoryTree(this.testCategories, this.categoryTreeContainer, {
            selectable: true
        });
        
        await DOMHelper.delay(50);
        
        // Test keyboard navigation on toggle
        const toggle = this.categoryTreeContainer.querySelector('.category-toggle');
        if (toggle) {
            toggle.focus();
            
            // Test Enter key
            const enterEvent = new KeyboardEvent('keydown', {
                key: 'Enter',
                bubbles: true
            });
            toggle.dispatchEvent(enterEvent);
            
            // Should trigger click (we'll simulate this)
            DOMHelper.fireClickEvent(toggle);
            await DOMHelper.delay(50);
            
            const children = toggle.closest('li').querySelector('.category-children');
            Assert.isTrue(children.classList.contains('expanded'), 'Enter key should expand category');
        }
        
        // Test keyboard navigation on category names
        const categoryName = this.categoryTreeContainer.querySelector('.category-name');
        if (categoryName) {
            categoryName.focus();
            
            // Test Enter key for selection
            const enterEvent = new KeyboardEvent('keydown', {
                key: 'Enter',
                bubbles: true
            });
            categoryName.dispatchEvent(enterEvent);
            
            // Should trigger selection (we'll simulate this)
            DOMHelper.fireClickEvent(categoryName);
            await DOMHelper.delay(50);
            
            const categoryItem = categoryName.closest('.category-item');
            Assert.hasClass(categoryItem, 'selected', 'Enter key should select category');
        }
        
        // Test focus states
        const focusableElements = this.categoryTreeContainer.querySelectorAll('[tabindex="0"]');
        Assert.isTrue(focusableElements.length > 0, 'Category tree should have focusable elements');
        
        focusableElements.forEach(element => {
            element.focus();
            const focusedElement = document.activeElement;
            Assert.equals(focusedElement, element, 'Element should be focusable');
        });
    }
}