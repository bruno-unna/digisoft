package com.digisoft.mss.model;

/**
 * A message is simply a type and a body, both of them simple strings.
 */
public class Message {
    private String messageType;
    private String messageBody;

    public String getMessageType() {
        return messageType;
    }

    public Message setMessageType(String messageType) {
        this.messageType = messageType;
        return this;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public Message setMessageBody(String messageBody) {
        this.messageBody = messageBody;
        return this;
    }
}
