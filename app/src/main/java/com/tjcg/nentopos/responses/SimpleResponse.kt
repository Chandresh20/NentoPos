package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName
class SimpleResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

 /*   @SerializedName("data")
    var menuData : Any? = null  */
}