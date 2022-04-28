package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ProductTaxes")
class ProductTax {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    var id: Int = 0

    @SerializedName("tax_lable")
    var taxLabel : String? = null

    @SerializedName("tax_percentage")
    var taxPercentage : String? = null

    @SerializedName("tax_type")
    var taxType : String? = null
}