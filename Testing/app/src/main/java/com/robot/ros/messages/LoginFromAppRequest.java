package com.robot.ros.messages;

public interface LoginFromAppRequest extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "com.robot.ros.messages/LoginFromAppRequest";
  static final java.lang.String _DEFINITION = "# Request message type \nstring userName \nstring password \n\n";
  java.lang.String getUserName();
  void setUserName(java.lang.String value);
  java.lang.String getPassword();
  void setPassword(java.lang.String value);
}
