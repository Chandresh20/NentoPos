package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName

class SubUserLoginResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var userData : SubUserData? = null

    class SubUserData (
        val Authorization: String,
        val LoginLogId: Any,
        val client_id: String,
        val email: String,
        val fullname: String,

        @SerializedName("id")
        val id: Int,

        val image: Any,
        val ip_address: Any,
        val isAdmin: Int,
        val is_free_user: String,
        val is_super_admin: Int,
        val last_login: Any,
        val last_logout: Any,
        val main_permission: MainPermission,
        val outlet_id: String,
        val outlet_name: String,
        val sub_user_permission: SubUserPermission,
        val user_level: String,
        val user_type: String
        )

    class MainPermission(
        val booking_reservation: String,
        val kitchen_display: String,
        val offline_mobile_pos: String,
        val pickup_app_frontend: String,
        val pos_equipment: String,
        val queue_management: String,
        val web_pos_system: String,
        val witress_app: String
    )

    class SubUserPermission(
        val accounts: Int,
        val addons_list: Int,
        val all_order: Int,
        val app_setting: String,
        val application_setting: Int,
        val bank: String,
        val card_terminal: Int,
        val category_list: Int,
        val counter_display: Int,
        val country: String,
        val currency: Int,
        val customer_list: Int,
        val customer_management: Int,
        val customer_type: Int,
        val dashboard_analytics: Int,
        val extra: Int,
        val hrm: Int,
        val kitchen_display: Int,
        val language: Int,
        val location: Int,
        val management: Int,
        val menu_management: Int,
        val menus_list: Int,
        val modifier_list: Int,
        val payment_method_list: Int,
        val payment_method_setting: Int,
        val payment_setup: Int,
        val pos: Int,
        val report_list: Int,
        val reservation_access: Int,
        val reservation_management: Int,
        val roll_permission: Int,
        val sms_setting: Int,
        val staff_settings: Int,
        val state: String,
        val stock_management: Int,
        val store_setup: Int,
        val table_management: Int,
        val unit_measurment: String,
        val user: Int,
        val variants_list: Int,
        val waiting_list: Int,
        val web_settings: Int
    )
}