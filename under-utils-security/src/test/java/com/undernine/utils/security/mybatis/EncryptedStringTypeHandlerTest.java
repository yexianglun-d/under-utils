package com.undernine.utils.security.mybatis;

import com.undernine.utils.security.crypto.AesGcmFieldEncryptor;
import com.undernine.utils.security.crypto.FieldEncryptionException;
import com.undernine.utils.security.crypto.StaticKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncryptedStringTypeHandlerTest {

    private final AesGcmFieldEncryptor encryptor = new AesGcmFieldEncryptor(
            StaticKeyProvider.ofBase64("k1", base64Key())
    );

    @AfterEach
    void tearDown() {
        EncryptedStringTypeHandler.clearDefaultFieldEncryptor();
    }

    @Test
    void shouldEncryptParameterBeforeWritingToPreparedStatement() throws Exception {
        EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler(encryptor);
        PreparedStatement statement = mock(PreparedStatement.class);

        handler.setNonNullParameter(statement, 1, "secret", null);

        verify(statement).setString(org.mockito.Mockito.eq(1), org.mockito.Mockito.startsWith("ENCv1:k1:"));
    }

    @Test
    void shouldDecryptResultSetValue() throws Exception {
        String ciphertext = encryptor.encrypt("secret");
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("phone")).thenReturn(ciphertext);
        EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler(encryptor);

        String value = handler.getNullableResult(resultSet, "phone");

        assertThat(value).isEqualTo("secret");
    }

    @Test
    void shouldUseStaticDefaultEncryptorForNoArgHandler() throws Exception {
        EncryptedStringTypeHandler.setDefaultFieldEncryptor(encryptor);
        PreparedStatement statement = mock(PreparedStatement.class);
        EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler();

        handler.setNonNullParameter(statement, 1, "secret", null);

        verify(statement).setString(org.mockito.Mockito.eq(1), anyString());
    }

    @Test
    void shouldOnlyClearDefaultEncryptorWhenOwnerTokenMatches() throws Exception {
        com.undernine.utils.security.crypto.FieldEncryptor firstEncryptor =
                mock(com.undernine.utils.security.crypto.FieldEncryptor.class);
        com.undernine.utils.security.crypto.FieldEncryptor secondEncryptor =
                mock(com.undernine.utils.security.crypto.FieldEncryptor.class);
        when(firstEncryptor.encrypt("secret")).thenReturn("first");
        when(secondEncryptor.encrypt("secret")).thenReturn("second");
        Object firstToken = EncryptedStringTypeHandler.registerDefaultFieldEncryptor(firstEncryptor);
        EncryptedStringTypeHandler.registerDefaultFieldEncryptor(secondEncryptor);
        PreparedStatement statement = mock(PreparedStatement.class);
        EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler();

        EncryptedStringTypeHandler.clearDefaultFieldEncryptor(firstToken);
        handler.setNonNullParameter(statement, 1, "secret", null);

        verify(firstEncryptor, never()).encrypt("secret");
        verify(secondEncryptor).encrypt("secret");
        verify(statement).setString(org.mockito.Mockito.eq(1), org.mockito.Mockito.eq("second"));
    }

    @Test
    void shouldFailWhenEncryptorIsNotConfigured() {
        EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler();

        assertThatThrownBy(() -> handler.setNonNullParameter(mock(PreparedStatement.class), 1, "secret", null))
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("FieldEncryptor is not configured for EncryptedStringTypeHandler");
    }

    private static String base64Key() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(key);
    }
}
