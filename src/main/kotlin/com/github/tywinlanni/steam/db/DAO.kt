package com.github.tywinlanni.steam.db

interface DAO {

    fun addNewGame(gameName: String, gamePrice: Double, languages: List<String> = listOf("en"))

    fun addNewUser(userLogin: String, nativeLang: String, lib: List<String>)

    fun getAllUsers() : List<User>

    fun getAllGames() : List<Game>

    fun mostExpensiveGameInLib(userLogin: String) : Game?

    fun mostPopularGame() : List<Game>

    fun valueGamesInUserLibDoesNotSupportNativeLang(userLogin: String) : Int

    fun equalsUsersLib(user1Login: String, user2Login: String)

    fun listUsersWithGameInLibWhichSupLang(lang: String) : List<User>
}