package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName

class CreateOrderResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var orderData : SuccessInfo? = null

    class SuccessInfo {

        @SerializedName("order_id")
        var orderId : Int? = null

  /*      @SerializedName("tokenno")
        var tokenNo : String? = null  */
    }
}