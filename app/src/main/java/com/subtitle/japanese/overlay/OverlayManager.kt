package com.subtitle.japanese.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.subtitle.japanese.R
import com.subtitle.japanese.databinding.OverlaySubtitleBinding
import com.subtitle.japanese.util.Constants

class OverlayManager(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var binding: OverlaySubtitleBinding? = null
    private var isShowing = false

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowing) return

        binding = OverlaySubtitleBinding.inflate(LayoutInflater.from(context))
        overlayView = binding!!.root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        // Enable dragging
        setupDragging(overlayView!!, params)

        windowManager.addView(overlayView, params)
        isShowing = true

        updateText(context.getString(R.string.overlay_initializing))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragging(view: View, params: WindowManager.LayoutParams) {
        var initialY = 0
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.y = initialY + (initialTouchY - event.rawY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    fun updateText(text: String) {
        if (!isShowing) return
        binding?.tvSubtitle?.text = text
    }

    fun setFontSize(sizeSp: Float) {
        if (!isShowing) return
        binding?.tvSubtitle?.textSize = sizeSp
    }

    fun setPosition(gravity: Int) {
        if (!isShowing) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
        }
        windowManager.updateViewLayout(overlayView!!, params)
    }

    fun hide() {
        if (!isShowing) return
        try {
            windowManager.removeView(overlayView)
        } catch (_: Exception) {
        }
        overlayView = null
        binding = null
        isShowing = false
    }

    fun isShowing(): Boolean = isShowing
}
