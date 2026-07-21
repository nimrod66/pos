package com.example.pos.compliance.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class CommunicationKeyManager {

    private static final Logger log = LoggerFactory.getLogger(CommunicationKeyManager.class);
    private static final String ALGORITHM = "AES";
    private static final String DERIVATION = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private final String masterPassphrase;

    public CommunicationKeyManager() {
        this.masterPassphrase = loadMasterPassphrase();
    }

    public String encrypt(String plainCmcKey) {
        try {
            SecretKey key = deriveKey(masterPassphrase);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainCmcKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encrypt communication key", e);
            throw new RuntimeException("Key encryption failed", e);
        }
    }

    public String decrypt(String encryptedCmcKey) {
        try {
            SecretKey key = deriveKey(masterPassphrase);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedCmcKey));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt communication key", e);
            throw new RuntimeException("Key decryption failed", e);
        }
    }

    public String maskKey(String rawKey) {
        if (rawKey == null || rawKey.length() <= 8) return "***";
        return rawKey.substring(0, 4) + "****" + rawKey.substring(rawKey.length() - 4);
    }

    private SecretKey deriveKey(String passphrase) throws Exception {
        byte[] salt = "etims-pos-salt".getBytes(StandardCharsets.UTF_8);
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(DERIVATION);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private String loadMasterPassphrase() {
        return System.getProperty("compliance.master.passphrase", "etims-default-passphrase-change-me");
    }
}
