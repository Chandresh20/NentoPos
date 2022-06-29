package com.tjcg.nentopos.repositories

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.adapters.KitchenAdapter
import com.tjcg.nentopos.api.ApiService
import com.tjcg.nentopos.data.*
import com.tjcg.nentopos.dialog.SendEmailDialog
import com.tjcg.nentopos.fragments.POSFragment
import com.tjcg.nentopos.responses.*
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class OrderRepository(ctx : Context) {

    private var mainScope : CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var ordersDatabase: OrdersDatabase = OrdersDatabase.getDatabase(ctx)
    private var oDao :OrdersDao = ordersDatabase.getOrdersDao()
    var syncRequired = false

    init {
        syncRequired = MainActivity.mainSharedPreferences
            .getBoolean(Constants.PREF_IS_SYNC_REQUIRES, false)
        val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                getAllOrdersOnline(ctx, Constants.selectedOutletId, 0,true)
            }
        }
        ctx.registerReceiver(updateReceiver, IntentFilter(Constants.ALL_ORDER_UPDATE_BROADCAST))
    }

    // online Operations
    fun getAllOrdersOnline(ctx: Context, outletId: Int, isAllData: Int ,selectedOutlet : Boolean) {
        if (!MainActivity.isInternetAvailable(ctx)) {
    //        Toast.makeText(ctx, "Internet Not Available", Toast.LENGTH_SHORT).show()
            updateViewModel()
            return
        }
        if (syncRequired) {
   //         Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
     /*   val isAllData = MainActivity.mainSharedPreferences.getInt(
            Constants.PREF_IS_ALL_DATA, 1)  */
        mainScope.launch {
            if (isAllData == 1) {
                deleteAllOrdersAsync(outletId).await()
                getAllOrdersOnline2(ctx, outletId, isAllData, selectedOutlet)
            } else {
                getAllOrdersOnline2(ctx, outletId, isAllData, selectedOutlet)
            }
        }

        //           GlobalOperations.orderViewModel.toggleLottie(true)
//            progressHandler.obtainMessage(0, true).sendToTarget()
    }

    private fun getAllOrdersOnline2(ctx: Context, outletId: Int, isAllData: Int, selectedOutlet: Boolean) {
        var dId = 0
        if (selectedOutlet) {
            dId = MainActivity.progressDialogRepository.getProgressDialog("Loading Orders data")
        } else {
            MainActivity.progressDialogRepository.showSmallProgressBar(
                "getting new Orders")
        }
        Log.d("AllOrdersReq", "$outletId, ${MainActivity.deviceID}, $isAllData, ${Constants.authorization}")
        ApiService.apiService?.getAllOrders(
            outletId, MainActivity.deviceID, isAllData, Constants.authorization)
            ?.enqueue(object: Callback<OrdersResponse?> {
                override fun onResponse(
                    call: Call<OrdersResponse?>,
                    response: Response<OrdersResponse?>
                ) {
                    Log.d("ordersRes", "response ${response.body()?.status}")
                    if (response.isSuccessful && response.body()?.status == "true") {
                        Log.d("ordersRes", "Good Response")
                        //           oDao.deleteAllOrders()  // should be removed after getEditOrderAPI update
                        mainScope.launch {
                            insertAllOrdersAsync(response.body()?.data as List<OrdersEntity>).await()
                            Log.d("ordersRes", "Data inserted to database ${response.body()?.data?.size}")
                            MainActivity.mainRepository.loadTableData(ctx, outletId, MainActivity.deviceID, MainActivity.deviceID,
                                1, false)
                            updateViewModel()
                  /*          MainActivity.mainSharedPreferences.edit()
                                .putInt(Constants.PREF_IS_ALL_DATA, 0).apply()  */
                        }
                        //       updateCustomerList(ctx, outletId, MainActivity.deviceID, "0")
                        //      Above line will be inserted after getEditOrderApi update
                    } else if (response.body()?.status == "false" && response.body()?.message != null &&
                        (response.body()?.message?.contains("Invalid")!! ||
                                response.body()?.message?.contains("invalid")!!)) {
                        MainActivity.logOutNow(ctx)
                    } else {
                        updateViewModel()
                    }
                    if (selectedOutlet) {
                        MainActivity.progressDialogRepository.dismissDialog(dId)
                    } else {
                        MainActivity.progressDialogRepository.closeSmallProgressBar()
                    }
                    MainActivity.orderViewModel.lastSyncTiming.value = Calendar.getInstance().timeInMillis
                }

                override fun onFailure(call: Call<OrdersResponse?>, t: Throwable) {
                    if (selectedOutlet) {
                        MainActivity.progressDialogRepository.dismissDialog(dId)
                    } else {
                        MainActivity.progressDialogRepository.closeSmallProgressBar()
                    }
                    Log.e("OrderApi", "Error: ${t.message}")
            /*        Toast.makeText(ctx, "An Error Occurred while getting data, Please try again",
                        Toast.LENGTH_SHORT).show()  */
                    val builder = AlertDialog.Builder(ctx).apply {
                        setMessage("Bad internet connectivity, unable to load data")
                        setPositiveButton("Try Again") { _, _ ->
                            getAllOrdersOnline(ctx,outletId, 0, selectedOutlet)
                        }
                        setNegativeButton("Close") { _, _ ->
                            (ctx as MainActivity).finishAffinity()
                        }
                    }
                    val dialog = builder.create()
                    dialog.show()
                }

            })
    }

    fun acceptRejectOrderOnline(ctx: Context, outletId: Int, orderId: Long, status: Int, acceptReject: Int,
        cookingTimeInMinute: Int, reason: String) {
        if (!MainActivity.isInternetAvailable(ctx)) {
      //      Toast.makeText(ctx, "Internet Not Available, updated in offline pending", Toast.LENGTH_SHORT).show()
            // change order status in offline
            acceptRejectOrdersOffline(ctx, outletId, orderId, 0, cookingTimeInMinute, acceptReject)
            return
        }
        if (syncRequired) {
 //           Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
        ApiService.apiService?.acceptRejectOnlineOrder(
            outletId, orderId, status, acceptReject, formatCookingTime(cookingTimeInMinute),
            reason, Constants.authorization)
            ?.enqueue(object : Callback<OrderAcceptResponse?> {
                override fun onResponse(
                    call: Call<OrderAcceptResponse?>,
                    response: Response<OrderAcceptResponse?>) {
                    Log.d("acceptRes", "response ${response.raw()}")
                    if (response.isSuccessful) {
                        if (response.body()?.status != null && response.body()?.status!!) {
                            Log.d("acceptRes", response.body()?.message!!)
                            acceptRejectOrdersOffline(ctx,outletId, orderId, 1, cookingTimeInMinute, acceptReject)
                        } else {
                            Toast.makeText(ctx, "An Error Occurred, Please try again",
                                Toast.LENGTH_SHORT).show()
                            Log.e("acceptRes", response.body()?.message!!)
                        }
                        if (response.body()?.status != null && !response.body()?.status!! && response.body()?.message != null &&
                            (response.body()?.message?.contains("Invalid")!! ||
                                    response.body()?.message?.contains("invalid")!!)) {
                            MainActivity.logOutNow(ctx)
                        }
                    }
                }

                override fun onFailure(call: Call<OrderAcceptResponse?>, t: Throwable) {
                    Log.e("OrderApi", "Error: ${t.message}")
                    Toast.makeText(ctx, "An Error Occurred, Please try again",
                        Toast.LENGTH_SHORT).show()
                    val cIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                    cIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_ACCEPT_ORDER)
                    ctx.sendBroadcast(cIntent)
                }
            })
    }

    private fun acceptRejectOrdersOffline(ctx:Context, outletId: Int, orderId: Long, isSync: Int, cookingTime: Int,
                                          acceptReject: Int) {
        mainScope.launch {
            val order = getSingleOrderAsync(outletId, orderId).await()
            if (order != null) {
                order.syncOrNot = isSync
                order.cookedtime = formatCookingTime(cookingTime)
                order.order_accept_date = getTodayString()
                order.order_status = if (acceptReject == 1) Constants.ORDER_STATUS_PROCESSING else
                    Constants.ORDER_STATUS_CANCELED
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = (cal.get(Calendar.MONTH)) +1
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minutes = cal.get(Calendar.MINUTE)
                val secs = cal.get(Calendar.SECOND)
                order.order_accept_date = "$year-$month-$day $hour:$minutes:$secs"
                order.editTime = System.currentTimeMillis()
                updateSingleOrderAsync(order).await()
                // sync will required after this changes..
                if (isSync == 0) {
                    MainActivity.mainSharedPreferences.edit()
                        .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                    syncRequired = true
                }
                updateViewModel()
                val cIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                cIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_ACCEPT_ORDER)
                ctx.sendBroadcast(cIntent)
            }

        }
    }

    fun completeOrderWithPayment(ctx: Context, customerPaid : Float, orderId: Long, outletId: Int) {
        if (!MainActivity.isInternetAvailable(ctx)) {
    //        Toast.makeText(ctx, "saving in offline", Toast.LENGTH_SHORT).show()
            completeOrderWithPaymentOffline(ctx, customerPaid, orderId, outletId, 0)
            return
        }
        if (syncRequired) {
     //       Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
        val dId = MainActivity.progressDialogRepository.getProgressDialog("Completing Order...")
        mainScope.launch {
            val order = getSingleOrderAsync(outletId, orderId).await()
            if (order != null) {
                val uOrder = OrderSyncEntity.OrderSync().apply {
                    this.orderId = order.order_id
                    outlet_id = order.outlet_id
                    waiter_id = order.waiter_id
                    order_accept_date = order.order_accept_date
                    cookedtime = order.cookedtime
                    discount = order.discount
                    tip_type = order.tip_type
                    tip_amount = order.tip_amount
                    added_tip_amount = order.added_tip_amount
                    tip_amount = order.tip_amount
                    totalamount = order.totalamount
                    customerpaid = customerPaid.toString()
                    customer_note = order.customer_note
                    anyreason = order.anyreason
                    order_status = Constants.ORDER_STATUS_SERVED
                    pis_driver_assigned = order.pis_driver_assigned
                    driver_user_id = order.driver_user_id
                    editTime = System.currentTimeMillis()
                }
                // collect item info
                val itemInfo = order.itemsInfo
                val itemsToSync = ArrayList<OrderSyncEntity.ItemInfoSyn>()
                if (!itemInfo.isNullOrEmpty()) {
                    for (item in itemInfo) {
                        val itemSyn = OrderSyncEntity.ItemInfoSyn().apply {
                            rowId = item.rowId
                            uniqueRecordId = item.uniqueRecordId
                            foodStatus = item.foodStatus
                        }
                        itemsToSync.add(itemSyn)
                    }
                    uOrder.itemInfoSyn = itemsToSync
                }
                val billInfo = order.billInfo
                if (billInfo != null) {
                    val billInfoSyn = OrderSyncEntity.BillInfoSyn().apply {
                        billId = billInfo.billId
                        billStatus = 1
                        paymentMethodId = billInfo.paymentMethodId
                        accountNumber = billInfo.accountNumber
                        selectedCard = billInfo.selectedCard
                        cardType = billInfo.cardType
                    }
                    uOrder.billInfoSyn = billInfoSyn
                }
                val orderNew = OrderSyncEntity()
                val orderList = ArrayList<OrderSyncEntity.OrderSync>()
                orderList.add(uOrder)
                orderNew.ordersSync = orderList
                val gson = Gson()
                val typeT = object: TypeToken<OrderSyncEntity>() { }
                val request = gson.toJson(orderNew, typeT.type)
                updateOrdersOnline(ctx, request)
            }
        }
        val successReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.d("ReceivedBroadcast", "${p1?.action}")
                if (p1?.action == Constants.SYNC_COMPLETE_BROADCAST) {
                    completeOrderWithPaymentOffline(ctx, customerPaid, orderId, outletId, 1)
                }
                MainActivity.progressDialogRepository.dismissDialog(dId)
            }
        }
        ctx.registerReceiver(successReceiver, IntentFilter(Constants.SYNC_COMPLETE_BROADCAST))
    }

    private fun completeOrderWithPaymentOffline
                (ctx: Context,customerPaid : Float, orderId: Long, outletId: Int, isSync: Int) {
        mainScope.launch {
            val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
            closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_COMPLETE_ORDER)
            if (isOfflineOrder(orderId)) {
                val offOrder = getOneOffline2OrderAsync(Constants.selectedOutletId, orderId).await()
                if (offOrder != null) {
                    offOrder.orderStatus = Constants.ORDER_STATUS_SERVED
                    offOrder.billStatus = 1
                    offOrder.customerPaid = customerPaid
                    updateSingleOfflineOrderAsync(offOrder).await()
                    updateViewModel()
                    closeIntent.putExtra(Constants.IS_SUCCESS, true)
                }
            } else {
                 val order = getSingleOrderAsync(outletId, orderId).await()
                if (order != null) {
                    order.syncOrNot = isSync
                    order.customerpaid = customerPaid.toString()
                    order.order_status = Constants.ORDER_STATUS_SERVED
                    val billInfo = order.billInfo
                    billInfo?.billStatus = 1
                    order.billInfo = billInfo
                    order.editTime = System.currentTimeMillis()
                    updateSingleOrderAsync(order).await()
                    updateViewModel()
                    if (isSync == 0) {
                     MainActivity.mainSharedPreferences.edit().putBoolean(
                         Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                        syncRequired = true
                    }
                    closeIntent.putExtra(Constants.IS_SUCCESS, true)
                }
            }
            ctx.sendBroadcast(closeIntent)
        }
    }

    private fun updateOrdersOnline(ctx: Context, ordersInJson: String)  {
        val syncIntent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
        Log.d("OrderSyncRequest", ordersInJson)
        ApiService.apiService?.syncOrders(ordersInJson, Constants.authorization)
            ?.enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(
                    call: Call<SimpleResponse>,
                    response: Response<SimpleResponse>
                ) {
                    Log.d("newSync", "API response: ${response.raw()}")
                    if (response.isSuccessful) {
                        Log.d("newSync", "Status : ${response.body()?.status}")
                        Log.d("newSync", "${response.body()?.message}")
                        if (response.body()?.status != null && response.body()?.status!!) {
                            syncIntent.putExtra(Constants.IS_SUCCESS, true)
                            ctx.sendBroadcast(syncIntent)
                            Log.d("newSync", "Send broadcast, close dialog and on_syn_completed")
                        } else if (response.body()?.status!= null && !response.body()?.status!! &&
                            (response.body()?.message?.contains("Invalid")!! ||
                                    response.body()?.message?.contains("invalid")!! )) {
                                        syncIntent.putExtra(Constants.IS_SUCCESS, false)
                            ctx.sendBroadcast(syncIntent)
                            MainActivity.logOutNow(ctx)
                        }
                    } else {
                        syncIntent.putExtra(Constants.IS_SUCCESS, false)
                        ctx.sendBroadcast(syncIntent)
                        Log.e("newSync", "API failure : ${response.errorBody()}")
                    }
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    syncIntent.putExtra(Constants.IS_SUCCESS, false)
                    ctx.sendBroadcast(syncIntent)
                    Log.e("newSync", "API failure : ${t.message}")
             /*       ctx.sendBroadcast(Intent(ON_SYN_FAILED))
                    val closeIntent = Intent(Constants.CLOSE_DIALOG)
                    closeIntent.putExtra(SharedPreferencesKeys.IS_SUCCESS, false)
                    ctx.sendBroadcast(closeIntent)  */
                    Log.d("newSync", "Send broadcast, syn_failed")
                }
            })
    }

    fun completeAllOrderItems(ctx: Context, orderId: Long) {
        if (!MainActivity.isInternetAvailable(ctx)) {
            mainScope.launch {
                 if(isOfflineOrder(orderId)) {
                    val offOrder = getOneOffline2OrderAsync(Constants.selectedOutletId, orderId).await()
                     if (offOrder != null) {
                         if (!offOrder.cartItems.isNullOrEmpty()) {
                             for (item in offOrder.cartItems!!) {
                                 item.foodStatus = 1
                             }
                         }
                         offOrder.orderStatus = Constants.ORDER_STATUS_READY
                         updateSingleOfflineOrderAsync(offOrder).await()
                     }
                 } else {
                     val order = getSingleOrderAsync(Constants.selectedOutletId, orderId).await()
                     if (order != null) {
                         if (!order.itemsInfo.isNullOrEmpty()) {
                             for (item in order.itemsInfo!!) {
                                 item.foodStatus = 1
                            }
                         }
                         if (!order.addOns.isNullOrEmpty()) {
                             for (item in order.addOns!!) {
                                 item.foodStatus = 1
                             }
                         }
                         order.order_status = Constants.ORDER_STATUS_READY
                         order.editTime = System.currentTimeMillis()
                         updateSingleOrderAsync(order).await()
                     }
                 }
                MainActivity.mainSharedPreferences.edit()
                    .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                syncRequired = true
                Log.d("KitchenUpdated", "Syn is set for $syncRequired")
                updateViewModel()
                val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS)
                ctx.sendBroadcast(closeIntent)
            }
            return
        }
         if (syncRequired) {
   //         Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
        mainScope.launch {
            val dId = MainActivity.progressDialogRepository.getProgressDialog("Updating Status...")
            val order = MainActivity.orderRepository.getSingleOrderAsync(Constants.selectedOutletId, orderId).await()
            if (order != null) {
                var allItemCompleted = true
                val items = order.itemsInfo
                if (!items.isNullOrEmpty()) {
                    for (item in items) {
                        delay(200)
                        Log.d("API call to finish", "${item.rowId}")
                        ApiService.apiService?.changeKitchenOrderStatus(Constants.selectedOutletId,
                            order.order_id, item.menuId ?: 0,1, item.rowId, 0, Constants.authorization)
                            ?.enqueue(object : Callback<SimpleResponse?> {
                                override fun onResponse(
                                    call: Call<SimpleResponse?>, response: Response<SimpleResponse?>
                                ) {
                                    if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                                        item.foodStatus = 1
                                        Log.d("ItemComplete", "${item.rowId}")
                                    } else {
                                        allItemCompleted =  false
                                        MainActivity.progressDialogRepository.showErrorDialog("Error changing status: ${response.body()?.message}")
                                        Log.e("ItemComplete", "failed: ${response.body()?.message}")
                                    }
                                }

                                override fun onFailure(call: Call<SimpleResponse?>, t: Throwable) {
                                    allItemCompleted =  false
                                    MainActivity.progressDialogRepository.showErrorDialog("Error changing status ${t.message}")
                                    Log.e("ItemComplete", "failed: ${t.message}")
                                }
                            })
                    }
                    val addONs = order.addOns
                    if (!addONs.isNullOrEmpty()) {
                        for (addon in addONs) {
                            delay(200)
                            ApiService.apiService?.changeKitchenOrderStatus(Constants.selectedOutletId,
                                order.order_id, 0,1, addon.rowId, addon.addOnId, Constants.authorization)
                                ?.enqueue(object : Callback<SimpleResponse?> {
                                    override fun onResponse(
                                        call: Call<SimpleResponse?>, response: Response<SimpleResponse?>
                                    ) {
                                        if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                                            addon.foodStatus = 1
                                            Log.d("AddOnComplete", "${addon.rowId}")
                                        } else {
                                            allItemCompleted =  false
                                            Log.e("AddOnComplete", "failed: ${response.body()?.message}")
                                            MainActivity.progressDialogRepository.showErrorDialog("Error changing status: ${response.body()?.message}")
                                        }
                                    }

                                    override fun onFailure(call: Call<SimpleResponse?>, t: Throwable) {
                                        allItemCompleted =  false
                                        MainActivity.progressDialogRepository.showErrorDialog("Error changing status ${t.message}")
                                        Log.e("AddOnComplete", "failed: ${t.message}")
                                    }
                                })
                        }
                    }
                }
                if(allItemCompleted) {
                    order.editTime = System.currentTimeMillis()
                    order.order_status = Constants.ORDER_STATUS_READY
                }
                updateSingleOrderAsync(order).await()
                MainActivity.progressDialogRepository.dismissDialog(dId)
                val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS)
                ctx.sendBroadcast(closeIntent)
                updateViewModel()
            }
        }
    }

    fun changeKitchenOrderStatusMulti(ctx: Context, operations: ArrayList<KitchenAdapter.Companion.KitchenOperation>) {
        if(!MainActivity.isInternetAvailable(ctx)) {
    //        Toast.makeText(ctx, "Internet Not Available, update in offline", Toast.LENGTH_SHORT).show()
            changeKitchenOrderStatusOfflineMulti(ctx, operations, isSync = 1)
            return
        }
        if (syncRequired) {
   //         Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
        changeKitchenOrderStatusMultiOnline(ctx, operations)
    }

    private fun changeKitchenOrderStatusMultiOnline(
        ctx: Context, operations: ArrayList<KitchenAdapter.Companion.KitchenOperation>) {
        mainScope.launch {
            val dId = MainActivity.progressDialogRepository.getProgressDialog("Updating Status...")
            for (operation in operations) {
                delay(200)
                val orderId = operation.orderId
                if (isOfflineOrder(orderId)) {
                    MainActivity.progressDialogRepository.showSyncDialog()
                    break
                }
                val menuId = operation.menuId
                val status = operation.foodStatus
                val rowId = operation.rowId
                val addOnId = operation.addOnId
                ApiService.apiService?.changeKitchenOrderStatus(Constants.selectedOutletId, orderId, menuId ?: 0,
                    status, rowId, addOnId, Constants.authorization)?.enqueue(object : Callback<SimpleResponse?> {
                    override fun onResponse(call: Call<SimpleResponse?>,
                                            response: Response<SimpleResponse?>) {
                        Log.d("kitchenOrderChange", "response: ${response.raw()}")
                        if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                            Log.d("KitchenOrder", "response: ${response.body()?.message}" )
                //            Toast.makeText(ctx, "Food Ready to serve", Toast.LENGTH_SHORT).show()
                            changeKitchenOrderStatusOffline2(ctx, Constants.selectedOutletId, orderId, status, 1, rowId)
                        } else {
                            if (response.body()?.status != null && response.body()?.status!! &&
                                (response.body()?.message?.contains("Invalid")!! ||
                                        response.body()?.message?.contains("invalid")!!)) {
                                MainActivity.logOutNow(ctx)
                            }
                //            Toast.makeText(ctx, "An Error Occurred, Please try again", Toast.LENGTH_SHORT).show()
                            MainActivity.progressDialogRepository.showErrorDialog("Error Changing Order Status: ${response.body()?.message}")
                            Log.e("kitchenOrderChange", "error: ${response.body()?.message} , ${response.body()?.status}, $orderId, $menuId, $status, $rowId")
                        }
                    }

                    override fun onFailure(call: Call<SimpleResponse?>, t: Throwable) {
             //           Toast.makeText(ctx, "An Error Occurred, Please try again", Toast.LENGTH_SHORT).show()
                        Log.e("kitchenOrderChange", "error: ${t.message}")
                        MainActivity.progressDialogRepository.showErrorDialog("Error Changing Order Status: ${t.message}")
                    }
                })
            }
            val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
            closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS)
            ctx.sendBroadcast(closeIntent)
            MainActivity.progressDialogRepository.dismissDialog(dId)
        }

    }

    private fun changeKitchenOrderStatusOfflineMulti(
        ctx: Context, operations: ArrayList<KitchenAdapter.Companion.KitchenOperation>, isSync: Int) {
        mainScope.launch {
            for (operation in operations) {
                val orderId = operation.orderId
                val qRowId = operation.rowId
                val status = operation.foodStatus
                if (isOfflineOrder(orderId)) {
                    val offOrder =
                        getOneOffline2OrderAsync(Constants.selectedOutletId, orderId).await()
                    if (offOrder != null) {
                        var allFoodCompleted = true
                        if (!offOrder.cartItems.isNullOrEmpty()) {
                            for (item in offOrder.cartItems!!) {
                                if (item.rowId == qRowId) {
                                    item.foodStatus = status
                                }
                                if (!item.rowId.isNullOrBlank() && item.foodStatus == 0) {
                                    allFoodCompleted = false
                                }
                            }
                        }
                        if (allFoodCompleted) {
                            offOrder.orderStatus = Constants.ORDER_STATUS_READY
                        }
                        updateSingleOfflineOrderAsync(offOrder).await()
                        Log.d("food status offline", "${offOrder.tmpOrderId}")
                    }
                } else {
                    val order = getSingleOrderAsync(Constants.selectedOutletId, orderId).await()
                    if (order != null) {
                        order.syncOrNot = isSync
                        val items = order.itemsInfo as ArrayList<OrdersEntity.ItemInfo>
                        var statusUpdated = false
                        var allFoodCompleted = true
                        if (!items.isNullOrEmpty()) {
                            for (item in items) {
                                if (item.rowId == qRowId) {
                                    item.foodStatus = status
                                    statusUpdated = true
                                }
                                if (item.foodStatus == 0) {
                                    allFoodCompleted = false
                                }
                            }
                        }
                        val addOns = order.addOns
                        if (!addOns.isNullOrEmpty()) {
                            for (addOn in addOns) {
                                if (addOn.rowId == qRowId) {
                                    addOn.foodStatus = status
                                    statusUpdated = true
                                }
                                if (addOn.foodStatus == 0) {
                                    allFoodCompleted = false
                                }
                            }
                        }
                        if (statusUpdated) {
                            if (allFoodCompleted) {
                                order.order_status = Constants.ORDER_STATUS_READY
                                Log.d("KitchenUpdated", "All Items Ready")
                            }
                            order.editTime = System.currentTimeMillis()
                            updateSingleOrderAsync(order).await()
                        }
                    }
                }
            }
            updateViewModel()
            val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
            closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS)
            ctx.sendBroadcast(closeIntent)
            if (isSync == 0) {
                MainActivity.mainSharedPreferences.edit()
                    .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                syncRequired = true
                Log.d("KitchenUpdated", "Syn is set for $syncRequired")
            }
            Log.d("KitchenUpdated", "Item status updated")
        }
    }

    fun placeOrderOnline(ctx: Context,orderRequest: ProductOrderRequest) {
        if (!MainActivity.isInternetAvailable(ctx)) {
            val newId = Calendar.getInstance().timeInMillis
            val offlineOrder2 = OfflineOrder2().apply {
                this.tmpOrderId = newId
                this.customerType = orderRequest.customerType
                this.billTime = orderRequest.billTime
                this.selectedCardType = orderRequest.selectedCardType
                this.driverUserId = orderRequest.driverUserId
                this.orderDate = orderRequest.orderDate
                this.outletId = orderRequest.outletId
                this.orderAcceptDate = orderRequest.orderAcceptDate
                this.billDate = orderRequest.billDate
                this.customerPaid = orderRequest.customerPaid
                this.table = orderRequest.table
                this.accountNo = orderRequest.accountNo
                this.driverAssigned = orderRequest.driverAssigned
                this.cardType = orderRequest.cardType
                this.discount = orderRequest.discount
                this.billStatus = orderRequest.billStatus
                this.cookedTime = orderRequest.cookedTime
                this.orderTime = orderRequest.orderTime
                this.tipAmount = orderRequest.tipAmount
                this.tipType = orderRequest.tipType
                this.createdBy = orderRequest.createdBy
                this.totalAmount = orderRequest.totalAmount
                this.paymentMethodId = orderRequest.paymentMethodId
                this.tipValue = orderRequest.tipValue
                this.waiterId = orderRequest.waiterId
                this.customerId = orderRequest.customerId
                this.receivedPaymentAmount = orderRequest.receivedPaymentAmount
                this.selectedCard = orderRequest.selectedCard
                this.orderDeliveredOrNot = orderRequest.orderDeliveredOrNot
                this.paymentReceivedOrNot = orderRequest.paymentReceivedOrNot
                this.orderStatus = orderRequest.orderStatus
                this.cartItems = orderRequest.cartItems
            }

            mainScope.launch {
                insertOffline2OrderAsync(offlineOrder2).await()
                //           Toast.makeText(ctx, "Inserted in offline database", Toast.LENGTH_SHORT).show()
                ctx.sendBroadcast(Intent(Constants.CLEAR_CART_BROADCAST))
                MainActivity.mainSharedPreferences.edit()
                    .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                MainActivity.progressDialogRepository.showAlertDialog(
                    "Successfully sent to kitchen")
                MainActivity.orderViewModel.updaterTodayOrders()
                syncRequired = true
                // insert in table data
                val tableId: Int = if (orderRequest.table.isNullOrBlank()) {
                    0
                } else {
                    (orderRequest.table ?: "0").toInt()
                }
                val tableData = MainActivity.mainRepository.getOneTableDataAsync(tableId).await()
                if (tableData != null) {
                    tableData.assignedOrNot = 1
                    val relatedOrders = TableData.RelatedOrderInfo().apply {
                        this.orderId = newId
                        this.customerType = orderRequest.customerType
                        this.orderStatus = orderRequest.orderStatus
                        this.customerPaid = orderRequest.customerPaid.toString()
                        val waiterInfo =
                            MainActivity.mainRepository.getOneSubUserDetailAsync(
                                orderRequest.waiterId ?: 0
                            ).await()
                        if (waiterInfo != null) {
                            this.firstName = waiterInfo.firstname
                            this.lastName = waiterInfo.lastname
                        }
                        this.billStatus = orderRequest.billStatus
                    }
                    val tableOrders =
                        tableData.relatedOrders as ArrayList<TableData.RelatedOrderInfo>
                    tableOrders.add(relatedOrders)
                    tableData.relatedOrders = tableOrders
                    MainActivity.mainRepository.updateOneTableDataAsync(
                        tableData
                    ).await()
                    Log.d("NewOrder", "Table data updated")
                } else {
                    Log.e("NewOrder", "Table data not found for $tableId")
                }
            }
            /*      Toast.makeText(ctx, "Internet Not available", Toast.LENGTH_SHORT).show()
                  val newId = Calendar.getInstance().timeInMillis
                  mainScope.launch {
                      val ordersEntity = OrdersEntity().apply {
                          this.order_id = newId
                          this.outlet_id = Constants.selectedOutletId
                          this.menu_id = null
                          this.customer_id = orderRequest.customerId
                          this.customer_type = orderRequest.customerType.toString()
                          this.order_date = orderRequest.orderDate
                          this.order_time = orderRequest.orderTime
                          this.cookedtime = orderRequest.cookedTime
                          this.table_no = orderRequest.table?.toInt()
                          this.discount = orderRequest.discount.toString()
                          this.tip_type = orderRequest.tipType
                          this.tip_value = orderRequest.tipValue.toString()
                          this.totalamount = orderRequest.totalAmount
                          this.customerpaid = orderRequest.customerPaid.toString()
                          this.customer_note = ""
                          this.order_status = orderRequest.orderStatus
                          this.syncOrNot = 0
                      }
                      val itemInfo = ArrayList<OrdersEntity.ItemInfo>()
                      if (!orderRequest.cartItems.isNullOrEmpty()) {
                          for (item in orderRequest.cartItems!!) {
                              val itemInfo1 = OrdersEntity.ItemInfo().apply {
                                  this.rowId = "NA"
                                  this.orderId = newId
                                  this.foodStatus = item.foodStatus
                                  this.menuQty = item.menuQty.toString()
                                  this.variantId = if (item.variantId.isNullOrBlank()) 0 else item.variantId?.toInt()
                                  this.productId = item.productId
                                  this.taxId = item.taxIds
                                  this.taxPercentage = item.taxPercentages
                                  this.is2xMod = item.is2xMode
                                  this.discountType = item.discountType.toString()
                                  this.itemDiscount = item.itemDiscount.toString()
                              }
                              val modifiers = ArrayList<OrdersEntity.ModifierInfo>()
                              val selectedModifiers = item.modifierIdStr?.split(",")
                              if (!selectedModifiers.isNullOrEmpty()) {
                                  for (selectedMod in selectedModifiers) {
                                      val modInfo = getOneModifierDataAsync(selectedMod.toInt()).await()
                                      modifiers.add(OrdersEntity.ModifierInfo().apply {
                                          this.modifierId = modInfo?.modifierId
                                          this.isHalfAndHalf = modInfo?.halfAndHalf
                                      })
                                  }
                              }
                              itemInfo1.modifierInfo = modifiers
                              itemInfo.add(itemInfo1)
                              break
                          }
                      }
                      ordersEntity.itemsInfo = itemInfo
                      val list = ArrayList<OrdersEntity>()
                      list.add(ordersEntity)
                      mainScope.launch {
                          MainActivity.orderRepository.insertAllOrdersAsync(list).await()
                          MainActivity.orderRepository.updateViewModel(false)
                      }
                  } */
            return
        }
        if (syncRequired) {
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
        val req = convertOrderRequestToJson(orderRequest)
        Log.d("NewOrderJson", req)
        val dialogId = MainActivity.progressDialogRepository
            .getProgressDialog("Placing Order")
        ApiService.apiService?.createNewOrder(req, Constants.authorization)?.enqueue( object :
            Callback<CreateOrderResponse?> {

            override fun onResponse(
                call: Call<CreateOrderResponse?>,
                response: Response<CreateOrderResponse?>
            ) {
                if (response.isSuccessful && response.body()?.status != null &&
                    response.body()?.status!!) {
                        val orderId = response.body()?.orderData?.orderId
             //       showAlertDialog(ctx, "Order Placed Successfully"
                 SendEmailDialog((orderId ?: 0).toLong()).show(MainActivity.fManager, "email")
                    MainActivity.orderRepository.getAllOrdersOnline(
                        ctx, Constants.selectedOutletId, 0,false)
             //       MainActivity.mainRepository.loadTableData(ctx, Constants.selectedOutletId,
           //         MainActivity.deviceID, MainActivity.deviceID, 1, false)
                    ctx.sendBroadcast(Intent(Constants.CLEAR_CART_BROADCAST))
                } else {
                    //                 showErrorDialog(ctx, "Error: ${response.body()?.message}")
                    MainActivity.progressDialogRepository.showErrorDialog(
                        "Error Placing order: ${response.body()?.message}"
                    )
                }
                Log.d("OrderPLaced", "${response.body()?.message}")
                MainActivity.progressDialogRepository.dismissDialog(dialogId)
            }

            override fun onFailure(call: Call<CreateOrderResponse?>, t: Throwable) {
                MainActivity.progressDialogRepository.showErrorDialog(
                    "Error Placing order: ${t.message}"
                )
                MainActivity.progressDialogRepository.dismissDialog(dialogId)
                Log.e("OrderPLaced", "Error: ${t.message}")
            }
        }
        )
    }

    private fun convertOrderRequestToJson(orderRequest: ProductOrderRequest) : String {
        val gson = Gson()
        val typeT = object: TypeToken<ProductOrderRequest>() { }
        return gson.toJson(orderRequest, typeT.type)
    }


    private fun startSync(ctx: Context, unSyncOrders: List<OrdersEntity>) {
        // collect not sync orders
        mainScope.launch {
            Log.d("Sync", "started")
            val orderSyncEntity = OrderSyncEntity()
            val oList = ArrayList<OrderSyncEntity.OrderSync>()
            val editedSyncO = getAllEditOrdersAsync(Constants.selectedOutletId).await()
            if (editedSyncO != null) {
                startEditedOrderSync(ctx, editedSyncO)
                Log.d("EditedSync", "Calling API")
            }
            if (unSyncOrders.isNullOrEmpty()) {
                Log.d("Unsync", "Unsync Orders not found")
                return@launch
            }
            for (order in unSyncOrders) {
                val uOrder = OrderSyncEntity.OrderSync().apply {
                    orderId = order.order_id
                    outlet_id = order.outlet_id
                    waiter_id = order.waiter_id
                    order_accept_date = order.order_accept_date
                    cookedtime = order.cookedtime
                    discount = order.discount
                    tip_type = order.tip_type
                    tip_amount = order.tip_amount
                    added_tip_amount = order.added_tip_amount
                    tip_amount = order.tip_amount
                    totalamount = order.totalamount
                    customerpaid = order.customerpaid
                    customer_note = order.customer_note
                    anyreason = order.anyreason
                    order_status = order.order_status
                    pis_driver_assigned = order.pis_driver_assigned
                    driver_user_id = order.driver_user_id
                    editTime = order.editTime
                }
                // collect item info
                val itemInfo = order.itemsInfo
                val itemsToSync = ArrayList<OrderSyncEntity.ItemInfoSyn>()
                if (!itemInfo.isNullOrEmpty()) {
                    for (item in itemInfo) {
                        val itemSyn = OrderSyncEntity.ItemInfoSyn().apply {
                            rowId = item.rowId
                            uniqueRecordId = item.uniqueRecordId
                            foodStatus = item.foodStatus
                        }
                        itemsToSync.add(itemSyn)
                    }
                    uOrder.itemInfoSyn = itemsToSync
                }
                // collect bill info
                val billInfo = order.billInfo
                if (billInfo != null) {
                    val billToSyn = OrderSyncEntity.BillInfoSyn().apply {
                        billId = billInfo.billId
                        billStatus = billInfo.billStatus
                        paymentMethodId = billInfo.paymentMethodId
                        accountNumber = billInfo.accountNumber
                        selectedCard = billInfo.selectedCard
                        cardType = billInfo.cardType
                    }
                    uOrder.billInfoSyn = billToSyn
                }
                oList.add(uOrder)
            }
            Log.d("newSync","Order ready to sync")
            Log.d("newSync","Converting to json")

            orderSyncEntity.ordersSync = oList
            val gson = Gson()
            val typeT = object: TypeToken<OrderSyncEntity>() { }
            val syncJson = gson.toJson(orderSyncEntity, typeT.type)
            Log.d("newSync","finished, now api call")
            updateOrdersOnline(ctx, syncJson)
        }
    }

    fun mapOfflineCustomers(ctx: Context, mapping: List<CustomerSyncResponse.CustomerMap>?) {
        mainScope.launch {
            Log.d("Mapping", "started")
            val unsyncOrders = getUnsyncOrdersAsync(Constants.selectedOutletId).await()
            if (unsyncOrders != null) {
                if (mapping == null) {
                    startSync(ctx, unsyncOrders)
                } else {
                    for (offOrder in unsyncOrders) {
                        if (isOfflineOrder(offOrder.customer_id ?: 0L)) {
                            val map = mapping?.find { it.customerTmpId==offOrder.customer_id }
                            if (map != null) {
                                offOrder.customer_id = map.customerOriginalId
                            }
                        }
                    }
                    startSync(ctx, unsyncOrders)
                }
            }
            val offlineOrders = getOffline2OrdersAsync(Constants.selectedOutletId).await()
            if (!offlineOrders.isNullOrEmpty()) {
                if (mapping == null) {
                    startUploadingOfflineOrders(ctx, offlineOrders)
                } else {
                    for (offOrder in offlineOrders) {
                        if (isOfflineOrder(offOrder.customerId ?: 0L)) {
                            val map = mapping.find { it.customerTmpId == offOrder.customerId }
                            if (map != null) {
                                offOrder.customerId = map.customerOriginalId
                            }
                        }
                    }
                    startUploadingOfflineOrders(ctx, offlineOrders)
                }
            }
        }
    }

    private fun startUploadingOfflineOrders(ctx: Context, offlineOrders: List<OfflineOrder2>?) {
        mainScope.launch {
            Log.d("OfflineOrders", "${offlineOrders?.size} offline orders to upload")
            val createMultipleOrderRequest = convertOfflineOrdersToJson(offlineOrders ?: emptyList())
            Log.d("MultiOrderRequest", createMultipleOrderRequest ?: "NA")
            ApiService.apiService?.createMultipleOrders(createMultipleOrderRequest, Constants.authorization)
                ?.enqueue(object : Callback<OfflineSyncResponse> {
                    override fun onResponse(
                        call: Call<OfflineSyncResponse>,
                        response: Response<OfflineSyncResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status!= null && response.body()?.status!!) {
                            Log.d("MultiOrder", "${response.body()?.message}")
                            val resData = response.body()?.sycResponse
                            Log.d("MultiOrderResSize", "${resData?.size}")
                            if (!resData.isNullOrEmpty()) {
                                mainScope.launch {
                                    for (syncData in resData) {
                                        if (syncData.status == true) {
                                            // delete offline record from database
                                            deleteOneOfflineOrderAsync(syncData.tmpOrderId ?: 0L).await()
                                            val intent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
                                            intent.putExtra(Constants.IS_SUCCESS, true)
                                            ctx.sendBroadcast(intent)
                                        } else {
                                            MainActivity.progressDialogRepository.showErrorDialog(
                                                "Error uploading record with tmpId: ${syncData.tmpOrderId}," +
                                                        "Please try again"
                                            )
                                        }
                                    }
                                    Log.d("Calling:", "GetAllORders")
                                }
                            }
                        } else {
                            Log.e("MultiOrder", "Error: ${response.body()?.status}, ${response.body()?.message}")
                            if (response.body()?.message?.lowercase()?.contains("invalid") == true) {
                                val intent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
                                intent.putExtra(Constants.IS_SUCCESS, false)
                                ctx.sendBroadcast(intent)
                                MainActivity.logOutNow(ctx)
                            }
                        }
                    }

                    override fun onFailure(call: Call<OfflineSyncResponse>, t: Throwable) {
                        Log.e("MultiOrder", "Error: ${t.message}")
                    }

                })
        }
    }

    private fun startEditedOrderSync(ctx: Context, eSync : OrderUpdateSync) {
        val gson = Gson()
        val typeT = object : TypeToken<OrderUpdateSync>() { }
        val req = gson.toJson(eSync, typeT.type)
        Log.d("EditedSync", "Req: $req")
        ApiService.apiService?.synEditedOrders(req, Constants.authorization)
            ?.enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(
                    call: Call<SimpleResponse>,
                    response: Response<SimpleResponse>
                ) {
                    Log.d("EditedSync", "Response: ${response.body()?.message}")
                    if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                        val syncIntent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
                        syncIntent.putExtra(Constants.IS_SUCCESS, true)
                        ctx.sendBroadcast(syncIntent)
                        mainScope.launch {
                            deleteEditedOrders(Constants.selectedOutletId).await()
                        }
                    } else {
                        val syncIntent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
                        syncIntent.putExtra(Constants.IS_SUCCESS, false)
                        ctx.sendBroadcast(syncIntent)
                    }
                }

                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    Log.e("EditedSync", "Error: ${t.message}")
                    val syncIntent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
                    syncIntent.putExtra(Constants.IS_SUCCESS, false)
                    ctx.sendBroadcast(syncIntent)
                }

            })
    }

    private fun convertOfflineOrdersToJson(offlineOrders2: List<OfflineOrder2>) : String? {
        val offlineOrderReq = OfflineOrderSyncRequest()
        offlineOrderReq.orders.addAll(offlineOrders2)
        val gson = Gson()
        val typeT = object : TypeToken<OfflineOrderSyncRequest>() { }
        return gson.toJson(offlineOrderReq, typeT.type)
    }

    fun assignDriverOnline(ctx: Context, orderId: Long, userId: Int)  {
        if (!MainActivity.isInternetAvailable(ctx)) {
     //       Toast.makeText(ctx, "Internet Not Available, update in offline", Toast.LENGTH_SHORT).show()
            assignDriverOffline(ctx, orderId, userId)
            return
        }
        if (syncRequired) {
    //        Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
      //      showSyncDialog(ctx)
            return
        }
        val dId = MainActivity.progressDialogRepository.getProgressDialog(
            "Assigning Delivery Person...")
        Log.d("Assigning Driver:", "$userId for order: $orderId")
        ApiService.apiService?.assignDeliveryBoy(orderId, userId, Constants.authorization)
            ?.enqueue(object : Callback<SimpleResponse?> {
                override fun onResponse(
                    call: Call<SimpleResponse?>,
                    response: Response<SimpleResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.status != null &&
                        response.body()?.status!!) {
                        Log.d("AssignDriver", "${response.body()?.message}")
                        assignDriverOffline(ctx, orderId, userId)
                    } else {
                        Log.d("AssignDriver", "status false, ${response.body()?.message}")
                        val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                        closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_DRIVER_ASSIGN)
                        ctx.sendBroadcast(closeIntent)
                    }
                    MainActivity.progressDialogRepository.dismissDialog(dId)
                }

                override fun onFailure(call: Call<SimpleResponse?>, t: Throwable) {
                    Toast.makeText(ctx, "An Error Occurred, Please try again", Toast.LENGTH_SHORT).show()
                    Log.e("AssignFailure", "${t.message}")
                    val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                    closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_DRIVER_ASSIGN)
                    ctx.sendBroadcast(closeIntent)
                    MainActivity.progressDialogRepository.dismissDialog(dId)
                }

            })
    }

    private fun assignDriverOffline(ctx: Context, orderId: Long, userId: Int) {
        mainScope.launch {
            val order = getSingleOrderAsync(Constants.selectedOutletId, orderId).await()
            if (order != null) {
                order.pis_driver_assigned = 1
                order.driver_user_id = userId
                order.editTime = System.currentTimeMillis()
                updateSingleOrderAsync(order).await()
                updateViewModel()
            }
            val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
            closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_DRIVER_ASSIGN)
            ctx.sendBroadcast(closeIntent)
        }
    }

    fun sendSmsOnline(ctx : Context, outletId: Int, customerId : Long, message : String) {
        val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
        closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_SEND_SMS)
        if (!MainActivity.isInternetAvailable(ctx)) {
    //        Toast.makeText(ctx, "Internet Not Available", Toast.LENGTH_SHORT).show()
            ctx.sendBroadcast(closeIntent)
            return
        }
        if (syncRequired) {
     //       Toast.makeText(ctx, "Sync Required", Toast.LENGTH_SHORT).show()
            MainActivity.progressDialogRepository.showSyncDialog()
            return
        }
        val dId = MainActivity.progressDialogRepository.getProgressDialog("Sending SMS....")
        ApiService.apiService?.sendOngoingOrderSms(outletId, customerId, message,
            Constants.authorization)
            ?.enqueue(object : Callback<SimpleResponse?> {
                override fun onResponse(
                    call: Call<SimpleResponse?>,
                    response: Response<SimpleResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.status != null &&
                        response.body()?.status!!) {
                        Log.d("SmsApi", "${response.body()?.message}")
                    } else {
                        Toast.makeText(ctx, "Sms Sending failed", Toast.LENGTH_SHORT).show()
                        Log.e("SmsError", "${response.body()?.message}")
                    }
                    ctx.sendBroadcast(closeIntent)
                    MainActivity.progressDialogRepository.dismissDialog(dId)
                }

                override fun onFailure(call: Call<SimpleResponse?>, t: Throwable) {
                    Log.e("SmsError", "${t.message}")
                    Toast.makeText(ctx, "Sms Sending failed", Toast.LENGTH_SHORT).show()
                    ctx.sendBroadcast(closeIntent)
                    MainActivity.progressDialogRepository.dismissDialog(dId)
                }
            })
    }

    fun updateOrder(ctx: Context, oRequest: OrderUpdateRequest, orderToUpdate: OrdersEntity) {
        if (MainActivity.isInternetAvailable(ctx)) {
            val pId = MainActivity.progressDialogRepository.getProgressDialog("Updating Order")
            val gson = Gson()
            val typeT = object : TypeToken<OrderUpdateRequest>() { }
            val requestJson = gson.toJson(oRequest, typeT.type)
            Log.d("updateRequest", requestJson)
            ApiService.apiService?.updateOrder(requestJson, Constants.authorization)
                ?.enqueue(object : Callback<SimpleResponse> {
                    override fun onResponse(
                        call: Call<SimpleResponse>,
                        response: Response<SimpleResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status != null
                            && response.body()?.status!!) {
                            MainActivity.progressDialogRepository.showAlertDialog(
                                "Updated Successfully")
                            updateOrderOffline(orderToUpdate,1)
                    //        MainActivity.orderRepository.getAllOrdersOnline(ctx, Constants.selectedOutletId,1, true)
                            ctx.sendBroadcast(Intent(Constants.EXIT_EDITING_MODE_BROADCAST))
                        } else {
                            MainActivity.progressDialogRepository.showErrorDialog(
                                "Error Updating Order: ${response.body()?.message}")
                        }
                        MainActivity.progressDialogRepository.dismissDialog(pId)
                    }

                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                        MainActivity.progressDialogRepository.dismissDialog(pId)
                        MainActivity.progressDialogRepository.showErrorDialog(
                            "Error Updating Order: ${t.message}"
                        )
                    }

                })
        } else {
            mainScope.launch {
                updateOrderOffline(orderToUpdate,1)
                MainActivity.mainSharedPreferences.edit()
                    .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                syncRequired = true
                if (!isOfflineOrder(orderToUpdate.order_id)) {
                    val editedListO = getAllEditOrdersAsync(Constants.selectedOutletId).await()
                    if (editedListO != null) {
                        // update list
                        val eObject = getAllEditOrdersAsync(Constants.selectedOutletId).await()
                        if (eObject != null) {
                            val eList = eObject.editedOrders as ArrayList<OrderUpdateRequest>
                            eList.add(oRequest)
                            eObject.editedOrders = eList
                            insertEditedOrdersAsync(eObject).await()
                            Log.d("EditedOrder", "Added to list")
                        }
                    } else {
                        // create new obj
                        val eOrderList = ArrayList<OrderUpdateRequest>()
                        eOrderList.add(oRequest)
                        val eObject = OrderUpdateSync().apply {
                            this.outletId = Constants.selectedOutletId
                            this.editedOrders = eOrderList
                        }
                        insertEditedOrdersAsync(eObject).await()
                        Log.d("EditedOrder", "new list created")
                    }
                }
                MainActivity.progressDialogRepository.showAlertDialog(
                    "Order edited successfully")
                ctx.sendBroadcast(Intent(Constants.EXIT_EDITING_MODE_BROADCAST))
            }
        }
    }

    fun updateOrderOffline(orderEntity: OrdersEntity?,isSync : Int) {
        mainScope.launch {
            if (orderEntity != null) {
                if (isOfflineOrder(orderEntity.order_id)) {
                    val oldOfflineOrder = getOneOffline2OrderAsync(Constants.selectedOutletId,
                        orderEntity.order_id).await()
                    val offlineOrder2 = OfflineOrder2().apply {
                        this.tmpOrderId = orderEntity.order_id
                        this.customerType = oldOfflineOrder?.customerType
                        this.billTime = oldOfflineOrder?.billTime
                        this.selectedCardType = oldOfflineOrder?.selectedCardType
                        this.driverUserId = oldOfflineOrder?.driverUserId
                        this.orderDate = oldOfflineOrder?.orderDate
                        this.outletId = oldOfflineOrder?.outletId
                        this.orderAcceptDate = oldOfflineOrder?.orderAcceptDate
                        this.billDate = oldOfflineOrder?.billDate
                        this.customerPaid = if (orderEntity.customerpaid.isNullOrBlank()) {
                            0f
                        } else {
                            (orderEntity.customerpaid ?: "0").toFloat()
                        }
                        this.table = orderEntity.table_no.toString()
                        this.accountNo = orderEntity.account_number
                        this.driverAssigned = orderEntity.pis_driver_assigned
                        this.cardType = oldOfflineOrder?.cardType
                        this.discount = if (orderEntity.discount.isNullOrBlank()) {
                            0f
                        } else {
                            (orderEntity.discount ?: "0").toFloat()
                        }
                        this.billStatus = orderEntity.billInfo?.billStatus
                        this.cookedTime = orderEntity.cookedtime
                        this.orderTime = orderEntity.order_time
                        this.tipAmount = if (orderEntity.tip_amount.isNullOrBlank()) {
                            0f
                        } else {
                            (orderEntity.tip_amount ?: "0").toFloat()
                        }
                        this.tipType = orderEntity.tip_type
                        this.createdBy = oldOfflineOrder?.createdBy
                        this.totalAmount = orderEntity.totalamount
                        this.paymentMethodId = if (orderEntity.payment_method_id.isNullOrBlank()) {
                            0
                        } else {
                            (orderEntity.payment_method_id ?: "0").toInt()
                        }
                        this.tipValue = if (orderEntity.tip_amount.isNullOrBlank()) {
                            0f
                        } else {
                            (orderEntity.tip_amount ?: "0").toFloat()
                        }
                        this.waiterId = orderEntity.waiter_id
                        this.customerId = orderEntity.customer_id
                        this.receivedPaymentAmount = if (orderEntity.received_payment_amount.isNullOrBlank()) {
                            0f
                        } else {
                            (orderEntity.received_payment_amount ?: "0").toFloat()
                        }
                        this.selectedCard = oldOfflineOrder?.selectedCard
                        this.orderDeliveredOrNot = orderEntity.pis_order_delivered
                        this.paymentReceivedOrNot = if (orderEntity.pis_payment_received.isNullOrBlank()) {
                            0
                        } else {
                            (orderEntity.pis_payment_received ?: "0").toInt()
                        }
                        this.orderStatus = orderEntity.order_status
                        this.cartItems = oldOfflineOrder?.cartItems
                    }
                    updateSingleOfflineOrderAsync(offlineOrder2).await()
                    Log.d("EditOrder", "updated offline")
                } else {
                    if (isSync == 0) {
                        MainActivity.mainSharedPreferences.edit()
                            .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                        syncRequired = true
                    }
                    orderEntity.syncOrNot = isSync
                    orderEntity.editTime = System.currentTimeMillis()
                    updateSingleOrderAsync(orderEntity).await()
                    Log.d("EditOrder", "updated offline")
                }
                updateViewModel()
            }

        /*    if (orderEntity != null) {
                orderEntity.customer_id = oRequest.customerId
                orderEntity.waiter_id = oRequest.waiterId
                orderEntity.cookedtime = oRequest.cookedTime
                orderEntity.table_no = oRequest.table
                orderEntity.discount = oRequest.discount.toString()
                orderEntity.tip_type = oRequest.tipType
                orderEntity.tip_value = oRequest.tipValue.toString()
                orderEntity.tip_amount = oRequest.tipAmount.toString()
                orderEntity.totalamount = oRequest.totalAmount
                orderEntity.customerpaid = oRequest.customerPaid.toString()
                orderEntity.order_status = oRequest.orderStatus
                orderEntity.account_number = oRequest.accountNo
                orderEntity.payment_method_id = oRequest.paymentMethodId
                orderEntity.syncOrNot = isSync
                orderEntity.editTime = System.currentTimeMillis()
            }
            val itemsInfo = orderEntity?.itemsInfo as ArrayList<OrdersEntity.ItemInfo>
            for (newItem in (oRequest.cartItems ?: emptyList())) {
                val alreadyExist = itemsInfo.find { it.productId == newItem.productId }
                if (alreadyExist != null) continue
                val newItemInfo = OrdersEntity.ItemInfo().apply {
                    this.rowId = "${System.currentTimeMillis()}row"
                    this.orderId = orderEntity.order_id
                    this.uniqueRecordId = "${System.currentTimeMillis()}uniqueRecord"
                    this.foodStatus = 0
                    this.menuId = orderEntity.menu_id
                    this.menuQty = (newItem.qty ?: 0).toString()
                    this.varientId = newItem.sizeId
                    this.productId= newItem.productId
                    val productInfo = MainActivity.mainRepository.getProductDataAsync(newItem.productId ?: 0).await()
                    if (productInfo !=null) {
                        this.taxId = productInfo.productTaxIds
                    }
             //       this.isHalfAndHalf =
            //        this.is2xMod =
                }
            }  */
        }
    }

    fun markAllOrdersAsSynchronized() {
        mainScope.launch {
            val unSyncOrders = getUnsyncOrdersAsync(Constants.selectedOutletId).await()
            if (!unSyncOrders.isNullOrEmpty()) {
                for (order in unSyncOrders) {
                    order.syncOrNot = 1
                    updateSingleOrderAsync(order).await()
                }
                updateViewModel()
            }
        }
    }

    fun getSingleOrderOnline(ctx: Context, outletId: Int, queries: String) {
        if (MainActivity.isInternetAvailable(ctx)) {
            ApiService.apiService?.getSingleOrderDetails(outletId, queries, Constants.authorization)
                ?.enqueue(object : Callback<OrdersResponse?> {
                    override fun onResponse(
                        call: Call<OrdersResponse?>,
                        response: Response<OrdersResponse?>
                    ) {
                        Log.d("SingleOrderAPI", "response :$response")
                        if (response.isSuccessful && response.body()?.status != null &&
                                response.body()?.status.equals("true")) {
                            val allOrders= response.body()?.data
                            mainScope.launch {
                                insertAllOrdersAsync(allOrders ?: emptyList()).await()
                                Log.d("SingleOrderAPI", "Edited in database")
                                updateViewModel()
                            }
                        }
                    }

                    override fun onFailure(call: Call<OrdersResponse?>, t: Throwable) {
                        Log.e("SingleOrderAPI", "Failed :${t.message}")
                    }

                })
        }
    }

    suspend fun insertAllOrdersAsync(orders: List<OrdersEntity>) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.insertOrdersData(orders)
            }
        }

  /*  private suspend fun insertOfflineOrderAsync(offlineOrder: OfflineOrder) =
        coroutineScope {
            async (Dispatchers.IO) {
                oDao.insertNewOfflineOrder(offlineOrder)
            }
        }  */

    private suspend fun insertOffline2OrderAsync(offlineOrder2: OfflineOrder2) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.insertNewOffline2Order(offlineOrder2)
            }
        }

    private suspend fun insertEditedOrdersAsync(editedOrderO : OrderUpdateSync) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.insertEditedOrders(editedOrderO)
            }
        }

    suspend fun getAllOrdersOfflineAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getOrdersData(outletId)
            }
        }

    suspend fun getTodaysOrderOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getOrdersByDate(outletId, getTodayString())
            }
        }

    private suspend fun getUnsyncOrdersAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getUnSyncOrders(outletId)
            }
        }

    suspend fun getSingleOrderAsync(outletId: Int, orderId: Long) : Deferred<OrdersEntity?> =
        coroutineScope {
            async(Dispatchers.IO) {
                var order : OrdersEntity? = null
                if (orderId > 1647955400000) {
                    val allOffline = MainActivity.orderViewModel.offlineOrders.value
                    if (!allOffline.isNullOrEmpty()) {
                        for (offOrder in allOffline) {
                            if (offOrder.order_id == orderId) {
                                order = offOrder
                                break
                            }
                        }
                    }
                } else {
                    order = oDao.getSingleOrder(outletId, orderId)
                }
                return@async order
            }
        }

 /*   suspend fun getAllOrdersByStatusAsync(outletId: Int, orderStatus: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getOrdersByStatus(outletId, orderStatus)
            }
        }

    suspend fun getKitchenItemsOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getKitchenOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getReadyOrdersOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getReadyOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getOnlineOrdersOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getOnlineOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getAcceptedOrdersOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getAcceptedOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getCompletedOrdersOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getCompletedOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getCancelledOrdersOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getCancelledOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getServedOrdersOfTodayOnlyAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getServedOrdersByDate(
                    outletId, todayString)
            }
        }

    suspend fun getFutureOrdersAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getFutureOrders(outletId)
            }
        } */

 /*   suspend fun getOfflineOrdersAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getAllOfflineOrders(outletId)
            }
        }  */

    suspend fun getOffline2OrdersAsync(outletId: Int) =
        coroutineScope {
            async (Dispatchers.IO){
                return@async oDao.getAllOffline2Orders(outletId)
            }
        }

    private suspend fun getOneOffline2OrderAsync(outletId: Int, tmpId: Long) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getOneOffline2Order(outletId, tmpId)
            }
        }

    private suspend fun getAllEditOrdersAsync(outletId: Int) : Deferred<OrderUpdateSync?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async oDao.getAllEditedOrders(outletId)
            }
        }

    fun changeKitchenOrderStatusOffline2(ctx:Context, outletId: Int,
                                         orderId: Long, status: Int,
                                         isSync: Int, qRowId: String) {
        mainScope.launch {
            if (isOfflineOrder(orderId)) {
               return@launch
            } else {
                val order = getSingleOrderAsync(outletId, orderId).await()
                if (order != null) {
                    order.syncOrNot = isSync
                    val items = order.itemsInfo as ArrayList<OrdersEntity.ItemInfo>
                    var statusUpdated = false
                    var allFoodCompleted = true
                    if (!items.isNullOrEmpty()) {
                        for (item in items) {
                            if (item.rowId == qRowId) {
                                item.foodStatus = status
                                statusUpdated = true
                            }
                            if (item.foodStatus == 0) {
                                allFoodCompleted = false
                            }
                        }
                    }
                    val addOns = order.addOns
                    if (!addOns.isNullOrEmpty()) {
                        for (addOn in addOns) {
                            if (addOn.rowId == qRowId) {
                                addOn.foodStatus = status
                                statusUpdated = true
                            }
                            if (addOn.foodStatus == 0) {
                                allFoodCompleted = false
                            }
                        }
                    }
                    if (statusUpdated) {
                        if (allFoodCompleted) {
                            order.order_status = Constants.ORDER_STATUS_READY
                            Log.d("KitchenUpdated", "All Items Ready")
                        }
                        order.editTime = System.currentTimeMillis()
                        updateSingleOrderAsync(order).await()
                    }
                }
                else {
                    Log.e("KitchenUpdate", "Unable to find item in database with row id: $qRowId")
                    Toast.makeText(ctx,
                        "Unable to update offline database, please sync for better results",
                        Toast.LENGTH_SHORT).show()
                }
                updateViewModel()
                val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS)
                ctx.sendBroadcast(closeIntent)
            }
        }
    }

  /*  fun changeKitchenOrderStatusOffline(ctx:Context, outletId: Int,
                                                     orderId: Long, status: Int,
                                                     isSync: Int, qRowId: String) {
        mainScope.launch {
            val order = getSingleOrderAsync(outletId, orderId).await()
            if (order != null) {
                order.syncOrNot = isSync
                val items = order.itemsInfo as ArrayList<OrdersEntity.ItemInfo>
                var statusUpdated = false
                var allFoodCompleted = true
                if (!items.isNullOrEmpty()) {
                    for (item in items) {
                        if (item.rowId == qRowId) {
                            item.foodStatus = status
                            statusUpdated = true
                        }
                        if (item.foodStatus == 0) {
                            allFoodCompleted = false
                        }
                    }
                }
                val addOns = order.addOns
                if (!addOns.isNullOrEmpty()) {
                    for (addOn in addOns) {
                        if (addOn.rowId == qRowId) {
                            addOn.foodStatus = status
                            statusUpdated = true
                        }
                        if (addOn.foodStatus == 0) {
                            allFoodCompleted = false
                        }
                    }
                }
                if (statusUpdated) {
                    // if all items are completed then change order status as ready
                    if (isOfflineOrder(order.order_id)) {
                        val offOrder = getOneOffline2OrderAsync(Constants.selectedOutletId, order.order_id).await()
                        if (offOrder != null) {
                            if (allFoodCompleted) {
                                offOrder.orderStatus = Constants.ORDER_STATUS_READY
                            }
                            if (!offOrder.cartItems.isNullOrEmpty()) {
                                for (item in offOrder.cartItems!!) {
                                    if (item.rowId == qRowId) {
                                        item.foodStatus = status
                                        Log.d("FoodStatus Changed of", item.rowId!!)
                                        break
                                    }
                                }
                                updateSingleOfflineOrderAsync(offOrder).await()
                            }
                        }
                    } else {
                        if(allFoodCompleted) {
                            order.order_status = Constants.ORDER_STATUS_READY
                            Log.d("KitchenUpdated", "All Items Ready")
                        }
                        updateSingleOrderAsync(order).await()
                    }
                    // sync will required after this changes..
                    if (isSync == 0) {
                        MainActivity.mainSharedPreferences.edit()
                            .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, true).apply()
                        syncRequired = true
                        Log.d("KitchenUpdated", "Syn is set for $syncRequired")
                    }
                    Log.d("KitchenUpdated", "Item status updated")
                } else {
                    Log.e("KitchenUpdate", "Unable to find item in database with row id: $qRowId")
                    Toast.makeText(ctx,
                        "Unable to update offline database, please sync for better results",
                        Toast.LENGTH_SHORT).show()
                }
                updateViewModel()
                val closeIntent = Intent(Constants.CLOSE_DIALOG_BROADCAST)
                closeIntent.putExtra(Constants.ID_DIALOG, Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS)
                ctx.sendBroadcast(closeIntent)
            }
        }
    }

    fun updateOfflineItemFoodStatus(outletId: Int, orderId: Long, productId: Int) {
        mainScope.launch {
            val offOrder = getOneOffline2OrderAsync(outletId, orderId).await()
            if (offOrder != null && !offOrder.cartItems.isNullOrEmpty()) {
                var orderCompleted = true
                for (item in offOrder.cartItems!!) {
                    if (item.productId == productId) {
                        item.foodStatus = 1
                    }
                    if (item.foodStatus == 0) {
                        orderCompleted = false
                    }
                }
                if (orderCompleted) {
                    offOrder.orderStatus = Constants.ORDER_STATUS_READY
                    updateViewModel()
                }
                updateSingleOfflineOrderAsync(offOrder).await()
                Log.d("KitchenUpdate", "Offline order $orderId - $productId")
            }


        }
    }   */

    private suspend fun updateSingleOfflineOrderAsync(offOrder : OfflineOrder2) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.updateOneOffline2Order(offOrder)
            }
        }


    private suspend fun updateSingleOrderAsync(order: OrdersEntity) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.updateSingleOrder(order)
            }
        }

    private suspend fun updateEditOrdersAsync(eOrders : OrderUpdateSync) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.insertEditedOrders(eOrders)
            }
        }

    private suspend fun deleteAllOrdersAsync(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.deleteAllOrders(outletId)
            }
        }

 /*   suspend fun deleteAllOrderDatabaseAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.deleteAllOrders()
                oDao.deleteAllOfflineOrders()
            }
        }

    private suspend fun deleteAllOfflineOrdersAsync(outletId: Int) =
        coroutineScope {
            async (Dispatchers.IO) {
                oDao.deleteAllOfflineOrders(outletId)
            }
        }  */

    private suspend fun deleteOneOfflineOrderAsync(tmpId: Long) =
        coroutineScope {
            async (Dispatchers.IO) {
                oDao.deleteOneOfflineOrder(tmpId)
            }
        }

    private suspend fun deleteEditedOrders(outletId: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                oDao.deleteEditedOrders(outletId)
            }
        }

 /*   private suspend fun deleteAllOffline2OrdersAsync(outletId: Int) =
        coroutineScope {
            async (Dispatchers.IO) {
                oDao.deleteAllOffline2Orders(outletId)
            }
        }  */

    fun updateViewModel() {
        MainActivity.orderViewModel.setAllOrders()
    /*    if (onlyToday) {
            MainActivity.orderViewModel.updaterTodayOrders()
       /*     mainScope.launch {
                val todayOrders = getTodaysOrderOnlyAsync(Constants.selectedOutletId).await()
                MainActivity.orderViewModel.updateByTodayOrders(todayOrders)
            } */
        } else {

        }  */
  //      MainActivity.orderViewModel.updatedDatabase()
    }

    private fun formatCookingTime(minutes: Int) : String {
        val hour = minutes / 60
        var hourStr = hour.toString()
        if (hourStr.length == 1) {
            hourStr = "0$hourStr"
        }
        val reminder = minutes % 60
        var remStr = reminder.toString()
        if (remStr.length == 1) {
            remStr = "0$remStr"
        }
        Log.d("cookingTime", "$hourStr:$remStr:00" )
        return "$hourStr:$remStr:00"
    }

    fun getTodayString() : String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val monthInt = cal.get(Calendar.MONTH)
        var month = (monthInt+1).toString()
        if (month.length == 1) {
            month = "0$month"
        }
        var date = cal.get(Calendar.DATE).toString()
        if (date.length == 1) {
            date = "0$date"
        }
        return "$year-$month-$date"
    }

    fun roundFloatValue(num: Float) : Float {
        val num100 = num *100
        val rNum100 = kotlin.math.round(num100)
        return rNum100/100
    }

    private fun isOfflineOrder(orderId: Long) : Boolean { //can also be used to checkoffline customer
        return orderId > 1647955400000
    }
}