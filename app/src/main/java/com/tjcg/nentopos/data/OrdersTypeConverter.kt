package com.tjcg.nentopos.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OrdersTypeConverter {

    @TypeConverter
    fun fromItemsInfo(list: List<OrdersEntity.ItemInfo>?) : String {
        val gson = Gson()
        val typeT = object : TypeToken<List<OrdersEntity.ItemInfo>>() { }
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toItemsInfo(string: String) : List<OrdersEntity.ItemInfo> {
        val gson = Gson()
        val typeT = object : TypeToken<List<OrdersEntity.ItemInfo>>() {}
        val list = gson.fromJson<List<OrdersEntity.ItemInfo>>(string, typeT.type)
        return if (list.isNullOrEmpty()) {
            ArrayList()
        } else {
            list
        }
    }

    @TypeConverter
    fun fromAddOns(addons: List<OrdersEntity.AddOn>?) :String {
        val gson = Gson()
        val typeT = object : TypeToken<List<OrdersEntity.AddOn>>() { }
        return gson.toJson(addons, typeT.type)
    }

    @TypeConverter
    fun toAddOns(str: String) : List<OrdersEntity.AddOn> {
        val gson = Gson()
        val typeT = object : TypeToken<List<OrdersEntity.AddOn>>() {}
        val list = gson.fromJson<List<OrdersEntity.AddOn>>(str, typeT.type)
        return if (list.isNullOrEmpty()) {
            ArrayList()
        } else {
            list
        }
    }


    @TypeConverter
    fun fromBillInfo(info : OrdersEntity.BillInfo?) : String {
        val gson = Gson()
        val typeT = object : TypeToken<OrdersEntity.BillInfo>() { }
        return gson.toJson(info, typeT.type)
    }

    @TypeConverter
    fun toBillInfo(string: String): OrdersEntity.BillInfo? {
        val gson = Gson()
        val typeT = object : TypeToken<OrdersEntity.BillInfo>() {}
        return gson.fromJson(string, typeT.type)
    }

    @TypeConverter
    fun fromCartItems(info : List<ProductOrderRequest.CartItem>?) : String {
        val gson = Gson()
        val typeT = object : TypeToken<List<ProductOrderRequest.CartItem>>() { }
        return gson.toJson(info, typeT.type)
    }

    @TypeConverter
    fun toCartItems(string: String): List<ProductOrderRequest.CartItem>? {
        val gson = Gson()
        val typeT = object : TypeToken<List<ProductOrderRequest.CartItem>>() {}
        return gson.fromJson(string, typeT.type)
    }

    @TypeConverter
    fun fromEditedOrders(eOrders : List<OrderUpdateRequest>?) : String? {
        val gson = Gson()
        val typeT = object : TypeToken<List<OrderUpdateRequest>>() { }
        return gson.toJson(eOrders, typeT.type)
    }

    @TypeConverter
    fun toEditedOrders(string: String): List<OrderUpdateRequest>? {
        val gson = Gson()
        val typeT = object : TypeToken<List<OrderUpdateRequest>>() {}
        return gson.fromJson(string, typeT.type)
    }

}