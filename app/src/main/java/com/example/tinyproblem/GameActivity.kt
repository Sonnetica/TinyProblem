package com.example.tinyproblem

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tinyproblem.databinding.ActivityGameBinding
import com.google.firebase.firestore.FirebaseFirestore

class GameActivity : AppCompatActivity() {

    lateinit var binding: ActivityGameBinding

    private var gameModel : GameModel? = null
    private val firestore = FirebaseFirestore.getInstance() // Firestore instance
    private val playersList = mutableListOf<String>() // List of players in the lobby
    private var playerName: String? = null
    private var gameId: String? = null // To hold the game ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize RecyclerView
        binding.playerListRecyclerView.layoutManager = LinearLayoutManager(this)  // LinearLayoutManager to display list vertically
        binding.playerListRecyclerView.adapter = PlayerAdapter(playersList)  // Set the adapter for RecyclerView

        // Retrieve the player name from the Intent passed from NameActivity
        playerName = intent.getStringExtra("keyName")
        gameId = intent.getStringExtra("gameId") // Get the gameId from intent

        // Fetch game data (if any)
        gameId?.let {
            GameData.fetchGameModel(it)  // Pass the gameId to fetch the correct game data
        }

        binding.startGameBtn.setOnClickListener {
            startGame()
        }

        // Observe changes to the game model
        GameData.gameModel.observe(this){
            gameModel = it
            setUI()
            listenForPlayers(it?.gameId) // Update players list when the lobby changes
        }
    }
    // Set UI for displaying the game state and lobby details
    fun setUI(){
        gameModel?.apply {
            binding.startGameBtn.visibility = //View.VISIBLE
                if (gameStatus == GameStatus.CREATED || gameStatus == GameStatus.JOINED) View.VISIBLE
                else View.INVISIBLE

//            binding.gameStatusText.text =
//                 when(gameStatus){
//                     GameStatus.CREATED -> {
//                         binding.startGameBtn.visibility = View.INVISIBLE
//                         "Game ID :"+ gameId
//                     }
//                     GameStatus.JOINED ->{
//                         "Click on start game"
//                     }
//                     GameStatus.INPROGRESS ->{
//                         binding.startGameBtn.visibility = View.INVISIBLE
//                         currentPlayer + " turn"
//                     }
//                     GameStatus.FINISHED ->{
//                         if(winner.isNotEmpty()) winner + " Won"
//                         else "Draw"
//                     }
//                 }
            binding.gameStatusText.text = when (gameStatus) {
                GameStatus.CREATED -> "Game ID: $gameId"
                GameStatus.JOINED -> "Waiting for players to join...$playerName" // Show the player's name
                GameStatus.INPROGRESS -> "Game in progress..."
                GameStatus.FINISHED -> "Game Over"
            }

        }
    }

    // Start the game by updating the game status in Firebase
    fun startGame() {
        gameModel?.apply {
            // Update the gameStatus and keep other fields the same
            val updatedGameModel = this.copy(
                gameStatus = GameStatus.INPROGRESS
            )
            updateGameData(updatedGameModel) // Save the updated game model to Firestore
        }
    }

    // Save updated game data to Firebase
    fun updateGameData(model : GameModel){
        GameData.saveGameModel(model)
    }

//    fun checkForWinner() {
//        // Hiders Or Seeker wins
//        // When all Hiders eliminated (Hiders = 0), Seeker wins
//        // Else if timer runs out and Hiders > 0, Hiders win
//    }
//
//    override fun onClick(v: View?) {
//        gameModel?.apply {
//            if(gameStatus!= GameStatus.INPROGRESS){
//                Toast.makeText(applicationContext,"Game not Started",Toast.LENGTH_SHORT).show()
//                return
//            }
//            //game is in progress
//            checkForWinner()
//            updateGameData(this)
//        }
// Listen for player changes in the lobby and update the UI
    private fun listenForPlayers(gameId: String?) {
        if (gameId.isNullOrEmpty()) return

        firestore.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error fetching players: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val model = it.toObject(GameModel::class.java)

                    // Update the players list from Firestore
                    playersList.clear()  // Clear the existing list
                    playersList.addAll(model?.players ?: emptyList())  // Add new player names

                    // If the adapter is not yet set, initialize it
                    if (binding.playerListRecyclerView.adapter == null) {
                        binding.playerListRecyclerView.adapter = PlayerAdapter(playersList)
                    } else {
                        // Simply update the data in the adapter
                        (binding.playerListRecyclerView.adapter as PlayerAdapter).notifyDataSetChanged()
                    }

                    setUI()  // Refresh the UI
                }
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