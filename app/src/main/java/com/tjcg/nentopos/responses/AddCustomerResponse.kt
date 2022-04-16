package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName
import com.tjcg.nentopos.data.CustomerData

class AddCustomerResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var customerData : CustomerList? = null

    class CustomerList {

        @SerializedName("customers_list")
        var customerList : List<CustomerData>? = null
    }
}