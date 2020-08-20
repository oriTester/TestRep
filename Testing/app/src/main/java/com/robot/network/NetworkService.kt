package com.robot.network

import retrofit2.Call
import retrofit2.http.*

interface NetworkService {
    @GET("streams.json")
    fun getStreams(): Call<List<StreamInfo>>
}