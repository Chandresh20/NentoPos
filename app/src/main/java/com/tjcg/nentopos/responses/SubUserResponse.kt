package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName
import com.tjcg.nentopos.data.SubUserData

class SubUserResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var subUsers : List<SubUserData>? = null
}