package com.example.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbTest {
    public static void main(String[] args) {
        try (Connection c = Database.getConnection()) {
            System.out.println("CONNECTED to database: " + c.getCatalog());

            // Optional: prove it can read tables
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM sys.tables ORDER BY name")) {
                while (rs.next()) {
                    System.out.println(rs.getString(1));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}