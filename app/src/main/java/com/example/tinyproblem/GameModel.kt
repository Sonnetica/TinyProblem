package com.example.tinyproblem

data class GameModel(
    val gameId: String = "",
    val players: MutableList<String> = mutableListOf(),
    val gameStatus: GameStatus = GameStatus.CREATED,
    val hidingPhaseEndTime: Long? = null, // Add this property
    val seekingPhaseEndTime: Long? = null // Add seeking phase if needed
)


enum class GameStatus{
    CREATED,
    JOINED,
    INPROGRESS,
    FINISHED
}