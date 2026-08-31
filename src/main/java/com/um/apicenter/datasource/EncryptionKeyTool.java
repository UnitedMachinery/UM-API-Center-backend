package com.um.apicenter.datasource;

import java.security.SecureRandom;
import java.util.Base64;

public final class EncryptionKeyTool {

    private EncryptionKeyTool() {
    }

    public static void main(String[] args) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        System.out.println(Base64.getEncoder().encodeToString(key));
    }
}
