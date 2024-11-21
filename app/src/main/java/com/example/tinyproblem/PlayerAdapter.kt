package com.example.tinyproblem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlayerAdapter(private val players: List<String>) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    // ViewHolder class to hold reference to each player's TextView
    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerNameText: TextView = itemView.findViewById(R.id.playerNameText)
    }

    // Create a new ViewHolder instance
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_item, parent, false)
        return PlayerViewHolder(view)
    }

    // Bind the player data to the ViewHolder
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.playerNameText.text = players[position]
    }

    // Return the number of items in the list
    override fun getItemCount(): Int {
        return players.size
    }
}