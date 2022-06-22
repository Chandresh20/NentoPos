package com.tjcg.nentopos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import kotlinx.coroutines.delay

class ScreenReceiver : BroadcastReceiver() {

    private var ctx : Context? = null
    private val timerRunnable: Runnable
    val handler : Handler = Handler(Looper.getMainLooper())
    var counter = Constants.SUB_USER_AUTO_SIGN_OUT
    var isLoggedOut = false

    init {
        timerRunnable = object : Runnable {
            override fun run() {
                counter -= 1
                if (counter <= 0) {
                    Log.d("TimerCount", "Finished")
                    if (ctx != null) {
                        MainActivity.mainRepository.logOutSubUser(ctx!!, MainActivity.deviceID)
                        isLoggedOut = true
                    }
                } else {
                    Log.d("TimerCount", "AutoSignOutIn: $counter minutes")
                    handler.postDelayed( this, 60000)
                }
            }
        }
    }

    private fun startTimer() {
        counter = Constants.SUB_USER_AUTO_SIGN_OUT
        if (Constants.isFromSubUser) handler.post(timerRunnable)
    }

    private fun stopTimer() {
        isLoggedOut = false
        handler.removeCallbacks(timerRunnable)
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        ctx = p0
        Log.d("screenReceiver", "$p1")
        when(p1?.action) {
            Intent.ACTION_SCREEN_ON -> {
                if (isLoggedOut) {
                    handler.postDelayed( {
                        MainActivity.logOutNow(p0!!)
                    }, 10000)
                }
                Log.d("AutoSignOut", "TimerStopped")
                stopTimer()
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("AutoSignOut", "TimerStarted")
                startTimer()
            }
        }
    }

}