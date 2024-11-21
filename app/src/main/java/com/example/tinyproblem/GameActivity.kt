package com.example.tinyproblem

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tinyproblem.databinding.ActivityGameBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.GridLayoutManager
import android.os.CountDownTimer

class GameActivity : AppCompatActivity() {

    lateinit var binding: ActivityGameBinding
    private var countDownTimer: CountDownTimer? = null // Declare timer here

    private var gameModel: GameModel? = null
    private val firestore = FirebaseFirestore.getInstance() // Firestore instance
    private val playersList = mutableListOf<String>() // List of players in the lobby
    private var playerName: String? = null
    private var gameId: String? = null // To hold the game ID
    private var secondTimerDuration: Long? = null // Duration for the second timer
    private var secondCountDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize binding
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up other functionality here...
        binding.setTimerBtn.setOnClickListener {
            if (playersList.isNotEmpty() && playersList[0] == playerName) {
                val timerInput = binding.timerInput.text.toString()
                if (timerInput.isNotEmpty()) {
                    val timerDuration = timerInput.toLongOrNull()
                    if (timerDuration != null && timerDuration > 0) {
                        setLobbyTimer(timerDuration)
                        Toast.makeText(this, "Timer set for $timerDuration seconds", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Enter a valid duration in seconds", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Timer duration cannot be empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Only the host can set the timer!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.setSecondTimerBtn.setOnClickListener {
            if (playersList.isNotEmpty() && playersList[0] == playerName) {
                val secondTimerInput = binding.secondTimerInput.text.toString()
                if (secondTimerInput.isNotEmpty()) {
                    val duration = secondTimerInput.toLongOrNull()
                    if (duration != null && duration > 0) {
                        secondTimerDuration = duration * 1000 // Convert to milliseconds
                        Toast.makeText(this, "Second timer set for $duration seconds", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Enter a valid duration in seconds", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Second timer duration cannot be empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Only the host can set the second timer!", Toast.LENGTH_SHORT).show()
            }
        }

        // Retrieve the gameId from the Intent passed from MainActivity
        gameId = intent.getStringExtra("gameId")
        playerName = intent.getStringExtra("keyName")

        // Fetch the game data based on the gameId
        gameId?.let {
            GameData.fetchGameModel(it)  // Pass the gameId to fetch the correct game data
        }

        // Initialize RecyclerView with GridLayoutManager for 2 columns
        binding.playerListRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.playerListRecyclerView.adapter = PlayerAdapter(playersList)

        // Observe game model changes and update the UI accordingly
        GameData.gameModel.observe(this) { gameModel ->
            this.gameModel = gameModel
            setUI()
            listenForPlayers(gameId)  // Update players list when the lobby changes
            listenForTimers()         // Synchronize timer across all clients
        }

        binding.startGameBtn.setOnClickListener {
            if (playersList.size > 1) {
                if (playersList[0] == playerName) {
                    startGameForHost(gameId!!)
                } else {
                    Toast.makeText(this, "Only the host can start the game!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "There must be more than one player to start the game.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.quitGame.setOnClickListener {
            quitGame()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        secondCountDownTimer?.cancel()
    }

    fun startGameForHost(gameId: String) {
        gameModel?.apply {
            if (gameStatus == GameStatus.CREATED || gameStatus == GameStatus.JOINED) {
                // Retrieve timer duration from user input or default to 60 seconds
                val timerInput = binding.timerInput.text.toString().toLongOrNull()
                val hidingPhaseEndTime = System.currentTimeMillis() + (timerInput?.times(1000) ?: 60000) // Default: 60s

                val updatedGameModel = this.copy(
                    gameStatus = GameStatus.INPROGRESS,
                    hidingPhaseEndTime = hidingPhaseEndTime
                )

                updateGameData(updatedGameModel) // Save updated game state to Firestore
                Toast.makeText(this@GameActivity, "Game started with hiding phase!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@GameActivity, "Game cannot be started in the current state.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Game data is not available.", Toast.LENGTH_SHORT).show()
        }
    }

    // Set UI for displaying the game state and lobby details
    fun setUI() {
        gameModel?.apply {
            // Update the visibility of buttons
            binding.startGameBtn.visibility = if (gameStatus in listOf(GameStatus.CREATED, GameStatus.JOINED)) View.VISIBLE else View.INVISIBLE
            val showTimerControls = gameStatus in listOf(GameStatus.CREATED, GameStatus.JOINED)
            binding.setTimerBtn.visibility = if (showTimerControls) View.VISIBLE else View.GONE
            binding.timerInput.visibility = if (showTimerControls) View.VISIBLE else View.GONE

            // Update timer text
            val timerText = when (gameStatus) {
                GameStatus.CREATED, GameStatus.JOINED -> "Waiting to start..."
                GameStatus.INPROGRESS -> {
                    val remainingTime = hidingPhaseEndTime?.let { (it - System.currentTimeMillis()) / 1000 }
                    if (remainingTime != null && remainingTime > 0) {
                        "Hiding time left: $remainingTime seconds"
                    } else {
                        "Hiding phase over! Seeking begins."
                    }
                }
                GameStatus.FINISHED -> "Game Over"
                else -> ""
            }
            binding.timerText.text = timerText

            // Update game status text
            binding.gameStatusText.text = when (gameStatus) {
                GameStatus.CREATED -> "Lobby Code: $gameId\nPlayers: ${playersList.size}/6\nWaiting to start..."
                GameStatus.JOINED -> "Lobby Code: $gameId\nWaiting for players (${playersList.size}/6)..."
                GameStatus.INPROGRESS -> "Game in progress (${playersList.size}/6)"
                GameStatus.FINISHED -> "Game Over"
            }
        }
    }

    private fun setLobbyTimer(timerDuration: Long) {
        gameId?.let { id ->
            firestore.collection("games").document(id)
                .update("hidingPhaseEndTime", System.currentTimeMillis() + timerDuration * 1000)
                .addOnSuccessListener {
                    Log.d("GameActivity", "Timer set for $timerDuration seconds.")
                }
                .addOnFailureListener { Log.e("GameActivity", "Failed to set timer: ${it.message}") }
        }
    }

    fun startGame() {
        gameModel?.apply {
            // Update the game status to INPROGRESS
            val updatedGameModel = this.copy(
                gameStatus = GameStatus.INPROGRESS
            )
            updateGameData(updatedGameModel)
        }
    }

    // Quit the game by removing the player from the list and updating the game status
    fun quitGame() {
        gameId?.let {
            firestore.collection("games").document(it)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val gameModel = documentSnapshot.toObject(GameModel::class.java)
                    gameModel?.apply {
                        // Remove the player from the game
                        val updatedPlayersList = playersList.toMutableList().apply {
                            remove(playerName)  // Remove the current player from the list
                        }
                        val updatedGameModel = this.copy(players = updatedPlayersList)

                        // Save the updated game model (after removing the player)
                        updateGameData(updatedGameModel)

                        // Optionally: If no players are left, delete the game
                        if (updatedPlayersList.isEmpty()) {
                            FirebaseFirestore.getInstance().collection("games")
                                .document(gameId)
                                .delete()
                                .addOnSuccessListener {
                                    Log.d("GameActivity", "Game deleted as no players are left.")
                                    // Optionally show a message to the user
                                    Toast.makeText(this@GameActivity, "Game is deleted as all players left.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { error ->
                                    Log.e("GameActivity", "Error deleting game: ${error.message}")
                                }
                        } else {
                            // If there are still players, update the game data without deleting the game
                            updateGameData(updatedGameModel)
                        }

                        // Close the activity (optional, depends on your design)
                        finish()  // This will close the activity and return to the previous screen
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("GameActivity", "Error fetching game data: ${exception.message}")
                    Toast.makeText(this, "Error quitting the game", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Save updated game data to Firebase
    fun updateGameData(model : GameModel){
        GameData.saveGameModel(model)
    }

    private fun listenForPlayers(gameId: String?) {
        if (gameId.isNullOrEmpty()) return

        firestore.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error fetching players: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val model = it.toObject(GameModel::class.java)

                    model?.let {
                        // Prevent players from joining if the game is in progress
                        if (it.gameStatus == GameStatus.INPROGRESS) {
                            Toast.makeText(
                                this,
                                "The game is already in progress. You cannot join now.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@let
                        }

                        playersList.clear()
                        playersList.addAll(it.players)

                        // Check if the player limit is reached
                        if (playersList.size >= 6) {
                            Toast.makeText(
                                this,
                                "Lobby is full. No new players can join.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        updatePlayerListUI()
                        this.gameModel = model
                        setUI()
                    }
                }
            }
    }

    private fun listenForTimers() {
        gameId?.let { id ->
            firestore.collection("games").document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("GameActivity", "Error listening for timer changes: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.toObject(GameModel::class.java)?.let { model ->
                        val currentTime = System.currentTimeMillis()

                        if (model.gameStatus == GameStatus.INPROGRESS) {
                            val remainingTime = model.hidingPhaseEndTime?.let { it - currentTime }
                                ?: 60000 // Default to 60 seconds if no timer is set

                            if (remainingTime > 0) {
                                startCountdownTimer(remainingTime)
                            } else {
                                binding.timerText.text = "Hiding phase over! Seeking begins."
                            }
                        } else {
                            countDownTimer?.cancel()
                            binding.timerText.text = "Waiting to start..."
                        }
                    }
                }
        }
    }

    private fun startSecondCountdownTimer(durationMillis: Long) {
        secondCountDownTimer?.cancel() // Cancel any existing second timer
        secondCountDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.timerText.text = "Time Left for hiders: $secondsLeft seconds remaining"
            }

            override fun onFinish() {
                binding.timerText.text = "Times up! Hider's won!"
                // Handle post-second-timer logic here
            }
        }
        secondCountDownTimer?.start()
    }

    private fun startCountdownTimer(durationMillis: Long) {
        countDownTimer?.cancel() // Cancel any existing timer
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.timerText.text = "Hiding time left: $secondsLeft seconds"
            }

            override fun onFinish() {
                binding.timerText.text = "Hiding phase over! Second timer starting..."
                // Start the second timer if it is set
                secondTimerDuration?.let {
                    startSecondCountdownTimer(it)
                }
            }
        }
        countDownTimer?.start()
    }

    private fun updatePlayerListUI() {
        Log.d("GameActivity", "Updating player list: $playersList")
        if (binding.playerListRecyclerView.adapter == null) {
            binding.playerListRecyclerView.adapter = PlayerAdapter(playersList)
        } else {
            (binding.playerListRecyclerView.adapter as PlayerAdapter).notifyDataSetChanged()
        }
    }

    fun finishGame() {
        gameModel?.apply {
            // Update game status to FINISHED
            val updatedGameModel = this.copy(
                gameStatus = GameStatus.FINISHED
            )

            // Save the updated game status
            updateGameData(updatedGameModel)

            // Delete the game from Firestore after the game ends
            FirebaseFirestore.getInstance().collection("games")
                .document(gameId)
                .delete()
                .addOnSuccessListener {
                    // Optionally, handle success, e.g., show a message
                    Log.d("GameData", "Game data deleted successfully")
                }
                .addOnFailureListener { error ->
                    // Optionally, handle failure
                    Log.e("GameData", "Error deleting game data: ${error.message}")
                }
        }
    }
}