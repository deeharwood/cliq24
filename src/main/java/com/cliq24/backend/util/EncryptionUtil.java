package com.cliq24.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    /**
     * Encrypt sensitive data (like OAuth tokens)
     */
    public String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }
    
    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}

