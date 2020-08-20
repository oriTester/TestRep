package com.robot.ros.messages;

public interface GetMediaNamesList extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/GetMediaNamesList";
    static final java.lang.String _DEFINITION = "# Request message type\nstring mediaType           # type of the media - photo, video etc\n---\n# Response message type\nint8 status               # actually boolean. 1 for success, 0 for failure \nstring message      # non-empty in case of failure \nstring[] nameList            # List of the media names \n";
}
