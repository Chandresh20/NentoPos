package com.tjcg.nentopos.responses

import com.google.gson.annotations.SerializedName
import com.tjcg.nentopos.data.OutletData

class LoginResponse {

    @SerializedName("status")
    var status: Boolean? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("data")
    var userData : UserData? = null

    class UserData {

        @SerializedName("user_details")
        var userDetails: UserDetails? = null

        @SerializedName("outlets")
        var outlets : List<OutletData>? = null

        @SerializedName("user_permissions")
        var userPermissions: UserPermissions? = null

        @SerializedName("Authorization")
        var authorization : String? = null
    }

    class UserDetails {

        @SerializedName("client_id")
        var clientId : String? = null

        @SerializedName("domain_name")
        var domainName: String? = null
        
        @SerializedName("hst_no")
        var hstNo: String? = null
    }

    class UserPermissions {

        @SerializedName("pos")
        var permissionPOS : Int? = null

        @SerializedName("all_order")
        var permissionAllOrders : Int? = null

        @SerializedName("dashboard_analytics")
        var permissionDashboard : Int? = null

        @SerializedName("counter_display")
        var permissionCounterDisplay : Int? = null

        @SerializedName("kitchen_display")
        var permissionKitchenDisplay : Int? = null

        @SerializedName("menus_list")
        var permissionMenuList :Int? = null

        @SerializedName("category_list")
        var permissionCategoryList : Int? = null

        @SerializedName("reservation_access")
        var permissionReservationAccess: Int? = null
    }
}