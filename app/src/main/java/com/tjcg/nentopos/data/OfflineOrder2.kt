package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "OfflineOrder2")
class OfflineOrder2 {

    @PrimaryKey
    @NonNull
    @SerializedName("tmp_order_id")
    var tmpOrderId : Long = 0

    @SerializedName("cutomertype")
    var customerType : Int? = null

    @SerializedName("bill_time")
    var billTime : String? = null

    @SerializedName("selected_card_type")
    var selectedCardType : String? = null

    @SerializedName("driver_user_id")
    var driverUserId : String? = null

    @SerializedName("order_date")
    var orderDate : String? = null

    @SerializedName("cart_items")
    var cartItems : List<ProductOrderRequest.CartItem>? = null

    @SerializedName("outlet_id")
    var outletId : Int? = null

    @SerializedName("order_accept_date")
    var orderAcceptDate: String? = null

    @SerializedName("bill_date")
    var billDate : String? = null

    @SerializedName("customerpaid")
    var customerPaid : Float? = null

    @SerializedName("table")
    var table : String? = null

    @SerializedName("account_number")
    var accountNo : String? = null

    @SerializedName("is_driver_assigned")
    var driverAssigned : Int? = 0

    @SerializedName("card_type")
    var cardType : Int? = null

    @SerializedName("discount")
    var discount: Float? = 0f

    @SerializedName("bill_status")
    var billStatus: Int? = 0

    @SerializedName("cookedtime")
    var cookedTime : String? = null

    @SerializedName("order_time")
    var orderTime: String? = null

    @SerializedName("tip_amount")
    var tipAmount : Float? = null

    @SerializedName("tip_type")
    var tipType : String? = null

    @SerializedName("create_by")
    var createdBy : String? = null

    @SerializedName("totalamount")
    var totalAmount : Float? = null

    @SerializedName("payment_method_id")
    var paymentMethodId: Int? = 0

    @SerializedName("tip_value")
    var tipValue : Float? = null

    @SerializedName("waiter_id")
    var waiterId : Int? = 0

    @SerializedName("customer_id")
    var customerId : Long? = null

    @SerializedName("received_payment_amount")
    var receivedPaymentAmount : Float? = null

    @SerializedName("selected_card")
    var selectedCard : String? = null

    @SerializedName("is_order_delivered")
    var orderDeliveredOrNot : Int? = 0

    @SerializedName("is_payment_received")
    var paymentReceivedOrNot : Int? = 0

    @SerializedName("order_status")
    var orderStatus : Int? = 2
}