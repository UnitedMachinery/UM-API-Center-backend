package com.um.apicenter.datasource;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataSourcePasswordCipherTest {

    @Test
    void encryptsWithRandomIvAndDecryptsWithSameKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        DataSourcePasswordCipher cipher = new DataSourcePasswordCipher(Base64.getEncoder().encodeToString(key));

        String first = cipher.encrypt("readonly-password");
        String second = cipher.encrypt("readonly-password");

        assertThat(first).startsWith("v1:").isNotEqualTo("readonly-password");
        assertThat(second).isNotEqualTo(first);
        assertThat(cipher.decrypt(first)).isEqualTo("readonly-password");
    }

    @Test
    void rejectsInvalidEncryptionKey() {
        assertThatThrownBy(() -> new DataSourcePasswordCipher("not-a-valid-key"))
                .isInstanceOf(IllegalStateException.class);
    }
}
