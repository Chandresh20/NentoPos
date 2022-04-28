package com.tjcg.nentopos.data

import com.google.gson.annotations.SerializedName

class ProductOrderRequest {

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
    var cartItems : List<CartItem>? = null

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

    class CartItem {

        @SerializedName("ProductsID")
        var productId : Int? = null

        @SerializedName("add_on_id")
        var addOnIds : String? = null

        @SerializedName("addonsqty")
        var addOnQty : Int? = null

        @SerializedName("combomain_menu_id")
        var comboMainMenuId: String? = null

        @SerializedName("discount_type")
        var discountType : Int? = null

        @SerializedName("food_status")
        var foodStatus : Int? = null

        @SerializedName("is_2x_mod")
        var is2xMode : String? = null

        @SerializedName("item_discount")
        var itemDiscount : Int? = null

        @SerializedName("item_menuid_string")
        var itemMenuIdString : String? = null

        @SerializedName("menuqty")
        var menuQty : Int? = null

        @SerializedName("modifier_qty")
        var modifierQty : Float? = null

        @SerializedName("modifier_id")
        var modifierIdStr : String? = null // e.g "352,353"

        @SerializedName("modifier_price")
        var modifierPriceStr : String? = null // e.g. "110.0, 40.0"

        @SerializedName("modifier_type")
        var modifierType : String? = null

        @SerializedName("modifier_type_string")
        var modifierTypeString : String? = null

        @SerializedName("order_notes")
        var orderNotes : String? = null

        @SerializedName("sub_mod_id")
        var subModIds : String? = null // e.g. "1594,1595,1596,1599,1600"

        @SerializedName("sub_mod_id_string")
        var subModIdStr : String? = null // e.g. "1594,1595,1596,1599,1600"

        @SerializedName("tax_id")
        var taxIds : String? = null // e.g. "3,6"

        @SerializedName("tax_percentage")
        var taxPercentages : String? = null // e.g. "10,13"

        @SerializedName("varientid")
        var variantId: String? = null

        var rowId: String? = null
    }
}