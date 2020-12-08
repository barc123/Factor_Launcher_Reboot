package com.factor.launcher.database

import androidx.room.*
import com.factor.launcher.model.UserApp

@Database(entities = [UserApp::class], version = 1)
abstract class AppListDatabase : RoomDatabase()
{
    abstract fun appListDao(): AppListDao
}

@Dao
interface AppListDao
{
    @Query("SELECT * FROM userApp")
    fun getAll(): List<UserApp>

    @Query("SELECT * FROM userApp WHERE labelOld LIKE:label OR labelNew LIKE :label ")
    fun findByName(label: String): List<UserApp>

    @Query("SELECT * FROM userApp WHERE name =:term LIMIT 1")
    fun findByPackage(term: String): UserApp

    @Insert
    fun insertAll(vararg users: UserApp)

    @Insert
    fun insert(user: UserApp)

    @Delete
    fun delete(user: UserApp)
}