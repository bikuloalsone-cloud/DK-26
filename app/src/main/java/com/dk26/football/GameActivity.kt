package com.dk26.football

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Commandes tactiles de DK 26.
 *
 * Cette première version crée l'écran de commandes et expose les actions
 * via GameCommandListener. Le raccordement au moteur OpenGL/à BallPhysics
 * sera fait dans l'étape suivante, sans modifier les boutons.
 */
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

/**
 * HUD réutilisable : peut être superposé plus tard à la GLSurfaceView
 * existante avec FrameLayout.addView().
 */
class GameControlsOverlay(
    activity: Activity,
    private val commandListener: GameCommandListener
) : FrameLayout(activity) {

    private var pressingEnabled = false
    private val pressingButton: Button

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        isClickable = false
        isFocusable = false

        val leftPanel = panel(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.START
        }

        addButton(leftPanel, "PASSE COURTE", GameCommand.PASS_SHORT)
        addButton(leftPanel, "PASSE EN PROFONDEUR", GameCommand.PASS_THROUGH)
        addButton(leftPanel, "PASSE LONGUE", GameCommand.PASS_LONG)
        addButton(leftPanel, "CENTRE", GameCommand.CROSS)
        addButton(leftPanel, "TACLE DEBOUT", GameCommand.TACKLE_STANDING)
        addButton(leftPanel, "TACLE GLISSÉ", GameCommand.TACKLE_SLIDE)

        val rightPanel = panel(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
        }

        addButton(rightPanel, "TIR", GameCommand.SHOOT)
        addButton(rightPanel, "TIR PUISSANT", GameCommand.SHOOT_POWER)
        addButton(rightPanel, "TIR PLACÉ", GameCommand.SHOOT_PLACED)
        addButton(rightPanel, "VOLÉE", GameCommand.SHOOT_VOLLEY)

        pressingButton = createButton("PRESSING : OFF").apply {
            setOnClickListener {
                pressingEnabled = !pressingEnabled
                text = if (pressingEnabled) "PRESSING : ON" else "PRESSING : OFF"
                commandListener.onGameCommand(
                    if (pressingEnabled) GameCommand.PRESSING_ON
                    else GameCommand.PRESSING_OFF
                )
            }
        }
        rightPanel.addView(pressingButton)

        val leftParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.START
        ).apply {
            leftMargin = dp(12)
            bottomMargin = dp(12)
        }

        val rightParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        ).apply {
            rightMargin = dp(12)
            bottomMargin = dp(12)
        }

        addView(leftPanel, leftParams)
        addView(rightPanel, rightParams)
    }

    private fun addButton(
        panel: LinearLayout,
        label: String,
        command: GameCommand
    ) {
        val button = createButton(label)
        button.setOnClickListener {
            commandListener.onGameCommand(command)
        }
        panel.addView(button)
    }

    private fun panel(activity: Activity): LinearLayout {
        return LinearLayout(activity).apply {
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.argb(95, 8, 12, 18))
                cornerRadius = dp(14).toFloat()
            }
        }
    }

    private fun createButton(label: String): Button {
        return Button(context).apply {
            text = label
            textSize = 10f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            minWidth = dp(112)
            minimumWidth = dp(112)
            minHeight = dp(42)
            minimumHeight = dp(42)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = GradientDrawable().apply {
                setColor(Color.argb(205, 20, 42, 65))
                cornerRadius = dp(9).toFloat()
                setStroke(dp(1), Color.argb(180, 220, 235, 255))
            }
            stateListAnimator = null
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

/**
 * Activité hôte du futur écran de match.
 *
 * Elle fournit déjà le HUD tactile. Les commandes sont volontairement
 * centralisées dans onGameCommand() pour permettre leur raccordement au
 * contrôleur de match sans changer l'interface.
 */
class GameActivity : Activity(), GameCommandListener {

    private lateinit var root: FrameLayout
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(8, 35, 20))
        }

        statusText = TextView(this).apply {
            text = "DK 26  •  ÉCRAN DE MATCH"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(18), dp(12), dp(12))
        }

        root.addView(
            statusText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
        )

        // HUD tactile prêt. Le moteur de match sera placé sous cette couche.
        root.addView(GameControlsOverlay(this, this))
        setContentView(root)
    }

    override fun onGameCommand(command: GameCommand) {
        // Point d'entrée unique pour relier les commandes au moteur.
        // Ne simule pas encore une action : aucune connexion au moteur
        // de match n'est présente dans le projet à cette étape.
        statusText.text = "DK 26  •  COMMANDE : ${command.name.replace('_', ' ')}"
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
