package com.github.neonll.mafiatimer

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.neonll.mafiatimer.ui.theme.ColorBlue
import com.github.neonll.mafiatimer.ui.theme.ColorRed
import com.github.neonll.mafiatimer.ui.theme.MafiaTimerTheme
import kotlin.math.min


class MainActivity : ComponentActivity() {

    var countdownTime = 60000L // 1 minute in milliseconds
    var isTimerRunning by mutableStateOf(false)
    var remainingTime by mutableStateOf(countdownTime)
    var isPaused by mutableStateOf(false)
    var pausedTime by mutableStateOf(0L)
    var isNight by mutableStateOf(false)

    private var countDownTimer: CountDownTimer? = null
    private var mAudioManager: AudioManager? = null
    private var mediaPlayer10Sec: MediaPlayer? = null

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
                        val paint = Paint()

                        cnvLogo(paint = paint)
                        Spacer(modifier = Modifier.height(30.dp))
                        cnvTimer(paint = paint)
                        Spacer(modifier = Modifier.height(30.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            btnDay()
                            Spacer(modifier = Modifier.width(16.dp))
                            btnNight()
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            btnPlay()
                            Spacer(modifier = Modifier.width(16.dp))
                            btnReset(60)
                            Spacer(modifier = Modifier.width(16.dp))
                            btnReset(30)
                        }

                    }
                }
            }
        }

        initializeTimer(remainingTime)

    }

    @Composable
    fun cnvLogo(paint: Paint) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val logo = BitmapFactory.decodeResource(resources,R.raw.mafia_logo)
            val aspectRatio = logo.width.toFloat() / logo.height.toFloat()

            val targetWidth = size.width
            val targetHeight = targetWidth / aspectRatio

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawBitmap(logo, null, RectF(0f, 0f, targetWidth, targetHeight), paint)
            }

        }
    }

    @Composable
    fun cnvTimer(paint: Paint) {
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

    }
    
    @Composable
    fun btnDay() {
        Button(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(20.dp),
            onClick = {
                mPlayerPause()
                isNight = false
                isPaused = false
                if (mediaPlayer10Sec != null) {
                    mediaPlayer10Sec!!.release()
                    mediaPlayer10Sec = null
                }
            },
            enabled = isNight
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_day),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

    }

    @Composable
    fun btnNight() {
        Button(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(20.dp),
            onClick = {
                mPlayerPlay(true)
                isNight = true
                countDownTimer?.cancel()
                pausedTime = countdownTime
                isTimerRunning = false
                isPaused = false
                if (mediaPlayer10Sec != null) {
                    mediaPlayer10Sec!!.release()
                    mediaPlayer10Sec = null
                }
            },
            enabled = !isNight
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_night),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    fun btnPlay() {
        Button(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = !isNight,
            onClick = {
                if (isTimerRunning) {
                    if (mediaPlayer10Sec != null && mediaPlayer10Sec!!.isPlaying) {
                        mediaPlayer10Sec!!.pause()
                    }
                    countDownTimer?.cancel()
                    isTimerRunning = false
                    isPaused = true
                    pausedTime = remainingTime
                } else {
                    if (isPaused) {
                        initializeTimer(pausedTime)
                        countDownTimer?.start()
                        isTimerRunning = true
                        isPaused = false
                        if (mediaPlayer10Sec != null && !mediaPlayer10Sec!!.isPlaying && mediaPlayer10Sec!!.currentPosition != 0) {
                            mediaPlayer10Sec!!.start()
                        }
                    } else {
                        if (remainingTime > 0) {
                            MediaPlayer.create(baseContext, R.raw.sound_start).start()
                            countDownTimer?.start()
                            isTimerRunning = true
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

    }

    @Composable
    fun btnReset(timeInSeconds: Int) {
        val iconId = if (timeInSeconds == 30) R.drawable.ic_reset_30 else R.drawable.ic_reset_60
        Button(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = !isNight,
            onClick = {
                if (mediaPlayer10Sec != null && mediaPlayer10Sec!!.isPlaying) {
                    mediaPlayer10Sec!!.release()
                    mediaPlayer10Sec = null
                }
                countDownTimer?.cancel()
                countdownTime = timeInSeconds * 1000L
                initializeTimer(countdownTime)
                remainingTime = countdownTime
                pausedTime = countdownTime
                isTimerRunning = false
                isPaused = false
            }
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

    }

    private fun initializeTimer(timeLimit: Long) {
        countDownTimer = object : CountDownTimer(timeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                if (millisUntilFinished / 1000 == 10L && mediaPlayer10Sec == null) {
                    mediaPlayer10Sec = MediaPlayer.create(baseContext, R.raw.sound_10sec)
                    mediaPlayer10Sec!!.start()
                }
            }

            override fun onFinish() {
                remainingTime = 0
                isTimerRunning = false
                if (mediaPlayer10Sec != null) {
                    mediaPlayer10Sec!!.release()
                    mediaPlayer10Sec = null
                }
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

