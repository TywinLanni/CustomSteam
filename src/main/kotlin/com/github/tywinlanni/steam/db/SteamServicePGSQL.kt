package com.github.tywinlanni.steam.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SteamServicePGSQL(private val database: Database) : DAO {

    fun chekAvailableTablesAndCreateThem() = transaction { SchemaUtils.create (Languages, GameLanguages, UsersLibrary) }

    fun dropAllDB() = transaction { SchemaUtils.drop(Languages, GameLanguages, Games, UsersLibrary, Users) }

    fun addStandardLang() {
        transaction {
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

    override fun mostExpensiveGameInLib(userLogin: String): Game? = transaction(database) {
        val user: User = User.find { Users.login eq userLogin }.firstOrNull() ?: return@transaction null
        return@transaction user.library.maxByOrNull { it.price }
    }

    override fun mostPopularGame(): List<Game> = transaction(database) {
        val gamesMap: MutableMap<String, Long> = mutableMapOf()
        addLogger(StdOutSqlLogger)
        Users.innerJoin(UsersLibrary).innerJoin(Games)
            .slice(Games.name, Users.id.count())
            .selectAll()
            .groupBy(Games.name)
            .forEach { gamesMap[it[Games.name]] = it[Users.id.count()] }
        return@transaction gamesMap
            .filter { it.value == gamesMap.maxOf { games -> games.value } }
            .map { Game.find { Games.name eq it.key }.first() }
    }

    override fun valueGamesInUserLibDoesNotSupportNativeLang(userLogin: String): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        //println(
        // User.find { Users.login eq userLogin }.first()
        //     .apply { println(nativeLanguage.liter) }
        //     .library.joinToString { it.languages.joinToString { lang -> lang.liter } }
        // )
        Users.innerJoin(UsersLibrary).innerJoin(Games).innerJoin(GameLanguages)
            .slice(Languages.liter)
            .select { Users.login eq userLogin and (GameLanguages.language neq Users.nativeLanguages) }
            .count()
            .toInt()
    }

    override fun equalsUsersLib(user1Login: String, user2Login: String) {
        val coincidencesGames = mutableListOf<String>()
        val onlyFirstUserHave = mutableListOf<String>()
        val lib2 = mutableListOf<String>()
        transaction {
            val query = Users.innerJoin(UsersLibrary).innerJoin(Games)
                .slice(Games.name, Users.login)
                .select { Users.login eq user1Login or (Users.login eq user2Login) }
                .groupBy(Games.name, Users.login)
            lib2.addAll(query.filter { it[Users.login] == user2Login }.map { it[Games.name] })
            query
                .filter { it[Users.login] == user1Login }
                .map { it[Games.name] }
                .forEach { game ->
                    if (lib2.contains(game))
                        coincidencesGames.add(game)
                    else
                        onlyFirstUserHave.add(game)
                }
        }
        println("Coincidences games: ${coincidencesGames.joinToString(", ")}" )
        println("unique games $user1Login: ${onlyFirstUserHave.joinToString(", ")}" )
        println("unique games $user2Login: ${lib2.filter { !coincidencesGames.contains(it) }.joinToString(", ")}" )
    }

    override fun listUsersWithGameInLibWhichSupLang(lang: String): List<User> =
        transaction(database) {
            val gamesWithLang = Games.innerJoin(GameLanguages).innerJoin(Languages)
                .slice(Games.columns)
                .select { Languages.liter eq lang }

            val query = Users.innerJoin(UsersLibrary).innerJoin(Games)
                .slice(Users.columns)
                .select { Games.name inList gamesWithLang.map { it[Games.name] } }
                .withDistinct()
            return@transaction User.wrapRows(query).toList()
        }
}