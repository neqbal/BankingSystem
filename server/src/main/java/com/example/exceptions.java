package com.example;

public class exceptions {
    
}

class CannotLoginException extends Exception {
    CannotLoginException(String msg) {
        super(msg);
    }
}

class CannotRegisterException extends Exception {
    CannotRegisterException(String msg) {
        super(msg);
    }
}

class NoAccountException extends Exception {
    NoAccountException(String msg) {
        super(msg);
    }
}

class InsufficientAmountException extends Exception {
    InsufficientAmountException(String msg) {
        super(msg);
    }
}

class CannotDepositException extends Exception {
    CannotDepositException(String msg) {
        super(msg);
    }
}

class InvalidAmountException extends Exception {
    InvalidAmountException(String msg) {
        super(msg);
    }
}

class CannotAuthenticateUser extends Exception {
    CannotAuthenticateUser(String msg) {
        super(msg);
    }
}

class CannotTransferException extends Exception {
    CannotTransferException(String msg) {
        super(msg);
    }
}

class CannotWithdrawException extends Exception {
    CannotWithdrawException(String msg) {
        super(msg);
    }
}