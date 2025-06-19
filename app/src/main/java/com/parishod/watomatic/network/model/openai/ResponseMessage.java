package com.parishod.watomatic.network.model.openai;

public class ResponseMessage {
    private String role;
    private String content;

    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
