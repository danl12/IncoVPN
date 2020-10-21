package com.danl.incovpn

import android.content.Context

class Native {

    external fun getBaseUrl(context: Context): String?

    external fun getToken(context: Context): String?

}