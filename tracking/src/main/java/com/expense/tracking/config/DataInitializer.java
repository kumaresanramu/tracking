package com.expense.tracking.config;

import com.expense.tracking.entity.Category;
import com.expense.tracking.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            log.info("Initializing default categories...");
            initializeDefaultCategories();
        }
    }
    
    private void initializeDefaultCategories() {
        List<Category> defaultCategories = Arrays.asList(
            Category.builder()
                .name("Food & Dining")
                .color("#FF6B6B")
                .description("Restaurants, groceries, and food-related expenses")
                .build(),
                
            Category.builder()
                .name("Transportation")
                .color("#4ECDC4")
                .description("Gas, public transport, car maintenance")
                .build(),
                
            Category.builder()
                .name("Shopping")
                .color("#45B7D1")
                .description("Clothing, electronics, and general shopping")
                .build(),
                
            Category.builder()
                .name("Entertainment")
                .color("#96CEB4")
                .description("Movies, games, hobbies, and entertainment")
                .build(),
                
            Category.builder()
                .name("Bills & Utilities")
                .color("#FFEAA7")
                .description("Electricity, water, internet, phone bills")
                .build(),
                
            Category.builder()
                .name("Healthcare")
                .color("#DDA0DD")
                .description("Medical expenses, pharmacy, health insurance")
                .build(),
                
            Category.builder()
                .name("Education")
                .color("#98D8C8")
                .description("Books, courses, tuition, educational materials")
                .build(),
                
            Category.builder()
                .name("Travel")
                .color("#F7DC6F")
                .description("Vacation, business travel, accommodation")
                .build(),
                
            Category.builder()
                .name("Home & Garden")
                .color("#BB8FCE")
                .description("Home improvement, furniture, gardening")
                .build(),
                
            Category.builder()
                .name("Personal Care")
                .color("#85C1E9")
                .description("Haircuts, cosmetics, personal hygiene")
                .build()
        );
        
        categoryRepository.saveAll(defaultCategories);
        log.info("Initialized {} default categories", defaultCategories.size());
        
        // Create some subcategories
        createSubcategories();
    }
    
    private void createSubcategories() {
        // Food subcategories
        Category food = categoryRepository.findByName("Food & Dining").orElse(null);
        if (food != null) {
            List<Category> foodSubcategories = Arrays.asList(
                Category.builder()
                    .name("Restaurants")
                    .parent(food)
                    .color("#FF6B6B")
                    .description("Dining out, takeout")
                    .build(),
                    
                Category.builder()
                    .name("Groceries")
                    .parent(food)
                    .color("#FF6B6B")
                    .description("Supermarket, food shopping")
                    .build(),
                    
                Category.builder()
                    .name("Coffee & Snacks")
                    .parent(food)
                    .color("#FF6B6B")
                    .description("Coffee shops, quick snacks")
                    .build()
            );
            categoryRepository.saveAll(foodSubcategories);
        }
        
        // Transportation subcategories
        Category transport = categoryRepository.findByName("Transportation").orElse(null);
        if (transport != null) {
            List<Category> transportSubcategories = Arrays.asList(
                Category.builder()
                    .name("Gas")
                    .parent(transport)
                    .color("#4ECDC4")
                    .description("Fuel for vehicles")
                    .build(),
                    
                Category.builder()
                    .name("Public Transport")
                    .parent(transport)
                    .color("#4ECDC4")
                    .description("Bus, train, subway tickets")
                    .build(),
                    
                Category.builder()
                    .name("Car Maintenance")
                    .parent(transport)
                    .color("#4ECDC4")
                    .description("Repairs, oil changes, car wash")
                    .build()
            );
            categoryRepository.saveAll(transportSubcategories);
        }
        
        log.info("Created subcategories for Food & Dining and Transportation");
    }
}