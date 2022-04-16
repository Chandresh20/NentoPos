package com.tjcg.nentopos.data

import com.google.gson.annotations.SerializedName

class OfflineOrderSyncRequest {

    @SerializedName("orders")
    val orders : ArrayList<OfflineOrder2> = ArrayList()
}