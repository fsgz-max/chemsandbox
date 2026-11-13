package com.example.chemsandbox

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class SandGLView(context: Context) : GLSurfaceView(context) {
    val sim = Simulation(200, 300)
    var brushElement: Element = Element.SAND
    var brushRadius = 8

    init {
        setEGLContextClientVersion(2)
        setRenderer(SandRenderer(sim))
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (width == 0 || height == 0) return true
        val gx = (e.x / width * sim.width).toInt()
        val gy = (e.y / height * sim.height).toInt()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> sim.paint(gx, gy, brushRadius, brushElement)
        }
        return true
    }
}
