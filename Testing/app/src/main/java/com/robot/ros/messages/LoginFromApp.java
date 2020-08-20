package com.robot.ros.messages;

public interface LoginFromApp extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "com.robot.ros.messages/LoginFromApp";
  static final java.lang.String _DEFINITION = "# Request message type \nstring userName \nstring password \n\n--- \n\n# Response message type \nint8 status      # actually boolean. 1 for success, 0 for failure \nstring message   # contains error string on failure \n";
}
