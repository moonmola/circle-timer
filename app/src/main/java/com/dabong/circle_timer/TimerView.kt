package com.dabong.circle_timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.getSystemService
import com.bumptech.glide.Glide
import com.dabong.circle_timer.databinding.ViewTimerBinding
import java.util.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt


class TimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {
    private val binding: ViewTimerBinding = ViewTimerBinding.inflate(LayoutInflater.from(context),this,true)

    init {
        setWillNotDraw(false)

        Glide.with(context)
            .load(R.drawable.start1)
            .into(binding.startButton)
        binding.startButton.setOnClickListener {
            if (isRunning) {
                stop()
            } else {
                start()
            }
        }
    }

    private val alarmManager by lazy {
        context?.let {
            getSystemService(
                it,
                AlarmManager::class.java
            )
        } as AlarmManager
    }
    private val pendingIntent by lazy {
        PendingIntent.getBroadcast(
            context, AlarmReceiver.NOTIFICATION_ID, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private var horizontalCenter: Float = 0f
    private var verticalCenter: Float = 0f

    var currentPercentage = 0

    private var x1 = 0f
    private var y1 = 0f


    var isRunning = false
    var isMinute = false
    private var radius = 300
    private var enableRadius = 600
    private var currentX = 0f
    private var angleDown = 0.0
    private var angleCurr = 0.0

    var thread: Thread? = null

    private val ovalSpace = RectF()
    private val parentOvalSpace = RectF()

    private val parentArcColor =
        context.resources?.getColor(R.color.gray, null) ?: Color.GRAY
    private val fillArcColor =
        context.resources?.getColor(R.color.colorPrimaryDark, null) ?: Color.GREEN

    private val parentArcPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = parentArcColor
        strokeWidth = 40f
    }

    private val fillArcPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = fillArcColor
        strokeWidth = 40f
        strokeCap = Paint.Cap.ROUND
    }
    private val fontPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        textSize = 100f
        textAlign = Paint.Align.CENTER
    }



    override fun onDraw(canvas: Canvas) {
        setSpace()
        canvas.let {
            drawBackgroundArc(it)
            drawInnerArc(it)
            it.drawText(
                String.format(Locale.US, "%02d:%02d", currentPercentage/60, currentPercentage%60),
                horizontalCenter,
                verticalCenter + 30,
                fontPaint
            )
        }
    }

    private fun drawBackgroundArc(it: Canvas) {
        it.drawArc(parentOvalSpace, 0f, 360f, true, parentArcPaint)

    }

    private fun drawInnerArc(canvas: Canvas) {
        canvas.drawArc(ovalSpace, -90f, angleCurr.toFloat(), true, fillArcPaint)
    }

    private fun setSpace() {
        horizontalCenter = (width.div(2)).toFloat()
        verticalCenter = (height.div(2)).toFloat()
        ovalSpace.set(
            horizontalCenter - radius,
            verticalCenter - radius,
            horizontalCenter + radius,
            verticalCenter + radius
        )
        parentOvalSpace.set(
            horizontalCenter - radius-40f,
            verticalCenter - radius-40f,
            horizontalCenter + radius+40f,
            verticalCenter + radius+40f
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mX = event.x
        val mY = event.y
        if (Math.pow(enableRadius.toDouble(), 2.0) > (Math.pow((horizontalCenter - mX).toDouble(), 2.0) + Math.pow((verticalCenter - mY).toDouble(), 2.0))) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> touchStart(mX, mY)
                MotionEvent.ACTION_MOVE -> touchMove(mX, mY)
                MotionEvent.ACTION_UP -> {}
            }
            invalidate()

        }
        return true
    }

    private fun touchStart(mX: Float, mY: Float) {
        x1 = mX
        y1 = mY
        angleDown = computeAngle(x1 - horizontalCenter, verticalCenter - y1)
    }

    private fun touchMove(mX: Float, mY: Float) {

        val dx = mX - horizontalCenter
        val dy = verticalCenter - mY

        if ((dx != 0f) && (dy != 0f)) {
            angleCurr = (computeAngle(dx, dy)/6).roundToInt()*6.toDouble()
            angleCurr = (computeAngle(dx, dy)).roundToInt().toDouble()

            currentX = mX
            currentX = angleCurr.toFloat()

            currentPercentage = (angleCurr/6).toInt()
            if (isMinute) {
                currentPercentage = (angleCurr*10).toInt()
            }


        }


    }

    private fun computeAngle(x: Float, y: Float): Double {
        val RADS_TO_DEGREES = 180 / PI
        var result = atan2(x, y) * RADS_TO_DEGREES
        if (result < 0) {
            result += 360
        }
        return result

    }

    fun start() {
        isRunning = true
        if (isRunning && currentPercentage > 0) {
            setAlarm(currentPercentage.toLong() * 1000)
            Glide.with(binding.root)
                .load(R.drawable.timer)
                .into(binding.startButton)
        }
        thread = Thread(Runnable {
            try {
                while (isRunning && currentPercentage > 0) {
                    Thread.sleep(1000)
                    currentPercentage -= 1
                    if(currentPercentage == 0) {
                        binding.startButton.post {
                            Glide.with(binding.root)
                                .load(R.drawable.start1)
                                .into(binding.startButton)
                        }
                        isRunning = false
                    }
                    angleCurr = (currentPercentage*6).toDouble()
                    if (isMinute) {
                        angleCurr = (currentPercentage/10).toDouble()
                    }
                    invalidate()
                }
            } catch (e: InterruptedException) {

            }
        })
        thread?.start()

    }

    private fun stop() {
        isRunning = false
        thread = null
        cancelAlarm()
        Glide.with(binding.root)
            .load(R.drawable.start)
            .into(binding.startButton)
    }

    private fun setAlarm(timer: Long) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + timer,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        alarmManager.cancel(pendingIntent)
    }

    fun convertTimeUnit() {
        if (isRunning) {
            return
        }
        isMinute = !isMinute

        if (isMinute) {
            currentPercentage = (angleCurr*10).toInt()
        } else {
            currentPercentage = (angleCurr/6).toInt()
        }
        invalidate()
    }

}