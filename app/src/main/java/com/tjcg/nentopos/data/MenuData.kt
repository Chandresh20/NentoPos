package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Menus")
class MenuData {

    @SerializedName("outlet_id")
    var outletId: String? = null

    @PrimaryKey
    @NonNull
    @SerializedName("menu_id")
    var menuId : Int = 0

    @SerializedName("menu_name")
    var menuName : String? = null

    @SerializedName("is_on_frontend")
    var onFrontend : Int? = null

    @SerializedName("is_on_pos")
    var onPos : Int? = null

    @SerializedName("position")
    var position : Int? = null

    @SerializedName("categories")
    var categories: List<CategoryData>? = null
}