package com.tjcg.nentopos.data

import com.google.gson.annotations.SerializedName

class OrderUpdateRequest {

    @SerializedName("bill_status")
    var billStatus: Int? = 0

    @SerializedName("cookedtime")
    var cookedTime : String? = null

    @SerializedName("customer_id")
    var customerId : Long? = null

    @SerializedName("customerpaid")
    var customerPaid : Float? = null

    @SerializedName("cutomertype")
    var customerType : String? = null

    @SerializedName("driver_user_id")
    var driverUserId : Int? = null

    @SerializedName("order_id")
    var orderID: Long? = null

    @SerializedName("tip_type")
    var tipType : String? = null

    @SerializedName("tip_value")
    var tipValue : Float? = null

    @SerializedName("is_payment_received")
    var paymentReceivedOrNot : Int? = 0

    @SerializedName("bill_date")
    var billDate : String? = null

    @SerializedName("card_type")
    var cardType : Int? = null

    @SerializedName("discount")
    var discount: Float? = 0f

    @SerializedName("is_order_delivered")
    var orderDeliveredOrNot : Int? = 0

    @SerializedName("received_payment_amount")
    var receivedPaymentAmount : Float? = null

    @SerializedName("outlet_id")
    var outletId : Int? = null

    @SerializedName("device_id")
    var deviceId : String? = null

    @SerializedName("payment_method_id")
    var paymentMethodId: String? = null

    @SerializedName("account_number")
    var accountNo : String? = null

    @SerializedName("selected_card")
    var selectedCard : String? = null

    @SerializedName("selected_card_type")
    var selectedCardType : String? = null

    @SerializedName("is_driver_assigned")
    var driverAssigned : Int? = 0

    @SerializedName("totalamount")
    var totalAmount : Float? = null

    @SerializedName("waiter_id")
    var waiterId : Int? = 0

    @SerializedName("order_date")
    var orderDate : String? = null

    @SerializedName("order_status")
    var orderStatus : Int? = 2

    @SerializedName("table")
    var table : Int? = null

    @SerializedName("tip_amount")
    var tipAmount : Float? = null

    @SerializedName("order_time")
    var orderTime: String? = null

    @SerializedName("bill_time")
    var billTime : String? = null

    @SerializedName("deleted_rows")
    var deletedRows: String? = null

    @SerializedName("cart_items")
    var cartItems : ArrayList<CartItem>? = null

    class CartItem {

        @SerializedName("catid")
        var catId : Int? = null

        @SerializedName("pid")
        var productId : Int? = null

        @SerializedName("sizeid")  // ==variantId
        var sizeId : Int? = null

        @SerializedName("varientname") // e.g. regular
        var variantName : String? = null

        @SerializedName("qty")
        var qty: Int? = null

        @SerializedName("addonsid")  // e.g. "500,502"
        var addOnIds : String? = null

        @SerializedName("adonsname") // e.g. "Cola,Pepsi"
        var addOnName : String? = null

        @SerializedName("adonsppprice") // e.g. "110,50"
        var addOnPrice: String? = null

        @SerializedName("adonsqty") // e.g "1,2"
        var addOnQty : String? = null

        @SerializedName("adonsunitprice")
        var addOnUnitPrice: String? = null

        @SerializedName("mainmodifierid")  // e.g. "1052, 1049"
        var mainModifierId : String? = null

        @SerializedName("mainmodifiername") // e.g. "Toppings,Traditional Toppings"
        var mainModifierName : String? = null

        @SerializedName("mainmodifierqty")  // e.g. "1,1"
        var mainModifierQty : String? = null

        @SerializedName("sub_mod_id") // e.g. "5089,5090,5091,4505,4506,4507"
        var subModIds : String? = null

        @SerializedName("sub_mod_name") // e.g. "Extra Cheese,Grilled Mushrooms,Onion,Homemade Sausage,Pepperoni,Bacon"
        var subModNames : String? = null

        @SerializedName("sub_mod_type_name") // e.g "whole,whole,whole,whole,first-half,second-half"
        var subModeTypeName: String? = null

        @SerializedName("sub_mod_type_int")  // e.g. "0,0,0,0,1,2"
        var subModeTypeInt : String? = null

        @SerializedName("main_mod_total_price")  // e.g. "230,130"
        var mainModTotalPrices : String? = null

        @SerializedName("is_half_and_half") // e.g. "0,1,1" 0=in Full, 1 = in Half and Half
        var sHalfAndHalf : String? = null

        @SerializedName("subMod2x") // e.g. "5091, 5090". Ids of 2x selected sub modifiers
        var subMod2x : String? = null

        @SerializedName("price")
        var price: Float? = null



   /*     @SerializedName("itemname")
        var itemName: String? = null

        @SerializedName("price")
        var price: Float? = null

        @SerializedName("modifierid")
        var modifierId : String? = null

        @SerializedName("modifierprice")
        var modifierPrice : String? = null

        @SerializedName("modifiername")
        var modifierName : String? = null

        @SerializedName("modifierType")
        var modifierType : String? = null

        @SerializedName("modifierTypeINT")
        var modifierTypeInt : String? = null

        @SerializedName("is_half_and_half")
        var isHalfAndHalf : String? = null

        @SerializedName("subMod2x")
        var subMod2x : String? = null

        @SerializedName("totalItemDiscountedPrice")
        var totalItemDiscountedPrice : Float? = null

        @SerializedName("itemDiscountType")
        var itemDiscountType : Int? = null

        @SerializedName("offer_value")
        var offerValue : Float? = null

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

        var rowId: String? = null  */
    }

}