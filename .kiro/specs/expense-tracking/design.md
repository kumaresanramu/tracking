# Design Document: Expense Tracking System

## Overview

The Expense Tracking System is a Progressive Web Application (PWA) built with a Spring Boot backend and modern web frontend. The system provides comprehensive expense management with local database storage, hierarchical categorization, payment reminders, and visual analytics. The architecture follows a client-server model with PWA capabilities and local data persistence.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        PWA[PWA Frontend]
        SW[Service Worker]
        IDB[IndexedDB Cache]
    end
    
    subgraph "Server Layer"
        API[Spring Boot API]
        DB[H2 Database]
    end
    
    PWA --> API
    SW --> IDB
    API --> DB
```

### Technology Stack

**Backend:**
- Spring Boot 3.x with Java 17+
- Spring Web (REST API)
- Spring Data JPA (Database operations)
- H2 Database (for development and local storage)

**Frontend:**
- Vanilla JavaScript ES6+ or React (lightweight PWA)
- Service Worker for offline functionality
- IndexedDB for client-side storage
- Chart.js for data visualization
- CSS Grid/Flexbox for responsive design

## Components and Interfaces

### Backend Components

#### 1. Expense Controller
```java
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    
    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody ExpenseRequest request);
    
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<List<Expense>> getExpensesByMonth(@PathVariable int year, @PathVariable int month);
    
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories();
    
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @RequestBody ExpenseRequest request);
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id);
}
```

#### 2. Payment Reminder Service
```java
@Service
public class PaymentReminderService {
    
    @Scheduled(fixedRate = 3600000) // Check every hour
    public void checkDuePayments();
    
    public void createReminder(PaymentReminder reminder);
    public List<PaymentReminder> getUpcomingReminders();
    public void markReminderAsPaid(Long reminderId);
    public void scheduleCustomNotification(Long reminderId, LocalDateTime notificationTime);
    public void updateReminderPreferences(Long reminderId, ReminderPreferences preferences);
}
```

### Frontend Components

#### 1. Expense Entry Component
```javascript
class ExpenseEntry {
    constructor() {
        this.categoryService = new CategoryService();
        this.expenseService = new ExpenseService();
    }
    
    async submitExpense(expenseData) {
        // Validate input
        // Submit to API or queue for offline sync
        // Update UI
    }
    
    loadCategories() {
        // Load hierarchical categories
        // Populate dropdown/tree view
    }
}
```

#### 2. Analytics Dashboard
```javascript
class AnalyticsDashboard {
    constructor() {
        this.chartRenderer = new ChartRenderer();
    }
    
    async loadMonthlyData(year, month) {
        // Fetch expense data
        // Generate charts and summaries
    }
    
    renderCategoryBreakdown(expenses) {
        // Create pie chart for categories
        // Show hierarchical breakdown
    }
}
```

#### 3. Service Worker
```javascript
// sw.js
self.addEventListener('fetch', event => {
    // Cache-first strategy for app shell
    // Network-first for API calls with fallback
});

self.addEventListener('install', event => {
    // Cache essential resources
});
```

## Data Models

### Core Entities

#### Expense Entity
```java
@Entity
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters, setters, constructors
}
```

#### Category Entity
```java
@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> subcategories = new ArrayList<>();
    
    private String color; // For UI visualization
    
    // Getters, setters, constructors
}
```

#### Payment Reminder Entity
```java
@Entity
public class PaymentReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Enumerated(EnumType.STRING)
    private ReminderFrequency frequency; // MONTHLY, QUARTERLY, YEARLY
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    private boolean active;
    private LocalDate lastPaid;
    
    // User-configurable notification preferences
    private Integer daysBefore; // How many days before due date to notify (default: 3)
    private LocalTime preferredNotificationTime; // User's preferred time (default: 9:00 AM)
    private boolean enableEmailNotification;
    private boolean enablePushNotification;
    private String customMessage; // Optional custom reminder message
    
    // Getters, setters, constructors
}
```

#### Reminder Preferences Entity
```java
@Entity
public class ReminderPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "reminder_id")
    private PaymentReminder reminder;
    
    private Integer daysBefore; // 1-30 days before due date
    private LocalTime notificationTime; // User's preferred time
    private boolean weekendsOnly; // Only notify on weekends
    private boolean weekdaysOnly; // Only notify on weekdays
    private Set<DayOfWeek> specificDays; // Specific days of week
    
    // Getters, setters, constructors
}
```

### Database Schema

The H2 database will store all data locally with the following main tables:

**Expenses Table:**
- id (Primary Key)
- amount (Decimal)
- date (Date)
- category_id (Foreign Key)
- description (String)
- created_at (Timestamp)
- updated_at (Timestamp)

**Categories Table:**
- id (Primary Key)
- name (String, unique)
- parent_id (Foreign Key, self-reference)
- color (String)

**Payment Reminders Table:**
- id (Primary Key)
- name (String)
- amount (Decimal)
- due_date (Date)
- frequency (Enum)
- category_id (Foreign Key)
- active (Boolean)
- last_paid (Date)
- days_before (Integer)
- preferred_notification_time (Time)
- enable_email_notification (Boolean)
- enable_push_notification (Boolean)
- custom_message (String)

## Error Handling

### Backend Error Handling
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityError(DataIntegrityViolationException e) {
        // Handle database constraint violations
        // Return appropriate error response
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(ValidationException e) {
        // Return validation error details
    }
}
```

