package com.github.tywinlanni.steam.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SteamServicePGSQL(private val database: Database) : DAO {

    fun chekAvailableTablesAndCreateThem() = transaction(database) { SchemaUtils.create (Languages, GameLanguages, UsersLibrary) }

    fun dropAllDB() = transaction(database) { SchemaUtils.drop(Languages, GameLanguages, Games, UsersLibrary, Users) }

    fun addStandardLang() {
        transaction(database) {
            val languages = "en ru fr de es cn".split(" ").map {
                Language.new {
                    liter = it
                }
            }
        }
    }

    override fun addNewGame(gameName: String, gamePrice: Double, languages: List<String>) {
        transaction(database) {
            SchemaUtils.create (Games)
            val newGame = Game.new {
                name = gameName
                price = gamePrice
            }
            newGame.languages = SizedCollection(Language.all().toList().filter { languages.contains(it.liter) })
        }
    }

    override fun addNewUser(userLogin: String, nativeLang: String, lib: List<String>) {
        transaction(database) {
            SchemaUtils.create (Users)
            val newUser = User.new {
                login = userLogin
                nativeLanguage = Language.all().find { it.liter == nativeLang } ?: Language.new { liter = nativeLang }
            }
            newUser.library = SizedCollection(Game.all().filter { lib.contains(it.name) })
        }
    }

    override fun getAllUsers(): List<User> = transaction(database) {
        User.all().toList()
    }

    override fun getAllGames(): List<Game> = transaction(database) {
        Game.all().toList()
    }

    override fun mostExpensiveGameInLib(userLogin: String): Game = transaction(database) {
        addLogger(StdOutSqlLogger)
        val gameRow = Users.innerJoin(UsersLibrary).innerJoin(Games)
            .slice(Games.columns)
            .select { Users.login eq userLogin }
            .orderBy(Games.price, SortOrder.DESC)
            .limit(1)
            .single()

        Game.wrapRow(gameRow)
    }

    override fun mostPopularGame(): List<Game> = transaction(database) {

        addLogger(StdOutSqlLogger)

        val popularGame =
            Users.innerJoin(UsersLibrary).innerJoin(Games)
                .slice(Users.id.count(), Games.id, Games.name)
                .selectAll()
                .groupBy(Games.id)
                .having { Users.id.count() eq Users.id.max() }

        Game.wrapRows(popularGame).toList()

    }

    override fun valueGamesInUserLibDoesNotSupportNativeLang(userLogin: String): Int = transaction(database) {

        addLogger(StdOutSqlLogger)

        Users.innerJoin(UsersLibrary).innerJoin(Games).innerJoin(GameLanguages)
            .slice(Languages.liter)
            .select { Users.login eq userLogin and (GameLanguages.language neq Users.nativeLanguages) }
            .count()
            .toInt()
    }

    override fun equalsUsersLib(user1Login: String, user2Login: String) {
        transaction(database) {

            val user1Games = Users.innerJoin(UsersLibrary).innerJoin(Games)
                .slice(Games.columns)
                .select { Users.login eq user1Login }
                .groupBy(Games.id)

            val user2Games = Users.innerJoin(UsersLibrary).innerJoin(Games)
                .slice(Games.columns)
                .select { Users.login eq user2Login }
                .groupBy(Games.id)

            println("Coincidences games: ${Game.wrapRows(user1Games.intersect(user2Games)).joinToString(", ") { it.name }}")
            println("User 1 unique games: ${Game.wrapRows(user1Games.except(user2Games)).joinToString(", ") { it.name }}")
            println("User 2 unique games: ${Game.wrapRows(user2Games.except(user1Games)).joinToString(", ") { it.name }}")

            println("User 1 games: ${Game.wrapRows(user1Games).joinToString(", ") { it.name }}")
            println("User 2 games: ${Game.wrapRows(user2Games).joinToString(", ") { it.name }}")

        }
    }

    override fun listUsersWithGameInLibWhichSupLang(lang: String): List<User> =
        transaction(database) {

            addLogger(StdOutSqlLogger)

            val langColumn = GameLanguages.innerJoin(Languages)
                .slice(Languages.columns)
                .select { Languages.liter eq lang }
                .limit(1)
                .single()

           val query = Users.innerJoin(UsersLibrary).innerJoin(Games).innerJoin(GameLanguages)
                .slice(Users.columns)
                .select { GameLanguages.language eq Language.wrapRow(langColumn).id }
                .withDistinct()

            return@transaction User.wrapRows(query).toList()
        }
}