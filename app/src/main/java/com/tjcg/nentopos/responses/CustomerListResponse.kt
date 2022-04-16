package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName
import com.tjcg.nentopos.data.CustomerData

class CustomerListResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var customers : List<CustomerData>? = null
}