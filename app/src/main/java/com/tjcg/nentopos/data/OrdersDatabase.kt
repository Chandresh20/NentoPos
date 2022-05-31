package com.tjcg.nentopos.data

import android.content.Context
import androidx.room.*

@Database(entities = [OrdersEntity::class, OfflineOrder::class, OfflineOrder2::class,
                     OrderUpdateSync::class], version = 1)
@TypeConverters(OrdersTypeConverter::class)
abstract class OrdersDatabase : RoomDatabase() {

    abstract fun getOrdersDao() : OrdersDao

    companion object {
        private var instance : OrdersDatabase? = null

        fun getDatabase(ctx: Context) : OrdersDatabase {
            return if (instance == null) {
                val build = Room.databaseBuilder(ctx, OrdersDatabase::class.java, "ordersDatabase")
                build.build()
            } else {
                instance!!
            }
        }
    }
}