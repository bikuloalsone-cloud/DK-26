
package com.dk26.football

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

enum class GameCommand {
    PASS_SHORT,
    PASS_THROUGH,
    PASS_LONG,
    CROSS,
    SHOOT,
    SHOOT_POWER,
    SHOOT_PLACED,
    SHOOT_VOLLEY,
    TACKLE_STANDING,
    TACKLE_SLIDE,
    PRESSING_ON,
    PRESSING_OFF
}

fun interface GameCommandListener {
    fun onGameCommand(command: GameCommand)
}

class GameControlsOverlay(
    activity: Activity,
    private val commandListener: GameCommandListener
) : FrameLayout(activity) {

    init {
        isClickable = false

        val leftPanel = createPanel(activity)
        val rightPanel = createPanel(activity)

        addButton(leftPanel, "PASSE", GameCommand.PASS_SHORT)
        addButton(leftPanel, "PASSE EN PROFONDEUR", GameCommand.PASS_THROUGH)
        addButton(leftPanel, "PASSE LONGUE", GameCommand.PASS_LONG)
        addButton(leftPanel, "CENTRE", GameCommand.CROSS)
        addButton(leftPanel, "TACLE", GameCommand.TACKLE_STANDING)
        addButton(leftPanel, "GLISSADE", GameCommand.TACKLE_SLIDE)

        addButton(rightPanel, "TIR", GameCommand.SHOOT)
        addButton(rightPanel, "TIR PUISSANT", GameCommand.SHOOT_POWER)
        addButton(rightPanel, "TIR PLACE", GameCommand.SHOOT_PLACED)
        addButton(rightPanel, "VOLEE", GameCommand.SHOOT_VOLLEY)
        addButton(rightPanel, "PRESSING", GameCommand.PRESSING_ON)
        addButton(rightPanel, "ARRETER PRESSING", GameCommand.PRESSING_OFF)

        val leftParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.START
        )
        leftParams.setMargins(18, 0, 0, 18)

        val rightParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )
        rightParams.setMargins(0, 0, 18, 18)

        addView(leftPanel, leftParams)
        addView(rightPanel, rightParams)
    }

    private fun createPanel(activity: Activity): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)

            background = GradientDrawable().apply {
                setColor(Color.argb(115, 5, 12, 8))
                cornerRadius = 18f
            }
        }
    }

    private fun addButton(
        panel: LinearLayout,
        label: String,
        command: GameCommand
    ) {
        val button = Button(context).apply {
            text = label
            textSize = 9f
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(5, 2, 5, 2)

            background = GradientDrawable().apply {
                setColor(Color.argb(205, 20, 65, 42))
                cornerRadius = 12f
                setStroke(1, Color.argb(190, 210, 230, 210))
            }

            setOnClickListener {
                commandListener.onGameCommand(command)
            }
        }

        val params = LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            42
        )
        params.setMargins(3, 3, 3, 3)
        panel.addView(button, params)
    }
}

class GameActivity : Activity(), GameCommandListener {

    private lateinit var gameView: GLSurfaceView
    private lateinit var renderer: DK26Renderer
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this)

        renderer = DK26Renderer()

        gameView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            preserveEGLContextOnPause = true
        }

        root.addView(
            gameView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        statusText = TextView(this).apply {
            text = "DK 26  |  MATCH"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 10, 16, 10)

            background = GradientDrawable().apply {
                setColor(Color.argb(175, 5, 20, 12))
                cornerRadius = 12f
            }
        }

        val statusParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        )
        statusParams.topMargin = 12

        root.addView(statusText, statusParams)

        val controls = GameControlsOverlay(this, this)

        root.addView(
            controls,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)
    }

    override fun onGameCommand(command: GameCommand) {

        when (command) {

            // PASSES
            GameCommand.PASS_SHORT -> {
                executeBallCommand("PASSE COURTE") {
                    renderer.passBall(0f, 0.35f)
                }
            }

            GameCommand.PASS_THROUGH -> {
                executeBallCommand("PASSE EN PROFONDEUR") {
                    renderer.throughPassBall(0f, 0.55f)
                }
            }

            GameCommand.PASS_LONG -> {
                executeBallCommand("PASSE LONGUE") {
                    renderer.longPassBall(0f, 0.72f)
                }
            }

            GameCommand.CROSS -> {
                executeBallCommand("CENTRE") {
                    renderer.crossBall(0.65f, 0.68f)
                }
            }

            // TIRS
            GameCommand.SHOOT -> {
                executeBallCommand("TIR") {
                    renderer.shootBall(0f, 0.65f)
                }
            }

            GameCommand.SHOOT_POWER -> {
                executeBallCommand("TIR PUISSANT") {
                    renderer.powerShotBall(0f, 1f)
                }
            }

            GameCommand.SHOOT_PLACED -> {
                executeBallCommand("TIR PLACE") {
                    renderer.placedShotBall(0.25f, 0.62f)
                }
            }

            GameCommand.SHOOT_VOLLEY -> {
                executeBallCommand("TIR TYPE VOLEE") {
                    renderer.shootBall(0f, 0.85f)
                }
            }

            // DEFENSE : LOGIQUE A DEVELOPPER
            GameCommand.TACKLE_STANDING -> {
                showPending("TACLE : IA DEFENSIVE A DEVELOPPER")
            }

            GameCommand.TACKLE_SLIDE -> {
                showPending("GLISSADE : ANIMATION A DEVELOPPER")
            }

            GameCommand.PRESSING_ON -> {
                showPending("PRESSING : ACTIVATION A DEVELOPPER")
            }

            GameCommand.PRESSING_OFF -> {
                showPending("PRESSING : DESACTIVATION A DEVELOPPER")
            }
        }
    }

    /**
     * Exécute une commande sur le thread OpenGL.
     */
    private fun executeBallCommand(
        label: String,
        action: () -> Unit
    ) {
        gameView.queueEvent {
            action()
        }

        statusText.text = "DK 26  |  $label"
    }

    private fun showPending(label: String) {
        statusText.text = "DK 26  |  $label"
    }

    override fun onPause() {
        if (::gameView.isInitialized) {
            gameView.onPause()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (::gameView.isInitialized) {
            gameView.onResume()
        }
    }
}
