package ru.runa.wfe.commons.dbmigration;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.springframework.beans.factory.annotation.Autowired;
import ru.runa.wfe.commons.ApplicationContextFactory;
import ru.runa.wfe.commons.DbType;

/**
 * Base class for database migration (which are applied at startup).
 * 
 * @author Dofs
 * @see DbMigrationManager
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public abstract class DbMigration {
    protected final Log log = LogFactory.getLog(getClass());
    protected final Dialect dialect = ApplicationContextFactory.getDialect();
    protected final DbType dbType = ApplicationContextFactory.getDbType();

    @Autowired
    protected SessionFactory sessionFactory;

    public void execute() throws Exception {
        val session = sessionFactory.getCurrentSession();
        executeDDL(session, "[DDLBefore]", getDDLQueriesBefore());
        session.setCacheMode(CacheMode.IGNORE);
        executeDML(session);
        session.flush();
        executeDDL(session, "[DDLAfter]", getDDLQueriesAfter());
    }

    protected List<String> getDDLQueriesBefore() {
        return Lists.newArrayList();
    }

    /**
     * Execute migration's DML statements in one transaction.
     * 
     * It's allowed to use only raw SQL because hibernate mappings could not work in old DB version.
     * 
     * @deprecated Use pure JDBC, by overriding {@link #executeDML(Connection)}.
     */
    @Deprecated
    public void executeDML(Session session) throws Exception {
        executeDML(session.connection());
    }

    public void executeDML(Connection conn) throws Exception {
    }

    protected List<String> getDDLQueriesAfter() {
        return Lists.newArrayList();
    }

    /**
     * Helper for subclasses.
     * <p>
     * ImmutableList.of() requires all list items to be non-null, but getDDLCreateSequence() may return null.
     * Arrays.asList() does not support add() and addAll() operations.
     */
    protected final <T> ArrayList<T> list(T... oo) {
        val result = new ArrayList<T>(oo.length);
        for (T o : oo) {
            result.add(o);
        }
        return result;
    }

    /**
     * Helper for subclasses.
     *
     * @return Result of last update.
     */
    protected final int executeUpdates(Connection conn, String... queries) throws Exception {
        try (val stmt = conn.createStatement()) {
            int result = 0;
            for (val q : queries) {
                if (!StringUtils.isBlank(q)) {
                    result = stmt.executeUpdate(q);
                }
            }
            return result;
        }
    }

    private void executeDDL(Session session, String category, List<String> queries) throws Exception {
        try (val stmt = session.connection().createStatement()) {
            for (val query : queries) {
                if (!StringUtils.isBlank(query)) {
                    log.info(category + ": " + query);
                    stmt.executeUpdate(query);
                }
            }
        }
    }

    private static void checkIndentifierLength(String id) {
        if (id != null && id.length() > 30) {
            throw new RuntimeException("Identifier \"" + id + "\".length " + id.length() + " > 30 (Oracle restriction)");
        }
    }

    protected final String getDDLCreateSequence(String sequenceName) {
        checkIndentifierLength(sequenceName);
        switch (dbType) {
            case ORACLE:
            case POSTGRESQL:
                return "create sequence " + sequenceName;
            default:
                return null;
        }
    }

    protected final String getDDLCreateSequence(String sequenceName, long nextValue) {
        checkIndentifierLength(sequenceName);
        switch (dbType) {
            case ORACLE:
            case POSTGRESQL:
                return "create sequence " + sequenceName + " start with " + nextValue;
            default:
                return null;
        }
    }

    protected final String getDDLDropSequence(String sequenceName) {
        switch (dbType) {
            case ORACLE:
            case POSTGRESQL:
                return "drop sequence " + sequenceName;
            default:
                return null;
        }
    }

    protected final String getDDLRenameSequence(String sequenceName, String newName) {
        checkIndentifierLength(newName);
        switch (dbType) {
            case ORACLE:
                return "rename " + sequenceName + " to " + newName;
            case POSTGRESQL:
                return "alter sequence " + sequenceName + " rename to " + newName;
            default:
                return null;
        }
    }

    /**
     * @deprecated Don't pass "unique" parameter, create named unique constraint instead.
     *
     * @see #getDDLCreateTable(String, List)
     * @see #getDDLCreateUniqueKey(String, String, String...)
     */
    @Deprecated
    protected final String getDDLCreateTable(String tableName, List<ColumnDef> columnDefinitions, String unique) {
        // See TODO below.
        checkIndentifierLength("pk_" + tableName);

        val query = new StringBuilder("CREATE TABLE " + tableName + " (");
        for (ColumnDef columnDef : columnDefinitions) {
            if (columnDefinitions.indexOf(columnDef) > 0) {
                query.append(", ");
            }
            query.append(columnDef.name).append(" ").append(columnDef.getSqlTypeName(dialect));
            if (columnDef.primaryKey) {
                // TODO Different SQL servers will generate different PK constraint name.
                //      Instead, should generate "pk_table_name" in separate statement (see getDDLCreatePrimaryKey())
                //      and warn+trim or fail if checkIdentifierLength("pk_table_name") fails.
                String primaryKeyModifier;
                switch (dbType) {
                case HSQL:
                case MSSQL:
                    primaryKeyModifier = "IDENTITY NOT NULL PRIMARY KEY";
                    break;
                case ORACLE:
                    primaryKeyModifier = "NOT NULL PRIMARY KEY";
                    break;
                case POSTGRESQL:
                    primaryKeyModifier = "PRIMARY KEY";
                    break;
                case MYSQL:
                    primaryKeyModifier = "NOT NULL PRIMARY KEY AUTO_INCREMENT";
                    break;
                case H2:
                    primaryKeyModifier = "GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY";
                    break;
                default:
                    primaryKeyModifier = "PRIMARY KEY";
                    break;
                }
                query.append(" ").append(primaryKeyModifier);
                continue;
            }
            if (!columnDef.allowNulls) {
                query.append(" NOT NULL");
            }
        }
        if (unique != null) {
            query.append(", UNIQUE ").append(unique);
        }
        query.append(")");
        return query.toString();
    }


    protected final String getDDLCreateTable(String tableName, List<ColumnDef> columnDefinitions) {
        return getDDLCreateTable(tableName, columnDefinitions, null);
    }


    protected final String getDDLRenameTable(String oldTableName, String newTableName) {
        checkIndentifierLength(newTableName);
        switch (dbType) {
            case MSSQL:
                return "sp_rename '" + oldTableName + "', '" + newTableName + "'";
            case MYSQL:
                return "RENAME TABLE " + oldTableName + " TO " + newTableName;
            default:
                return "ALTER TABLE " + oldTableName + " RENAME TO " + newTableName;
        }
    }

    protected final String getDDLDropTable(String tableName) {
        return "DROP TABLE " + tableName;
    }

    protected final String getDDLCreateIndex(String tableName, String indexName, String... columnNames) {
        checkIndentifierLength(indexName);
        for (val cn : columnNames) {
            checkIndentifierLength(cn);
        }
        String conjunctedColumnNames = Joiner.on(", ").join(columnNames);
        return "CREATE INDEX " + indexName + " ON " + tableName + " (" + conjunctedColumnNames + ")";
    }

    protected final String getDDLCreateUniqueKey(String tableName, String constraintName, String... columnNames) {
        checkIndentifierLength(constraintName);
        for (val cn : columnNames) {
            checkIndentifierLength(cn);
        }
        String conjunctedColumnNames = Joiner.on(", ").join(columnNames);
        return "ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName + " UNIQUE (" + conjunctedColumnNames + ")";
    }

    protected final String getDDLRenameIndex(String tableName, String indexName, String newIndexName) {
        checkIndentifierLength(newIndexName);
        switch (dbType) {
            case MSSQL:
                return "sp_rename '" + tableName + "." + indexName + "', '" + newIndexName + "'";
            case H2:
            case ORACLE:
            case POSTGRESQL:
                return "alter index " + indexName + " rename to " + newIndexName;
            default:
                throw new NotImplementedException();  // TODO ...
        }
    }

    protected final String getDDLDropIndex(String tableName, String indexName) {
        switch (dbType) {
            case H2:
            case ORACLE:
            case POSTGRESQL:
                return "DROP INDEX " + indexName;
            default:
                return "DROP INDEX " + indexName + " ON " + tableName;
        }
    }

    protected final String getDDLCreateForeignKey(String tableName, String keyName, String columnName, String refTableName, String refColumnName) {
        checkIndentifierLength(keyName);
        return "ALTER TABLE " + tableName + " ADD CONSTRAINT " + keyName + " FOREIGN KEY (" + columnName + ") REFERENCES " + refTableName + " ("
                + refColumnName + ")";
    }

    protected final String getDDLCreatePrimaryKey(String tableName, String keyName, String columnName) {
        checkIndentifierLength(keyName);
        return "ALTER TABLE " + tableName + " ADD CONSTRAINT " + keyName + " PRIMARY KEY (" + columnName + ")";
    }

    protected final String getDDLRenameForeignKey(String keyName, String newKeyName) {
        checkIndentifierLength(newKeyName);
        switch (dbType) {
            case MSSQL:
                return "sp_rename '" + keyName + "', '" + newKeyName + "'";
            default:
                throw new NotImplementedException();  // TODO ...
        }
    }

    protected final String getDDLDropForeignKey(String tableName, String keyName) {
        String constraint;
        switch (dbType) {
            case MYSQL:
                constraint = "FOREIGN KEY";
                break;
            default:
                constraint = "CONSTRAINT";
                break;
        }
        return "ALTER TABLE " + tableName + " DROP " + constraint + " " + keyName;
    }

    protected final String getDDLCreateColumn(String tableName, ColumnDef columnDef) {
        String lBraced = "";
        String rBraced = "";
        if (dbType == DbType.ORACLE) {
            lBraced = "(";
            rBraced = ")";
        }
        String query = "ALTER TABLE " + tableName + " ADD " + lBraced;
        query += columnDef.name + " " + columnDef.getSqlTypeName(dialect);
        if (columnDef.defaultValue != null) {
            query += " DEFAULT " + columnDef.defaultValue;
        }
        if (!columnDef.allowNulls) {
            query += " NOT NULL";
        }
        query += rBraced;
        return query;
    }

    protected final String getDDLRenameColumn(String tableName, String oldColumnName, ColumnDef newColumnDef) {
        switch (dbType) {
            case ORACLE:
            case POSTGRESQL:
                return "ALTER TABLE " + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnDef.name;
            case MSSQL:
                return "sp_rename '" + tableName + "." + oldColumnName + "', '" + newColumnDef.name + "', 'COLUMN'";
            case MYSQL:
                return "ALTER TABLE " + tableName + " CHANGE " + oldColumnName + " " + newColumnDef.name + " " + newColumnDef.getSqlTypeName(dialect);
            default:
                return "ALTER TABLE " + tableName + " ALTER COLUMN " + oldColumnName + " RENAME TO " + newColumnDef.name;
        }
    }

    protected final String getDDLModifyColumn(String tableName, String columnName, String sqlTypeName) {
        switch (dbType) {
            case ORACLE:
                return "ALTER TABLE " + tableName + " MODIFY(" + columnName + " " + sqlTypeName + ")";
            case POSTGRESQL:
                return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " TYPE " + sqlTypeName;
            case MYSQL:
                return "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + sqlTypeName;
            default:
                return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + sqlTypeName;
        }
    }

    protected final String getDDLModifyColumnNullability(String tableName, String columnName, String currentSqlTypeName,
            @SuppressWarnings("SameParameterValue") boolean nullable) {
        switch (dbType) {
            case H2:
            case HSQL:
                return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " SET " + (nullable ? "NULL" : "NOT NULL");
            case MYSQL:
                return "ALTER TABLE " + tableName + " MODIFY " + columnName + " " + currentSqlTypeName + " " + (nullable ? "NULL" : "NOT NULL");
            case ORACLE:
                return "ALTER TABLE " + tableName + " MODIFY(" + columnName + " " + (nullable ? "NULL" : "NOT NULL") + ")";
            case POSTGRESQL:
                return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + (nullable ? "DROP" : "SET") + " NOT NULL";
            default:
                return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + currentSqlTypeName + " " + (nullable ? "NULL" : "NOT NULL");
        }
    }

    protected final String getDDLDropColumn(String tableName, String columnName) {
        return "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
    }

    protected final String getDDLTruncateTable(String tableName) {
        return "TRUNCATE TABLE " + tableName;
    }

    protected final String getDDLTruncateTableUsingDelete(@SuppressWarnings("SameParameterValue") String tableName) {
        return "DELETE FROM " + tableName;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public static class ColumnDef {
        private boolean primaryKey;
        private final String name;
        private int sqlType;
        private String sqlTypeName;
        private final boolean allowNulls;
        private String defaultValue;

        /**
         * @deprecated Use shortcut subclasses (BigintColumnDef, etc.); create missing subclasses. Finally, make this constructor protected.
         */
        @Deprecated
        public ColumnDef(String name, int sqlType, boolean allowNulls) {
            checkIndentifierLength(name);
            this.name = name;
            this.sqlType = sqlType;
            this.allowNulls = allowNulls;
        }

        /**
         * @deprecated Use shortcut subclasses; create missing subclasses. Finally, make this constructor protected.
         */
        @Deprecated
        public ColumnDef(String name, String sqlTypeName, boolean allowNulls) {
            checkIndentifierLength(name);
            this.name = name;
            this.sqlTypeName = sqlTypeName;
            this.allowNulls = allowNulls;
        }

        /**
         * Creates column def which allows null values.
         *
         * @deprecated Use shortcut subclasses; create missing subclasses. Finally, delete this constructor.
         */
        @Deprecated
        public ColumnDef(String name, int sqlType) {
            this(name, sqlType, true);
        }

        /**
         * Creates column def which allows null values.
         *
         * @deprecated Use shortcut subclasses; create missing subclasses. Finally, delete this constructor.
         */
        @Deprecated
        public ColumnDef(String name, String sqlTypeName) {
            this(name, sqlTypeName, true);
        }

        public String getSqlTypeName(Dialect dialect) {
            if (sqlTypeName != null) {
                return sqlTypeName;
            }
            return dialect.getTypeName(sqlType);
        }

        public ColumnDef setPrimaryKey() {
            primaryKey = true;
            return this;
        }

        public ColumnDef setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
    }

    @SuppressWarnings({"WeakerAccess", "deprecation"})
    public class BigintColumnDef extends ColumnDef {
        public BigintColumnDef(String name, boolean allowNulls) {
            super(name, Types.BIGINT, allowNulls);
        }
        public BigintColumnDef(String name) {
            this(name, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class BlobColumnDef extends ColumnDef {
        public BlobColumnDef(String name, boolean allowNulls) {
            super(name, dialect.getTypeName(Types.BLOB), allowNulls);
        }
        public BlobColumnDef(String name) {
            this(name, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class BooleanColumnDef extends ColumnDef {
        public BooleanColumnDef(String name, boolean allowNulls) {
            super(name, dialect.getTypeName(Types.BIT), allowNulls);
        }
        public BooleanColumnDef(String name) {
            this(name, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class CharColumnDef extends ColumnDef {
        public CharColumnDef(String name, int length, boolean allowNulls) {
            super(name, dialect.getTypeName(Types.CHAR, length, length, length), allowNulls);
        }
        public CharColumnDef(String name, int length) {
            this(name, length, true);
        }
    }

    /**
     * @deprecated Use TimestampColumnDef: I believe it's effectively the same but more clear.
     */
    @Deprecated
    @SuppressWarnings({"unused", "WeakerAccess"})
    public class DateColumnDef extends ColumnDef {
        public DateColumnDef(String name, boolean allowNulls) {
            super(name, dialect.getTypeName(Types.DATE), allowNulls);
        }
        public DateColumnDef(String name) {
            this(name, true);
        }
    }


    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class DoubleColumnDef extends ColumnDef {
        public DoubleColumnDef(String name, boolean allowNulls) {
            super(name, dialect.getTypeName(Types.DOUBLE), allowNulls);
        }
        public DoubleColumnDef(String name) {
            this(name, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class IntColumnDef extends ColumnDef {
        public IntColumnDef(String name, boolean allowNulls) {
            super(name, Types.INTEGER, allowNulls);
        }
        public IntColumnDef(String name) {
            this(name, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class TimestampColumnDef extends ColumnDef {
        public TimestampColumnDef(String name, boolean allowNulls) {
            super(name, Types.TIMESTAMP, allowNulls);
        }
        public TimestampColumnDef(String name) {
            this(name, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "deprecation"})
    public class VarcharColumnDef extends ColumnDef {
        public VarcharColumnDef(String name, int length, boolean allowNulls) {
            super(name, dialect.getTypeName(Types.VARCHAR, length, length, length), allowNulls);
        }
        public VarcharColumnDef(String name, int length) {
            this(name, length, true);
        }
    }
}