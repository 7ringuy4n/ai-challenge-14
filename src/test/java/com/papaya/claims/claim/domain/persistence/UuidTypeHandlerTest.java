package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class UuidTypeHandlerTest {
    private final UuidTypeHandler handler = new UuidTypeHandler();

    @Test
    void bindsUuidAndReadsUuidOrDatabaseTextAcrossAllSupportedResultShapes() throws Exception {
        UUID id = UUID.randomUUID();
        Object[] bound = new Object[1];
        PreparedStatement prepared = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setObject")) bound[0] = args[1];
                    return defaultValue(method.getReturnType());
                });
        handler.setNonNullParameter(prepared, 2, id, JdbcType.OTHER);
        assertThat(bound[0]).isEqualTo(id);

        assertThat(handler.getNullableResult(resultSet(id), "claim_id")).isEqualTo(id);
        assertThat(handler.getNullableResult(resultSet(id.toString()), 3)).isEqualTo(id);
        assertThat(handler.getNullableResult(callable(id.toString()), 4)).isEqualTo(id);
        assertThat(handler.getNullableResult(resultSet(null), "claim_id")).isNull();
    }

    @Test
    void malformedDatabaseUuidIsRejectedAndSqlExceptionsArePropagated() throws Exception {
        assertThatThrownBy(() -> handler.getNullableResult(resultSet("not-a-uuid"), 1))
                .isInstanceOf(IllegalArgumentException.class);
        ResultSet failing = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getObject")) throw new java.sql.SQLException("read failed");
                    return defaultValue(method.getReturnType());
                });
        assertThatThrownBy(() -> handler.getNullableResult(failing, "id"))
                .isInstanceOf(java.sql.SQLException.class).hasMessage("read failed");
    }

    private static ResultSet resultSet(Object value) {
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? value : defaultValue(method.getReturnType()));
    }

    private static CallableStatement callable(Object value) {
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                new Class<?>[]{CallableStatement.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? value : defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        return 0d;
    }
}
