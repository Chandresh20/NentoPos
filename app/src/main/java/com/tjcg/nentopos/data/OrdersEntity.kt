package com.tjcg.nentopos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "orders")
class OrdersEntity {

    @PrimaryKey
    @SerializedName("order_id")
    @Expose
    var order_id: Long = 0

    @SerializedName("outlet_id")
    @Expose
    var outlet_id: Int? = null

    @SerializedName("menu_id")
    @Expose
    var menu_id: Int? = null

    @SerializedName("saleinvoice")
    @Expose
    var saleinvoice: String? = null

    @SerializedName("customer_id")
    @Expose
    var customer_id: Long? = null

    @SerializedName("cutomertype")
    @Expose
    var cutomertype: String? = null

    @SerializedName("isthirdparty")
    @Expose
    var thirdpartyOrNot: String? = null

    @SerializedName("waiter_id")
    @Expose
    var waiter_id: Int? = null

    @SerializedName("kitchen")
    @Expose
    var kitchen: String? = null

    @SerializedName("order_date")
    @Expose
    var order_date: String? = null

    @SerializedName("order_time")
    @Expose
    var order_time: String? = null

    @SerializedName("order_accept_date")
    @Expose
    var order_accept_date: String? = null

    @SerializedName("cookedtime")
    @Expose
    var cookedtime: String? = null

    @SerializedName("table_no")
    @Expose
    var table_no: Int? = null

    @SerializedName("tokenno")
    @Expose
    var tokenno: String? = null

    @SerializedName("discount")
    @Expose
    var discount: String? = null

    @SerializedName("tip_type")
    @Expose
    var tip_type: String? = null

    @SerializedName("added_tip_amount")
    @Expose
    var added_tip_amount: String? = null

    @SerializedName("tip_value")
    @Expose
    var tip_value: String? = null

    @SerializedName("tip_amount")
    @Expose
    var tip_amount :String? = null

    @SerializedName("totalamount")
    @Expose
    var totalamount: Float? = null

    @SerializedName("customerpaid")
    @Expose
    var customerpaid: String? = null

    @SerializedName("customer_note")
    @Expose
    var customer_note: String? = null

    @SerializedName("anyreason")
    @Expose
    var anyreason: String? = null

    @SerializedName("order_status")
    @Expose
    var order_status: Int? = null

    @SerializedName("is_driver_assigned")
    @Expose
    var pis_driver_assigned: Int? = null

    @SerializedName("driver_user_id")
    @Expose
    var driver_user_id: Int? = null

    @SerializedName("is_order_delivered")
    @Expose
    var pis_order_delivered: Int? = null

    @SerializedName("is_payment_received")
    @Expose
    var pis_payment_received: String? = null

    @SerializedName("received_payment_amount")
    @Expose
    var received_payment_amount: String? = null

    @SerializedName("order_pickup_at")
    @Expose
    var order_pickup_at: String? = null

    @SerializedName("future_order_date")
    @Expose
    var future_order_date: String? = null

    @SerializedName("future_order_time")
    @Expose
    var future_order_time: String? = null

    @SerializedName("nofification")
    @Expose
    var nofification: String? = null

    @SerializedName("online_order_notification")
    @Expose
    var online_order_notification: String? = null

    @SerializedName("kitchen_notification")
    @Expose
    var kitchen_notification: String? = null

    @SerializedName("orderacceptreject")
    @Expose
    var orderacceptreject: String? = null

    @SerializedName("is_frontend_order")
    @Expose
    var pis_frontend_order: String? = null

    @SerializedName("is_qr_order")
    @Expose
    var pis_qr_order: String? = null

    @SerializedName("customer_name")
    @Expose
    var customer_name: String? = "NA"

    @SerializedName("customer_type")
    @Expose
    var customer_type: String? = "NA"

    @SerializedName("first_name")
    @Expose
    var first_name: String? = null

    @SerializedName("last_name")
    @Expose
    var last_name: String? = null

    @SerializedName("tablename")
    @Expose
    var tablename: String? = "NA"

    @SerializedName("bill_status")
    @Expose
    var bill_status: String? = "NA"

    @SerializedName("account_number")
    @Expose
    var account_number: String? = "NA"

    @SerializedName("payment_method_id")
    @Expose
    var payment_method_id: String? = "NA"

    var syncOrNot: Int? = 1

