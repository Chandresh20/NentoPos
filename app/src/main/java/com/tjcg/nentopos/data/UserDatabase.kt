package com.tjcg.nentopos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OutletData::class, CustomerData::class, CustomerOffline::class,
    TableData::class, SubUserData::class, CustomerTypeData::class, CardTerminalData::class],
    version = 1)

abstract class UserDatabase : RoomDatabase() {

    abstract fun getUserDao() : UserDao
    companion object {
        var instance : UserDatabase? = null
        fun getDatabase(ctx: Context) : UserDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(ctx, UserDatabase::class.java, "UserDatabase")
                    .build()
            }
            return instance!!
        }
    }
}