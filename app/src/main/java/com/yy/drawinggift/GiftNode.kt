package com.yy.drawinggift

import android.graphics.Matrix


data class GiftNode(
    var x: Float = 0F,
    var y: Float = 0F,
    var icon: String? = null
    , var matrix: Matrix = Matrix()
)
