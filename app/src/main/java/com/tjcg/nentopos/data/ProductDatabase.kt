package com.tjcg.nentopos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database (entities = [MenuData::class, CategoryData::class, ProductData::class,
    ProductTax::class, ProductVariants::class, ProductAddOns::class, ProductModifier::class,
                      ProductSubModifier::class, DiscountData::class], version = 1)
@TypeConverters(ProductTypeConverter::class)
abstract class ProductDatabase : RoomDatabase() {

    abstract fun getProductDao() : ProductDao

    companion object {
        var instance : ProductDatabase? = null
        fun getDatabase(ctx : Context) : ProductDatabase {
            return if (instance == null) {
                Room.databaseBuilder(ctx, ProductDatabase::class.java, "ProductDatabase").build()
            } else {
                instance!!
            }
        }
    }

}