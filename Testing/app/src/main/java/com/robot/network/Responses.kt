package com.robot.network

data class BaseResponse<T>(
    val status: String,
    val message: String,
    val code: String?,
    val data: T?
)

data class StreamInfo(
    val id: String,
    val name: String
)


