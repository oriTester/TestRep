package com.robot.ros

import com.robot.R
import com.robot.model.Command
import com.robot.model.Speechrecognition
import com.robot.model.SpeechResult
import com.robot.utils.parseAs

class CommandManager {

    private var commands = mutableListOf<Command>()
    private var defaultResult = SpeechResult()

    companion object {
        private var INSTANCE: CommandManager? = null
        private var TAG = "CommandManager"

        val instance: CommandManager
            get() {
                if (INSTANCE == null) {
                    INSTANCE = CommandManager()
                }
                return INSTANCE!!
            }
    }

    init {
        defaultResult.text = "Sorry i don't understand you"

        val result = parseAs<Speechrecognition>(R.raw.speechrecognition)

        result?.modes?.forEach {
            if(it.name == "google_commands") {
                commands.addAll(it.commands)
            }
        }
    }

    fun getResult(text:String): SpeechResult {
        val command = getCommand(text) ?: return defaultResult

        val result = SpeechResult()
        result.command = command
        result.text = "Command sent"
        return result
    }

    private fun getCommand(text:String): Command? {
        commands.forEach {command ->
           if( command.mainToken?.let { it -> text.contains(it, true) } == true) {
               val split = text.split(" ")
               if (split.count() > 1) {
                   command.paramTokens?.token?.forEach {token->
                       if(text.contains(token,true)) {
                           return command
                       }
                   }
               }
           }
        }

        return null
    }

}