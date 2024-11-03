package com.example;

import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class UserController {

    @FXML private TextField usernameInput;
    @FXML private PasswordField psswdInput;
    @FXML private PasswordField confpsswdInput;
    @FXML private DatePicker dobInput;
    @FXML private TextField emailInput;
    @FXML private TextField currAccInput;
    @FXML private TextField savAccInput;
    

    @FXML private Label username;
    @FXML private Label dob;
    @FXML private Label email;
    @FXML private Label checkAmnt;
    @FXML private Label savAmnt;
    
    @FXML private TextField currentDepoInput;
    @FXML private TextField savDepoInput;

    @FXML private ComboBox<String> accountComboBox;
    @FXML private TextField amountInput;
    @FXML private TextField receiver;

    @FXML private void handleMouseReleased(MouseEvent event) {
        String buttonID = ((Button) event.getSource()).getId();
        switch(buttonID) {
            case "CURRENTDEPO":
                if(currentDepoInput.getText().isEmpty()) {
                    displayError("Amount cannot be empty");
                } else {
                    client.SendMessage("DEPOSIT/CURRENT/" + currentDepoInput.getText() + "/", null);
                    String str[] = client.ReadFromSocket().split("/");
                    if(str[0].equals("DEPOSITED")) {
                        displaySuccess("DEPOSITED");
                        updateDashBoard(str[1], str[2]);
                    }
                }
                break;
            case "SAVINGDEPO":
                if(savDepoInput.getText().isEmpty()) {
                    displayError("Amount cannot be empty");
                } else {
                    client.SendMessage("DEPOSIT/SAVINGS/" + savDepoInput.getText() + "/", null);
                    String str[] = client.ReadFromSocket().split("/");

                    if(str[0].equals("DEPOSITED")) {
                        displaySuccess("DEPOSITED");
                        updateDashBoard(str[1], str[2]);
                    }
                }
                break;
            case "LOGIN":
                try {
                    client.SendMessage("LOGIN/" + LoginCreds(), null);
                    String str[] = client.ReadFromSocket().split("/");
                    if(str[0].equals("WELCOME")) {
                        displayDashBoard((Stage) ((Button) event.getSource()).getScene().getWindow(), str);

                    }
                } catch(EmptyFieldException e) {
                    displayError(e.getMessage());
                }
                break;
                
            case "REGISTERPAGE":
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("register.fxml"));
                    Parent homePageRoot = loader.load();
    
                    Scene homePageScene = new Scene(homePageRoot);
        
                    Stage currentStage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        
                    currentStage.setScene(homePageScene);
                    currentStage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "REGISTER":
                System.out.println(buttonID);
                try {
                    client.SendMessage("REGISTER/" + RegisterCreds(), null);
                    String str = client.ReadFromSocket();
                    if(str != null && str.equals("REGISTERED")) {
                        displayLoginPage((Stage) ((Button) event.getSource()).getScene().getWindow());
                    } else {
                        displayError(str);
                    }
                } catch(Exception e) {
                    displayError(e.getMessage());
                }
                break;
            
            case "TRANSFERPAGE":
                try {
                    Stage transferStage = new Stage();
                    transferStage.setTitle("Fund Transder");

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("transfer.fxml"));
                    Parent root = loader.load();

                    UserController u = loader.getController();
                    u.accountComboBox.setItems(FXCollections.observableArrayList("current", "savings"));
                    Scene scene = new Scene(root);

                    transferStage.setScene(scene);
                    transferStage.show();
                } catch(IOException e) {
                    e.printStackTrace();
                }
                break;
            
            case "TRANSFER":
            {
                String money = amountInput.getText();
                String acc = accountComboBox.getValue();
                String rec = receiver.getText();
                client.SendMessage("TRANSFER/", money + "/" + acc + "/" + rec + "/");
                String str = client.ReadFromSocket();
                if(str != null && str.equals("TRANSFERED")) {
                    displaySuccess(str);
                } else {
                    displayError("Cannot transfer");
                }
            }
                break;
        }
    }

    public void updateDashBoard(String place, String data) {
        switch(place) {
            case "CURRENT":
                Platform.runLater(() -> {
                    checkAmnt.setText(data);
                });
                break;
            case "SAVINGS":
                Platform.runLater(() -> {
                    savAmnt.setText(data);
                });
                break;
        }
    }

    public void displayDashBoard(Stage currentStage, String[] str) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
            Parent homePageRoot = loader.load();

            UserController c = loader.getController();
            c.username.setText(str[1]);
            c.dob.setText(str[2]);
            c.email.setText(str[3]);
            c.checkAmnt.setText(str[4]);
            c.savAmnt.setText(str[5]);
            // Create a new scene with the loaded FXML layout
            Scene homePageScene = new Scene(homePageRoot);

            Stage st = currentStage;
            st.setScene(homePageScene);
            st.show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void displayError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Womp Womp!!");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void displaySuccess(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void displayLoginPage(Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent homePageRoot = loader.load();

            // Create a new scene with the loaded FXML layout
            Scene homePageScene = new Scene(homePageRoot);

            Stage st = currentStage;
            st.setScene(homePageScene);
            st.show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    public String LoginCreds() throws EmptyFieldException{
        if(usernameInput.getText().isEmpty() || psswdInput.getText().isEmpty()) {
            throw new EmptyFieldException("Please fill in all fields to login");
        }
        String username = usernameInput.getText();
        String psswd = psswdInput.getText();
        System.out.println(username + '/' + psswd + '/');
        return username + '/' + psswd + '/';
    }

    public String RegisterCreds() throws EmptyFieldException, CannotRegisterException{
        if (usernameInput.getText().isEmpty() || psswdInput.getText().isEmpty() 
            || confpsswdInput.getText().isEmpty() || dobInput.getEditor().getText().isEmpty() 
            || emailInput.getText().isEmpty() || currAccInput.getText().isEmpty()
            || savAccInput.getText().isEmpty()) {
            throw new EmptyFieldException("Please fill in all fields to register");
        }
        
        String username = usernameInput.getText();
        String psswd = psswdInput.getText();
        String confPsswd = confpsswdInput.getText();
        String date = dobInput.getEditor().getText();
        String email = emailInput.getText();
        String cA = currAccInput.getText();
        String sA = savAccInput.getText();

        if(psswd.equals(confPsswd)) {
            return username + '/' + psswd + "/" + date.replaceAll("\\/", "\\-") + "/" + email + "/" + cA + "/" + sA + "/";
        } else {
            throw new CannotRegisterException("Passwords do not match");
        }
    }
}

class User {
    String username;
    String psswd;

    User(String username, String psswd) {
        this.username = username;
        this.psswd = psswd;
    }

    @Override
    public String toString() {
        return username + '/' + psswd + '/';
    }
}

class RegisterUser {
    String username;
    String psswd;
    String date;
    String email;
    String checkAmount;
    String savAmount;
    RegisterUser(String username, String psswd, String date, String email, String checkAmount, String savAmount) {
        this.username = username;
        this.psswd = psswd;
        this.date = date;
        this.email = email;
        this.checkAmount = checkAmount;
        this.savAmount = savAmount;
    }

    @Override
    public String toString() {
        return username + '/' + psswd + "/" + date.replaceAll("\\/", "\\-") + "/" + email + "/" + checkAmount + "/" + savAmount + "/";
    }
}
