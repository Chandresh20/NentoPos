package com.tjcg.nentopos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "SubUsers")
class SubUserData  {

    @PrimaryKey
    @SerializedName("id")
    var id: Int = 0

    @SerializedName("outlet")
    var outletId: Int? = null

    @SerializedName("firstname")
    var firstname: String? = null

    @SerializedName("lastname")
    var lastname: String? = null

    @SerializedName("about")
    var about: String? = null

    @SerializedName("countrycodetel")
    var countrycodetel: String? = null

    @SerializedName("phone_no")
    var phone_no: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("role_name")
    var role_name: String? = null

    @SerializedName("role_description")
    var role_description: String? = null

    @SerializedName("pin")
    var pin: String? = null

    @SerializedName("pos")
    var pos: String? = null

    @SerializedName("management")
    var management: String? = null

    @SerializedName("menus_list")
    var menus_list: String? = null

    @SerializedName("variants_list")
    var variants_list: String? = null

    @SerializedName("addons_list")
    var addons_list: String? = null

    @SerializedName("category_list")
    var category_list: String? = null

    @SerializedName("reservation_access")
    var reservation_access: String? = null

    @SerializedName("waiting_list")
    var waiting_list: String? = null

    @SerializedName("customer_management")
    var customer_management: String? = null

    @SerializedName("store_setup")
    var store_setup: String? = null

    @SerializedName("all_order")
    var all_order: String? = null

    @SerializedName("dashboard_analytics")
    var dashboard_analytics: String? = null

    @SerializedName("kitchen_display")
    var kitchen_display: String? = null

    @SerializedName("counter_display")
    var counter_display: String? = null

    @SerializedName("reservation_management")
    var reservation_management: String? = null

    @SerializedName("table_management")
    var table_management: String? = null

    @SerializedName("customer_list")
    var customer_list: String? = null

    @SerializedName("report_list")
    var report_list: String? = null

    @SerializedName("menu_management")
    var menu_management: String? = null

    @SerializedName("outlet_name")
    var outletName : String? = null

    override fun toString(): String = "$firstname $lastname"
}