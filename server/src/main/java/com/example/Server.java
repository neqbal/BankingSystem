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

    void createServer() {
        try {
            ss = new ServerSocket(PORT, BACKLOG);
            System.out.println("Server created on PORT " + PORT);
            
            while (true) {
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
        String[] arr = info.split("/");
        this.username = arr[0];
        this.dob = arr[1];
        this.email = arr[2];
        this.currAmount = Integer.parseInt(arr[3]);
        this.savAmount = Integer.parseInt(arr[4]);
    }

    void updateSavAmount(int n) {
        savAmount += n;
    }

    void updateCurrAmount(int n) {
        currAmount += n;
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
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(s.getOutputStream(), true);

            SendMessage("Connected");

            String inputLine;
            while (true) {
                System.out.println("Waiting for client message");
                if ((inputLine = in.readLine()) != null) {
                    System.out.println("Received: " + inputLine);
                    ProcessClientMessage(inputLine);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            CloseConnection();
        }
    }

    public void CloseConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (s != null) s.close();
            if (currUserID != null) bankService.DeAuthenticateUser(currUserID);

            System.out.println(currUserID + " closed connection.");
        } catch (IOException e) {
            System.out.println("Error closing resources");
            e.printStackTrace();
        }
    }

    public void ProcessClientMessage(String inputLine) {
        System.out.println("Received: " + inputLine);
        String[] parts = inputLine.split("/");
        switch (parts[0]) {
            case "REGISTER":
                RegisterClient(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
                break;
            case "LOGIN":
                LoginClient(parts[1], parts[2]);
                break;
            case "DEPOSIT":
                ClientDeposit(parts[1], Integer.parseInt(parts[2]));
                break;
            case "WITHDRAW":
                System.out.println("Withdrawing");
                handleWithdraw(parts[1], Integer.parseInt(parts[2]));
                break;
            case "TRANSFER":
                Transfer(Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]));
                break;
            case "UPDATES":
                Updates();
                break;
            case "LOGOUT":
                handleLogout();
                break;
        }
    }

    public void handleWithdraw(String account, int amount) {
        try {
            bankService.QuerryWithdraw(currUserID, account, amount);
            System.out.println(account);
            if ("CURRENT".equalsIgnoreCase(account)) {
                ci.updateCurrAmount(-amount);
                SendMessage("WITHDRAWN/CURRENT/" + ci.currAmount);
            } else if ("SAVINGS".equalsIgnoreCase(account)) {
                ci.updateSavAmount(-amount);
                SendMessage("WITHDRAWN/SAVINGS/" + ci.savAmount);
            }
        } catch (CannotWithdrawException e) {
            SendMessage(e.getMessage());
        }
    }

    public void Transfer(int money, String acc, int receiver) {
        try {
            if (bankService.TransferQuerry(currUserID, money, acc, receiver)) {
                SendMessage("TRANSFERRED");
                
                ClientHandler receiverHandler = Server.threadIdToClientHandler.get(Server.clientIdToThreadId.get(receiver));
                receiverHandler.ci.updateCurrAmount(money);
                if (acc.equals("current")) {
                    ci.updateCurrAmount(-money);    
                } else {
                    ci.updateSavAmount(-money);
                }
            }
        } catch (CannotTransferException | NoAccountException e) {
            SendMessage(e.getMessage());
        }
    }

    public void LoginClient(String username, String password) {
        try {
            int userID = bankService.QuerryLogin(username, password);
            if (userID != 0) {
                currUserID = userID;
                Server.clientIdToThreadId.put(currUserID, ThreadID);
                System.out.println("Logged in as " + currUserID);

                String info = GetClientInfo();
                ci = new ClientInfo(info);
                SendMessage("WELCOME/" + info + currUserID);
            }
        } catch (CannotLoginException e) {
            SendMessage(e.getMessage());
        }
    }

    public void RegisterClient(String name, String password, String dob, String email, String checkAmnt, String savAmnt) {
        try {
            if (bankService.QuerryRegister(name, password, dob, email, checkAmnt, savAmnt)) {
                SendMessage("REGISTERED");
            }
        } catch (CannotRegisterException e) {
            SendMessage(e.getMessage());
        }
    }

    public void ClientDeposit(String account, int amount) {
        try {
            bankService.QuerryDeposit(currUserID, account, amount);
            if ("CURRENT".equalsIgnoreCase(account)) {
                ci.updateCurrAmount(amount);
                SendMessage("DEPOSITED/CURRENT/" + ci.currAmount);
            } else if ("SAVINGS".equalsIgnoreCase(account)) {
                ci.updateSavAmount(amount);
                SendMessage("DEPOSITED/SAVINGS/" + ci.savAmount);
            }
            System.out.println("Deposited " + amount + " to " + account + " account.");
        } catch (CannotDepositException e) {
            SendMessage(e.getMessage());
        }
    }

    public void Updates() {
        SendMessage("UPDATES/" + ci.currAmount + "/" + ci.savAmount);
    }

    public void handleLogout() {
        bankService.DeAuthenticateUser(currUserID);
        SendMessage("LOGGED_OUT");
        ci = null;
        currUserID = null;
        System.out.println("Logged out user " + currUserID);
    }

    public String GetClientInfo() {
        return bankService.QuerryClientInfo(currUserID);
    }

    public void SendMessage(String message) {
        out.println(message);
        System.out.println("Sent: " + message);
    }
}
