package com.tjcg.nentopos.data

import com.google.gson.annotations.SerializedName

class OrderSyncEntity {

    @SerializedName("orders")
    var ordersSync : List<OrderSync>? =null

    class OrderSync {

        @SerializedName("order_id")
        var orderId : Long? = null

        @SerializedName("outlet_id")
        var outlet_id: Int? = null

        @SerializedName("waiter_id")
        var waiter_id: Int? = null

        @SerializedName("order_accept_date")
        var order_accept_date: String? = null

        @SerializedName("cookedtime")
        var cookedtime: String? = null


        @SerializedName("discount")
        var discount: String? = null

        @SerializedName("tip_type")
        var tip_type: String? = null

        @SerializedName("tip_value")
        var tip_value: String? = null

        @SerializedName("added_tip_amount")
        var added_tip_amount: String? = null

        @SerializedName("tip_amount")
        var tip_amount: String? = null

        @SerializedName("totalamount")
        var totalamount: Float? = null

        @SerializedName("customerpaid")
        var customerpaid: String? = null

        @SerializedName("customer_note")
        var customer_note: String? = null

        @SerializedName("anyreason")
        var anyreason: String? = null

        @SerializedName("order_status")
        var order_status: Int? = null

        @SerializedName("is_driver_assigned")
        var pis_driver_assigned: Int? = null

        @SerializedName("driver_user_id")
        var driver_user_id: Int? = null

        @SerializedName("item_info")
        var itemInfoSyn : List<ItemInfoSyn>? = null

        @SerializedName("billinfo")
        var billInfoSyn : BillInfoSyn? = null
    }

    class ItemInfoSyn {

        @SerializedName("row_id")
        var rowId : String? = null

        @SerializedName("unique_record_id")
        var uniqueRecordId : String? = null

        @SerializedName("food_status")
        var foodStatus : Int? = null

    }

    class BillInfoSyn {

        @SerializedName("bill_id")
        var billId : String? = null

        @SerializedName("bill_status")
        var billStatus : Int? = null

        @SerializedName("payment_method_id")
        var paymentMethodId : String? = null

        @SerializedName("account_number")
        var accountNumber : String? = null

        @SerializedName("selected_card")
        var selectedCard : String? = null

        @SerializedName("card_type")
        var cardType :String? = null
    }
}