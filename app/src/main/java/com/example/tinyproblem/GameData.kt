package com.example.tinyproblem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore

object GameData {
    private var _gameModel: MutableLiveData<GameModel> = MutableLiveData()
    var gameModel: LiveData<GameModel> = _gameModel
    var myID = ""

    fun saveGameModel(model: GameModel) {
        _gameModel.postValue(model)

        // It will always be an online game
        Firebase.firestore.collection("games")
            .document(model.gameId)
            .set(model)
    }

    // Method to update the game model for the lobby host's phone (Change screen to reflect appropriate layout)
    fun fetchGameModel() {
        gameModel.value?.apply {
            Firebase.firestore.collection("games")
                .document(gameId)
                .addSnapshotListener { value, error ->
                    val model = value?.toObject((GameModel::class.java))
                    _gameModel.postValue(model)
                }
        }
    }
}


