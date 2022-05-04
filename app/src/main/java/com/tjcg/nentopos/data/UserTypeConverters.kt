package com.tjcg.nentopos.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserTypeConverters {

    @TypeConverter
    fun fromRelatedOrderInfo(list: List<TableData.RelatedOrderInfo>) : String? {
        val typeT = object: TypeToken<List<TableData.RelatedOrderInfo>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toRelatedOrderInfo(str: String) : List<TableData.RelatedOrderInfo>? {
        val typeT = object : TypeToken<List<TableData.RelatedOrderInfo>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }
}