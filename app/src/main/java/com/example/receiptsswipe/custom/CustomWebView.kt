package com.example.receiptsswipe.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView

class CustomWebView : WebView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    private var swipe: () -> Unit = {}

    fun setCallback(swipeCallback: () -> Unit) {
        this.swipe = swipeCallback
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        super.onTouchEvent(ev)
        performClick()
        if (ev?.action == MotionEvent.ACTION_UP) {
            swipe.invoke()
        }
        return true
    }
}