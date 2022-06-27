package com.tjcg.nentopos.api

import com.tjcg.nentopos.responses.*
import retrofit2.Call
import retrofit2.http.*

interface Apis {

    @POST("loginSuperAdmin")
    @FormUrlEncoded
    fun loginSuperUser(@Field("email") email: String,
                        @Field("password") password: String,
                    @Field("device_token") deviceToken : String,
                    @Field("device_type") deviceType : String) : Call<LoginResponse?>

    @POST("loginSubUser")
    @FormUrlEncoded
    fun loginSubUser(
            //        @Field("email") emailId: String?,
                     @Field("pin") pin: String?,
                     @Field("device_id") deviceId : String,
                     @Field("domain_name") domain_name: String?): Call<SubUserLoginResponse?>
           //          @Field("device_token") device_token: String?,
            //        @Field("device_type") device_type: String?

    @POST("getLocationBasedMenusProducts")
    @FormUrlEncoded
    fun getAllProducts(@Field("outlet_id") outletId: Int,
            @Field("unique_id") uniqueId: String,
        @Field("device_id") deviceId: String, @Field("is_all_data") isAllData: Int,
        @Header("Authorization") authorization : String) : Call<MenuResponse?>

    @POST("createOrder")
    fun createNewOrder(@Body request: String,
                       @Header("Authorization") authorization : String) : Call<CreateOrderResponse?>

    @POST("getAllCustomerList")
    @FormUrlEncoded
    fun getCustomerList(@Field ("outlet_id") outletId: Int,
                        @Field("device_id") deviceId: String, @Field("is_all_data") isAllData: Int,
                        @Header("Authorization") authorization : String) : Call<CustomerListResponse>?

    @POST("getTableList")
    @FormUrlEncoded
    fun getTableList(@Field ("outlet_id") outletId: Int,
                     @Field("unique_id") uniqueId: String,
                     @Field("device_id") deviceId: String, @Field("is_all_data") isAllData: Int,
                     @Header("Authorization") authorization : String) : Call<TableResponse>?

    @POST("getAllOrderList")
    @FormUrlEncoded
    fun getAllOrders(@Field("outlet_id") outlet_id: Int,
                     @Field("device_id") device_id: String?,
                     @Field("is_all_data") is_all_data: Int = 1,
                     @Header("Authorization") authHeader: String?) : Call<OrdersResponse?>

    @POST("getSingleOrderDetails")
    @FormUrlEncoded
    fun getSingleOrderDetails(@Field("outlet_id") outletId: Int,
                        @Field("order_ids") orderIds: String,
                              @Header("Authorization") authHeader: String?) : Call<OrdersResponse?>

    @POST("getSubUsers")
    @FormUrlEncoded
    fun getSubUsers(@Field("outlet_id") outletId: Int,
                    @Field("unique_id") uniqueId: String,
                    @Field("device_id") device_id: String?,
                    @Field("is_all_data") is_all_data: Int = 1,
                    @Header("Authorization") authHeader: String?) : Call<SubUserResponse?>

    @POST("acceptOnlineOrder")
    @FormUrlEncoded
    fun acceptRejectOnlineOrder(@Field("outlet_id") outlet_id: Int,
                                @Field("order_id") order_id: Long,
                                @Field("order_status") order_status: Int,
                                @Field("acceptreject") acceptReject: Int,
                                @Field("cooking_time") cooking_time: String?,
                                @Field("reason") reason: String?,
                                @Header("Authorization") authHeader: String): Call<OrderAcceptResponse>

    @POST("changeKitchenOrderStatus")
    @FormUrlEncoded
    fun changeKitchenOrderStatus(@Field("outlet_id") outlet_id: Int,
                                 @Field("order_id") order_id: Long,
                                 @Field("menuid") menu_id: Int,
                                 @Field("status") status: Int,
                                 @Field("row_id") rowId : String?,
                                 @Field("add_on_id") addOnId : Int?,
                                 @Header("Authorization") authHeader: String?) : Call<SimpleResponse>

    @POST("updateOrderSync")
    fun syncOrders(@Body ordersSycJson: String?,
                   @Header("Authorization") authHeader: String?) : Call<SimpleResponse>

    @POST("createMultipleOrder")
    fun createMultipleOrders(@Body multipleOrderJson : String?,
                             @Header("Authorization") authHeader: String?) : Call<OfflineSyncResponse>

    @POST("createSyncCustomer")
    fun syncOfflineCustomers(@Body offlineCustomersJson: String,
        @Header("Authorization") authHeader: String) : Call<CustomerSyncResponse>

    @POST("addToCartUpdateSync")
    fun synEditedOrders(@Body eSyncJson: String,
                        @Header("Authorization") authHeader: String) : Call<SimpleResponse>

    @POST("getDiscountList")
    @FormUrlEncoded
    fun getDiscountList(@Field("device_id") deviceId: String,
    @Field("is_all_data") isAllData: Int, @Field("outlet_id") outletId: Int,
    @Header("Authorization") authHeader: String) : Call<DiscountResponse>

    @GET("getCustomerTypes")
    fun getCustomerTypes(@Header("Authorization") authHeader: String) : Call<CustomerTypeResponse>

    @POST("addCustomer")
    @FormUrlEncoded
    fun addCustomer(@Header("Authorization") authHeader: String,
    @Field("user_id") userId: String, @Field("outlet_id") outletId: Int,
                    @Field("menu_id") menuId: Int?, @Field("customer_name") customerName: String,
    @Field("customer_category") customerCategory: Int, @Field("customer_lastname") custLastName: String?,
    @Field("customer_email") customerEmail: String, @Field("country_code") countryCode: String,
    @Field("customer_phone") customerPhone: String, @Field("customer_address") customerAddress: String,
    @Field("note") note : String?) : Call<AddCustomerResponse>

    @POST("assignDeliveryBoy")
    @FormUrlEncoded
    fun assignDeliveryBoy(
        @Field("order_id") order_id: Long,
        @Field("delivery_person_id") delivery_person_id: Int,
        @Header("Authorization") authHeader: String): Call<SimpleResponse>

    @POST("sendOngoingOrderSMS")
    @FormUrlEncoded
    fun sendOngoingOrderSms(
        @Field("outlet_id") outlet_id: Int,
        @Field("customer_id") customerId: Long,
        @Field("message") message : String?,
        @Header("Authorization") authHeader: String?): Call<SimpleResponse>

    @POST("sendOrderEmail")
    @FormUrlEncoded
    fun sendOrderEmail(
        @Field("outlet_id") outlet_id: Int,
        @Field("order_id") orderId : Int,
        @Field("email") email : String,
        @Header("Authorization") authHeader: String) : Call<SimpleResponse>

    @POST("getCartTypeList")
    @FormUrlEncoded
    fun getCardTerminals(
        @Field("outlet_id") outletId: Int,
        @Field("device_id") deviceId: String,
        @Field("is_all_data") isAllData: Int,
        @Header("Authorization") authHeader: String) : Call<CardTerminalResponse>

    @POST("addToCartUpdate")
    fun updateOrder(
        @Body updateOrderRequest : String,
        @Header("Authorization") authHeader: String) : Call<SimpleResponse>

    @POST("logoutFromDevice")
    @FormUrlEncoded
    fun logoutFromDevice(
        @Field("device_id") deviceId: String,
        @Header("Authorization") authString: String) : Call<String>
}