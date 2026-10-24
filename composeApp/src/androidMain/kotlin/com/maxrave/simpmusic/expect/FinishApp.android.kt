package com.maxrave.simpmusic.expect

import androidx.appcompat.app.AppCompatActivity
import org.koin.mp.KoinPlatform.getKoin

actual fun finishApp() {
    getKoin().get<AppCompatActivity>().finish()
}
