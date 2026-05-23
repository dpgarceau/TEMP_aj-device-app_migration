package com.aerojudge.judge.dto;

public class APIErrorMessage {
    private String result;
    private String message;

    public APIErrorMessage(String result, String message) {
        this.result = result;
        this.message = message;
    }
    public String getResult() {
        return result;
    }
    public void setResult(String result) {
        this.result = result;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
