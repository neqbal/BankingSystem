package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    static ServerSocket ss;
    private static final int PORT = 9999;
    private static final int BACKLOG = 10;

    public static ConcurrentHashMap<Integer, ClientHandler> threadIdToClientHandler = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> clientIdToThreadId = new ConcurrentHashMap<>();
    private int clientCounter = 0;
    ExecutorService ex = Executors.newFixedThreadPool(BACKLOG);

    void crerateServer() {
        try {
            ss = new ServerSocket(PORT, BACKLOG);
            System.out.println("Server created on PORT " + PORT);
            
            while(true) {
                Socket s = ss.accept();
                ClientHandler ch = new ClientHandler(s, clientCounter);
                threadIdToClientHandler.put(clientCounter, ch);
                clientCounter += 1;
                ex.submit(ch);                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
class ClientInfo {
    String username, dob, email;
    int savAmount;
    int currAmount;
    List<String> log = new ArrayList<>();

    ClientInfo(String info) {
        String arr[] = info.split("/");
        this.username = arr[0];
        this.dob = arr[1];
        this.email = arr[2];
        this.currAmount = Integer.parseInt(arr[3]);
        this.savAmount = Integer.parseInt(arr[4]);
    }
    void updateSavAmount(int n) {
        savAmount = n;
    }

    void updateCurrAmount(int n) {
        currAmount = n;
    }

    void updateLogs(String l) {
        log.add(l);
    }
}
class ClientHandler implements Runnable {
    private Socket s;
    private BufferedReader in;
    private PrintWriter out;
    private Integer currUserID = null;
    private ClientInfo ci;
    private int ThreadID;
    ClientHandler(Socket s, int ThreadID) {
        this.s = s;
        this.ThreadID = ThreadID;
    }

    @Override
    public void run(){
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(s.getOutputStream(), true);

            SendMessage("Connected");

            String inputLine;
            while(true) {
                System.out.println("Waiting for client message");
                if((inputLine = in.readLine()) != null) {
                    ProcessClientMessage(inputLine);
                } else {
                    break;
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            CloseConnection();
        }
    }

    public void CloseConnection() {
        try {
            in.close();
            out.close();
            s.close();
            if(currUserID != null) bankService.DeAuthenticateUser(currUserID);

            System.out.println(currUserID + " closed");
        } catch (IOException e) {
            System.out.println("Error closing resources");
            e.printStackTrace();
        }
    }
    public void ProcessClientMessage(String inputLine) {
        System.out.println(inputLine);
        String parts[] = inputLine.split("/");
        switch(parts[0]) {
            case "REGISTER" :
            {
                String username = parts[1];
                String psswd = parts[2];
                String dob = parts[3];
                String email = parts[4];
                String currAmnt = parts[5];
                String savAmnt = parts[6];
                RegisterClient(username, psswd, dob, email, currAmnt, savAmnt);
            }
                break;
            case "LOGIN": 
            {
                String username = parts[1];
                String psswd = parts[2];
                LoginClient(username, psswd);
                String info = GetClientInfo();
                ci = new ClientInfo(info);
                SendMessage("WELCOME/" + info);
            }
                break;
            case "DEPOSIT":
            {
                String account = parts[1];
                String amount = parts[2];
                ClientDeposit(account, Integer.parseInt(amount));
            }
                break;
            case "WITHDRAW":
                break;
            case "TRANSFER":
            {
                int money = Integer.valueOf(parts[1]);
                String acc = parts[2];
                int receiver = Integer.valueOf(parts[3]);
                Transfer(money, acc, receiver);
            }
                break;
            case "LOGOUT":
                break;
        }
    }

    public void SendMessage(String post) {
        System.out.println("posting");
        out.println(post);
        System.out.println("posted");
    }

    public void Transfer(int money, String acc, int receiver) {
        try {
            if(bankService.TransferQuerry(currUserID, money, acc, receiver)) {
                SendMessage("TRANSFERED");
            }
        } catch(CannotTransferException | NoAccountException e) {
            SendMessage(e.getMessage());
        }
    }

    public void LoginClient(String username, String psswd) {
        try {
            int a = 0;
            if((a = bankService.QuerryLogin(username, psswd)) != 0) {
                currUserID = a;
                Server.clientIdToThreadId.put(currUserID, ThreadID);
                System.out.println(currUserID);
            }
        } catch(CannotLoginException e) {
            SendMessage(e.getMessage());
        }
    }

    public void RegisterClient(String name, String psswd, String dob, String email, String checkAmnt, String savAmnt) {
        System.out.println("Registering Client");
        try {
            if(bankService.QuerryRegister(name, psswd, dob, email, checkAmnt, savAmnt)) {
                SendMessage("REGISTERED");
            }
        } catch(CannotRegisterException e) {
            SendMessage(e.getMessage());
        }
    }

    public void handleLogout() {
        bankService.DeAuthenticateUser(currUserID);
        SendMessage("Logged Out");
    }

    public String GetClientInfo() {
        String info = bankService.QuerryClientInfo(currUserID);
        return info;
    }
    public void ClientDeposit(String acc, int amnt) {
        System.out.println("depositing");
        try {
            int a = 0;
            switch(acc) {
                case "CURRENT":
                    a = bankService.QuerryDeposit(currUserID ,"current", amnt);
                    ci.updateCurrAmount(a);
                    SendMessage("DEPOSITED/" + acc + "/" + Integer.toString(ci.currAmount) + "/");
                    break;
                case "SAVINGS":
                    a = bankService.QuerryDeposit(currUserID ,"savings", amnt);
                    ci.updateSavAmount(a);
                    SendMessage("DEPOSITED/" + acc + "/" + Integer.toString(ci.savAmount) + "/");
                    break;
            }
        } catch(CannotDepositException e) {
            SendMessage(e.getMessage());
        }
    }
    public void handleWithdraw(String psswd, Double amnt) {} 
}
