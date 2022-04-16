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

    var outletId : Int? = null

    override fun toString(): String {
        return tableName ?: "NA"
    }
}