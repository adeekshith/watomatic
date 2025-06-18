package com.parishod.watomatic.network.model.openai;

import java.util.List;

public class OpenAIResponse {
    private List<Choice> choices;
    // Add other fields like 'id', 'object', 'created', 'model', 'usage' if needed

    // Getters and setters
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
}
