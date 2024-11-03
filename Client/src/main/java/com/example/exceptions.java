package com.example;

public class exceptions {
    
}

class EmptyFieldException extends Exception {
    EmptyFieldException(String msg) {
        super(msg);
    }
}

class CannotRegisterException extends Exception {
    CannotRegisterException(String msg) {
        super(msg);
    }
}