package com.papaya.claims.claim.domain.persistence;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/** Explicit PostgreSQL UUID mapping for MyBatis versions without a built-in UUID handler. */
@MappedTypes(UUID.class)
@MappedJdbcTypes(JdbcType.OTHER)
public final class UuidTypeHandler extends BaseTypeHandler<UUID> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, UUID value, JdbcType jdbcType)
            throws SQLException {
        statement.setObject(index, value);
    }

    @Override
    public UUID getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return uuid(resultSet.getObject(columnName));
    }

    @Override
    public UUID getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return uuid(resultSet.getObject(columnIndex));
    }

    @Override
    public UUID getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return uuid(statement.getObject(columnIndex));
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID id) return id;
        return UUID.fromString(value.toString());
    }
}
