//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;

/**
 * Helper class for working with SQL databases through JDBC. This class provides
 * several convenience methods for performing common operations. It is intended
 * to be used when performing ad-hoc database queries, not to replace situations
 * in which a full ORM framework would normally be used.
 */
public final class DatabaseHelper {
    
    public static final CharMatcher SAFE_NAMES = Escape.CHARACTER.or(CharMatcher.anyOf("_:"));
    
    private DatabaseHelper() {
    }
    
    /**
     * Opens a JDBC connection to a MySQL or MariaDB database server, without 
     * selecting a specific database.
     */
    public static Connection openConnection(String host, String user, String password)
            throws SQLException {
        return openConnection(host, null, user, password);
    }
    
    /**
     * Opens a JDBC connection to a MySQL or MariaDB database server, selecting
     * the database with the specified name.
     */
    public static Connection openConnection(String host, String databaseName, String user, 
            String password) throws SQLException {
        String connectionString = "jdbc:mysql://" + SAFE_NAMES.retainFrom(host);
        if (databaseName != null) {
            connectionString += "/" + SAFE_NAMES.retainFrom(databaseName);
        }
        
        initDatabaseDriver();
        
        return DriverManager.getConnection(connectionString, user, password);
    }
    
    private static void initDatabaseDriver() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (Exception e) {
            // By default the MariaDB driver is used. However, this is not
            // required.
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (Exception e2) {
                // If none of the attempted drivers is present on the
                // classpath whatever drivers are provided by the
                // platform are used instead.
            }
        }
    }

    /**
     * Creates a database with the specified name. If a database with that name
     * already exists this method does nothing.
     */
    public static void createDatabase(Connection connection, String name) throws SQLException {
        doUpdateQuery(connection, "CREATE DATABASE IF NOT EXISTS " + SAFE_NAMES.retainFrom(name));
    }
    
    /**
     * Drops the current database.
     */
    public static void dropDatabase(Connection connection, String name) throws SQLException {
        doUpdateQuery(connection, "DROP DATABASE " + SAFE_NAMES.retainFrom(name));
    }
    
    /**
     * Returns a list containing the names of all databases on the database
     * server.
     */
    public static List<String> showDatabases(Connection connection) throws SQLException {
        List<String> databaseNames = new ArrayList<>();
        for (Map<String, Object> row : query(connection, "SHOW DATABASES")) {
            databaseNames.add((String) row.get(row.keySet().iterator().next()));
        }
        return databaseNames;
    }
    
    /**
     * Returns a list containing the names of all tables in the current database.
     */
    public static List<String> showTables(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        for (Map<String, Object> row : query(connection, "SHOW TABLES")) {
            tableNames.add((String) row.get(row.keySet().iterator().next()));
        }
        return tableNames;
    }
    
    /**
     * Returns a list containing the names of all columns within the table with
     * the specified name.
     */
    public static List<String> showColumnNames(Connection connection, String table) throws SQLException {
        String sql = "SHOW COLUMNS FROM " + SAFE_NAMES.retainFrom(table);
        List<String> tableNames = new ArrayList<>();
        for (Map<String, Object> row : query(connection, sql)) {
            tableNames.add((String) row.get("Field"));
        }
        return tableNames;
    }
    
    /**
     * Returns a list containing the names of all indexes that exist for the
     * table with the specified name.
     */
    public static List<String> showIndexes(Connection connection, String table) throws SQLException {
        String sql = "SHOW INDEX FROM " + SAFE_NAMES.retainFrom(table);
        List<String> indexNames = new ArrayList<>();
        for (Map<String, Object> row : query(connection, sql)) {
            indexNames.add((String) row.get("Key_name"));
        }
        return indexNames;
    }
    
    /**
     * Performs a query that alters the contents or structure of the database
     * (e.g. INSERT, UPDATE, DELETE). The query is sent to the database as a 
     * prepared statement.
     */
    public static void doUpdateQuery(Connection connection, String sql, List<Object> params)
            throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        }
    }
    
    /**
     * Performs a query that alters the contents or structure of the database
     * (e.g. INSERT, UPDATE, DELETE). The query is sent to the database as a 
     * prepared statement.
     */
    public static void doUpdateQuery(Connection connection, String sql, Object... params)
            throws SQLException {
        doUpdateQuery(connection, sql, ImmutableList.copyOf(params));
    }
    
    /**
     * Performs a SELECT query and returns its results. The query is sent to the 
     * database as a prepared statement.
     */
    public static List<Map<String, Object>> query(Connection connection, String sql, 
            List<Object> params) throws SQLException {
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet results = stmt.executeQuery();
            mappedResults = mapResults(results);
        }
        return mappedResults;
    }
    
    /**
     * Performs a SELECT query and returns its results. The query is sent to the 
     * database as a prepared statement.
     */
    public static List<Map<String, Object>> query(Connection connection, String sql, 
            Object... params) throws SQLException {
        return query(connection, sql, ImmutableList.copyOf(params));
    }

    private static List<Map<String, Object>> mapResults(ResultSet results) throws SQLException {
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        ResultSetMetaData metadata = results.getMetaData();
        while (results.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                String column = metadata.getColumnName(i);
                row.put(column, results.getObject(column));
            }
            mappedResults.add(row);
        }
        return mappedResults;
    }
    
    /**
     * Performs a SELECT query and returns its first result. If the query did not
     * produced any results this returns {@code null}. The query is sent to the 
     * database as a prepared statement.
     */
    public static Map<String, Object> queryFirst(Connection connection, String sql, 
            List<Object> params) throws SQLException {
        List<Map<String, Object>> results = query(connection, sql, params);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }
    
    /**
     * Performs a SELECT query and returns its first result. If the query did not
     * produced any results this returns {@code null}. The query is sent to the 
     * database as a prepared statement.
     */
    public static Map<String, Object> queryFirst(Connection connection, String sql, 
            Object... params) throws SQLException {
        return queryFirst(connection, sql, ImmutableList.copyOf(params));
    }
}
