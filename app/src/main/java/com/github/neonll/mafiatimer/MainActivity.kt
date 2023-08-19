package com.github.neonll.mafiatimer

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.neonll.mafiatimer.ui.theme.MafiaTimerTheme
import com.github.neonll.mafiatimer.ui.theme.ColorBlue
import com.github.neonll.mafiatimer.ui.theme.ColorRed
import kotlin.math.min


class MainActivity : ComponentActivity() {

    var countdownTime = 60000L // 1 minute in milliseconds
    var isTimerRunning by mutableStateOf(false)
    var remainingTime by mutableStateOf(countdownTime)
    var isPaused by mutableStateOf(false)
    var pausedTime by mutableStateOf(0L)

    private var countDownTimer: CountDownTimer? = null
    private var mAudioManager: AudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MafiaTimerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager

                        var isNextTrack by remember { mutableStateOf(false) }

                        val paint = Paint()

                        Canvas(modifier = Modifier.size(140.dp)) {
                            val logo = BitmapFactory.decodeResource(resources,R.raw.mafia_logo)
                            val aspectRatio = logo.width.toFloat() / logo.height.toFloat()

                            val targetWidth = size.width
                            val targetHeight = targetWidth / aspectRatio

                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawBitmap(logo, null, RectF(0f, 0f, targetWidth, targetHeight), paint)
                            }

                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = isNextTrack,
                                onCheckedChange = {
                                    isNextTrack = it
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Next track on Reset",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Canvas(
                            modifier = Modifier
                                .size(260.dp)
                        ) {
                            val circleRadius = min(size.width, size.height) / 2
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val startAngle = -90f
                            val sweepAngle = 360f * (remainingTime.toFloat() / countdownTime.toFloat())

                            drawArc(
                                color = if (remainingTime / 1000 <= 10) ColorRed else ColorBlue,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(centerX - circleRadius, centerY - circleRadius),
                                size = androidx.compose.ui.geometry.Size(circleRadius * 2, circleRadius * 2),
                                style = Stroke(20f)
                            )

                            paint.apply {
                                isAntiAlias = true
                                textSize = 300f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }

                            drawIntoCanvas { canvas ->
                                val text = "${remainingTime / 1000}"
                                if (remainingTime / 1000 <= 10) {
                                    paint.apply {
                                        color = ColorRed.hashCode()
                                    }
                                } else {
                                    paint.apply {
                                        color = Color.White.hashCode()
                                    }
                                }
                                canvas.nativeCanvas.drawText(text, centerX, centerY + 100, paint)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                modifier = Modifier.size(100.dp),
                                shape = RoundedCornerShape(20.dp),
                                onClick = {
                                    if (isTimerRunning) {
                                        countDownTimer?.cancel()
                                        isTimerRunning = false
                                        isPaused = true
                                        pausedTime = remainingTime
                                        mPlayerPause()
                                    } else {
                                        if (isPaused) {
                                            initializeTimer(pausedTime)
                                            countDownTimer?.start()
                                            isTimerRunning = true
                                            isPaused = false
                                            mPlayerPlay(false)
                                        } else {
                                            if (remainingTime > 0) {
                                                countDownTimer?.start()
                                                isTimerRunning = true
                                                mPlayerPlay(isNextTrack)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Image(
                                    painter = painterResource(id = if (isTimerRunning) R.drawable.ic_pause else R.drawable.ic_start),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                modifier = Modifier.size(100.dp),
                                shape = RoundedCornerShape(20.dp),
                                onClick = {
                                    countDownTimer?.cancel()
                                    countdownTime = 60000L
                                    initializeTimer(countdownTime)
                                    remainingTime = countdownTime
                                    pausedTime = countdownTime
                                    isTimerRunning = false
                                    isPaused = false
                                    mPlayerPause()
                                }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_reset),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                modifier = Modifier.size(100.dp),
                                shape = RoundedCornerShape(20.dp),
                                onClick = {
                                    countDownTimer?.cancel()
                                    countdownTime = 30000L
                                    initializeTimer(countdownTime)
                                    remainingTime = countdownTime
                                    pausedTime = countdownTime
                                    isTimerRunning = false
                                    isPaused = false
                                    mPlayerPause()
                                }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_reset_30),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                    }
                }
            }
        }


        initializeTimer(remainingTime)

    }

    private fun initializeTimer(timeLimit: Long) {
        countDownTimer = object : CountDownTimer(timeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
            }

            override fun onFinish() {
                remainingTime = 0
                isTimerRunning = false
                mPlayerPause()
            }
        }
    }

    private fun mPlayerPause() {
        val event = KeyEvent(
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PAUSE
        )
        mAudioManager?.dispatchMediaKeyEvent(event)
    }

    private fun mPlayerPlay(isNextTrack: Boolean) {
        val event = KeyEvent(
            KeyEvent.ACTION_DOWN,
            if (isNextTrack) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PLAY
        )
        mAudioManager?.dispatchMediaKeyEvent(event)
    }
}
