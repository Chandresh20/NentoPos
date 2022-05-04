package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Tables")
class TableData {

    @PrimaryKey
    @NonNull
    @SerializedName("tableid")
    var tableId: Int = 0

    @SerializedName("tablename")
    var tableName : String? = null

    @SerializedName("person_capicity")
    var personCapacity : Int? = null

    @SerializedName("table_icon")
    var tableIcon : String? = null

    @SerializedName("icon_position")
    var iconPosition : String? = null

    @SerializedName("is_assign_customer")
    var assignedOrNot: Int? = null

    @SerializedName("related_orders")
    var relatedOrders : List<RelatedOrderInfo>? = null

    var outletId : Int? = null

    class RelatedOrderInfo {

        @SerializedName("order_id")
        var orderId : Long? = null

        @SerializedName("order_status")
        var orderStatus : Int? = null
    }

    override fun toString(): String {
        return tableName ?: "NA"
    }
}