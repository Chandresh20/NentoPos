package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Categories")
class CategoryData {

    @PrimaryKey
    @NonNull
    @SerializedName("category_id")
    var categoryId: Int = 0

    @SerializedName("category_name")
    var categoryName : String? = null

    @SerializedName("products")
    var productSummaries : List<ProductData>? = null
}