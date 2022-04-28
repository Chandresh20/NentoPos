package com.tjcg.nentopos.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProductTypeConverter {

    @TypeConverter
    fun fromCategory(list: List<CategoryData>) : String? {
        val typeT = object: TypeToken<List<CategoryData>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toCategory(str: String) : List<CategoryData>? {
        val typeT = object : TypeToken<List<CategoryData>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromProducts(list: List<ProductData>) : String? {
        val typeT = object: TypeToken<List<ProductData>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toProducts(str: String) : List<ProductData>? {
        val typeT = object : TypeToken<List<ProductData>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromVariants(list: List<ProductVariants>) : String? {
        val typeT = object: TypeToken<List<ProductVariants>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toVariants(str: String) : List<ProductVariants>? {
        val typeT = object : TypeToken<List<ProductVariants>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromAddOns(list: List<ProductAddOns>) : String? {
        val typeT = object: TypeToken<List<ProductAddOns>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toAddOns(str: String) : List<ProductAddOns>? {
        val typeT = object : TypeToken<List<ProductAddOns>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromTaxes(list: List<ProductTax>) : String? {
        val typeT = object: TypeToken<List<ProductTax>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toTaxes(str: String) : List<ProductTax>? {
        val typeT = object : TypeToken<List<ProductTax>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromModifiers(list: List<ProductModifier>) : String? {
        val typeT = object: TypeToken<List<ProductModifier>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toModifiers(str: String) : List<ProductModifier>? {
        val typeT = object : TypeToken<List<ProductModifier>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromSubModifiers(list: List<ProductSubModifier>) : String? {
        val typeT = object: TypeToken<List<ProductSubModifier>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toSubModifiers(str: String) : List<ProductSubModifier>? {
        val typeT = object : TypeToken<List<ProductSubModifier>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }

    @TypeConverter
    fun fromVariantModPrice(list: List<ProductSubModifier.VariantModifierPrice>?) : String? {
        val typeT = object: TypeToken<List<ProductSubModifier.VariantModifierPrice>>() { }
        val gson = Gson()
        return gson.toJson(list, typeT.type)
    }

    @TypeConverter
    fun toVariantModPrice(str: String) : List<ProductSubModifier.VariantModifierPrice>? {
        val typeT = object : TypeToken<List<ProductSubModifier.VariantModifierPrice>>() { }
        val gson = Gson()
        return gson.fromJson(str, typeT.type)
    }
}