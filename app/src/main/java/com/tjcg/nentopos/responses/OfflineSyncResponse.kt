package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName

class OfflineSyncResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var sycResponse : List<SyncResponse>? = null

    class SyncResponse {

        @SerializedName("status")
        var status : Boolean? = null

        @SerializedName("tmp_order_id")
        var tmpOrderId : Long? = null

        @SerializedName("order_id")
        var orderId : Int? = null

   /*     @SerializedName("tokenno")
        var tokenNo : String? = null  */
    }
}