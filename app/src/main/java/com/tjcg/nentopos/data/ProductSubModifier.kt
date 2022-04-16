package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ProductSubModifiers")
class ProductSubModifier {

    @PrimaryKey
    @NonNull
    @SerializedName("sub_modifier_id")
    var subModifierId : Int = 0

    @SerializedName("global_sub_modifier_id")
    var globalSubModifierId : Int? = null

    @SerializedName("global_sub_modifier_name")
    var subModifierName : String? = null

    @SerializedName("variant_modifier_price")
    var variantModifierPrice : List<VariantModifierPrice>? = null

    @SerializedName("normal_modifier_price")
    var normalModifierPrice : String? = null

    class VariantModifierPrice {

        @SerializedName("variant_id")
        var variantId : Int? = null

        @SerializedName("modifier_price")
        var modifierPrice : String? = null
    }
}