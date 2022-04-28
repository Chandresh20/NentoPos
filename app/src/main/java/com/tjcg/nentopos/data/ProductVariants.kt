package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Variants")
class ProductVariants {

    @SerializedName("item_menu_variant_id")
    var itemMenuVariantId: String? = null

    @PrimaryKey
    @NonNull
    @SerializedName("variantid")
    var variantId: Int = 0

    @SerializedName("variantName")
    var variantName: String? = null

    @SerializedName("variant_price")
    var variantPrice: String? =null
}