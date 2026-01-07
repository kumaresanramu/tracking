# Google Sheets API Setup Documentation

This document provides step-by-step instructions for setting up Google Sheets API authentication for the Expense Tracking System.

## Prerequisites

- Google Cloud Platform account
- Google Sheets spreadsheet for expense tracking

## Setup Steps

### 1. Create a Google Cloud Project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note down your project ID

### 2. Enable Google Sheets API

1. In the Google Cloud Console, navigate to "APIs & Services" > "Library"
2. Search for "Google Sheets API"
3. Click on it and press "Enable"

### 3. Create Service Account Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "Service Account"
3. Fill in the service account details:
   - Name: `expense-tracker-service`
   - Description: `Service account for expense tracking application`
4. Click "Create and Continue"
5. Skip the optional steps and click "Done"

### 4. Generate Service Account Key

1. In the "Credentials" page, find your newly created service account
2. Click on the service account email
3. Go to the "Keys" tab
4. Click "Add Key" > "Create New Key"
5. Select "JSON" format and click "Create"
6. Save the downloaded JSON file as `credentials.json` in `src/main/resources/`

### 5. Create Google Sheets Spreadsheet

1. Go to [Google Sheets](https://sheets.google.com/)
2. Create a new spreadsheet
3. Name it "Expense Tracker" or your preferred name
4. Copy the spreadsheet ID from the URL (the long string between `/d/` and `/edit`)
   - Example: `https://docs.google.com/spreadsheets/d/SPREADSHEET_ID_HERE/edit`

### 6. Share Spreadsheet with Service Account

1. In your Google Sheets spreadsheet, click "Share"
2. Add the service account email (found in your credentials.json file)
3. Give it "Editor" permissions
4. Click "Send"

### 7. Configure Application

1. Copy `src/main/resources/credentials.json.template` to `src/main/resources/credentials.json`
2. Replace the template values with your actual service account credentials
3. Set the environment variable `GOOGLE_SHEETS_SPREADSHEET_ID` to your spreadsheet ID:
   ```bash
   export GOOGLE_SHEETS_SPREADSHEET_ID=your_spreadsheet_id_here
   ```

   Or add it to your application.properties:
   ```properties
   google.sheets.spreadsheet.id=your_spreadsheet_id_here
   ```

## Security Notes

- **Never commit credentials.json to version control**
- Add `credentials.json` to your `.gitignore` file
- In production, use environment variables or cloud-native credential management
- The service account should have minimal required permissions

## Testing the Setup

1. Start the application
2. Check the logs for successful Google Sheets connection
3. Create a test expense through the API
4. Verify the expense appears in your Google Sheets spreadsheet

## Troubleshooting

### Common Issues

1. **403 Forbidden Error**: 
   - Ensure the service account has access to the spreadsheet
   - Check that the Google Sheets API is enabled

2. **404 Not Found Error**:
   - Verify the spreadsheet ID is correct
   - Ensure the spreadsheet exists and is accessible

3. **Authentication Errors**:
   - Check that credentials.json is properly formatted
   - Verify the service account key is valid

4. **Permission Errors**:
   - Ensure the service account has "Editor" access to the spreadsheet
   - Check that the spreadsheet is shared with the service account email

## Production Deployment

For production environments:

1. Use Google Cloud's Application Default Credentials
2. Set up proper IAM roles and permissions
3. Use environment variables for configuration
4. Consider using Google Cloud Secret Manager for credential storage

## API Rate Limits

Google Sheets API has the following limits:
- 300 requests per minute per project
- 100 requests per 100 seconds per user

The application implements basic retry logic with exponential backoff to handle rate limiting.