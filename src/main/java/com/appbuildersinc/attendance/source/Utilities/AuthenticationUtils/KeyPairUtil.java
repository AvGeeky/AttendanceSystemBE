package com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils;

import com.appbuildersinc.attendance.source.database.MongoDB.KeypairStore;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

//KeyPairUtil is a utility class for encrypting and decrypting data using RSA public/private key pairs
@Service
public class KeyPairUtil {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();

    static PublicKey publicKey;
    static PrivateKey privateKey;

    static {
        try {
            String passphrase = dotenv.get("KEYPAIR_PASSPHRASE");
            KeyPair kp = KeypairStore.getKeyPair(passphrase);
            publicKey = kp.getPublic();
            privateKey  = kp.getPrivate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Encrypts the data using the public key
    public String encryptString(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }


    // Method to decrypt data using the private key
    public String decryptString(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData);
    }
}
