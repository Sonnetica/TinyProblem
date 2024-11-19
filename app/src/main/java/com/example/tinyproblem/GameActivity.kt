package com.example.tinyproblem

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tinyproblem.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity(),View.OnClickListener {

    lateinit var binding: ActivityGameBinding

    private var gameModel : GameModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameData.fetchGameModel()

        binding.btn0.setOnClickListener(this)
        binding.btn1.setOnClickListener(this)

        binding.startGameBtn.setOnClickListener {
            startGame()
        }

        GameData.gameModel.observe(this){
            gameModel = it
            setUI()
        }
    }

    fun setUI(){
        gameModel?.apply {

            binding.startGameBtn.visibility = View.VISIBLE

            binding.gameStatusText.text =
                 when(gameStatus){
                     GameStatus.CREATED -> {
                         binding.startGameBtn.visibility = View.INVISIBLE
                         "Game ID :"+ gameId
                     }
                     GameStatus.JOINED ->{
                         "Click on start game"
                     }
                     GameStatus.INPROGRESS ->{
                         binding.startGameBtn.visibility = View.INVISIBLE
                         currentPlayer + " turn"
                     }
                     GameStatus.FINISHED ->{
                         if(winner.isNotEmpty()) winner + " Won"
                         else "Draw"
                     }
                 }

        }
    }

    fun startGame() {
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.INPROGRESS
                )
            )
        }
    }

    fun updateGameData(model : GameModel){
        GameData.saveGameModel(model)
    }

    fun checkForWinner() {
        // Hiders Or Seeker wins
        // When all Hiders eliminated (Hiders = 0), Seeker wins
        // Else if timer runs out and Hiders > 0, Hiders win
    }

    override fun onClick(v: View?) {
        gameModel?.apply {
            if(gameStatus!= GameStatus.INPROGRESS){
                Toast.makeText(applicationContext,"Game not Started",Toast.LENGTH_SHORT).show()
                return
            }
            //game is in progress
            checkForWinner()
            updateGameData(this)
        }
    }
}