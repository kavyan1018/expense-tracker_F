package com.expensetracker.exception;

/** Thrown for missing records and for records owned by someone else, so ids can't be probed. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what) {
        super(what + " not found");
    }
}
