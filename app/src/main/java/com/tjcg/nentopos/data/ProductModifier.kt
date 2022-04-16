package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ProductModifiers")
class ProductModifier {

    @PrimaryKey
    @NonNull
    @SerializedName("modifier_id")
    var modifierId: Int = 0

    @SerializedName("global_modifier_id")
    var globalModifierId : Int?  = null

    @SerializedName("global_modifier_name")
    var modifierName : String? = null

    @SerializedName("is_half_and_half")
    var halfAndHalf : String? = null

    @SerializedName("is_2x")
    var available2x : Int? = null

    @SerializedName("total_modifiers")
    var totalModifiers : Int? = null

    @SerializedName("modifier_included")
    var modifiersIncluded : Int? = null

    @SerializedName("choosable_modifier")
    var choosableModifiers : Int? = null

    @SerializedName("product_sub_modifiers")
    var subModifiers : List<ProductSubModifier>? = null
}