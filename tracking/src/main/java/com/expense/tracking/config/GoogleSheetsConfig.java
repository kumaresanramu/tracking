package com.expense.tracking.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
@Slf4j
public class GoogleSheetsConfig {

    private static final String APPLICATION_NAME = "Expense Tracker";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.sheets.credentials.path:src/main/resources/credentials.json}")
    private String credentialsFilePath;

    @Value("${google.sheets.enabled:false}")
    private boolean googleSheetsEnabled;

    @Bean
    @ConditionalOnProperty(name = "google.sheets.enabled", havingValue = "true")
    @Primary
    public Sheets sheetsClient() throws IOException, GeneralSecurityException {
        log.info("Initializing Google Sheets client with credentials from: {}", credentialsFilePath);
        
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleCredentials credentials = getCredentials();
        
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "google.sheets.enabled", havingValue = "false", matchIfMissing = true)
    @Primary
    public Sheets mockSheetsClient() {
        log.warn("Google Sheets integration is disabled. Using mock client for development.");
        return null; // GoogleSheetsService will handle null client gracefully
    }

    private GoogleCredentials getCredentials() throws IOException {
        try {
            // Check if credentials file exists
            if (Files.exists(Paths.get(credentialsFilePath))) {
                log.info("Loading Google Sheets credentials from file: {}", credentialsFilePath);
                return GoogleCredentials.fromStream(new FileInputStream(credentialsFilePath))
                        .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
            } else {
                log.info("Credentials file not found, trying default credentials");
                // Fallback to default credentials (for production/cloud deployment)
                return GoogleCredentials.getApplicationDefault()
                        .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
            }
        } catch (Exception e) {
            log.error("Failed to load Google Sheets credentials: {}", e.getMessage());
            throw new IOException("Failed to load Google Sheets credentials", e);
        }
    }
}