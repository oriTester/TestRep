package com.robot.ros.messages;

public interface RopBatteryServiceResponse extends org.ros.internal.message.Message  {
    static final java.lang.String _TYPE = "com.robot.ros.messages/RopBatteryServiceResponse";
    byte getLevel();
    void setLevel(byte value);
}
