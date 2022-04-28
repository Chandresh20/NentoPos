package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Outlets")
class OutletData {

    @PrimaryKey
    @NonNull
    @SerializedName("outlet_id")
    var outletId : Int = 0

    @SerializedName("unique_id")
    var uniqueId: String? = null

    @SerializedName("name")
    var outletName : String? = null

    @SerializedName("email")
    var email : String? = null

    @SerializedName("phoneno")
    var phoneNo : String? = null

    @SerializedName("address")
    var address : String? = null

    @SerializedName("is_default")
    var sDefault : Int? = null
}