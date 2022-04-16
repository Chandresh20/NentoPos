package com.tjcg.nentopos.responses

class OrderAcceptResponse(
    val data: Data,
    val message: String,
    val status: Boolean) {

    data class Data(
        val order_id: String,
        val tokenNo: Int)
}