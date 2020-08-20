package com.robot.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder

import okhttp3.OkHttpClient

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class Network private constructor() {
    lateinit var api: NetworkApi

    companion object Singleton {
        val networkConfiguration =
            NetworkConfiguration("http://friday.hyperlync.com:3131/")
        var instance: Network = Network()
    }

    init {
        initNetwork()
    }

    private fun initNetwork() {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

        val client = OkHttpClient().newBuilder().build()

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(networkConfiguration.baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = NetworkApi(retrofit.create(NetworkService::class.java))
    }
}

data class NetworkConfiguration(val baseUrl: String)