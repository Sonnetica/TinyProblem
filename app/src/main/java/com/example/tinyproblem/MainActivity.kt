package com.example.tinyproblem

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tinyproblem.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlin.random.Random
import kotlin.random.nextInt

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val playerName = intent.getStringExtra("keyName")

        if (playerName.isNullOrEmpty()) {
            // Show an error message or handle it
            Toast.makeText(this, "Player name is required", Toast.LENGTH_SHORT).show()
            return
        }
//        binding.playOfflineBtn.setOnClickListener {
//            createOfflineGame()
//        }

        binding.createOnlineGameBtn.setOnClickListener {
            createOnlineGame(playerName)
        }

        binding.joinOnlineGameBtn.setOnClickListener {
            val gameId = binding.gameIdInput.text.toString() // Get the game ID from an EditText field
            val playerName = intent.getStringExtra("keyName") ?: "DefaultPlayerName"  // Default player name if null

            if (gameId.isNotEmpty()) {
                // Disable the button while the game join process is ongoing
                binding.joinOnlineGameBtn.isEnabled = false
                joinOnlineGame(gameId, playerName)
            } else {
                Toast.makeText(this, "Please enter a game ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    fun createOfflineGame() {
//        GameData.saveGameModel(
//            GameModel(
//                gameStatus = GameStatus.JOINED
//            )
//        )
//        startGame()
//    }

    fun createOnlineGame(playerName: String) {
        // Replace myID assignment with a variable from the "Enter Player Name" box if needed.
        GameData.myID = playerName
        // Generate a random 4-digit gameId
        val gameId = Random.nextInt(1000..9999).toString()

        // Save the game model with the newly generated gameId and the initial player
        GameData.saveGameModel(
            GameModel(
                gameStatus = GameStatus.CREATED,
                gameId = gameId,
                players = mutableListOf(GameData.myID) // Add the current player to the list
            )
        )

        startGame()
    }

    fun joinOnlineGame(gameId: String, playerName: String) {
        // Fetch the game from Firestore
        FirebaseFirestore.getInstance().collection("games")
            .document(gameId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val game = documentSnapshot.toObject(GameModel::class.java)

                if (game != null) {
                    // Add the current player to the list of players
                    if (!game.players.contains(playerName)) {
                        game.players.add(playerName)
                    }
                    game.gameStatus = GameStatus.JOINED
                    // Save the updated game back to Firestore
                    FirebaseFirestore.getInstance().collection("games")
                        .document(gameId)
                        .set(game)
                        .addOnSuccessListener {
                            // Successfully joined the game
                            startGame()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(
                                this,
                                "Failed to join game: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.joinOnlineGameBtn.isEnabled = true
                        }
                } else {
                    Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show()
                    binding.joinOnlineGameBtn.isEnabled = true
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch game: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.joinOnlineGameBtn.isEnabled = true
            }

    }

    fun startGame() {
        val i = Intent(this, GameActivity::class.java)

        // Get the gameId from the saved model or GameData
        GameData.gameModel.observe(this) { model ->
            val gameId = model?.gameId ?: ""
            i.putExtra("lobbyId", gameId)
            startActivity(i)
        }
    }
}