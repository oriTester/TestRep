package com.robot.ros.messages;

public interface RopBrainServiceRequest extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/RopBrainServiceRequest";
    java.lang.String getCommandID();
    void setCommandID(java.lang.String value);
    java.lang.String getSource();
    void setSource(java.lang.String value);
    java.lang.String getParams();
    void setParams(java.lang.String value);
}