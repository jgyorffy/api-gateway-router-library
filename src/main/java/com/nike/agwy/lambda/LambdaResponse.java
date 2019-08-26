package com.nike.agwy.lambda;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LambdaResponse {
    private Object responseObject;
    private Map<String, String> headers;
    private int returnCode;

    public LambdaResponse(Object responseObject, Map<String, String> headers, int returnCode) {
        this.responseObject = responseObject;
        this.headers = Optional.ofNullable(headers).orElse(new HashMap<>());
        this.returnCode = returnCode;
    }

    public static LambdaResponseBuilder builder() {
        return new LambdaResponseBuilder();
    }

    public <T> T getResponseObject() {
        return (T) responseObject;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public int getReturnCode() {
        return returnCode;
    }

    public static class LambdaResponseBuilder {
        private Object responseObject;
        private Map<String, String> headers = new HashMap<>();
        private int returnCode;

        public LambdaResponseBuilder responseObject(Object responseObject) {
            this.responseObject = responseObject;
            return this;
        }

        public LambdaResponseBuilder headers(Map<String, String> headers) {
            this.headers = Optional.ofNullable(headers).orElse(new HashMap<>());
            return this;
        }

        public LambdaResponseBuilder returnCode(int returnCode) {
            this.returnCode = returnCode;
            return this;
        }

        public LambdaResponse build() {
            return new LambdaResponse(responseObject, headers, returnCode);
        }
    }
}
