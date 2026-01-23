# Expense Tracker PWA

A comprehensive Progressive Web Application for personal expense tracking with advanced notification features, budget alerts, and payment reminders.

## 🚀 Features

- **Expense Management**: Add, edit, and categorize expenses with ease
- **Budget Tracking**: Set monthly budgets with intelligent threshold alerts
- **Payment Reminders**: Create and manage recurring payment reminders
- **Push Notifications**: Real-time notifications for budget alerts and reminders
- **PWA Support**: Install as a native app on mobile and desktop
- **Offline Capability**: Works offline with background sync
- **Analytics Dashboard**: Comprehensive spending analytics and insights
- **Google Sheets Integration**: Sync data with Google Sheets for backup

## 🛠️ Technology Stack

- **Backend**: Java 17, Spring Boot 3.x, Spring Data JPA
- **Database**: H2 (development), PostgreSQL (production ready)
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **PWA**: Service Worker, Web App Manifest, Push API
- **Notifications**: Web Push Protocol with VAPID authentication
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Property-based testing with jqwik

## 📋 Prerequisites

- Java 17 or higher
- Gradle 7.x or higher
- Modern web browser with PWA support
- (Optional) Google Sheets API credentials for integration

## 🔧 Local Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd expense-tracker
```

### 2. Environment Configuration

Create `src/main/resources/application-local.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 Console (for development)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# VAPID Keys for Push Notifications (generate new ones for production)
vapid.public.key=your-vapid-public-key
vapid.private.key=your-vapid-private-key
vapid.subject=mailto:your-email@example.com

# Google Sheets Integration (optional)
google.sheets.credentials.file=credentials.json
google.sheets.spreadsheet.id=your-spreadsheet-id
```

### 3. Generate VAPID Keys

Run the VAPID key generator:

```bash
./gradlew test --tests VapidKeyGeneratorTest
```

Copy the generated keys to your `application-local.properties` file.

### 4. Google Sheets Setup (Optional)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google Sheets API
4. Create service account credentials
5. Download the JSON file as `src/main/resources/credentials.json`
6. Share your Google Sheet with the service account email

### 5. Build and Run

```bash
# Build the application
./gradlew build

# Run with local profile
./gradlew bootRun --args='--spring.profiles.active=local'
```

The application will be available at `http://localhost:8080`

### 6. Database Access

- H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

## 🧪 Testing

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Categories

```bash
# Unit tests only
./gradlew test --tests "*Test"

# Integration tests
./gradlew test --tests "*IntegrationTest"

# Property-based tests
./gradlew test --tests "*PropertyTest"
```

### Push Notification Testing

1. Start the application
2. Navigate to `http://localhost:8080/test-push-notifications.html`
3. Follow the test instructions to verify push notification functionality

## 📱 PWA Installation

### Desktop (Chrome/Edge)
1. Visit the application URL
2. Click the install icon in the address bar
3. Follow the installation prompts

### Mobile (Android/iOS)
1. Open the app in your mobile browser
2. Tap "Add to Home Screen" from the browser menu
3. The app will be installed as a native app

## 🔔 Notification Setup

### Browser Permissions
1. Allow notifications when prompted
2. For manual setup: Browser Settings → Site Settings → Notifications → Allow

### Push Notification Features
- **Budget Alerts**: Notifications when spending exceeds 80% of budget
- **Payment Reminders**: Scheduled reminders for upcoming payments
- **Daily Reminders**: Optional daily expense tracking reminders
- **Weekly Summaries**: Weekly spending summary notifications

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/expense/tracking/
│   │   ├── controller/          # REST controllers
│   │   ├── service/             # Business logic
│   │   ├── entity/              # JPA entities
│   │   ├── repository/          # Data access layer
│   │   ├── config/              # Configuration classes
│   │   └── dto/                 # Data transfer objects
│   └── resources/
│       ├── static/              # Frontend assets
│       │   ├── js/              # JavaScript files
│       │   ├── css/             # Stylesheets
│       │   └── icons/           # PWA icons
│       ├── templates/           # Email templates
│       └── db/migration/        # Database migrations
└── test/
    ├── java/                    # Test classes
    └── resources/               # Test resources
```

## 🚀 Production Deployment

### 1. Database Configuration

Update `application-prod.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/expense_tracker
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

### 2. Environment Variables

Set the following environment variables:

```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export VAPID_PUBLIC_KEY=your_vapid_public_key
export VAPID_PRIVATE_KEY=your_vapid_private_key
export VAPID_SUBJECT=mailto:your-email@example.com
```

### 3. Build Production JAR

```bash
./gradlew bootJar -Pprod
```

### 4. Run Production

```bash
java -jar build/libs/expense-tracker-*.jar --spring.profiles.active=prod
```

## 🔧 Configuration Options

### Notification Settings
- `notification.budget.threshold`: Budget warning threshold (default: 80%)
- `notification.daily.reminder.time`: Daily reminder time (default: 20:00)
- `notification.quiet.hours.start`: Quiet hours start (default: 22:00)
- `notification.quiet.hours.end`: Quiet hours end (default: 08:00)

### PWA Settings
- `pwa.cache.version`: Cache version for updates
- `pwa.offline.enabled`: Enable offline functionality
- `pwa.background.sync.enabled`: Enable background sync

## 🐛 Troubleshooting

### Push Notifications Not Working
1. Check browser console for errors
2. Verify VAPID keys are correctly configured
3. Ensure HTTPS is enabled (required for push notifications)
4. Test using `/test-push-notifications.html`

### Database Issues
1. Check database connection settings
2. Verify migration scripts have run successfully
3. Check H2 console for development debugging

### PWA Installation Issues
1. Ensure HTTPS is enabled
2. Verify manifest.json is accessible
3. Check service worker registration in browser dev tools

## 📚 API Documentation

### Expense Management
- `GET /api/expenses` - List expenses
- `POST /api/expenses` - Create expense
- `PUT /api/expenses/{id}` - Update expense
- `DELETE /api/expenses/{id}` - Delete expense

### Notification Management
- `GET /api/notification-settings` - Get user notification settings
- `PUT /api/notification-settings` - Update notification settings
- `POST /api/notifications/test` - Send test notification

### Push Subscriptions
- `GET /api/push-subscriptions/vapid-public-key` - Get VAPID public key
- `POST /api/push-subscriptions` - Subscribe to push notifications
- `DELETE /api/push-subscriptions/unsubscribe` - Unsubscribe

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
1. Check the troubleshooting section above
2. Review the test files for examples
3. Check browser console for error messages
4. Use the built-in test pages for debugging

## 🔄 Version History

- **v1.0.0**: Initial release with basic expense tracking
- **v1.1.0**: Added PWA support and offline functionality
- **v1.2.0**: Implemented push notifications and budget alerts
- **v1.3.0**: Added payment reminders and Google Sheets integration
- **v1.4.0**: Enhanced notification system with comprehensive testing