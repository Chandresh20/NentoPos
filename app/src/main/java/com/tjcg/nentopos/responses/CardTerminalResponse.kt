package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName
import com.tjcg.nentopos.data.CardTerminalData

class CardTerminalResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var cardData : CardType? = null

    class CardType {

        @SerializedName("card_type")
        var cardTypes: List<CardTerminalData>? = null
    }
}