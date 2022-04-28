package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ProductAddOns")
class ProductAddOns {

    @PrimaryKey
    @NonNull
    @SerializedName("add_on_id")
    var addOnId : Int = 0

    @SerializedName("row_id")
    var rowId : Int? = null

    @SerializedName("add_on_name")
    var addOnName : String? = null

    @SerializedName("addons_price")
    var addonPrice : String? = null

    @SerializedName("tax_id")
    var addOnTaxIds : String? = null

    @SerializedName("addon_taxes")
    var addOnTaxes : List<ProductTax>? = null

}