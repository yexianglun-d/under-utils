package com.undernine.utils.security.mybatis;

import com.undernine.utils.security.crypto.FieldEncryptionException;
import com.undernine.utils.security.crypto.FieldEncryptor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MyBatis 字符串字段加密 TypeHandler。
 * <p>
 * 该 handler 只在实体字段显式声明时生效，不做全局隐式加密。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    private static final AtomicReference<DefaultFieldEncryptorRegistration> DEFAULT_REGISTRATION =
            new AtomicReference<>();

    private final FieldEncryptor fieldEncryptor;

    public EncryptedStringTypeHandler() {
        this(null);
    }

    public EncryptedStringTypeHandler(FieldEncryptor fieldEncryptor) {
        this.fieldEncryptor = fieldEncryptor;
    }

    public static void setDefaultFieldEncryptor(FieldEncryptor fieldEncryptor) {
        DEFAULT_REGISTRATION.set(new DefaultFieldEncryptorRegistration(new Object(), fieldEncryptor));
    }

    public static Object registerDefaultFieldEncryptor(FieldEncryptor fieldEncryptor) {
        Object ownerToken = new Object();
        DEFAULT_REGISTRATION.set(new DefaultFieldEncryptorRegistration(ownerToken, fieldEncryptor));
        return ownerToken;
    }

    public static void clearDefaultFieldEncryptor() {
        DEFAULT_REGISTRATION.set(null);
    }

    public static void clearDefaultFieldEncryptor(Object ownerToken) {
        DefaultFieldEncryptorRegistration current = DEFAULT_REGISTRATION.get();
        if (current != null && current.ownerToken == ownerToken) {
            DEFAULT_REGISTRATION.compareAndSet(current, null);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, encryptor().encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    private String decrypt(String value) {
        if (value == null) {
            return null;
        }
        return encryptor().decrypt(value);
    }

    private FieldEncryptor encryptor() {
        DefaultFieldEncryptorRegistration registration = DEFAULT_REGISTRATION.get();
        FieldEncryptor actual = fieldEncryptor != null ? fieldEncryptor
                : registration == null ? null : registration.fieldEncryptor;
        if (actual == null) {
            throw new FieldEncryptionException("FieldEncryptor is not configured for EncryptedStringTypeHandler");
        }
        return actual;
    }

    private static final class DefaultFieldEncryptorRegistration {
        private final Object ownerToken;
        private final FieldEncryptor fieldEncryptor;

        private DefaultFieldEncryptorRegistration(Object ownerToken, FieldEncryptor fieldEncryptor) {
            this.ownerToken = ownerToken;
            this.fieldEncryptor = fieldEncryptor;
        }
    }
}
