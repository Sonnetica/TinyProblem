package com.example.tinyproblem

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tinyproblem.databinding.ActivityNameBinding

class NameActivity : AppCompatActivity() {

    lateinit var binding: ActivityNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize View Binding
        binding = ActivityNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        binding.nextBtn.setOnClickListener {
            val playerName = binding.playerNameInput.text.toString() // Retrieve the player's name

            // Check if the name is blank
            if (playerName.isEmpty()) {
                // Show an error message on the EditText
                binding.playerNameInput.error = "Please enter your name"
                return@setOnClickListener // Stop further execution
            }

            // Proceed to the next page if the name is valid
            nextPage(playerName)
        }
    }
    fun nextPage(playerName: String) {
        val n = Intent(this,MainActivity::class.java)

        // get EditText view from activity_name.xml
        val playerNameET = findViewById<EditText>(R.id.player_name_input)

        n.putExtra("keyName", playerNameET.text)
        startActivity(n)
    }
}