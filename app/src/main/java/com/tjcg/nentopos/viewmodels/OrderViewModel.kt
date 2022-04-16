package com.tjcg.nentopos.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.data.CustomerData
import com.tjcg.nentopos.data.OfflineOrder2
import com.tjcg.nentopos.data.OrdersEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class OrderViewModel : ViewModel() {

    private val mainScope = CoroutineScope(Dispatchers.Main)
 //   private val orderRepository = MainActivity.orderRepository

    private val _outletName = MutableLiveData<String>()
    val outletName : LiveData<String> = _outletName

    private val _allOrders = MutableLiveData<List<OrdersEntity>>()
    val allOrders : LiveData<List<OrdersEntity>> = _allOrders

    private val _allPendingOrders = MutableLiveData<List<OrdersEntity>>()
    val allPendingOrders : LiveData<List<OrdersEntity>> = _allPendingOrders

    private val _allProcessingOrders = MutableLiveData<List<OrdersEntity>>()
    val allProcessingOrders : LiveData<List<OrdersEntity>> = _allProcessingOrders

    private val _allReadyOrders = MutableLiveData<List<OrdersEntity>>()
    val allReadyOrders : LiveData<List<OrdersEntity>> = _allReadyOrders

    private val _allCompletedOrders = MutableLiveData<List<OrdersEntity>>()
    val allCompletedOrders : LiveData<List<OrdersEntity>> = _allCompletedOrders

    private val _allCancelledOrders = MutableLiveData<List<OrdersEntity>>()
    val allCancelledOrders : LiveData<List<OrdersEntity>> = _allCancelledOrders

    private val _allFutureOrders = MutableLiveData<List<OrdersEntity>>()
    val allFutureOrders : LiveData<List<OrdersEntity>> = _allFutureOrders

    private val _todayOrders = MutableLiveData<List<OrdersEntity>>()
    val todayOrders : LiveData<List<OrdersEntity>> = _todayOrders

    private val _completedOrders = MutableLiveData<List<OrdersEntity>>()
    val completedOrders : LiveData<List<OrdersEntity>> = _completedOrders

    private val _cancelledOrders = MutableLiveData<List<OrdersEntity>>()
    val cancelledOrders : LiveData<List<OrdersEntity>> = _cancelledOrders

    private val _onlineOrdersCount = MutableLiveData<Int>(0)
    val onlineOrdersCount : LiveData<Int> = _onlineOrdersCount

    private val _inProgress = MutableLiveData<Boolean>(false)
    val inProgress : LiveData<Boolean> = _inProgress

    private val _futureOrders = MutableLiveData<List<OrdersEntity>>()
    val futureOrders : LiveData<List<OrdersEntity>> = _futureOrders

    private val _kitchenOrdersToday = MutableLiveData<List<OrdersEntity>>()
    val kitchenOrdersToday : LiveData<List<OrdersEntity>> = _kitchenOrdersToday

    private val _readyOrdersToday = MutableLiveData<List<OrdersEntity>>()
    val readyOrdersToday : LiveData<List<OrdersEntity>> = _readyOrdersToday

    private val _onlineOrders = MutableLiveData<List<OrdersEntity>>()
    val onlineOrders : LiveData<List<OrdersEntity>> = _onlineOrders

    private val _acceptedOrders = MutableLiveData<List<OrdersEntity>>()
    val acceptOrders : LiveData<List<OrdersEntity>> = _acceptedOrders

    private val _servedOrders = MutableLiveData<List<OrdersEntity>>()
    val servedOrders : LiveData<List<OrdersEntity>>  = _servedOrders


    private val _ongoingOrdersCount = MutableLiveData<Int>(0)
    val ongoingOrdersCount : LiveData<Int> = _ongoingOrdersCount

    private val _onlineWithFutureOrderCount = MutableLiveData<Int>(0)
    val onlineWithFutureOrderCount : LiveData<Int> = _onlineWithFutureOrderCount

    private val _newCustomerData = MutableLiveData<List<CustomerData>>()
    val newCustomersData : LiveData<List<CustomerData>> = _newCustomerData

    private val _activeFutureOrders = MutableLiveData<List<OrdersEntity>>()
    val activeFutureOrders : LiveData<List<OrdersEntity>> = _activeFutureOrders

    private val _steadyFutureOrders = MutableLiveData<List<OrdersEntity>>()
    val steadyFutureOrders : LiveData<List<OrdersEntity>> = _steadyFutureOrders

    private val _offlineOrders = MutableLiveData<List<OrdersEntity>>()
    val offlineOrders : LiveData<List<OrdersEntity>> = _offlineOrders

    val lastSyncTiming = MutableLiveData<Long>()

    fun setNewCustomers(newList: List<CustomerData>) {
        _newCustomerData.value = newList
    }

    fun setOutletName(outName: String) {
        _outletName.value = outName
    }

    fun setAllOrders() {
        CoroutineScope(Dispatchers.Main).launch {
            _activeFutureOrders.value = emptyList()
            _steadyFutureOrders.value = emptyList()
            _onlineWithFutureOrderCount.value = 0
            updaterTodayOrders()
            val orders = MainActivity.orderRepository.getAllOrdersOfflineAsync(Constants.selectedOutletId).await() as ArrayList<OrdersEntity>
            val offlineOrders = MainActivity.orderRepository.getOffline2OrdersAsync(Constants.selectedOutletId).await()
            if (!offlineOrders.isNullOrEmpty()) {
                val convertedOfflines = convertOfflineOrdersToOrderEntity(offlineOrders)
                orders.addAll(convertedOfflines)
            }
            _allOrders.value = orders
            val allPendingOrders = ArrayList<OrdersEntity>()
            val allProcessingOrders = ArrayList<OrdersEntity>()
            val allReadyOrders = ArrayList<OrdersEntity>()
            val allServedOrders = ArrayList<OrdersEntity>()
            val allCanceledOrders = ArrayList<OrdersEntity>()
            val allFutureOrders = ArrayList<OrdersEntity>()
            for (order in orders) {
                when(order.order_status) {
                    Constants.ORDER_STATUS_PENDING -> {
                        setFutureOrderType(order)
                        allPendingOrders.add(order)
                    }
                    Constants.ORDER_STATUS_PROCESSING -> {
                        allProcessingOrders.add(order)
                    }
                    Constants.ORDER_STATUS_READY -> {
                        allReadyOrders.add(order)
                    }
                    Constants.ORDER_STATUS_SERVED -> {
                        allServedOrders.add(order)
                    }
                    Constants.ORDER_STATUS_CANCELED -> {
                        allCanceledOrders.add(order)
                    }
                }
            }
            _allPendingOrders.value = allPendingOrders
            _allProcessingOrders.value = allProcessingOrders
            _allReadyOrders.value = allReadyOrders
            _allCompletedOrders.value = allServedOrders
            _allCancelledOrders.value = allCanceledOrders
            _allFutureOrders.value = allFutureOrders
        }


   /*     _allOrders.postValue(orders)
        Log.d("orderViewModel", "All orders set: ${orders.size}")
        mainScope.launch {
            val allPendingOrders = orderRepository.getAllOrdersByStatusAsync(
                Constants.selectedOutletId, Constants.ORDER_STATUS_PENDING).await()
            _allPendingOrders.value = allPendingOrders
            val allProcessingOrders = orderRepository.getAllOrdersByStatusAsync(
                Constants.selectedOutletId, Constants.ORDER_STATUS_PROCESSING).await()
            _allProcessingOrders.value = allProcessingOrders
            val allCompletedOrders = orderRepository.getAllOrdersByStatusAsync(
                Constants.selectedOutletId, Constants.ORDER_STATUS_SERVED).await()
            _allCompletedOrders.value = allCompletedOrders
            val allCancelledOrders = orderRepository.getAllOrdersByStatusAsync(
                Constants.selectedOutletId, Constants.ORDER_STATUS_CANCELED).await()
            _allCancelledOrders.value = allCancelledOrders
            val kitchenList = orderRepository.getKitchenItemsOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _kitchenOrdersToday.value = kitchenList
            val readyOrders = orderRepository.getReadyOrdersOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _readyOrdersToday.value = readyOrders
            val onlineOrderList = orderRepository.getOnlineOrdersOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _onlineOrders.value = onlineOrderList
            _onlineOrdersCount.value = onlineOrderList.size
            val acceptedOrders = orderRepository.getAcceptedOrdersOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _acceptedOrders.value = acceptedOrders
            val completedOrders = orderRepository.getCompletedOrdersOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _completedOrders.value = completedOrders
            val cancelledOrders = orderRepository.getCancelledOrdersOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _cancelledOrders.value = cancelledOrders
            val servedOrders = orderRepository.getServedOrdersOfTodayOnlyAsync(
                Constants.selectedOutletId).await()
            _servedOrders.value = servedOrders
            val futureOrders = orderRepository.getFutureOrdersAsync(Constants.selectedOutletId).await()
            _futureOrders.value = futureOrders
            setOnGoingOrdersCount()
            setOnlineOrdersWithFutureOrderCount()
        }  */
    }

    private fun broadcastTodayOrdersUpdate(todayOrders : List<OrdersEntity>) {
        val kitchenOrders = ArrayList<OrdersEntity>()
        val onlineOrders = ArrayList<OrdersEntity>()
        val readyOrders = ArrayList<OrdersEntity>()
        val acceptedOrders = ArrayList<OrdersEntity>()
        for (order in todayOrders) {
            when (order.order_status) {
                Constants.ORDER_STATUS_PENDING -> {
                    setFutureOrderType(order)
                    if (order.futureOrderType == Constants.FUTURE_ORDER_NULL || order.futureOrderType == Constants.FUTURE_ORDER_NEAR) {
                        onlineOrders.add(order)
                    }
                }
                Constants.ORDER_STATUS_PROCESSING -> {
                    kitchenOrders.add(order)
                    if (!order.order_accept_date.isNullOrBlank()) {
                        acceptedOrders.add(order)
                    }
                }
                Constants.ORDER_STATUS_READY -> {
                    readyOrders.add(order)
                }
            }
        }
        _kitchenOrdersToday.value =  kitchenOrders
        _onlineOrders.value = onlineOrders
        _readyOrdersToday.value = readyOrders
        _acceptedOrders.value = acceptedOrders
        _onlineOrdersCount.value = onlineOrders.size
        _onlineWithFutureOrderCount.value = onlineOrders.size // + (_activeFutureOrders.value?.size ?: 0)
        _ongoingOrdersCount.value = (kitchenOrders.size + readyOrders.size)
    }

    fun updaterTodayOrders() {
        mainScope.launch {
            val todayOrders=
                MainActivity.orderRepository.getTodaysOrderOnlyAsync(Constants.selectedOutletId).await() as ArrayList<OrdersEntity>
            val offlineOrders: List<OfflineOrder2>? = MainActivity.orderRepository.getOffline2OrdersAsync(Constants.selectedOutletId).await()
            if (!offlineOrders.isNullOrEmpty()) {
                val offList = convertOfflineOrdersToOrderEntity(offlineOrders)
                _offlineOrders.value = offList
                todayOrders.addAll(offList)
            }
            broadcastTodayOrdersUpdate(todayOrders)
        }
    }

    fun setFutureOrderType(order: OrdersEntity) {
        if (order.future_order_date == null) {
            order.futureOrderType = Constants.FUTURE_ORDER_NULL
            return
        }
        val todayString = MainActivity.orderRepository.getTodayString()
        if (order.future_order_date == todayString) {
            if (order.future_order_time.isNullOrBlank()) {
                order.futureOrderType = Constants.FUTURE_ORDER_NULL
                return
            }
            val fTimings = order.future_order_time?.split(":")
            val fCalendar = Calendar.getInstance()
            fCalendar.set(Calendar.HOUR_OF_DAY, fTimings?.get(0)?.toInt() ?: 0)
            fCalendar.set(Calendar.MINUTE, fTimings?.get(1)?.toInt() ?: 0)
            fCalendar.set(Calendar.SECOND, fTimings?.get(2)?.toInt() ?: 0)
            if ((Calendar.getInstance().timeInMillis + 1800000) >= fCalendar.timeInMillis) {
                order.futureOrderType = Constants.FUTURE_ORDER_NEAR
                addToActiveFutureOrders(order)
            } else {
                order.futureOrderType = Constants.FUTURE_ORDER_FAR
                addToSteadyOrders(order)
            }
        } else {
            order.futureOrderType = Constants.FUTURE_ORDER_FAR
            addToSteadyOrders(order)
        }
    }

    private fun addToSteadyOrders(order: OrdersEntity) {
        val steadyOrders : ArrayList<OrdersEntity> = if (_steadyFutureOrders.value.isNullOrEmpty()) ArrayList()
            else (_steadyFutureOrders.value as ArrayList<OrdersEntity>)
        val findIt = steadyOrders.find { it.order_id == order.order_id }
        if (findIt == null) {
            steadyOrders.add(order)
            _steadyFutureOrders.value = steadyOrders
        }
    }

    private fun addToActiveFutureOrders(order: OrdersEntity) {
        val activeOrder : ArrayList<OrdersEntity> = if (_activeFutureOrders.value.isNullOrEmpty()) ArrayList()
            else (_activeFutureOrders.value as ArrayList<OrdersEntity>)
        val findIt = activeOrder.find { it.order_id == order.order_id }
        if (findIt == null) {
            activeOrder.add(order)
            _activeFutureOrders.value = activeOrder
            _onlineWithFutureOrderCount.value = (_onlineWithFutureOrderCount.value ?: 0) + 1
        }
    }

    private fun convertOfflineOrdersToOrderEntity(offlineOrders : List<OfflineOrder2>) : ArrayList<OrdersEntity>{
        val orders = ArrayList<OrdersEntity>()
    /*    val gson = Gson()
        val typeT = object: TypeToken<List<OfflineOrder2>>() { }
        val offOrderStr = gson.toJson(offlineOrders, typeT.type)
        Log.d("offlineOrderJson", offOrderStr)  */
        for (offOrder in offlineOrders) {
            val ordersEntity = OrdersEntity().apply {
                this.order_id = offOrder.tmpOrderId
                this.outlet_id = offOrder.outletId
                this.menu_id = null
                this.saleinvoice = null
                this.customer_id = offOrder.customerId
                this.customer_type = offOrder.customerType.toString()
                this.thirdpartyOrNot = null
                this.order_date = offOrder.orderDate
                this.order_time = offOrder.orderTime
                this.order_accept_date = null
                this.cookedtime = offOrder.cookedTime
                this.table_no = offOrder.table?.toInt()
                this.tokenno = null
                this.discount = offOrder.discount.toString()
                this.tip_type = offOrder.tipType
                this.added_tip_amount = null
                this.tip_value = offOrder.tipValue.toString()
                this.tip_amount = null
                this.totalamount = offOrder.totalAmount
                this.customerpaid = offOrder.customerPaid.toString()
                this.customer_note = ""
                this.anyreason = ""
                this.order_status = offOrder.orderStatus
                this.pis_driver_assigned = offOrder.driverAssigned
                this.driver_user_id = if (offOrder.driverUserId.isNullOrBlank()) 0 else offOrder.driverUserId?.toInt()
                this.pis_order_delivered = offOrder.orderDeliveredOrNot
                this.pis_payment_received = offOrder.paymentReceivedOrNot.toString()
                this.received_payment_amount = offOrder.receivedPaymentAmount.toString()
                this.order_pickup_at = null
                this.future_order_date = null
                this.future_order_time = null
                this.account_number = offOrder.accountNo
                this.payment_method_id = offOrder.paymentMethodId.toString()
                this.waiter_id = offOrder.waiterId
                this.syncOrNot = 0
            }
            val items = ArrayList<OrdersEntity.ItemInfo>()
            val addOns = ArrayList<OrdersEntity.AddOn>()
            Log.d("offItems", "${offOrder.tmpOrderId} : ${offOrder.cartItems?.size}")
            if (!offOrder.cartItems.isNullOrEmpty()) {
                for (cItem in offOrder.cartItems!!) {
                    if (!cItem.addOnIds.isNullOrBlank() && cItem.addOnIds!!.length == 1) {
                        val addOn = OrdersEntity.AddOn().apply {
                            this.addOnId = cItem.addOnIds?.toInt()
                            this.rowId = "addOn:${Calendar.getInstance().timeInMillis}"
                            this.addOnQty = cItem.addOnQty?.toString()
                            this.foodStatus = cItem.foodStatus
                            this.taxIds = cItem.taxIds
                            this.taxPers = cItem.taxPercentages
                        }
                        addOns.add(addOn)
                        continue
                    }
                    Log.d("offItems", "Found Product Id: ${cItem.productId}")
                    var productAlreadyAdded = false
                    if (!items.isNullOrEmpty()) {
                        for (item in items) {
                            if (item.productId == cItem.productId) {
                                productAlreadyAdded = true
                                val newMod = OrdersEntity.ModifierInfo().apply {
                                    this.modifierId = if (cItem.modifierIdStr.isNullOrBlank()) {
                                        0
                                    } else {
                                        (cItem.modifierIdStr ?: "0").toInt()
                                    }
                                    this.isHalfAndHalf = "1"
                                }
                                when (cItem.modifierType) {
                                    "0" -> {
                                        val newSubMod = OrdersEntity.SubModifierWhole().apply {
                                            val s = this
                                            s.subModifierId = if (cItem.subModIds.isNullOrBlank()) {
                                                    0
                                            } else {
                                                (cItem.subModIds ?: "0").toInt()
                                            }
                                            s.is2xMode = cItem.is2xMode
                                            mainScope.launch {
                                                val subModData = MainActivity.mainRepository
                                                    .getOneSubModifierDataAsync(subModifierId ?: 0).await()
                                                s.subModifierPrice = if (cItem.variantId.isNullOrBlank()) {
                                                    subModData?.normalModifierPrice
                                                } else {
                                                    "0"
                                                }

                                            }
                                        }
                                        val modifiers : ArrayList<OrdersEntity.ModifierInfo> =
                                            if (item.modifierInfo.isNullOrEmpty()) {
                                                ArrayList()
                                            } else {
                                                item.modifierInfo as ArrayList<OrdersEntity.ModifierInfo>
                                            }
                                        var modToReplace : OrdersEntity.ModifierInfo? = null
                                        if (!modifiers.isNullOrEmpty()) {
                                            for (mod1 in modifiers) {
                                                val modIdStr: Int = if (cItem.modifierIdStr.isNullOrBlank()) {
                                                    0
                                                } else {
                                                    (cItem.modifierIdStr ?: "0").toInt()
                                                }
                                                if (mod1.modifierId == modIdStr) {
                                                    Log.d("modifier:", "${mod1.modifierId} already available")
                                                    modToReplace = mod1
                                                    break
                                                }
                                            }
                                        }
                                        if (modToReplace != null) {
                                            val subList : ArrayList<OrdersEntity.SubModifierWhole> =
                                                if (modToReplace.subModifier?.subModifierWhole.isNullOrEmpty()) {
                                                    ArrayList()
                                                } else {
                                                    modToReplace.subModifier?.subModifierWhole as ArrayList<OrdersEntity.SubModifierWhole>
                                                }
                                            subList.add(newSubMod)
                                            modToReplace.subModifier?.subModifierWhole = subList
                                            Log.d("OffItems", "SubModifier inserted as Whole: ${cItem.subModIds} for ${cItem.modifierIdStr}")
                                        }
                                        if (modifiers.isNullOrEmpty() || modToReplace == null) {
                                            val subList = ArrayList<OrdersEntity.SubModifierWhole>()
                                            subList.add(newSubMod)
                                            val subMods = OrdersEntity.SubModifier().apply {
                                                this.subModifierWhole = subList
                                            }
                                            newMod.subModifier = subMods
                                            modifiers.add(newMod)
                                            item.modifierInfo = modifiers
                                            Log.d("OffItems", "SubModifier inserted as Whole: ${cItem.subModIds} for ${cItem.modifierIdStr}")
                                        }
                                    }
                                    "1" -> {
                                        val newSubMod = OrdersEntity.SubModifierFirstHalf().apply {
                                            val s= this
                                            s.subModifierId = (cItem.subModIds ?: "0").toInt()
                                            s.is2xMode = cItem.is2xMode
                                            mainScope.launch {
                                                val subData = MainActivity.mainRepository
                                                    .getOneSubModifierDataAsync(subModifierId ?: 0).await()
                                                s.subModifierPrice = if (cItem.variantId.isNullOrBlank()) {
                                                    subData?.normalModifierPrice ?: "0"
                                                } else {
                                                    "0"
                                                }
                                            }
                                        }
                                        val modifiers : ArrayList<OrdersEntity.ModifierInfo> =
                                            if (item.modifierInfo.isNullOrEmpty()) {
                                                ArrayList()
                                            } else {
                                                item.modifierInfo as ArrayList<OrdersEntity.ModifierInfo>
                                            }
                                        var modToReplace : OrdersEntity.ModifierInfo? = null
                                        if (!modifiers.isNullOrEmpty()) {
                                            for (mod1 in modifiers) {
                                                if (mod1.modifierId == (cItem.modifierIdStr ?: "0").toInt()) {
                                                    Log.d("modifier:", "${mod1.modifierId} already available")
                                                    modToReplace = mod1
                                                    break
                                                }
                                            }
                                        }
                                        if (modToReplace != null) {
                                            val subList : ArrayList<OrdersEntity.SubModifierFirstHalf> =
                                                if (modToReplace.subModifier?.subModifierFirstHalf.isNullOrEmpty()) {
                                                    ArrayList()
                                                } else {
                                                    modToReplace.subModifier?.subModifierFirstHalf as ArrayList<OrdersEntity.SubModifierFirstHalf>
                                                }
                                            subList.add(newSubMod)
                                            modToReplace.subModifier?.subModifierFirstHalf = subList
                                            Log.d("OffItems", "SubModifier inserted as FHalf: ${cItem.subModIds} for ${cItem.modifierIdStr}")
                                        }
                                        if (modifiers.isNullOrEmpty() || modToReplace == null) {
                                            val subList = ArrayList<OrdersEntity.SubModifierFirstHalf>()
                                            subList.add(newSubMod)
                                            val subMods = OrdersEntity.SubModifier().apply {
                                                this.subModifierFirstHalf = subList
                                            }
                                            newMod.subModifier = subMods
                                            modifiers.add(newMod)
                                            item.modifierInfo = modifiers
                                            Log.d("OffItems", "SubModifier inserted as FHalf: ${cItem.subModIds} for ${cItem.modifierIdStr}")
                                        }
                                    }
                                    "2" -> {
                                        val newSubMod = OrdersEntity.SubModifierSecondHalf().apply {
                                            val s = this
                                            s.subModifierId = (cItem.subModIds ?: "0").toInt()
                                            s.is2xMode = cItem.is2xMode
                                            mainScope.launch {
                                                val subData = MainActivity.mainRepository
                                                    .getOneSubModifierDataAsync(subModifierId ?: 0).await()
                                                s.subModifierPrice = if (cItem.variantId.isNullOrBlank()) {
                                                    subData?.normalModifierPrice ?: "0"
                                                } else {
                                                    "0"
                                                }
                                            }
                                        }
                                        val modifiers : ArrayList<OrdersEntity.ModifierInfo> =
                                            if (item.modifierInfo.isNullOrEmpty()) {
                                                ArrayList()
                                            } else {
                                                item.modifierInfo as ArrayList<OrdersEntity.ModifierInfo>
                                            }
                                        var modToReplace : OrdersEntity.ModifierInfo? = null
                                        if (!modifiers.isNullOrEmpty()) {
                                            for (mod1 in modifiers) {
                                                if (mod1.modifierId == (cItem.modifierIdStr ?: "0").toInt()) {
                                                    Log.d("modifier:", "${mod1.modifierId} already available")
                                                    modToReplace = mod1
                                                    break
                                                }
                                            }
                                        }
                                        if (modToReplace != null) {
                                            val subList : ArrayList<OrdersEntity.SubModifierSecondHalf> =
                                                if (modToReplace.subModifier?.subModifierSecondHalf.isNullOrEmpty()) {
                                                    ArrayList()
                                                } else {
                                                    modToReplace.subModifier?.subModifierSecondHalf as ArrayList<OrdersEntity.SubModifierSecondHalf>
                                                }
                                            subList.add(newSubMod)
                                            modToReplace.subModifier?.subModifierSecondHalf = subList
                                            Log.d("OffItems", "SubModifier inserted as SHalf: ${cItem.subModIds} for ${cItem.modifierIdStr}")
                                        }
                                        if (modifiers.isNullOrEmpty() || modToReplace == null) {
                                            val subList = ArrayList<OrdersEntity.SubModifierSecondHalf>()
                                            subList.add(newSubMod)
                                            val subMods = OrdersEntity.SubModifier().apply {
                                                this.subModifierSecondHalf = subList
                                            }
                                            newMod.subModifier = subMods
                                            modifiers.add(newMod)
                                            item.modifierInfo = modifiers
                                            Log.d("OffItems", "SubModifier inserted as SHalf: ${cItem.subModIds} for ${cItem.modifierIdStr}")
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                    if (productAlreadyAdded) {
                        Log.d("offItems", "skipping already product ${cItem.productId}")
                        continue
                    }
                    val itemInfo = OrdersEntity.ItemInfo().apply {
                        this.rowId = cItem.rowId
                        this.orderId = offOrder.tmpOrderId
                        this.uniqueRecordId = null
                        this.foodStatus = cItem.foodStatus
                        this.menuId = null
                        this.menuQty = cItem.menuQty.toString()
                        this.varientId = if (cItem.variantId.isNullOrBlank()) 0 else cItem.variantId?.toInt()
                        this.productId = cItem.productId
                        this.taxId = cItem.taxIds
                        this.taxPercentage = cItem.taxPercentages
                        this.isHalfAndHalf = null
                        this.is2xMod = cItem.is2xMode
                        this.discountType = cItem.discountType.toString()
                        this.itemDiscount = cItem.itemDiscount.toString()
                        this.orderNote = cItem.orderNotes
                    }
                    items.add(itemInfo)
                }
            }
            val billInfo = OrdersEntity.BillInfo().apply {
                this.billStatus = offOrder.billStatus
                this.paymentMethodId = offOrder.paymentMethodId.toString()
            }
            ordersEntity.itemsInfo = items
            ordersEntity.addOns = addOns
            ordersEntity.billInfo = billInfo
            orders.add(ordersEntity)
        }
        return orders
       /*     val itemInfo = ArrayList<OrdersEntity.ItemInfo>()
            if (!orderRequest.cartItems.isNullOrEmpty()) {
                for (item in orderRequest.cartItems!!) {
                    val itemInfo1 = OrdersEntity.ItemInfo().apply {
                        this.rowId = "NA"
                        this.orderId = newId
                        this.foodStatus = item.foodStatus
                        this.menuQty = item.menuQty.toString()
                        this.varientId = if (item.variantId.isNullOrBlank()) 0 else item.variantId?.toInt()
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
            ordersEntity.itemsInfo = itemInfo  */
    }
}