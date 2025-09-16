package com.appbuildersinc.attendance.source.Utilities.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {

    public FirebaseToken verifyIdToken(String idToken) throws Exception {
        // Verifies the token and decodes it
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
