package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "CardTerminals")
class CardTerminalData {

    @PrimaryKey
    @NonNull
    @SerializedName("card_terminalid")
    var cardTerminalId : Int = 0

    @SerializedName("outlet_id")
    var outletId: Int? = null

    @SerializedName("terminal_name")
    var terminalName : String? = null
}