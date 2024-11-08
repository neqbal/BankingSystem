package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class bankService {
    private static database db = new database();

    public static boolean QuerryRegister(String username, String psswd, String dob, String email, String currAmnt, String savAmnt) throws CannotRegisterException {
        System.out.println("Querying client");
        String query = "INSERT INTO users (id, username, password, dob, email, current, savings, auth) VALUES (null, ?, ?, ?, ?, ?, ?, 0)";
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = db.getConnection();
            pstmt = con.prepareStatement(query);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        if(findReceiverUser(username)) {
            throw new CannotRegisterException("User name not available");
        } else {
            try {
                pstmt.setString(1, username);
                pstmt.setString(2, psswd);
                pstmt.setString(3, dob);
                pstmt.setString(4, email);
                pstmt.setString(5, currAmnt);
                pstmt.setString(6, savAmnt);

                pstmt.executeUpdate();
                System.out.println("User registered");
                con.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        } 
        return true;
    } 

    public static Integer QuerryLogin(String username, String psswd) throws CannotLoginException {
        if(!findReceiverUser(username)) throw new CannotLoginException("User does not exist");
        String query = "SELECT * FROM users WHERE username = (?)";
        Connection con = null;
        String password = null;
        int id = 0, auth = 0;

        try {
            con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet res = pstmt.executeQuery();
            
            if(res.next()) {
                password = res.getString("password");
                id = res.getInt("id");
                auth = res.getInt("auth");
                con.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
            return 0;
        }

        if(auth == 1) {
            throw new CannotLoginException("User already logged in");
        }

        if(password.equals(psswd)) {
            AuthenticateUser(id);    
            return id;
        } else {
            throw new CannotLoginException("Username or password incorrect");
        }
    }

    public static boolean AuthenticateUser(int id) {
        boolean flag = false;
        try {
            String query = "UPDATE users SET auth = 1 WHERE id = (?)";
            Connection con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, id);

            if (pstmt.executeUpdate() > 0) {
                flag = true;
            }
            con.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean DeAuthenticateUser(int id) {
        System.out.println("Deauthenticating user");
        try {
            String query = "UPDATE users SET auth = 0 WHERE id = (?)";
            Connection con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, id);

            if (pstmt.executeUpdate() > 0) {
                con.close();
                return true;
            }
            con.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String QuerryClientInfo(int id) {
        try {
            String query = "SELECT username, dob, email, current, savings FROM users WHERE id = (?)";
            Connection con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, id);
            String str = null;
            ResultSet res = pstmt.executeQuery();
            if(res.next()) {
                str = res.getString("username") + "/" + res.getString("dob") + "/" + res.getString("email") + "/" + res.getInt("current") + "/" + res.getInt("savings") + "/";
            }
            con.close();
            return str;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "/";
    }

    public static Integer QuerryDeposit(int id, String acc, int amnt) throws CannotDepositException {
        String senderQuery = "SELECT " + acc + " FROM users WHERE id = (?)";
        String updateQuery = "UPDATE users SET " + acc + " = (?) WHERE id = (?)";
        Connection con = null;
        int balance = 0;

        try {
            con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(senderQuery);
            pstmt.setInt(1, id);
            ResultSet res = pstmt.executeQuery();
            if(res.next()) {
                balance = res.getInt(1);
            }

            PreparedStatement pstmt2 = con.prepareStatement(updateQuery);
            pstmt2.setInt(1, balance + amnt);
            pstmt2.setInt(2, id);
            pstmt2.executeUpdate();
            con.close();
        } catch(Exception e) {
            e.printStackTrace();
            throw new CannotDepositException("Cannot deposit");
        }
        return balance + amnt;
    }

    public static Integer QuerryWithdraw(int id, String acc, int amnt) throws CannotWithdrawException {
        String balanceQuery = "SELECT " + acc + " FROM users WHERE id = (?)";
        String updateQuery = "UPDATE users SET " + acc + " = (?) WHERE id = (?)";
        Connection con = null;
        int balance = 0;

        try {
            con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(balanceQuery);
            pstmt.setInt(1, id);
            ResultSet res = pstmt.executeQuery();
            if(res.next()) {
                balance = res.getInt(1);
            }

            if (balance < amnt) {
                throw new CannotWithdrawException("Insufficient funds for withdrawal");
            }

            PreparedStatement pstmt2 = con.prepareStatement(updateQuery);
            pstmt2.setInt(1, balance - amnt);
            pstmt2.setInt(2, id);
            pstmt2.executeUpdate();
            con.close();
        } catch(Exception e) {
            e.printStackTrace();
            throw new CannotWithdrawException("Cannot withdraw funds");
        }
        return balance - amnt;
    }

    public static boolean findReceiverUser(String username) {
        String query = "SELECT username FROM users WHERE username= (?)";
        Connection con = null;
        boolean flag = false;
        try {
            con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                flag = true;
            }
            con.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean TransferQuerry(int id, int money, String acc, int receiver) throws CannotTransferException, NoAccountException {
        String receiverExists = "SELECT current FROM users WHERE id = ?";
        String checkSenderBalance = "SELECT " + acc + " FROM users WHERE id = ?";
        String transfer = "BEGIN; " +
                        "UPDATE users SET " + acc + " = " + acc + " - " + money + " WHERE id = "+id+"; " +
                        "UPDATE users SET current = current + "+money+" WHERE id = "+receiver+"; " +
                        "COMMIT;";
        
        Connection con = null;
        try {
            con = db.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        PreparedStatement pstmt = null;
        boolean flag1 = true;
        boolean flag2 = true;
        
        try {
            pstmt = con.prepareStatement(receiverExists);
            pstmt.setInt(1, receiver);
            ResultSet res = pstmt.executeQuery();
            if (!res.next()) flag1 = false;

            pstmt = con.prepareStatement(checkSenderBalance);
            pstmt.setInt(1, id);
            res = pstmt.executeQuery();
            if (res.next() && res.getInt(1) < money) flag2 = false;

            con.close();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        if(!flag1) throw new NoAccountException("Account does not exist");
        if(!flag2) throw new CannotTransferException("Not enough funds");

        try {
            con = db.getConnection();
            Statement st = con.createStatement();
            st.executeUpdate(transfer);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
