package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class client extends Application{
    static BufferedReader in;
    static PrintWriter out;
    static UserInfo ui = new UserInfo();
    Socket s;
    private static Scene scene;
    private static Object lock = new Object();
    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("login"));
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(client.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        new client().connectToServer();
    }

    public void connectToServer() {
        try {
            s = new Socket("localhost", 9999);
            out = new PrintWriter(s.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String str = ReadFromSocket();
            if(str != null && str.equals("Connected")) {
                launch();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static String ReadFromSocket() {
        synchronized(lock) {
            String request = null;
            try {
                System.out.println("Reading from socket");
                request = in.readLine();
                System.out.println(request);
            } catch(IOException e) {
                e.printStackTrace();
            }
            return request;
        }
    }

    public static void SendMessage(String command, Object o) {
        System.out.println("Sending request");
        if(o != null) {
            out.println(command + o.toString());
            System.out.println(command + o.toString());
        }
        else { 
            out.println(command);
            System.out.println(command);
        }
        
        
        System.out.println("Request sent");
    }

}

class UserInfo {
    String username;
    String userID;
    UserInfo(){}
    UserInfo(String username, String userID) {
        this.userID = userID;
        this.username = username;
    }

    public void setUserName(String name) {
        this.username = name;
    }

    public void setUserId(String id) {
        this.userID = id;
    }
}