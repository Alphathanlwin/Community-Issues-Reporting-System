package com.uit.scirs.common.exception;

public class AccountNotApprovedException extends RuntimeException {

    public AccountNotApprovedException(String message) {
        super(message);
    }
}
