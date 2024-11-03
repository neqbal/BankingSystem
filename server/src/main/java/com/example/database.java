package com.example;

import java.sql.*;

public class database {
    public void initDB() {
        Connection c = null;
        Statement stmt = null;
        try {
            c = DriverManager.getConnection("jdbc:sqlite:./src/main/resources/com/example/database.db");
            System.out.println("Opened databse succesfully");

            stmt = c.createStatement();
            String users = "CREATE TABLE users" +
                    "(id                   INTEGER PRIMARY KEY AUTOINCREMENT," + 
                    "username              TEXT                NOT NULL," +
                    "password              TEXT                NOT NULL," +
                    "dob                   TEXT                NOT NULL," +
                    "email                 TEXT                NOT NULL," +
                    "current               INTEGER             NOT NULL," +
                    "savings               INTEGER             NOT NULL," +
                    "auth                  INTEGER             NOT NULL)";

            String transactions = "CREATE TABLE transactions" +
                                "(id                   INTEGER PRIMARY KEY AUTOINCREMENT," + 
                                "sender                TEXT                NOT NULL," +
                                "receiver              TEXT                NOT NULL," +
                                "amount                INTEGER             NOT NULL)";
            stmt.executeUpdate(users);
            stmt.executeUpdate(transactions);
            stmt.close();
            c.close();
            System.out.println("Table created succesfully");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

    }

    @SuppressWarnings("exports")
    public Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:./src/main/resources/com/example/database.db");
    } 
}
