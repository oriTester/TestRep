package com.robot.ros.messages;

public interface RopGetFileResponse extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/RopGetFileResponse";
    boolean getStatus();
    void setStatus(boolean value);
    java.lang.String getMessage();
    void setMessage(java.lang.String value);
    org.jboss.netty.buffer.ChannelBuffer getBuffer();
    void setBuffer(org.jboss.netty.buffer.ChannelBuffer value);
}
