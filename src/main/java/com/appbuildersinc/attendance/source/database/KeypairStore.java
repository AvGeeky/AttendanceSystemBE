package com.appbuildersinc.attendance.source.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


// KeypairStore is a class that handles the storage and retrieval of key pairs
@Repository
public class KeypairStore {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    static String uri = dotenv.get("API_KEY");

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri + "/?serverSelectionTimeoutMS=60000"))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("AttendEz");
            collection = database.getCollection("Users");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKeyFromPassphrase(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 10, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static void generateAndStoreKeyPair(String passphrase) throws Exception {
        // Generate RSA keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // Generate salt and derive AES key
        byte[] salt = generateSalt();
        SecretKey secretKey = deriveKeyFromPassphrase(passphrase, salt);

        // Encrypt private key
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
        byte[] encryptedPrivateKey = cipher.doFinal(keyPair.getPrivate().getEncoded());

        // Append salt to encrypted private key
        byte[] encryptedPrivateKeyWithSalt = new byte[encryptedPrivateKey.length + salt.length];
        System.arraycopy(encryptedPrivateKey, 0, encryptedPrivateKeyWithSalt, 0, encryptedPrivateKey.length);
        System.arraycopy(salt, 0, encryptedPrivateKeyWithSalt, encryptedPrivateKey.length, salt.length);

        // Store in MongoDB
        Document doc = new Document("doctype", "keypair")
                .append("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))
                .append("encryptedPrivateKey", Base64.getEncoder().encodeToString(encryptedPrivateKeyWithSalt));
        collection.deleteOne(Filters.eq("doctype", "keypair"));
        collection.insertOne(doc);
    }

    public static KeyPair getKeyPair(String passphrase) throws Exception {
        Document doc = collection.find(Filters.eq("doctype", "keypair")).first();
        if (doc == null) throw new IllegalStateException("Keypair not found");

        byte[] publicKeyBytes = Base64.getDecoder().decode(doc.getString("publicKey"));
        byte[] encryptedPrivateKeyWithSalt = Base64.getDecoder().decode(doc.getString("encryptedPrivateKey"));

        // Extract salt from the end
        int saltLength = 16;
        int encryptedLength = encryptedPrivateKeyWithSalt.length - saltLength;
        byte[] encryptedPrivateKey = new byte[encryptedLength];
        byte[] salt = new byte[saltLength];
        System.arraycopy(encryptedPrivateKeyWithSalt, 0, encryptedPrivateKey, 0, encryptedLength);
        System.arraycopy(encryptedPrivateKeyWithSalt, encryptedLength, salt, 0, saltLength);

        // Decrypt private key
        SecretKey secretKey = deriveKeyFromPassphrase(passphrase, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
        byte[] privateKeyBytes = cipher.doFinal(encryptedPrivateKey);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        return new KeyPair(publicKey, privateKey);
    }


    //RUN THIS (ONE TIME PROCESS) TO CREATE AND STORE KEYPAIR
    public static void main(String[] args) throws Exception {

        KeypairStore.generateAndStoreKeyPair(dotenv.get("KEYPAIR_PASSPHRASE"));
    }
}