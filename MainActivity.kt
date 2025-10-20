package com.example.ekdk

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.graphics.Color
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show activation dialog immediately
        a.show(this)

        // Optional: you can show a placeholder layout while waiting for activation
        val placeholderLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            val welcomeText = TextView(this@MainActivity).apply {
                text = "Welcome!"
                textSize = 24f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            addView(welcomeText)
        }

        setContentView(placeholderLayout)
    }
}