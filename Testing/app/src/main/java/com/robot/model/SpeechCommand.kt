package com.robot.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName

import java.util.*

@JsonRootName("speechrecognition")
data class Speechrecognition(
    @set:JsonProperty("mode")
    var modes: List<Mode> = ArrayList()
)

data class Mode(
    @set:JsonProperty("name")
    var name: String? = null,

    @set:JsonProperty("paramsFile")
    var paramsFile: String? = null,

    @set:JsonProperty("command")
    var commands: List<Command> = ArrayList()
)

data class Command(
    @set:JsonProperty("id")
    var id: String? = null,

    @set:JsonProperty("params")
    var params: String? = null,

    @set:JsonProperty("mainToken")
    var mainToken: String? = null,

    @set:JsonProperty("paramTokens")
    var paramTokens: ParamTokens? = null
)

class ParamTokens {
    @set:JsonProperty("token")
    var token: List<String> = ArrayList()
}