package com.tjcg.nentopos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Edited_Orders")
class OrderUpdateSync {

    @PrimaryKey
    @SerializedName("outlet_id")
    var outletId: Int = 0

    @SerializedName("order_Data")
    var editedOrders : List<OrderUpdateRequest>? = null
}