package com.robot.network

import retrofit2.Call
import java.util.Date

class NetworkApi(private val networkService: NetworkService) {
    fun streams(): Call<List<StreamInfo>> = networkService.getStreams()
}