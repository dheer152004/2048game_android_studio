package com.example.game2048

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var gameBoard: GridLayout
    private lateinit var scoreValueTextView: TextView
    private lateinit var highScoreValueTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var newGameButton: Button

    private var board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { 0 } }
    private var score = 0
    private var highScore = 0
    private var gameTimeSeconds = 0
    private var isGameOver = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private lateinit var gestureDetector: GestureDetector

    companion object {
        private const val BOARD_SIZE = 4
        private const val PREFS_NAME = "Game2048Prefs"
        private const val HIGH_SCORE_KEY = "highScore"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gameBoard = findViewById(R.id.game_board)
        // Ensure the board can receive touch events
        gameBoard.isClickable = true
        gameBoard.isFocusable = true
        scoreValueTextView = findViewById(R.id.score_value)
        highScoreValueTextView = findViewById(R.id.high_score_value)
        timeTextView = findViewById(R.id.time_label)
        newGameButton = findViewById(R.id.new_game_button)

        gameBoard.rowCount = BOARD_SIZE
        gameBoard.columnCount = BOARD_SIZE

        loadHighScore()
        initBoard()

        newGameButton.setOnClickListener {
            startNewGame()
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            // Must return true here so GestureDetector will continue to handle gestures
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                    // Horizontal swipe
                    if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            move(Direction.RIGHT)
                        } else {
                            move(Direction.LEFT)
                        }
                        return true
                    }
                } else {
                    // Vertical swipe
                    if (kotlin.math.abs(diffY) > SWIPE_THRESHOLD && kotlin.math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            move(Direction.DOWN)
                        } else {
                            move(Direction.UP)
                        }
                        return true
                    }
                }
                return false
            }
        })

        gameBoard.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun loadHighScore() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        highScore = prefs.getInt(HIGH_SCORE_KEY, 0)
        highScoreValueTextView.text = highScore.toString()
    }

    private fun saveHighScore() {
        if (score > highScore) {
            highScore = score
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putInt(HIGH_SCORE_KEY, highScore)
                apply()
            }
            highScoreValueTextView.text = highScore.toString()
        }
    }

    private fun initBoard() {
        board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { 0 } }
        score = 0
        gameTimeSeconds = 0
        isGameOver = false
        scoreValueTextView.text = "0"
        timeTextView.text = "TIME: 00:00"

        gameBoard.removeAllViews()
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                val tile = TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(j, 1f)
                        rowSpec = GridLayout.spec(i, 1f)
                        setMargins(4, 4, 4, 4)
                    }
                    gravity = Gravity.CENTER
                    textSize = 24f
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.tile_background)
                }
                gameBoard.addView(tile)
            }
        }
        addRandomTile()
        addRandomTile()
        updateUI()
        startTimer()
    }

    private fun startNewGame() {
        stopTimer()
        initBoard()
    }

    private fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                if (board[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }

        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            board[row][col] = if (Random.nextFloat() < 0.9) 2 else 4
        }
    }

    private fun updateUI() {
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                val tile = gameBoard.getChildAt(i * BOARD_SIZE + j) as TextView
                val value = board[i][j]
                tile.text = if (value == 0) "" else value.toString()
                tile.setBackgroundColor(getTileColor(value))
                tile.setTextColor(getTileTextColor(value))
            }
        }
        scoreValueTextView.text = score.toString()
        saveHighScore()
        if (isGameOver) {
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show()
            stopTimer()
        }
    }

    private fun getTileColor(value: Int): Int {
        return when (value) {
            0 -> Color.parseColor("#CDC1B4") // Empty tile (matches background)
            2 -> Color.parseColor("#EEE4DA") // Light beige
            4 -> Color.parseColor("#EDE0C8") // Pale yellow
            8 -> Color.parseColor("#F2B179") // Orange
            16 -> Color.parseColor("#F59563") // Dark orange
            32 -> Color.parseColor("#F67C5F") // Red-orange
            64 -> Color.parseColor("#F65E3B") // Strong red
            128 -> Color.parseColor("#EDCF72") // Yellow gold
            256 -> Color.parseColor("#EDCC61") // Bright gold
            512 -> Color.parseColor("#EDC850") // Deep gold
            1024 -> Color.parseColor("#EDC53F") // Golden yellow
            2048 -> Color.parseColor("#EDC22E") // Victory gold
            else -> Color.parseColor("#3C3A32") // Super gold for higher values
        }
    }

    private fun getTileTextColor(value: Int): Int {
        return when (value) {
            0 -> Color.TRANSPARENT // Empty tile has no text
            2, 4 -> Color.parseColor("#776E65") // Dark text for light tiles
            else -> Color.parseColor("#F9F6F2") // Light text for darker tiles
        }
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
    
    // Transpose the board (rows become columns and vice-versa)
    private fun transpose(matrix: Array<Array<Int>>): Array<Array<Int>> {
        val newMatrix = Array(BOARD_SIZE) { Array(BOARD_SIZE) { 0 } }
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                newMatrix[i][j] = matrix[j][i]
            }
        }
        return newMatrix
    }

    // Reverse the order of elements in each row
    private fun reverse(matrix: Array<Array<Int>>): Array<Array<Int>> {
        return matrix.map { it.reversedArray() }.toTypedArray()
    }

    private fun move(direction: Direction) {
        if (isGameOver) return

        val originalBoard = board.map { it.clone() }.toTypedArray()

        board = when (direction) {
            Direction.LEFT -> {
                board.map { slideAndCombine(it) }.toTypedArray()
            }
            Direction.RIGHT -> {
                val reversed = reverse(board)
                val moved = reversed.map { slideAndCombine(it) }.toTypedArray()
                reverse(moved)
            }
            Direction.UP -> {
                val transposed = transpose(board)
                val moved = transposed.map { slideAndCombine(it) }.toTypedArray()
                transpose(moved)
            }
            Direction.DOWN -> {
                val transposed = transpose(board)
                val reversed = reverse(transposed)
                val moved = reversed.map { slideAndCombine(it) }.toTypedArray()
                val unreversed = reverse(moved)
                transpose(unreversed)
            }
        }

        val boardChanged = !board.contentDeepEquals(originalBoard)

        if (boardChanged) {
            addRandomTile()
            updateUI()
            if (checkForGameOver()) {
                isGameOver = true
            }
        }
    }

    private fun slideAndCombine(line: Array<Int>): Array<Int> {
        // 1. Slide: Remove zeros and push numbers to the left
        val slid = line.filter { it != 0 }.toMutableList()
        
        // 2. Combine: Merge identical adjacent numbers
        var i = 0
        while (i < slid.size - 1) {
            if (slid[i] == slid[i+1]) {
                slid[i] *= 2
                score += slid[i]
                slid.removeAt(i + 1)
            }
            i++
        }

        // 3. Pad with zeros to the right
        while(slid.size < BOARD_SIZE) {
            slid.add(0)
        }
        return slid.toTypedArray()
    }

    private fun checkForGameOver(): Boolean {
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                if (board[i][j] == 0) return false
                if (i < BOARD_SIZE - 1 && board[i][j] == board[i + 1][j]) return false
                if (j < BOARD_SIZE - 1 && board[i][j] == board[i][j + 1]) return false
            }
        }
        return true
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (!isGameOver) {
                    gameTimeSeconds++
                    val minutes = gameTimeSeconds / 60
                    val seconds = gameTimeSeconds % 60
                    timeTextView.text = String.format("%02d:%02d", minutes, seconds)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
    }
}
