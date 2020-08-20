package xtend_rop;

public interface BrainCommand extends org.ros.internal.message.Message {
    static final java.lang.String _TYPE = "xtend_rop/BrainCommand";
    static final java.lang.String _DEFINITION = "string commandID  #Type of the command - TakePicture, Move etc\nstring source  #Source of the command: App (Android app), Sensor (Fire alarm) or Voice\nstring params  #parameters of the command - photo name, map name etc\n";

    java.lang.String getCommandID();
    void setCommandID(java.lang.String value);
    java.lang.String getSource();
    void setSource(java.lang.String value);
    java.lang.String getParams();
    void setParams(java.lang.String value);
}
