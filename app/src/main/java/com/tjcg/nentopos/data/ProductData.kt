package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Products")
class ProductData {

    @PrimaryKey
    @NonNull
    @SerializedName("ProductsID")
    var productId : Int = 0

    @SerializedName("category_id")
    var categoryId: Int? = null

    @SerializedName("ProductName")
    var productName : String? = null

    @SerializedName("ProductPrice")
    var productPrice : String? = null

    @SerializedName("tax_id")
    var productTaxIds : String? = null

    @SerializedName("is_display_in_pos")
    var sDisplayInPOS : Int? = null

    @SerializedName("is_scheduled_item")
    var sScheduledItem : Int? = null

    @SerializedName("scheduled_day")
    var scheduledDay : String? = null

    @SerializedName("scheduled_start_time")
    var scheduledStartTime : String? = null

    @SerializedName("scheduled_end_time")
    var scheduledEndTime : String? = null

    @SerializedName("product_taxes")
    var productTaxes : List<ProductTax>? = null

    @SerializedName("product_variants")
    var productVariants : List<ProductVariants>? = null

    @SerializedName("product_addons")
    var productAddOns : List<ProductAddOns>? = null

    @SerializedName("product_modifiers")
    var productModifiers : List<ProductModifier>? = null
}