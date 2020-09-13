package com.yy.drawinggift

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GiftDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IGiftDrawView {

    private var iconBitmap: Bitmap? = null
    private var iconUrl: String = ""
    private val giftListNodes = mutableListOf<MutableList<GiftNode>>()
    private var giftNodeLine: MutableList<GiftNode>? = null

    private var mX = 0F
    private var mY = 0F
    private var mWidth = 0
    private var mHeight = 0
    private var mGiftRect = Rect()
    private var mCurrentNode: GiftNode? = null
    private var bitmapWidth = 0
    private var bitmapHeight = 0

    private var backgroundBitmap: Bitmap? = null
    private val mCanvas = Canvas()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        backgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444)
        mCanvas.setBitmap(backgroundBitmap)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || iconBitmap == null) {
            return super.onTouchEvent(event)
        }
        if (mGiftRect.isEmpty) {
            mGiftRect.set(
                bitmapWidth / 2,
                bitmapHeight / 2,
                mWidth - bitmapWidth / 2,
                mHeight - bitmapHeight / 2
            )
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mX = event.x
                mY = event.y
                if (mX < bitmapWidth / 2)
                    mX = (bitmapWidth / 2).toFloat()
                if (mX > mWidth - bitmapWidth / 2) {
                    mX = (mWidth - bitmapWidth / 2).toFloat()
                }
                if (mY < bitmapHeight / 2)
                    mY = (bitmapHeight / 2).toFloat()
                if (mY > mWidth - bitmapHeight / 2) {
                    mY = (mWidth - bitmapHeight / 2).toFloat()
                }
                giftNodeLine = mutableListOf()
                mCurrentNode = GiftNode().apply {
                    x = mX
                    y = mY
                    icon = iconUrl
                }
                invalidate()
                giftNodeLine!!.add(mCurrentNode!!)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                var rect = Rect(
                    mX.toInt() - bitmapWidth,
                    mY.toInt() - bitmapHeight,
                    mX.toInt() + bitmapWidth,
                    mY.toInt() + bitmapHeight
                )
                if (rect.contains(event.x.toInt(), event.y.toInt()).not()
                    && mGiftRect.contains(event.x.toInt(), event.y.toInt())
                ) {
                    mX = event.x
                    mY = event.y
                    giftNodeLine!!.add(GiftNode().apply {
                        x = mX
                        y = mY
                        icon = iconUrl
                    })
                    mCurrentNode = GiftNode().apply {
                        x = mX
                        y = mY
                        icon = iconUrl
                    }
                    giftNodeLine!!.add(mCurrentNode!!)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                giftListNodes.add(giftNodeLine!!)
                mCurrentNode = null
            }
        }
        return false
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (iconBitmap == null) {
            return
        }
        mCurrentNode?.let {
            mCanvas.drawBitmap(
                iconBitmap,
                it.x - bitmapWidth / 2F,
                it.y - bitmapHeight / 2F,
                null
            )
            canvas?.drawBitmap(backgroundBitmap, 0F, 0F, null)
        }
    }

    override fun setIconBitMap(iconBitmap: Bitmap?) {
        this.iconBitmap = iconBitmap
        if (iconBitmap == null) {
            this.bitmapWidth = 0
            this.bitmapHeight = 0
        } else {
            this.bitmapWidth = iconBitmap.width
            this.bitmapHeight = iconBitmap.height
        }
    }

    override fun setIconUrl(iconUrl: String) {

    }
}