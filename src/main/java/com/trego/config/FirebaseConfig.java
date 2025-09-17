package com.trego.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    
    @Value("${firebase.project-id}")
    private String projectId;
    
    @Value("${firebase.credentials-path}")
    private String credentialsPath;
    
    @Value("${firebase.storage-bucket}")
    private String storageBucket;
    
    @Bean
    @Primary
    public FirebaseApp firebaseApp() throws IOException {
        logger.info("Initializing Firebase with project ID: {}", projectId);
        
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FileInputStream serviceAccount = new FileInputStream(credentialsPath);
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .setStorageBucket(storageBucket)
                        .setDatabaseUrl("https://" + projectId + "-default-rtdb.firebaseio.com/")
                        .build();
                
                FirebaseApp app = FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully with app name: {}", app.getName());
                return app;
                
            } catch (Exception e) {
                logger.error("Failed to initialize Firebase: {}", e.getMessage(), e);
                throw new IOException("Failed to initialize Firebase", e);
            }
        } else {
            logger.info("Firebase already initialized, returning existing app");
            return FirebaseApp.getInstance();
        }
    }
    
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        logger.info("Creating FirebaseAuth bean");
        return FirebaseAuth.getInstance(firebaseApp);
    }
    
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        logger.info("Creating Firestore client bean");
        return FirestoreClient.getFirestore(firebaseApp);
    }
    
    @Bean
    public StorageClient storageClient(FirebaseApp firebaseApp) {
        logger.info("Creating Storage client bean");
        return StorageClient.getInstance(firebaseApp);
    }
}