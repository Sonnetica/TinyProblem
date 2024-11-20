package com.example.tinyproblem

data class GameModel(
    val gameId: String = "",// gameId to store the unique game code
    var gameStatus: GameStatus = GameStatus.CREATED,
    var players: MutableList<String> = mutableListOf(), // List to track players
)

enum class GameStatus{
    CREATED,
    JOINED,
    INPROGRESS,
    FINISHED
}