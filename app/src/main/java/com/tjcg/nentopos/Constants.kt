package com.tjcg.nentopos

class Constants {

    companion object {
        const val CONNECTION_TIMEOUT = 20L
        const val READ_TIMEOUT = 20L
        const val WRITE_TIMEOUT = 20L
        const val BASE_URL = "https://menuonline.com/api/"

        const val SUCCESS_BROADCAST = "com.tjcg.nentopos.success"
        const val FAILURE_BROADCAST = "com.tjcg.nentopos.failure"
        const val OUTLET_CHANGE_BROADCAST = "com.tjcg.nentopos.outlet_change"
        const val PRODUCT_UPDATE_BROADCAST = "com.tjcg.nentopos.update"
        const val CLEAR_CART_BROADCAST = "com.tjcg.nentopos.clearcart"
        const val PAYMENT_DONE_BROADCAST = "com.tjcg.nentopos.payment-done"
        const val ALL_ORDER_UPDATE_BROADCAST = "com.tjcg.nentopos.allorders"
        const val LOG_OUT_NOW_BROADCAST = "com.tjcg.nentopos.logout"
        const val CLOSE_DIALOG_BROADCAST = "com.tjcg.nentopos.close-dialog"
        const val SYNC_COMPLETE_BROADCAST = "com.tjcg.nentopos.sync-completed"
        const val SUB_USER_LOGIN_BROADCAST = "com.tjcg.nentopos.subuserlogin"
        const val CUSTOMER_ADDED_BROADCAST = "com.tjcg.nentopos.customer-added"

        const val IS_SUCCESS = "IsSuccess"

        const val error = "error"

        const val subCartAnimDuration = 100L
        const val cartFile = "CartData"

        var isNewLogin = true
        var isFromSubUser = false
        var loggedInSubUserId = -1
        var authorization = "-1"
        var firebaseToken = ""
        var uniqueId = HashMap<Int, String>()
        var selectedOutletId = 1
        var clientId = "-1"
        var currencySign = "$"
        var databaseBusy = false

        const val ORDER_STATUS_PENDING = 1
        const val ORDER_STATUS_PROCESSING = 2
        const val ORDER_STATUS_READY = 3
        const val ORDER_STATUS_SERVED = 4
        const val ORDER_STATUS_CANCELED = 5

        const val PAYMENT_METHOD_CASH = 1
        const val PAYMENT_METHOD_DEBIT = 2
        const val PAYMENT_METHOD_CREDIT = 3

        const val STATUS_NOT_PAID = 0
        const val STATUS_PAID = 1

        const val ACCEPT_ORDER = 1
        const val REJECT_ORDER = 2

        const val TAX_IN_PERCENT = "0"
        const val TAX_IN_RATE = "1"

        const val FUTURE_ORDER_NULL = 0
        const val FUTURE_ORDER_FAR = 1
        const val FUTURE_ORDER_NEAR = 2


        fun getOrderStatus(status: Int): String {
            return when (status) {
                ORDER_STATUS_PENDING -> {
                    "Pending"
                }
                ORDER_STATUS_PROCESSING -> {
                    "Processing"
                }
                ORDER_STATUS_READY -> {
                    "Ready"
                }
                ORDER_STATUS_SERVED -> {
                    "Served"
                }
                ORDER_STATUS_CANCELED -> {
                    "Canceled"
                }
                else -> {
                    "NA"
                }
            }
        }

        //Dialog Ids
        const val ID_DIALOG = "dialogId"
        const val ID_DIALOG_KITCHEN_ITEM_DETAILS = 1
        const val ID_DIALOG_ACCEPT_ORDER =2
        const val ID_DIALOG_DRIVER_ASSIGN = 3
        const val ID_DIALOG_COMPLETE_ORDER = 4
        const val ID_DIALOG_SEND_SMS = 5

        // sharedPreference keys
        const val PREFS_MAIN = "menuOnlinePreferences"
        const val PREF_AUTHORIZATION = "authorization"
        const val PREF_CLIENT_ID = "clientId"
        const val PREF_PERMISSION_POS = "permissionPOS"
        const val PREF_PERMISSION_ALL_ORDERS = "permissionAllOrders"
        const val PREF_PERMISSION_DASHBOARD = "permissionDashboard"
        const val PREF_PERMISSION_KITCHEN = "permissionKitchen"
        const val PREF_PERMISSION_COUNTER = "permissionCounterDisplay"
        const val PREF_PERMISSION_RESERVATION = "permissionReservation"
        const val PREF_PERMISSION_CUSTOMER_LIST = "permissionCustomerList"
        const val PREF_PERMISSION_REPORT_LIST = "permissionReportList"
        const val PREF_PERMISSION_TABLE = "permissionTable"
        const val PREF_PERMISSION_MENU = "permissionMenu"
        const val PREF_IS_NEW_LOGIN = "newLogin"
        const val PREF_IS_LOGGED_IN = "isLoggedIn"
        const val PREF_IS_SYNC_REQUIRES = "isSyncRequired"
        const val PREF_IS_ALL_DATA = "isAllData"
        const val PREF_SELECTED_OUTLET = "selectedOutlet"
        const val PREF_FIRST_INSTALLED = "firstInstalled_1.0Beta02"
        const val PREF_SUPER_USER_EMAIL = "superUserEmail"
        const val PREF_SUPER_USER_DOMAIN = "superUserDomain"
        const val PREF_HST_NUMBER = "hstNumber"
        const val PREF_SYN_TIME = "synTiming"
    }
}