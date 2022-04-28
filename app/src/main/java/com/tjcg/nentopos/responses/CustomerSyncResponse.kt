package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName

class CustomerSyncResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var customersMap : List<CustomerMap>? = null

    class CustomerMap {

        @SerializedName("customer_temp_id")
        val customerTmpId : Long? = null

        @SerializedName("customer_original_id")
        val customerOriginalId: Long? = null
    }
}