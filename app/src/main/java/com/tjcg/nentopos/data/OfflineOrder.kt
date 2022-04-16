package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "OfflineOrders")
class OfflineOrder {

    @PrimaryKey
    @NonNull
    @SerializedName("tmp_order_id")
    var id : Long = 0

    var outletId : Int? = null
}