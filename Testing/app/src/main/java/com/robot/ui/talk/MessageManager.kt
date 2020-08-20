package com.robot.ui.talk

class MessageManager {

    companion object {
        private var INSTANCE: MessageManager? = null
        private var TAG = "MessageManager"

        val instance: MessageManager
            get() {
                if (INSTANCE == null) {
                    INSTANCE = MessageManager()
                }
                return INSTANCE!!
            }
    }

    var messages = mutableListOf<Message>()
}