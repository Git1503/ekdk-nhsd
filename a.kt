package com.example.ekdk

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class a {
    companion object {
        private const val PREFS_NAME = "ActivationPrefs"
        private val client = OkHttpClient()

        fun show(activity: Activity) {
            val dialog = Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
            }

            val outerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
            }

            val card = CardView(activity).apply {
                radius = 30f
                cardElevation = 12f
                setCardBackgroundColor(Color.parseColor("#F2EAF4"))
                useCompatPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(30, 60, 30, 60) }
            }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 70, 60, 60)
                dividerDrawable = ColorDrawable(Color.TRANSPARENT)
                showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
                dividerPadding = 20
            }

            val deviceId = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // ---------------- EditText ----------------
            val editKey = createEditText(activity, "Enter 🔑🔑").apply {
                setText(prefs.getString("saved_key", ""))
            }

            // Top row: Copy ID button + Input box
            val idButton = createButton(activity, "COPY ID", "#0099CC") {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("DeviceID", deviceId))
                Toast.makeText(activity, "Device ID copied", Toast.LENGTH_SHORT).show()
            }.apply {
                // Make button slightly smaller
                setPadding(35, 25, 35, 25)
                minimumHeight = 100
                textSize = 18f
            }

            innerLayout.addView(createRow(
                activity,
                idButton,
                editKey,
                weights = floatArrayOf(1f, 2.5f),
                margin = 20
            ))

            // Middle: Status text with fade-in
            val statusText = TextView(activity).apply {
                text = ""
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(Color.RED)
            }
            innerLayout.addView(statusText)

            // Bottom row: Remember key + Activate button
            val checkBox = CheckBox(activity).apply { text = "Remember key"; setTextColor(Color.BLACK) }
            val activateBtn = createButton(activity, "ACTIVATE", "#00C6A3") {
                val key = editKey.text.toString().trim()
                if (key.isEmpty()) {
                    updateStatusText(statusText, "Please enter key")
                    return@createButton
                }
                updateStatusText(statusText, "Verifying...")
                if (!isInternetAvailable(activity)) {
                    updateStatusText(statusText, "Connection failed")
                    return@createButton
                }
                verifyKey(activity, deviceId, key, dialog, statusText, checkBox.isChecked)
            }

            innerLayout.addView(createRow(
                activity,
                checkBox,
                activateBtn,
                weights = floatArrayOf(1.2f, 1.8f),
                margin = 20
            ))

            card.addView(innerLayout)
            outerLayout.addView(card)
            dialog.setContentView(outerLayout)

            dialog.window?.apply {
                val metrics = activity.resources.displayMetrics
                setLayout((metrics.widthPixels * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setGravity(Gravity.CENTER)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.4f)
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                // 🚫 Disable screenshots and screen recording
                addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Animation for dialog appearance
            outerLayout.scaleX = 0.8f
            outerLayout.scaleY = 0.8f
            outerLayout.alpha = 0f
            outerLayout.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).start()

            dialog.show()
        }

        // ---------------- Helper functions ----------------
        private fun createButton(activity: Activity, text: String, colorHex: String, onClick: () -> Unit) = Button(activity).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(50, 35, 50, 35)
            minimumHeight = 120
            background = GradientDrawable().apply {
                cornerRadius = 25f
                setColor(Color.parseColor(colorHex))
            }

            setOnClickListener {
                // Animate button click: shrink then grow
                this.animate().scaleX(0.95f).scaleY(0.95f).setDuration(50).withEndAction {
                    this.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                }.start()

                // Execute original click
                onClick()
            }
        }

        private fun createEditText(activity: Activity, hint: String) = EditText(activity).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 18f
            minimumHeight = 120
            setPadding(35, 30, 35, 30)
            background = GradientDrawable().apply {
                cornerRadius = 25f
                setColor(Color.parseColor("#DADADA")) // dim input
            }
        }

        private fun createRow(activity: Activity, left: android.view.View, right: android.view.View, weights: FloatArray = floatArrayOf(1f, 2f), margin: Int = 0): LinearLayout {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[0]).apply { rightMargin = margin })
                addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[1]))
            }
        }

        private fun isInternetAvailable(activity: Activity): Boolean {
            val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        // ---------------- Fade-in status text ----------------
        private fun updateStatusText(statusText: TextView, message: String, color: Int = Color.RED) {
            statusText.alpha = 0f
            statusText.setTextColor(color)
            statusText.text = message
            statusText.animate().alpha(1f).setDuration(300).start()
        }

        private fun verifyKey(activity: Activity, deviceId: String, key: String, dialog: Dialog, statusText: TextView, remember: Boolean) {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/Git1503/upn-1/main/temp.json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity.runOnUiThread { updateStatusText(statusText, "Connection failed") }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        activity.runOnUiThread { updateStatusText(statusText, "Server error") }
                        return
                    }

                    val body = response.body?.string()?.trim()
                    if (body.isNullOrEmpty()) {
                        activity.runOnUiThread { updateStatusText(statusText, "Empty response") }
                        return
                    }

                    try {
                        val jsonArray = JSONArray(body)
                        var valid = false
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val jsonKey = obj.getString("key")
                            val jsonDeviceId = obj.getString("deviceid")
                            val expiry = obj.getString("expiry")
                            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                            val expiryDate = sdf.parse(expiry)
                            if (jsonKey.equals(key, true) && jsonDeviceId.equals(deviceId, true) &&
                                expiryDate != null && Date().before(expiryDate)
                            ) {
                                valid = true
                                break
                            }
                        }

                        activity.runOnUiThread {
                            if (valid) {
                                Toast.makeText(activity, "Activation Successful!", Toast.LENGTH_SHORT).show()
                                if (remember) activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                    .edit().putString("saved_key", key).apply()
                                dialog.dismiss()
                            } else updateStatusText(statusText, "Invalid key")
                        }
                    } catch (e: Exception) {
                        activity.runOnUiThread { updateStatusText(statusText, "Invalid JSON format") }
                    }
                }
            })
        }
    }
}