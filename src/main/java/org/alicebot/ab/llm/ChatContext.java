package org.alicebot.ab.llm;

import org.alicebot.ab.utils.SprintUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class ChatContext {
    private String systemPrompt;
    private final List<ChatMessage> messages = new LinkedList<>();
    private int maxHistory = 0;
    private String addParams;
    private String model;


    public ChatContext(ChatContext other) {
        if (other != null) {
            this.systemPrompt = other.systemPrompt;
            this.maxHistory = other.maxHistory;
            this.addParams = other.addParams;
            this.model = other.model;
            this.messages.addAll(other.messages);
        }
    }

    public ChatContext() {}

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public String getAddParams() {
        return addParams;
    }

    public void setAddParams(String addParams) {
        this.addParams = addParams;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void addUserMessage(String content) {
        addMessage(new ChatMessage(Role.USER, content));
    }

    public void addAssistantMessage(String content) {
        addMessage(new ChatMessage(Role.ASSISTANT, content));
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        if (maxHistory > 0 && messages.size() > maxHistory) {
            messages.removeFirst();
        }
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public enum Role {
        USER, ASSISTANT
    }

    public record ChatMessage(Role role, String content) {
        public ChatMessage(Role role, String content) {
            this.role = role;
            this.content = content != null ? content.replaceAll("\\<.*?\\>", "") : "";
        }

        @NotNull
        @Override
        public String toString() {
            return "ChatMessage{" +
                    "role=" + role +
                    ", content='" + SprintUtils.shorten(content) + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ChatContext{" +
                "systemPrompt='" + systemPrompt + '\'' +
                ", messages=" + messages +
                ", maxHistory=" + maxHistory +
                ", addParams='" + addParams + '\'' +
                ", model='" + model + '\'' +
                '}';
    }
}