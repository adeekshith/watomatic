package com.parishod.watomatic.network.model.openai;

public class Choice {
    private ResponseMessage message;
    // Add other fields like 'index', 'finish_reason' if needed

    // Getters and setters
    public ResponseMessage getMessage() { return message; }
    public void setMessage(ResponseMessage message) { this.message = message; }
}
