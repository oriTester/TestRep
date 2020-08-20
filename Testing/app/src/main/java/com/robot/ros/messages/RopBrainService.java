package com.robot.ros.messages;

public interface RopBrainService extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "com.robot.ros.messages/RopBrainService";
    static final java.lang.String _DEFINITION = "# Request message type\nstring commandID           # Type of the command - TakePicture, Move etc\nstring source           # Source of the command: App (Android app), Sensor (Fire alarm) or Voice\nstring params           # parameters of the command - photo name, map name etc\n---\n# Response message type\nstring command           # Type of the command - TakePicture, Move etc\nbool status            # true - OK, false - an error occurred \nstring message           # non-empty in case of failure \nstring userData           # behavior result, for example path to a picture that was taken\n";
}