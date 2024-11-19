package com.example.tinyproblem

data class GameModel (
    var gameId : String = "-1",
    var winner : String = "",
    var gameStatus : GameStatus = GameStatus.CREATED,
    var currentPlayer : String = "X"
)

enum class GameStatus{
    CREATED,
    JOINED,
    INPROGRESS,
    FINISHED
}