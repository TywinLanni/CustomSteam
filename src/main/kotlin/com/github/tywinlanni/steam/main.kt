package com.github.tywinlanni.steam

import com.github.tywinlanni.steam.db.SteamServicePGSQL
import org.jetbrains.exposed.sql.*


fun main() {

    val db = Database.connect("jdbc:postgresql://localhost:5432/steam", driver = "org.postgresql.Driver",
        user = "postgres", password = "asdfgh")

    val service = SteamServicePGSQL(db).apply {
        //dropAllDB()
        chekAvailableTablesAndCreateThem()
        addStandardLang()
    }


    val allLangList = listOf("en", "ru", "es", "pl", "de", "cn")

    /*for (i in 1..100) {
        val firstIndexLang = (0..allLangList.lastIndex).random()
        val lastIndexLang = (firstIndexLang..allLangList.lastIndex).random()
        service.addNewGame("GTA $i", (0..100).random().toDouble(), allLangList.subList(firstIndexLang, lastIndexLang))
    }

    for (i in 1..100) {
        val userLib = mutableListOf<String>()
        for (j in 1..(1..50).random()) {
            userLib.add("GTA $j")
        }
        service.addNewUser("Tywin $i", allLangList.random(), userLib)
    }*/

    println(service.mostPopularGame().joinToString { it.name })

    println(service.valueGamesInUserLibDoesNotSupportNativeLang("Tywin 1"))

    service.equalsUsersLib("Tywin 1", "Tywin 2")

    println(service.listUsersWithGameInLibWhichSupLang("de").joinToString { it.login })
}