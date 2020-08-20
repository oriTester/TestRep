package com.robot.ros.messages;

public interface LoginFromAppResponse extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "com.robot.ros.messages/LoginFromAppResponse";
  static final java.lang.String _DEFINITION = "\n# Response message type \nint8 status      # actually boolean. 1 for success, 0 for failure \nstring message   # contains error string on failure ";
  byte getStatus();
  void setStatus(byte value);
  java.lang.String getMessage();
  void setMessage(java.lang.String value);
}
