package com.yy.drawinggift

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs


class GiftDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IGiftDrawView {

    companion object {
        const val TAG = "GiftDrawView"
        const val ACTION_DOWN = 1
        const val ACTION_MOVE = 2
        const val ACTION_UP = 3
        const val ACTION_UNDO = 4
        const val ACTION_DRAWING = 5
        const val ACTION_REPLAY = 6
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
    private val mDisappearPaint = Paint()
    private val scaleAnimator by lazy {
        ValueAnimator.ofFloat(1.0F, 2F).apply {
            duration = 800
            addUpdateListener {
                val scale = it.animatedValue as Float
                mDisappearPaint.alpha = ((2.0F - scale) * 255).toInt()
                giftListNodes.forEach { list ->
                    list.forEach { node ->
                        node.matrix.reset()
                        node.matrix.preTranslate(node.x - iconWidth / 2F, node.y - iconHeight / 2F)
                        node.matrix.postScale(scale, scale, node.x, node.y)
                    }
                }
                invalidate()
            }
        }
    }
    private lateinit var giftNumListener: (Int) -> Unit

    private val handleNodeThread by lazy {
        HandlerThread(TAG).apply {
            start()
        }
    }

    private val mHandler by lazy {
        object : Handler(handleNodeThread.looper) {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                when (msg?.what) {
                    ACTION_DOWN -> {
                        val point = msg.obj as PointF
                        touchDown(point.x, point.y)
                    }
                    ACTION_MOVE -> {
                        val point = msg.obj as PointF
                        touchMove(point.x, point.y)
                    }
                    ACTION_UP -> {
                        val point = msg.obj as PointF
                        touchUp(point.x, point.y)
                    }
                    ACTION_DRAWING -> {
                        invalidate()
                    }
                    ACTION_UNDO -> undoDraw()
                    ACTION_REPLAY -> doReplay()
                }
            }
        }
    }


    private fun touchDown(x: Float, y: Float) {
        Log.d(TAG, "touchDown")
        mX = x
        mY = y
        val halfIcWidth = iconWidth / 2F
        val halfIcHeight = iconHeight / 2F
        //边界检测
        mX = if (mX < halfIcWidth) halfIcWidth else mX
        mX = if (mX > mWidth - halfIcWidth) mWidth - halfIcWidth else mX
        mY = if (mY < halfIcHeight) halfIcHeight else mY
        mY = if (mY > mHeight - halfIcHeight) mHeight - halfIcHeight else mY
        giftNodeLine = mutableListOf()
        GiftNode(mX, mY, iconUrl).let {
            giftNodeLine!!.add(it)
            drawIcon(it)
        }
        postInvalidate()
    }

    private fun touchMove(x: Float, y: Float) {
        setGiftRectIfNeed()
        if (abs(abs(abs(x - mX)) - iconWidth.toFloat()) < maxDistance / 4
            && abs(abs(y - mY) - iconHeight.toFloat()) < maxDistance / 4
        ) {
            Log.d(TAG, "in area node $x  $y")
            if (mGiftRect.contains(x, y)) {
                mX = x
                mY = y
                GiftNode(mX, mY, iconUrl).let {
                    giftNodeLine!!.add(it)
                    drawIcon(it)
                }
                postInvalidate()
            }
            return
        }
        if (abs(x - mX) > iconWidth + maxDistance || abs(y - mY) > iconHeight + maxDistance) {
            path.reset()
            path.moveTo(mX, mY)
            path.quadTo((mX + x) / 2, (mY + y) / 2, x, y)
            pathMeasure.setPath(path, false)
            var length = pathMeasure.length
            val pos = floatArrayOf(x, y)
            while (abs(pos[0] - mX) > iconWidth || abs(pos[1] - mY) > iconHeight) {
                length -= 2
                pathMeasure.getPosTan(length, pos, null)
            }
            if (mGiftRect.contains(pos[0], pos[1])) {
                mX = pos[0]
                mY = pos[1]
                GiftNode(mX, mY, iconUrl).let {
                    giftNodeLine!!.add(it)
                    drawIcon(it)
                }
                postInvalidate()
                Log.d(TAG, "measure ${pos[0]} ${pos[1]}")
            }
        }
    }

    private fun touchUp(x: Float, y: Float) {
        touchMove(x, y)
        giftListNodes.add(giftNodeLine!!)
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
                mHandler.sendMessage(
                    mHandler.obtainMessage(ACTION_DOWN, PointF(event.x, event.y))
                )
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
                isClear = false
                initBackgroundBitmap()
                canvas?.drawBitmap(backgroundBitmap, 0F, 0F, null)
            } else {
                backgroundBitmap?.let {
                    canvas?.drawBitmap(it, 0F, 0F, null)
                }
            }
        } else {
            if (!isDisappear) {
                backgroundBitmap?.let {
                    canvas?.drawBitmap(it, 0F, 0F, null)
                }
            } else {
                drawIconDisappear(canvas)
            }
        }

    }

    private fun drawIcon(giftNode: GiftNode) {
        giftNode.let {
            iconBitmap?.let { icon ->
                it.matrix.reset()
                it.matrix.postTranslate(it.x - iconWidth / 2F, it.y - iconHeight / 2F)
                mCanvas.drawBitmap(
                    icon, it.matrix, null
                )
            }
        }
    }

    private fun drawIconDisappear(canvas: Canvas?) {
        giftListNodes.forEach {
            it.forEach { node ->
                canvas?.save()
                canvas?.drawBitmap(iconBitmap, node.matrix, mDisappearPaint)
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
        //设置url
    }

    override fun setIsDrawing(isDrawing: Boolean) {
        this.isDrawing = isDrawing
    }

    override fun clearBoard() {
        isDrawing = true
        isClear = true
        invalidate()
    }

    override fun undo(): Boolean {
        val lastIndex = giftListNodes.lastIndex
        if (lastIndex == -1) {
            return false
        }
        giftListNodes.removeAt(lastIndex)
        mHandler.sendEmptyMessage(ACTION_UNDO)
        return true
    }

    private fun undoDraw() {
        initBackgroundBitmap()
        giftListNodes.forEach {
            it.forEach { node ->
                drawIcon(node)
            }
        }
        postInvalidate()
    }

    override fun replay() {
        mHandler.sendEmptyMessage(ACTION_REPLAY)
    }

    private fun doReplay() {
        isDrawing = false
        isDisappear = false
        giftListNodes.forEach {
            it.forEach { node ->
                drawIcon(node)
                postInvalidate()
                Thread.sleep(50)
            }
        }
        isDisappear = true
        scaleAnimator?.start()
    }

    override fun clearData() {
        giftListNodes.forEach {
            it.clear()
        }
        giftListNodes.clear()
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearData()
        scaleAnimator.cancel()
        backgroundBitmap?.recycle()
        iconBitmap?.recycle()
        handleNodeThread.quitSafely()
    }
}