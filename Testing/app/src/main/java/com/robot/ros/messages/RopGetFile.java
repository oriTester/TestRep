package com.robot.ros.messages;

public interface RopGetFile extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/RopGetFile";
    static final java.lang.String _DEFINITION = "# Request message type\nstring path           # type of the media - photo, video etc\n---\n# Response message type\nbool status               # actually boolean. 1 for success, 0 for failure \nstring message      # non-empty in case of failure \nuint8[] buffer            # List of the media names \n";
}
