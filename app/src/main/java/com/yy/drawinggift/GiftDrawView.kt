package com.yy.drawinggift

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.DocumentsContract
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sqrt

class GiftDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IGiftDrawView {

    companion object {

        const val TAG = "GiftDrawView"
        const val ACTION_DOWN = 1
        const val ACTION_MOVE = 2
        const val ACTION_UP = 3
        const val ACTION_DISAPPEAR = 10
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
    private var maxDistance = 8
    private var pathMeasure = PathMeasure()
    private var path = Path()

    private var backgroundBitmap: Bitmap? = null
    private val mCanvas = Canvas()
    private var isDrawing = true
    private var isDisappear = false
    private var isClear = false
    private val scaleAnimator = ValueAnimator.ofFloat(1.0F, 1.5F)
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
                ACTION_UP -> {
                    val point = msg.obj as PointF
                    touchUp(point.x, point.y)
                }
                ACTION_DISAPPEAR -> handleDisappear()
            }
        }
    }

    private fun touchDown() {
        Log.d(TAG, "touchDown")
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
            mX - iconWidth + maxDistance, mY - iconHeight + maxDistance,
            mX + iconWidth - maxDistance,
            mY + iconHeight - maxDistance
        )
        if (rect.contains(x, y)) {
            return
        }
        if (abs(x - mX) > iconWidth || abs(y - mY) > iconHeight) {
            path.reset()
            path.moveTo(mX, mY)
            path.quadTo((mX + x) / 2, (mY + y) / 2, x, y)
            pathMeasure.setPath(path, false)
            var length = pathMeasure.length
            var pos = floatArrayOf(x, y)
            while (abs(pos[0] - mX) > iconWidth || abs(pos[1] - mY) > iconHeight) {
                length -= 2
                pathMeasure.getPosTan(length, pos, null)
            }
            if (mGiftRect.contains(pos[0], pos[1])) {
                mX = pos[0]
                mY = pos[1]
                mCurrentNode = GiftNode(mX, mY, iconUrl)
                giftNodeLine!!.add(mCurrentNode!!)
                invalidate()
                Log.d(TAG, "measure ${pos[0]} ${pos[1]}")
            }
        } else {
            if (mGiftRect.contains(x, y)) {
                mX = x
                mY = y
                Log.d(TAG, "event $x $y")
                mCurrentNode = GiftNode(mX, mY, iconUrl)
                giftNodeLine!!.add(mCurrentNode!!)
                invalidate()
            }
        }
    }

    private fun touchUp(x: Float, y: Float) {
        setGiftRectIfNeed()
        if (mGiftRect.contains(x, y)) {
            mX = x
            mY = y
            mCurrentNode = GiftNode(mX, mY, iconUrl)
            giftNodeLine!!.add(mCurrentNode!!)
            invalidate()
        }
        giftListNodes.add(giftNodeLine!!)
        mCurrentNode = null
    }


    private fun handleDisappear() {
        isDisappear = true
        scaleAnimator.duration = 800
        scaleAnimator.addUpdateListener {
            val scale = it.animatedValue as Float
            giftListNodes.forEach { list ->
                list.forEach { node ->
                    node.matrix.setTranslate(node.x - iconWidth / 2F, node.y - iconHeight / 2F)
                    node.matrix.postScale(scale, scale)
                }
            }
            invalidate()
        }
        scaleAnimator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        initBackgroundBitmap()
    }

    private fun initBackgroundBitmap() {
        backgroundBitmap?.recycle()
        backgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444)
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
                mHandler.sendMessage(
                    mHandler.obtainMessage(ACTION_MOVE, PointF(event.x, event.y))
                )
            }
            MotionEvent.ACTION_UP -> {
                mHandler.sendMessage(mHandler.obtainMessage(ACTION_UP, PointF(event.x, event.y)))
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isDrawing) {
            if (isClear) {
                initBackgroundBitmap()
                isClear = false
                backgroundBitmap?.let {
                    canvas?.drawBitmap(it, 0F, 0F, null)
                }
            } else {
                drawIcon(canvas, mCurrentNode)
            }
        } else {
            if (!isDisappear) {
                drawIcon(canvas, mCurrentNode)
            } else {
                drawIconDisappear(canvas)
            }
        }

    }

    private fun drawIcon(canvas: Canvas?, giftNode: GiftNode?) {
        giftNode?.let {
            iconBitmap?.let { icon ->
                it.matrix.reset()
                it.matrix.postTranslate(it.x - iconWidth / 2F, it.y - iconHeight / 2F)
                mCanvas.drawBitmap(
                    icon, it.matrix,
                    null
                )
            }
        }
        backgroundBitmap?.let {
            canvas?.drawBitmap(it, 0F, 0F, null)
        }
    }

    private fun drawIconDisappear(canvas: Canvas?) {
        giftListNodes.forEach {
            it.forEach { node ->
                canvas?.save()
                canvas?.drawBitmap(iconBitmap, node.matrix, null)
                canvas?.restore()
            }
        }
    }

    override fun setIconBitMap(iconBitmap: Bitmap?) {
        this.iconBitmap = iconBitmap
        if (iconBitmap == null) {
            iconWidth = 0
            iconHeight = 0
        } else {
            iconWidth = iconBitmap.width
            iconHeight = iconBitmap.height
        }
        emptyGiftRect()
    }

    override fun setIconUrl(iconUrl: String) {
    }

    override fun setIsDrawing(isDrawing: Boolean) {
        this.isDrawing = isDrawing
    }

    override fun clearBoard(isClearData: Boolean) {
        isDrawing = true
        isClear = true
        invalidate()
    }

    override fun replay() {
        clearBoard()
        thread {
            isDrawing = false
            isDisappear = false
            giftListNodes.forEach {
                it.forEach { node ->
                    this@GiftDrawView.mCurrentNode = node
                    this@GiftDrawView.postInvalidate()
                    Thread.sleep(50)
                }
            }
            mHandler.sendEmptyMessage(ACTION_DISAPPEAR)
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