package com.tk.chains.exceptions;

public class DuplicateBusinessIdException extends RuntimeException {

    public DuplicateBusinessIdException(String businessId) {
        super(businessId);
    }

}
