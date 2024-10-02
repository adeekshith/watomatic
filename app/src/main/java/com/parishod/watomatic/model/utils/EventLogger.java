package com.parishod.watomatic.model.utils;

import java.util.HashMap;

public class EventLogger {
    private final String SERVICE_ENABLED = "se";
    private final String SUPPORTED_PACKAGE = "sp";
    private final String NEW_NOTIFICATION = "nn";
    private final String GROUP_REPLY_ENABLED = "gre";
    private final String GROUP_MESSAGE = "gm";
    private final String CAN_REPLY_NOW = "crn";
    private final String CONTACTS_REPLY_ENABLED = "cre";
    private final String CONTACTS_REPLY_REASON = "crr";
    private final String REMOTE_INPUTS_EMPTY = "rie";
    private final String REPLY_ERROR = "err";

    private HashMap event;

    public EventLogger(){
        event = new HashMap();
    }

    public void setServiceEnabled(boolean isServiceEnabled){
        event.put(SERVICE_ENABLED, isServiceEnabled);
    }

    public void setSupportedPackage(boolean isPackageSupported){
        event.put(SUPPORTED_PACKAGE, isPackageSupported);
    }

    public void setNewNotification(boolean isNew){
        event.put(NEW_NOTIFICATION, isNew);
    }

    public void setGroupReplyEnabled(boolean isGroupReplyEnabled){
        event.put(GROUP_REPLY_ENABLED, isGroupReplyEnabled);
    }

    public void setIsGroupMessage(boolean isGroupMsg){
        event.put(GROUP_MESSAGE, isGroupMsg);
    }

    public void setCanSendReplyNow(boolean canSendReplyNow){
        event.put(CAN_REPLY_NOW, canSendReplyNow);
    }

    public void setContactsReplyEnabled(boolean isContactsReplyEnabled){
        event.put(CONTACTS_REPLY_ENABLED, isContactsReplyEnabled);
    }

    public void setContactsReplyReason(String contactsReplyReason){
        event.put(CONTACTS_REPLY_REASON, contactsReplyReason);
    }

    public void setRemoteInputsEmpty(boolean remoteInputsEmpty){
        event.put(REMOTE_INPUTS_EMPTY, remoteInputsEmpty);
    }

    public void setReplyErrReason(String replyErrReason){
        event.put(REPLY_ERROR, replyErrReason);
    }

    public HashMap getEvent(){
        return event;
    }
}