    // added for sorting in counter display
    var remainedTime:Long = 0L
    // added for futureOrderType (changes as per time)
    var futureOrderType : Int? = null

    // added after AllOrder API update

    @SerializedName("item_info")
    var itemsInfo : List<ItemInfo>? = null

    @SerializedName("addons")
    var addOns : List<AddOn>? = null

    @SerializedName("billinfo")
    var billInfo : BillInfo? = null

    class ItemInfo {

        @SerializedName("row_id")
        var rowId : String? = null

        @SerializedName("order_id")
        var orderId: Long? = null

        @SerializedName("unique_record_id")
        var uniqueRecordId : String? = null

        @SerializedName("food_status")
        var foodStatus : Int? = null

        @SerializedName("menu_id")
        var menuId: Int? = null

        @SerializedName("menuqty")
        var menuQty : String? = null

        @SerializedName("varientid")
        var varientId :Int? = null

        @SerializedName("ProductsID")
        var productId : Int? = null

        @SerializedName("tax_id")
        var taxId : String? = null

        @SerializedName("tax_percentage")
        var taxPercentage : String? = null

        @SerializedName("is_half_and_half")
        var isHalfAndHalf : String? = null

        @SerializedName("is_2x_mod")
        var is2xMod : String? = null

        @SerializedName("discount_type")
        var discountType: String? = null

        @SerializedName("item_discount")
        var itemDiscount: String? = null

        @SerializedName("ProductPrice")
        var productPrice : String? = null

        @SerializedName("modifier_info")
        var modifierInfo : List<ModifierInfo>? = null

        @SerializedName("order_notes")
        var orderNote : String? = null
    }

    class ModifierInfo {

        @SerializedName("modifier_id")
        var modifierId : Int? = null

        @SerializedName("is_half_and_half")
        var isHalfAndHalf : String? = null

        @SerializedName("sub_modifier")
        var subModifier : SubModifier? = null

        //      @SerializedName("sub_modifier")
        //      var subModifiers : List<SubModifierAsArray>? = null
    }

    class SubModifier {

        @SerializedName("modifier_list")
        var subModifierList : List<SubModifierList>? = null

        @SerializedName("Whole")
        var subModifierWhole : List<SubModifierWhole>? = null

        @SerializedName("First_half")
        var subModifierFirstHalf : List<SubModifierFirstHalf>? = null

        @SerializedName("Second_half")
        var subModifierSecondHalf : List<SubModifierSecondHalf>? = null
    }

    class SubModifierList {

        @SerializedName("sub_mod_id")
        var subModifierId : Int? = null

        @SerializedName("is_2x_mod")
        var is2xMode : String? = null

        @SerializedName("submodifier_price")
        var subModifierPrice : String? = null

    }

    class SubModifierWhole {

        @SerializedName("sub_mod_id")
        var subModifierId : Int? = null

        @SerializedName("is_2x_mod")
        var is2xMode : String? = null

        @SerializedName("submodifier_price")
        var subModifierPrice : String? = null
    }

    class SubModifierFirstHalf {

        @SerializedName("sub_mod_id")
        var subModifierId : Int? = null

        @SerializedName("is_2x_mod")
        var is2xMode : String? = null

        @SerializedName("submodifier_price")
        var subModifierPrice : String? = null

    }

    class SubModifierSecondHalf {

        @SerializedName("sub_mod_id")
        var subModifierId : Int? = null

        @SerializedName("is_2x_mod")
        var is2xMode : String? = null

        @SerializedName("submodifier_price")
        var subModifierPrice : String? = null
    }

    class AddOn {

        @SerializedName("row_id")
        var rowId : String? = null

        @SerializedName("add_on_id")
        var addOnId : Int? = null

        @SerializedName("addonsqty")
        var addOnQty : String? = null

        @SerializedName("food_status")
        var foodStatus : Int? = null

        @SerializedName("tax_id")
        var taxIds : String? = null

        @SerializedName("tax_percentage")
        var taxPers : String? = null
    }

    class BillInfo {

        @SerializedName("bill_id")
        var billId : String? = null

        @SerializedName("sub_mod_id")
        var subModifierId : String? = null

        @SerializedName("submodifier_price")
        var subModifierPrice : String? = null

        @SerializedName("service_charge")
        var serviceCharge : String? = null

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

        @SerializedName("total_amount")
        var totalAmount : String? = null

        @SerializedName("discount")
        var discountAmount : String? = null
    }
}