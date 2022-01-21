package com.github.tywinlanni.steam.db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object Users: IntIdTable() {
    val login = varchar("login", 50).uniqueIndex()
    val nativeLanguages = reference("native languages", Languages)
}

object UsersLibrary : Table() {
    val user = reference("user", Users)
    val game = reference("game", Games)
    override val primaryKey = PrimaryKey(user, game, name = "PK_UserGames")
}

object GameLanguages : Table() {
    val game = reference("game", Games)
    val language = reference("language", Languages)
    override val primaryKey = PrimaryKey(game, language, name = "PK_GamesLanguages")
}

object Languages : IntIdTable() {
    val liter = varchar("liter", 2)
}

object Games: IntIdTable() {
    val name = varchar("name", 100)
    val price = double("price")
}

class Language(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Language>(Languages)

    var liter by Languages.liter
}

class User(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<User>(Users)

    var login by Users.login
    var nativeLanguage by Language referencedOn Users.nativeLanguages
    var library by Game via UsersLibrary
}

class Game(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<Game>(Games)

    var name by Games.name
    var price by Games.price
    var languages by Language via GameLanguages
}