package com.digisoft.mss.model;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple list of message types upon which a subscriber is interested.
 */
public class Subscription {
    private Set<String> messageTypes;

    public Subscription() {
        messageTypes = new HashSet<>();
    }

    public Set<String> getMessageTypes() {
        return messageTypes;
    }

    public Subscription setMessageTypes(Set<String> messageTypes) {
        this.messageTypes = messageTypes;
        return this;
    }
}
