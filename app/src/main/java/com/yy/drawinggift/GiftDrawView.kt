package com.yy.drawinggift

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.logging.Logger
import kotlin.concurrent.thread

class GiftDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IGiftDrawView {

    companion object {

        const val ACTION_DOWN = 1
        const val ACTION_MOVE = 2
        const val ACTION_UP = 3
    }

    private var iconBitmap: Bitmap? = null
    private var iconUrl: String = ""
    private val giftListNodes = mutableListOf<MutableList<GiftNode>>()
    private var giftNodeLine: MutableList<GiftNode>? = null

    private var mX = 0F
    private var mY = 0F
    private var mWidth = 0
    private var mHeight = 0
    private var mGiftRect = RectF()
    private var mCurrentNode: GiftNode? = null
    private var iconWidth = 0
    private var iconHeight = 0

    private var backgroundBitmap: Bitmap? = null
    private val mCanvas = Canvas()
    private var isDrawing = true
    private var isClear = false
    private lateinit var giftNumListener: (Int) -> Unit

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                ACTION_DOWN -> touchDown()
                ACTION_MOVE -> {
                    val point = msg.obj as PointF
                    touchMove(point.x, point.y)
                }
                ACTION_UP -> touchUp()
            }
        }
    }

    private fun touchDown() {
        val halfIcWidth = iconWidth / 2F
        val halfIcHeight = iconHeight / 2F
        //边界检测
        mX = if (mX < halfIcWidth) halfIcWidth else mX
        mX = if (mX > mWidth - halfIcWidth) mWidth - halfIcWidth else mX
        mY = if (mY < halfIcHeight) halfIcHeight else mY
        mY = if (mY > mHeight - halfIcHeight) mHeight - halfIcHeight else mY
        giftNodeLine = mutableListOf()
        mCurrentNode = GiftNode(mX, mY, iconUrl)
        giftNodeLine!!.add(mCurrentNode!!)
        invalidate()
    }

    private fun touchMove(x: Float, y: Float) {
        setGiftRectIfNeed()
        val rect = RectF(
            mX - iconWidth, mY - iconHeight,
            mX + iconWidth,
            mY + iconHeight
        )
        if (rect.contains(x, y).not() && mGiftRect.contains(x, y)
        ) {
            mX = x
            mY = y
            mCurrentNode = GiftNode(mX, mY, iconUrl)
            giftNodeLine!!.add(mCurrentNode!!)
            invalidate()
        }
    }

    private fun touchUp() {
        giftListNodes.add(giftNodeLine!!)
        mCurrentNode = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        backgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444)
        mCanvas.setBitmap(backgroundBitmap)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!checkAvailable() || !isDrawing) {
            return super.onTouchEvent(event)
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mX = event.x
                mY = event.y
                mHandler.sendEmptyMessage(ACTION_DOWN)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.d("DRAW", "${event.x}  ${event.y}")
                mHandler.sendMessage(
                    mHandler.obtainMessage(ACTION_MOVE, PointF(event.x, event.y))
                )
            }
            MotionEvent.ACTION_UP -> mHandler.sendEmptyMessage(ACTION_UP)
        }
        return false
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isClear) {
            backgroundBitmap?.recycle()
            backgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444)
            mCanvas.setBitmap(backgroundBitmap)
            isClear = false
        }
        if (iconBitmap == null || backgroundBitmap == null) {
            return
        }
        mCurrentNode?.let {
            mCanvas.drawBitmap(
                iconBitmap, it.x - iconWidth / 2F,
                it.y - iconHeight / 2F,
                null
            )
        }
        canvas?.drawBitmap(backgroundBitmap, 0F, 0F, null)
    }

    override fun setIconBitMap(iconBitmap: Bitmap?) {
        this.iconBitmap = iconBitmap
        if (iconBitmap == null) {
            this.iconWidth = 0
            this.iconHeight = 0
        } else {
            this.iconWidth = iconBitmap.width
            this.iconHeight = iconBitmap.height
        }
        emptyGiftRect()
    }

    override fun setIconUrl(iconUrl: String) {
    }

    override fun setIsDrawing(isDrawing: Boolean) {
        this.isDrawing = isDrawing
    }

    override fun clearBoard() {
        this.isClear = true
        invalidate()
    }

    override fun replay() {
        thread {
            giftListNodes.forEach {
                it.forEach { node ->
                    this@GiftDrawView.mCurrentNode = node
                    this@GiftDrawView.postInvalidate()
                    Thread.sleep(50)
                }
            }
        }
    }

    private fun checkAvailable(): Boolean {
        if (iconBitmap == null || mHeight == 0 || mWidth == 0) {
            return false
        }
        return true
    }

    private fun setGiftRectIfNeed() {
        if (mGiftRect.isEmpty) {
            mGiftRect.set(
                iconWidth / 2F, iconHeight / 2F, mWidth - iconWidth / 2F,
                mHeight - iconHeight / 2F
            )
        }
    }

    private fun emptyGiftRect() {
        mGiftRect.setEmpty()
    }
}