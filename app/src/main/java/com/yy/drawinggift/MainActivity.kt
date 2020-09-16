package com.yy.drawinggift

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        draw_view.setIconBitMap(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.bangbangtang
            )

        )
        btb_clear.setOnClickListener {
            draw_view.clearBoard()
//            draw_view.clearData()
            draw_view.setIsDrawing(true)
        }

        btb_undo.setOnClickListener {
//            draw_view.clearBoard()
            draw_view.undo()
        }
        btb_replay.setOnClickListener {
            draw_view.clearBoard()
            draw_view.replay()
        }
    }
}
