package com.um.apicenter.datasource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class DataSourcePasswordCipher {

    private static final String VERSION = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public DataSourcePasswordCipher(@Value("${DATASOURCE_ENCRYPTION_KEY:}") String encodedKey) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException();
            }
            key = new SecretKeySpec(decodedKey, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATASOURCE_ENCRYPTION_KEY 必须是 Base64 编码的 32 字节密钥。", exception);
        }
    }

    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = Arrays.copyOf(iv, iv.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return VERSION + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("无法加密数据源密码。", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || !encryptedValue.startsWith(VERSION)) {
            throw new IllegalStateException("数据源密码格式不受支持。");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue.substring(VERSION.length()));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException();
            }
            byte[] iv = Arrays.copyOf(payload, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("无法解密数据源密码。", exception);
        }
    }
}
