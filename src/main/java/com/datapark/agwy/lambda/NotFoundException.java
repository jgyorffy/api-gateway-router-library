package com.datapark.agwy.lambda;

class NotFoundException extends RuntimeException {

    NotFoundException(String msg) {
        super(msg);
    }
}
