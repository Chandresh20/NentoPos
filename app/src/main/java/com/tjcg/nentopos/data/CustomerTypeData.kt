package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "CustomerType")
class CustomerTypeData {

    @PrimaryKey
    @NonNull
    @SerializedName("cutomertype")
    var customerType : Int = 0

    @SerializedName("type")
    var typeName : String? = null

    override fun toString(): String {
        return typeName ?: "NA"
    }
}