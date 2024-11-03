package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class bankService {
    private static database db = new database();

    public static boolean QuerryRegister(String username, String psswd, String dob, String email, String currAmnt, String savAmnt) throws CannotRegisterException {
        System.out.println("Querrying client");
        String querry = "INSERT INTO users (id, username, password, dob, email, current, savings, auth) VALUES (null, ?, ?, ?, ?, ?, ?, 0)";
        Connection con = null;
        PreparedStatement pstmt=null;
        try {
            con = db.getConnection();
            pstmt = con.prepareStatement(querry);
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

    public static Integer QuerryLogin(String username, String psswd) throws CannotLoginException{
        String querry = "SELECT * FROM users WHERE username = (?)";
        Connection con = null;
        String password = null;
        int id = 0, auth = 0;

        try {
            con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(querry);

            pstmt.setString(1, username);
            ResultSet res = pstmt.executeQuery();
            
            if(res.next()) {
                password =  res.getString("password");
                id = Integer.parseInt(res.getString("id"));
                auth = res.getInt("auth");
                con.close();
            } else {

            }
        } catch(Exception e) {
            e.printStackTrace();
            return 0;
        }

    
        if(auth == 1) {
            System.out.println("user already logged in ");
            throw new CannotLoginException("user already logged in");
        }

        System.out.println("checking password " + password + " jhsd " + psswd);
        
        if(password.equals(psswd)) {
            System.out.println("password correct");
            AuthenticateUser(id);    
            return id;
        } else {
            throw new CannotLoginException("Username or password incorrect");
        }
    }

    public static boolean AuthenticateUser(int id) {
        boolean flag = false;
        try {
            String querry = "UPDATE users SET auth = 1 WHERE id = (?)";
            Connection con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(querry);
            pstmt.setString(1, Integer.toString(id));

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
        System.out.println("deauthenticating user");
        try {
            String querry = "UPDATE users SET auth = 0 WHERE id = (?)";
            Connection con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(querry);
            pstmt.setString(1, Integer.toString(id));

            if (pstmt.executeUpdate() > 0) {
                con.close();
                return true;
            } else {
                con.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String QuerryClientInfo(int id) {
        try {
            String querry = "SELECT username, dob, email, current, savings FROM users WHERE id = (?)";
            Connection con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(querry);
            pstmt.setString(1, Integer.toString(id));
            String str = null;
            ResultSet res = pstmt.executeQuery();
            if(res.next()) {
                str =  res.getString("username") + "/" + res.getString("dob") + "/" + res.getString("email") + "/" + res.getString("current") + "/" + res.getString("savings") + "/";
            }
            con.close();
            return str;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return "/";
    }
    public static Integer QuerryDeposit(int id,String acc, int amnt) throws CannotDepositException{
       String senderQuerry = "SELECT " + acc + " FROM users WHERE id = (?)";
       String querry2 = "UPDATE users SET " + acc + " = (?) WHERE id = (?)";
       Connection con = null;
       int a = 0;
       try {
            con = db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(senderQuerry);
            pstmt.setString(1, Integer.toString(id));

            ResultSet res = pstmt.executeQuery();
            
            if(res.next()) {
                a = Integer.valueOf(res.getString(1));
            }

            PreparedStatement pstmt2 = con.prepareStatement(querry2);
            pstmt2.setString(1, Integer.toString(a + amnt));
            pstmt2.setString(2, Integer.toString(id));

            pstmt2.executeUpdate();
            con.close();
       } catch(Exception e) {
            e.printStackTrace();
            throw new CannotDepositException("cannot deposit");
       }
       return a+amnt;
    }

    public static double withdraw(String username, double amnt) {
        return amnt;
    }

    public static boolean findReceiverUser(String username) {
        String querry = "SELECT username FROM users WHERE username= (?)";
        Connection con = null;
        boolean flag = false;
        try {
            con = db.getConnection();
            PreparedStatement smnt = con.prepareStatement(querry);
            smnt.setString(1, username);

            ResultSet rs = smnt.executeQuery();
            if(rs.next()) {
                flag = true;
            }
            con.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean TransferQuerry(int id, int money, String acc, int receiver) throws CannotTransferException, NoAccountException{
        String receiverExists = "SELECT current FROM users WHERE id = " + receiver;
        Connection con = null;
        boolean b1 = false;
        try {
            con = db.getConnection();
            Statement st = con.createStatement();
            ResultSet res = st.executeQuery(receiverExists);
            if(res.next()) {
                b1 = true; 
            }
            con.close();
        } catch (Exception e) {

        } 

        if(!b1) throw new CannotTransferException("Account does not exist");

        String checkSenderBalance = "SELECT " + acc + " FROM users WHERE id = " + id;
        boolean b2 = false;
        try {
            con = db.getConnection();
            Statement st = con.createStatement();
            ResultSet res = st.executeQuery(checkSenderBalance);
            if(res.next()) {
                if(Integer.valueOf(res.getString(acc)) >= money ) {
                    b2 = true;
                }
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        } 

        if(!b2) throw new CannotTransferException("Not enough funds");

        String t = "BEGIN; " +
                   "UPDATE users SET " + acc + " = " + acc + " - " + money + " WHERE id = " + id + " ; " +
                   "UPDATE users SET current = current + " + money + " WHERE id = " + receiver + " ; " +
                   "COMMIT;";
        try {
            con = db.getConnection();
            Statement st = con.createStatement();
            st.executeUpdate(t);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
