package com.parishod.watomatic.network.model.atomatic;

import com.google.gson.annotations.SerializedName;

public class AtomaticAIResponse {
    @SerializedName("reply")
    private String reply;

    @SerializedName("remainingAtoms")
    private int remainingAtoms;

    public AtomaticAIResponse() {
    }

    public AtomaticAIResponse(String reply, int remainingAtoms) {
        this.reply = reply;
        this.remainingAtoms = remainingAtoms;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public int getRemainingAtoms() {
        return remainingAtoms;
    }

    public void setRemainingAtoms(int remainingAtoms) {
        this.remainingAtoms = remainingAtoms;
    }
}

