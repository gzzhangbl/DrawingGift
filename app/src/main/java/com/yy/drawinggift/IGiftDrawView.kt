package com.yy.drawinggift

import android.graphics.Bitmap

interface IGiftDrawView {
    /**
     * 设置礼物icon bitmap
     */
    fun setIconBitMap(iconBitmap: Bitmap?)

    /**
     * 设置礼物的icon Url
     */
    fun setIconUrl(iconUrl: String)

    /**
     * 设置是否可以绘制
     */
    fun setIsDrawing(isDrawing: Boolean)

    /**
     * 清屏
     */
    fun clearBoard(isClearData: Boolean = true)

    /**
     * 重放
     */
    fun replay()
}