package com.robot.ros.messages;

public interface GetMediaNamesListResponse extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/GetMediaNamesListResponse";
    byte getStatus();
    void setStatus(byte value);
    java.lang.String getMessage();
    void setMessage(java.lang.String value);
    java.util.List<java.lang.String> getNameList();
    void setNameList(java.util.List<java.lang.String> value);
}