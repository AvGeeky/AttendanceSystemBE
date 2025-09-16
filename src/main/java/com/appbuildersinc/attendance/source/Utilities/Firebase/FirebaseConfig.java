package com.appbuildersinc.attendance.source.Utilities.Firebase;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() throws Exception {

        String projectId = System.getenv("FIREBASE_PROJECT_ID");
        String privateKey = System.getenv("FIREBASE_PRIVATE_KEY")
                .replace("\\n", "\n");
        String private_key_id = System.getenv("FIREBASE_PRIVATE_KEY_ID");
        String clientEmail = System.getenv("FIREBASE_CLIENT_EMAIL");
        String client_id = System.getenv("FIREBASE_CLIENT_ID");
        String client_x509_cert_url = System.getenv("FIREBASE_CLIENT_X509_CERT_URL");


        String json = String.format("""
        {
          "type": "service_account",
          "project_id": "%s",
          "private_key_id": "%s",
          "private_key": "%s",
          "client_email": "%s",
          "client_id": "%s",
          "auth_uri": "https://accounts.google.com/o/oauth2/auth",
          "token_uri": "https://oauth2.googleapis.com/token",
          "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
          "client_x509_cert_url": "%s",
          "universe_domain": "googleapis.com"
        }
        """, projectId,private_key_id, privateKey, clientEmail, client_id, client_x509_cert_url);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(ServiceAccountCredentials.fromStream(
                        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
                ))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
