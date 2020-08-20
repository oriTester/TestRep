package com.robot.utils

import com.robot.application.MainApplication

import devliving.online.securedpreferencestore.DefaultRecoveryHandler
import devliving.online.securedpreferencestore.SecuredPreferenceStore

object PreferencesManager {
    private val preferenceStore: SecuredPreferenceStore by lazy {
        try {
            SecuredPreferenceStore.init(MainApplication.applicationContext(), null, null, null, DefaultRecoveryHandler())
        } catch (expected: Throwable) {
        }

        SecuredPreferenceStore.getSharedInstance()
    }

    const val IP_ADDRESS = "ROBOT_IP_NEW"
    const val USER_NAME = "USER_NAME"

    fun putString(key: String, value: String) = preferenceStore.edit().putString(key, value).apply()
    fun getString(key: String): String? = preferenceStore.getString(key, null)
    fun putBoolean(key: String, value: Boolean) = preferenceStore.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String, defValue: Boolean = false): Boolean = preferenceStore.getBoolean(key, defValue)
    fun putLong(key: String, value: Long) = preferenceStore.edit().putLong(key, value).apply()
    fun getLong(key: String, defValue: Long = 0L) = preferenceStore.getLong(key, defValue)

    fun isIPAdded(): Boolean {
        val ip =
            getString(IP_ADDRESS)
        return !ip.isNullOrEmpty()
    }
}