package com.robot.ros.messages;

public interface RopBrainServiceResponse extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/RopBrainServiceResponse";
    boolean getStatus();
    void setStatus(boolean value);
    java.lang.String getMessage();
    void setMessage(java.lang.String value);
    java.lang.String getCommand();
    void setCommand(java.lang.String value);
    java.lang.String getUserData();
    void setUserData(java.lang.String value);
}