package com.suman.demo

import android.content.Context
import android.content.SharedPreferences

class PrefManager internal constructor(mContext: Context) {
    var editor: SharedPreferences.Editor? = null

    companion object {
        var pref: SharedPreferences? = null
        // const val ADS_SHOW_TIME = 10
    }

    init {
        if (pref == null) {
            pref = mContext.getSharedPreferences("WebView", Context.MODE_PRIVATE)
        }
    }
}