package com.tjcg.nentopos

import android.os.Handler
import android.os.Looper
import android.view.View

class Animator {

    var viewToBlink : View? =null
    var duration = 0L

    private val fadeHandler = Handler(Looper.getMainLooper())
    private val fadeRunnable1 = Runnable {
        viewToBlink?.animate()?.alpha(1f)?.duration = duration
        fadeHandler.postDelayed(fadeRunnable2,duration)
    }
    private val fadeRunnable2: Runnable = Runnable {
        viewToBlink?.animate()?.alpha(0f)?.duration = duration
        fadeHandler.postDelayed(fadeRunnable1, duration)
    }

    fun startBlinking(v: View, duration: Long) {
        viewToBlink = v
        this.duration = duration
        fadeHandler.post(fadeRunnable1)
    }

    fun stopBlinking() {
        fadeHandler.removeCallbacks(fadeRunnable1)
        fadeHandler.removeCallbacks(fadeRunnable2)
    }
}