package com.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    // If you're using SQL Server Express, you might need \\SQLEXPRESS (see note below)
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;" +
                    "databaseName=Flight_Sys;" +
                    "encrypt=true;" +
                    "trustServerCertificate=true;" +
                    "loginTimeout=5;";

    private static final String USER = "flightuser";
    private static final String PASS = "Flight123!";

    public static Connection getConnection() throws SQLException {
        try {
            // Optional with modern JDBC, but harmless and sometimes fixes "no driver" issues
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ignored) { }

        return DriverManager.getConnection(URL, USER, PASS);
    }
}