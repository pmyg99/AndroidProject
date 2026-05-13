package com.example.petowner

import android.app.Application

class MyApp : Application() {
    companion object {
        private lateinit var instance: MyApp

        fun getContext(): Application = instance


    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}