### Frontend Error Handling
```javascript
class ErrorHandler {
    static async handleApiError(error) {
        if (error.status === 0) {
            // Network error - show offline message
            NotificationService.showOfflineMessage();
        } else if (error.status >= 500) {
            // Server error - show retry option
            NotificationService.showRetryableError(error.message);
        }
    }
}
```

## Testing Strategy

The testing approach combines unit tests for specific functionality and property-based tests for universal correctness properties.

### Unit Testing
- **Spring Boot Tests**: Test controllers, services, and repositories
- **Frontend Tests**: Test components, services, and utilities
- **Integration Tests**: Test API endpoints and Google Sheets integration
- **PWA Tests**: Test service worker functionality and offline capabilities

### Property-Based Testing
Property-based tests will validate universal properties using JUnit 5 with jqwik for Java backend testing.

**Configuration**: Each property test runs minimum 100 iterations with random input generation.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Expense Data Persistence
*For any* valid expense with date, amount, category, subcategory, and description, storing the expense should result in all fields being retrievable with identical values.
**Validates: Requirements 1.1**

### Property 2: Category Hierarchy Display
*For any* category with subcategories, selecting that category should display all and only its direct subcategories.
**Validates: Requirements 1.2**

### Property 3: Monthly Expense Organization
*For any* set of expenses across different months, viewing expenses should group them correctly by month with proper category hierarchy maintained within each month.
**Validates: Requirements 1.3**

### Property 4: Local Data Persistence
*For any* newly created expense, the data should be immediately stored in the local database and retrievable.
**Validates: Requirements 2.2**

### Property 5: Data Modification Persistence
*For any* expense modification, the corresponding database entry should reflect the same changes immediately after the update operation.
**Validates: Requirements 2.3**

### Property 6: Reminder Scheduling
*For any* recurring expense setup, the system should create reminder entries with the correct frequency and due dates.
**Validates: Requirements 3.1**

### Property 7: Notification Timing
*For any* payment reminder with a due date, a notification should be triggered exactly 3 days before the due date.
**Validates: Requirements 3.2**

### Property 8: Reminder Content Completeness
*For any* triggered reminder, the notification should contain the expense name, amount, and due date.
**Validates: Requirements 3.3**

### Property 9: Reminder to Expense Conversion
*For any* reminder marked as paid, a corresponding expense record should be created with matching details.
**Validates: Requirements 3.4**

### Property 10: PWA Functionality
*For any* expense creation or viewing operation, the PWA should function correctly with local data storage.
**Validates: Requirements 4.2**

### Property 11: Data Caching
*For any* essential application data, it should be cached locally and available for fast access.
**Validates: Requirements 4.4**

### Property 12: Monthly Trend Visualization
*For any* set of expenses across multiple months, the analytics page should display accurate monthly trend data.
**Validates: Requirements 5.1**

### Property 13: Category Breakdown Accuracy
*For any* selected month with expenses, the category-wise breakdown should show correct totals for each category.
**Validates: Requirements 5.2**

### Property 14: Hierarchical Category Totals
*For any* hierarchical category structure, parent category totals should equal the sum of all subcategory amounts.
**Validates: Requirements 6.5**

### Property 15: API CRUD Completeness
*For any* expense entity, all CRUD operations (create, read, update, delete) should be available through RESTful endpoints.
**Validates: Requirements 7.1**

### Property 16: Input Validation
*For any* incoming expense data, invalid data should be rejected with appropriate error messages, while valid data should be processed successfully.
**Validates: Requirements 7.4**

### Property 17: Data Integrity
*For any* database operation, data integrity constraints should be maintained and violations should be handled gracefully.
**Validates: Requirements 9.2**

### Property 18: Performance Consistency
*For any* expense operation, the system should maintain responsive performance even with large datasets.
**Validates: Requirements 9.5**

## Testing Strategy

### Dual Testing Approach
The system will use both unit tests and property-based tests to ensure comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and error conditions
- **Property tests**: Verify universal properties across all inputs
- Both approaches are complementary and necessary for complete validation

### Property-Based Testing Configuration
- **Framework**: jqwik for Java backend, fast-check for JavaScript frontend
- **Iterations**: Minimum 100 iterations per property test
- **Tagging**: Each test tagged with format: **Feature: expense-tracking, Property {number}: {property_text}**

### Unit Testing Focus Areas
- Database operations and data persistence
- Input validation and error handling
- Payment reminder scheduling logic
- PWA service worker functionality
- Specific UI component behaviors

### Integration Testing
- End-to-end expense creation and storage workflows
- Payment reminder workflow testing
- Cross-browser PWA functionality
- Database integrity and constraint testing

The testing strategy ensures both functional correctness through property-based testing and practical reliability through comprehensive unit and integration tests.