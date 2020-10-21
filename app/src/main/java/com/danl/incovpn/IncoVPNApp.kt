package com.danl.incovpn

import dagger.hilt.android.HiltAndroidApp
import de.blinkt.openvpn.core.ICSOpenVPNApplication

@HiltAndroidApp
class IncoVPNApp : ICSOpenVPNApplication() {

    companion object {

        lateinit var TOKEN: String
            private set
        lateinit var BASE_URL: String
            private set
        lateinit var API_URL: String
            private set

        lateinit var IMAGE_URL: String
            private set

        const val KEY_SELECTED_COUNTRY = "selected_country"
        const val KEY_CONNECT_AT_START = "connect_at_start"
        const val KEY_SAVE_COUNTRY = "save_country"

        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate() {
        super.onCreate()
        val native = Native()
        TOKEN = native.getToken(this)!!
        BASE_URL = native.getBaseUrl(this)!!
        API_URL = "$BASE_URL/api/"
        IMAGE_URL = "$BASE_URL/static/images/"
    }

}