package com.example;

public class App {
    public static void main(String args[]) {
        new database().initDB();
        new Server().crerateServer();
    }
}
