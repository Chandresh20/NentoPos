package com.tjcg.nentopos

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tjcg.nentopos.data.*
import com.tjcg.nentopos.databinding.*
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import com.tjcg.nentopos.MainActivity.Companion.mainRepository
import com.tjcg.nentopos.dialog.PaymentDialog
import com.tjcg.nentopos.fragments.InvoiceFragment
import com.tjcg.nentopos.fragments.POSFragment

const val SUB_MODE_FULL = 0
// const val SUB_MODE_F_HALF = 1
//  const val SUB_MODE_S_HALF = 2

@SuppressLint("SetTextI18n")
class Cart(val ctx: Context, private val binding: IncludeCartLayoutBinding, subBinding: IncludeCartModifierBinding) {

    private var itemAdapter: CartProductAdapter
    private var taxAdapter : TaxAdapter
    private val items = ArrayList<ItemInCart>()
    private val taxes = ArrayList<TaxSummary>()
    private var cartViewModel: CartViewModel
    private var productData : ProductData? = null
    private val taxMap = HashMap<String, Float>()
    private val subCart = SubCart(ctx, subBinding)
    private var cookingTimeInMin = 5
    private var isTaxesShowing = false
    private var grandTotal = 0f
    private var customerPaid = 0f
    private var tipAmount = 0f
    private var paymentMethod : Int? = null
    private var newOrderStatus = Constants.ORDER_STATUS_SERVED
    private var billStatus = 0
    private var categoryDiscounts : List<DiscountData> = emptyList()
    private var productDiscounts : List<DiscountData> = emptyList()
    private var categoryDiscount = 0f
    private var productDiscount = 0f
    private var inEditingMode = false
    private var orderToUpdate : OrdersEntity? = null
    private var removedWhileEdit = ArrayList<String?>()
 //   private var selectedTable = 0
/*    private var categoryDiscountPercentage = false
    private var categoryDiscount = 0f
    private var productDiscountPercentage = false
    private var productDiscount = 0f  */

    init {
        binding.itemRecyclerView.layoutManager = LinearLayoutManager(ctx)
        binding.taxRecyclerView.layoutManager = LinearLayoutManager(ctx)
        cartViewModel = ViewModelProvider(ctx as AppCompatActivity).get(CartViewModel::class.java)
        itemAdapter = CartProductAdapter(items)
        taxAdapter = TaxAdapter(taxes)
        binding.itemRecyclerView.adapter = itemAdapter
        binding.taxRecyclerView.adapter = taxAdapter
        binding.cartClearBtn.setOnClickListener {
            clearTheCart()
        }
        binding.changeOutletBtn.setOnClickListener {
            MainActivity.changeOutletBtn.performClick()
        }
        binding.logoutBtn.setOnClickListener {
            if (Constants.isFromSubUser) {
                mainRepository.logOutSubUser(ctx, MainActivity.deviceID)
            }
            MainActivity.logOutNow(ctx)
        }
        binding.addDiscountBtn.setOnClickListener {
            val discountDialog = DiscountDialog.getInstance(ctx, cartViewModel)
            discountDialog.show(ctx.supportFragmentManager, "discount")
        }
        binding.taxView.setOnClickListener {
            if (isTaxesShowing) {
                binding.taxRecyclerView.visibility = View.GONE
                isTaxesShowing = false
            } else{
                binding.taxRecyclerView.visibility = View.VISIBLE
                isTaxesShowing = true
            }
        }
        binding.addTipBtn.setOnClickListener {
            var tipDialog : AlertDialog? = null
            val builder = AlertDialog.Builder(ctx).apply {
                val tipBinding = DialogTipBinding.inflate(LayoutInflater.from(ctx))
                var tipAmount1 = 0f
                var inPercentage = false
                tipBinding.tipEd.setText(tipAmount.toString())
                tipBinding.radioTipType.setOnCheckedChangeListener { _, i ->
                    when (i) {
                        R.id.radioAmount -> {
                            inPercentage = false
                            if (!tipBinding.tipEd.text.isNullOrBlank()) {
                                tipAmount1 = tipBinding.tipEd.text.toString().toFloat()
                                tipBinding.totalTip.text = "Tip ( ${Constants.currencySign}${tipBinding.tipEd.text} )"
                            }
                            tipBinding.tipType.text = "$"
                        }
                        R.id.radioPercentage -> {
                            inPercentage = true
                            if (!tipBinding.tipEd.text.isNullOrBlank()) {
                                tipAmount1 = (grandTotal * tipBinding.tipEd.text.toString().toFloat() / 100)
                                tipBinding.totalTip.text = "Tip ( ${Constants.currencySign}${tipAmount1.format()} )"
                            }
                            tipBinding.tipType.text = "%"
                        }

                    }
                }
                tipBinding.tipEd.doOnTextChanged { text, _, _, _ ->
                    if (text.isNullOrBlank()) {
                        tipAmount1 = 0f
                        tipBinding.totalTip.text = "Tip"
                        return@doOnTextChanged
                    }
                    if (inPercentage) {
                        tipAmount1 = grandTotal * text.toString().toFloat() / 100
                        tipBinding.totalTip.text = "Tip ( ${Constants.currencySign}${tipAmount1.format()} )"
                        return@doOnTextChanged
                    }
                    tipAmount1 = text.toString().toFloat()
                    tipBinding.totalTip.text = "Tip ( ${Constants.currencySign}${tipAmount1.format()} )"
                }
                tipBinding.closeButton.setOnClickListener {
                    tipDialog?.dismiss()
                }
                tipBinding.closeButton2.setOnClickListener {
                    tipDialog?.dismiss()
                }
                tipBinding.confirm.setOnClickListener {
                    tipAmount = tipAmount1
                    updateTotalAmounts(false)
                    binding.tipText.text = "${Constants.currencySign} $tipAmount"
                    tipDialog?.dismiss()
                }
                setView(tipBinding.root)
            }
            tipDialog = builder.create()
            tipDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            tipDialog.show()
        }
        binding.cookingTimeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                cookingTimeInMin = if (p1 <= 5) {
                    5
                } else {
                    p1
                }
                binding.cookingTimeText.text = "$cookingTimeInMin"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) { }

            override fun onStopTrackingTouch(p0: SeekBar?) { }

        })
        binding.checkoutBtn.setOnClickListener {
            if (items.isNullOrEmpty()) {
                MainActivity.progressDialogRepository.showErrorDialog("Cart is Empty..")
                return@setOnClickListener
            }
            if (selectedTableCompanion == 0) {
                MainActivity.progressDialogRepository.showErrorDialog("Please select Table...")
                return@setOnClickListener
            }
            customerPaid = 0f
            paymentMethod = 0
            newOrderStatus = Constants.ORDER_STATUS_SERVED
            val paymentDialog = PaymentDialog(ctx, grandTotal, PaymentDialog.FROM_CHECKOUT)
            paymentDialog.show(MainActivity.fManager, "payment")
            val paymentReceiver = object: BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    customerPaid = p1?.getFloatExtra(PaymentDialog.paymentAmount, 0f) ?: 0f
                    paymentMethod = p1?.getIntExtra(PaymentDialog.paymentMethod, 0)
                    newOrderStatus = p1?.getIntExtra(PaymentDialog.orderStatus, Constants.ORDER_STATUS_SERVED) ?:
                        Constants.ORDER_STATUS_SERVED
                    billStatus = 1
                    checkoutCart(items)
                    ctx.unregisterReceiver(this)
                }
            }
            ctx.registerReceiver(paymentReceiver, IntentFilter(Constants.PAYMENT_DONE_BROADCAST))
        }

        binding.sendToKitchenBtn.setOnClickListener {
            if (items.isNullOrEmpty()) {
                MainActivity.progressDialogRepository.showErrorDialog("Cart is Empty..")
                return@setOnClickListener
            }
            if (selectedTableCompanion == 0) {
                MainActivity.progressDialogRepository.showErrorDialog("Please select Table...")
                return@setOnClickListener
            }
            var kDialog : AlertDialog? = null
            val builder = AlertDialog.Builder(ctx).apply {
                val dBinding = DialogSendToKitchenBinding.inflate(LayoutInflater.from(ctx))
                dBinding.saveBtn.setOnClickListener {
                    Toast.makeText(ctx, "Yet to be implemented", Toast.LENGTH_SHORT).show()
                }
                dBinding.sendBtn.setOnClickListener {
                    customerPaid = 0f
                    paymentMethod = 0
                    newOrderStatus = Constants.ORDER_STATUS_PROCESSING
                    billStatus = 0
                    kDialog?.dismiss()
                    checkoutCart(items)
                }
                setView(dBinding.root)
            }
            kDialog = builder.create()
            kDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            kDialog.show()
        }
        updateTableList()
        cartViewModel.discountPer.observe( ctx, {   percent ->
            binding.addDiscountBtn.text = "$percent%"
            updateTotalAmounts(false)
        })
        CoroutineScope(Dispatchers.Main).launch {
            val oldItems = getCartDataAsync().await()
            Log.d("CART","data: ${oldItems.size}")
            insertModifiedProductInCart(oldItems)
        }
        val outletChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                binding.tableBtn.visibility = View.VISIBLE
                binding.tableSpinner.visibility = View.GONE
                binding.cartClearBtn.performClick()
            }
        }
        val editExitReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                exitEditingMode()
            }
        }
        ctx.registerReceiver(outletChangeReceiver, IntentFilter(Constants.OUTLET_CHANGE_BROADCAST))
        ctx.registerReceiver(outletChangeReceiver, IntentFilter(Constants.OUTLET_CHANGE_BROADCAST))
        ctx.registerReceiver(editExitReceiver, IntentFilter(Constants.EXIT_EDITING_MODE_BROADCAST))
        if (tableAdapter != null) {
            for (i in 0 until tableAdapter!!.count) {
                val tableData = tableAdapter!!.getItem(i) as TableData
                Log.d("LoadedTable", "${tableData.tableId} : $selectedTableCompanion")
                if (tableData.tableId == selectedTableCompanion) {
                    Handler(Looper.getMainLooper()).postDelayed( {
                        binding.tableSpinner.setSelection(i)
                        binding.tableBtn.performClick()
                    }, 1000)
                    break
                }
        }
        }

    }

    fun updateTableList() {
        CoroutineScope(Dispatchers.Main).launch {
            val tables = mainRepository
                .getTablesForOutletAsync(Constants.selectedOutletId).await()
            Log.d("Available Tables for ${Constants.selectedOutletId}", "${tables?.size}")
            if (!tables.isNullOrEmpty()) {
                tableAdapter = ArrayAdapter(ctx, R.layout.item_light_spinner, R.id.light_spinner_text, tables)
                binding.tableSpinner.adapter = tableAdapter
    //            binding.tableSpinner.visibility = View.VISIBLE
                binding.tableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        p2: Int,
                        p3: Long
                    ) {
                        selectedTableCompanion = tables[p2].tableId
                        Log.d("Selected Table", "${tables[p2].tableId}")
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) { }
                }
         //       binding.tableBtn.visibility = View.GONE
                binding.tableBtn.setOnClickListener {
                    binding.tableSpinner.visibility = View.VISIBLE
                    it.visibility = View.GONE
                }
            } else {
                Log.d( "Tables","waiting for table data...")
                delay(2000)
                updateTableList()
            }
        }
    }

    fun clearTheCart() {
        tipAmount = 0f
        binding.cookingTimeSeek.progress = 5
        binding.tipText.text = Constants.currencySign +"0"
        items.clear()
        itemAdapter.notifyDataSetChanged()
        cartViewModel.setDiscount(0f)
        updateTotalAmounts(false)
    }

    fun checkoutCart(items: List<ItemInCart>) {
        if (inEditingMode) {
            if (orderToUpdate == null) {
                Log.e("Checkout", "OrderToUpdate is nulll")
                return
            }
            val updatedEntity: OrdersEntity = orderToUpdate!!
            val updateRequest = OrderUpdateRequest().apply {
                this.driverUserId = orderToUpdate?.driver_user_id
                this.customerId = orderToUpdate?.customer_id
                this.orderID = orderToUpdate?.order_id
                this.tipType = orderToUpdate?.tip_type
                this.tipValue = this@Cart.tipAmount
                this.paymentReceivedOrNot = this@Cart.billStatus
                this.billDate = null
                this.cardType = null
                this.cookedTime = mainRepository.formatCookingTime(this@Cart.cookingTimeInMin)
                this.discount = cartViewModel.discountPer.value
                this.orderDeliveredOrNot = orderToUpdate?.pis_order_delivered
                this.receivedPaymentAmount =  this@Cart.customerPaid
                this.billStatus = this@Cart.billStatus
                this.outletId = orderToUpdate?.outlet_id
                this.deviceId = MainActivity.deviceID
                this.paymentMethodId = paymentMethod.toString()
                this.accountNo = if (orderToUpdate?.account_number.isNullOrBlank()
                    || orderToUpdate?.account_number.equals("NA")) {
                    null
                } else {
                    orderToUpdate?.account_number
                }
                this.selectedCard = null
                this.selectedCardType = null
                this.driverAssigned = orderToUpdate?.pis_driver_assigned
                this.totalAmount = this@Cart.grandTotal
                this.customerPaid = (orderToUpdate?.customerpaid ?: "0").toFloat()
                this.waiterId = orderToUpdate?.waiter_id
                this.orderDate = orderToUpdate?.order_date
                this.orderStatus = this@Cart.newOrderStatus
                this.table = selectedTableCompanion
                this.customerType = if (orderToUpdate?.customer_type.isNullOrBlank()
                    || orderToUpdate?.customer_type.equals("NA")) {
                    "0"
                } else {
                    orderToUpdate?.customer_type
                }
                this.tipAmount = this@Cart.tipAmount
                this.orderTime = orderToUpdate?.order_time
                this.billTime = null
            }
            updatedEntity.apply {
                this.cookedtime = mainRepository.formatCookingTime(this@Cart.cookingTimeInMin)
                this.table_no = selectedTableCompanion
                this.discount = cartViewModel.discountPer.value.toString()
                this.tip_value = this@Cart.tipAmount.toString()
                this.totalamount = this@Cart.grandTotal
                this.order_status = this@Cart.newOrderStatus
                this.billInfo?.billStatus =  this@Cart.billStatus
            }
            updateRequest.deletedRows = if (removedWhileEdit.isNotEmpty()) {
                removedWhileEdit.joinToString(separator = ",")
            } else {
                ""
            }
            val addOnsId = ArrayList<Int>()
            val addOnsPrice = ArrayList<Float>()
            val addOnsQty = ArrayList<Int>()
            val addOnsName = ArrayList<String>()
            val itemsInRequest = ArrayList<OrderUpdateRequest.CartItem>()
            CoroutineScope(Dispatchers.Main).launch {
                for (item in items) {
                    if (item.fromEdit) continue
                    if (item.isAddOn) {
                        val addOnData = mainRepository.getOneAddOnDataAsync(item.id ?: 0).await()
                        if (addOnData != null) {
                            addOnsId.add(addOnData.addOnId)
                            val addPrice : Float = if (addOnData.addonPrice.isNullOrBlank()) {
                                0f
                            } else {
                                (addOnData.addonPrice ?: "0").toFloat()
                            }
                            addOnsPrice.add(addPrice)
                            addOnsQty.add(item.qty ?: 0)
                            addOnsName.add(item.name ?: "NA")
                        }
                        continue
                    }
                    val productData = mainRepository.getProductDataAsync(item.id ?: 0).await()
                    val variantData =
                        mainRepository.getOneVariantDataAsync(item.variantId ?: 0).await()
                    if (productData != null) {
                        val itemInRequest = OrderUpdateRequest.CartItem().apply {
                            this.productId = item.id
                            this.variantName = (variantData?.variantName ?: "")
                            this.qty = item.qty
                            this.price = item.price
                            this.catId = productData.categoryId
                            this.sizeId = (variantData?.variantId ?: 0)
                            this.addOnIds = null
                            this.addOnPrice = null
                            this.addOnUnitPrice = null
                            this.addOnQty = null
                            this.addOnName = null
                            val modifiers = ArrayList<Int>()
                            val modifierNames = ArrayList<String>()
                            val modifiersQty = ArrayList<Int>()
                            val subModIds = ArrayList<Int>()
                            val subModNames = ArrayList<String>()
                            val subModTypeNames = ArrayList<String>()
                            val subModTypeInt = ArrayList<Int>()
                            val mainModTotalPriceStr = ArrayList<Float>()
                            val modifiersHalfAndHalf = ArrayList<String>()
                            val subMod2xIds = ArrayList<Int>()
                            for (modf in item.modifiers) {
                                modifiers.add(modf.modId ?: 0)
                                modifiersQty.add(1)
                       //         modifierPrices.add(modf.modPrice ?: 0f)
                                val oneModData =
                                    mainRepository.getOneModifierDataAsync(modf.modId ?: 0).await()
                                if (oneModData != null) {
                                    modifierNames.add(oneModData.modifierName ?: "NA")
                                    if (oneModData.halfAndHalf == "1") {
                                        modifiersHalfAndHalf.add(oneModData.modifierId.toString())
                                    }
                                    var modPrice = 0f
                                    for (subModf in modf.subMods) {
                                        val subData = mainRepository.getOneSubModifierDataAsync(subModf.id).await()
                                        if (subModf.is2xMod == 1) {
                                            subMod2xIds.add(subModf.id)
                                        }
                                        if (subData != null) {
                                            subModIds.add(subModf.id)
                                            subModNames.add(subData.subModifierName ?: "NA")
                                            when (subModf.modeType) {
                                                0 -> {
                                                    subModTypeNames.add("Whole")
                                                    subModTypeInt.add(0)
                                                }
                                                1 -> {
                                                    subModTypeNames.add("First Half")
                                                    subModTypeInt.add(1)
                                                }
                                                2 -> {
                                                    subModTypeNames.add("Second Half")
                                                    subModTypeInt.add(2)
                                                }
                                            }
                                            modPrice += if (subData.normalModifierPrice.isNullOrBlank()) {
                                            0f
                                        } else {
                                            (subData.normalModifierPrice ?: "0").toFloat()
                                        }
                                        }
                                    }
                                    mainModTotalPriceStr.add(modPrice)
                                }
                            }
                            this.mainModifierId = modifiers.joinToString(separator = ",")
                   //         this.modifierPrice = modifierPrices.joinToString(separator = ",")
                            this.mainModifierName = modifierNames.joinToString(separator = ",")
                            this.mainModifierQty = modifiersQty.joinToString(separator = ",")
                            this.subModIds = subModIds.joinToString(separator = ",")
                            this.subModNames = subModNames.joinToString(separator = ",")
                            this.subModeTypeName = subModTypeNames.joinToString(separator = ",")
                            this.subModeTypeInt = subModTypeInt.joinToString(separator = ",")
                            this.mainModTotalPrices = mainModTotalPriceStr.joinToString(separator = ",")
                            this.sHalfAndHalf = modifiersHalfAndHalf.joinToString(separator = ",")
                            this.subMod2x = subMod2xIds.joinToString(separator = ",")
                            this.price = item.price
                        }
                        itemsInRequest.add(itemInRequest)
                    } else {
                        Log.d("ProductNotFound", "${item.id}")
                        Toast.makeText(ctx, "Error getting data", Toast.LENGTH_SHORT).show()
                    }


                    // for updateEntity
                    val newItemInfo = OrdersEntity.ItemInfo().apply {
                        this.rowId = "${item.id}"
                        this.orderId = updatedEntity.order_id
                        this.uniqueRecordId = "${System.currentTimeMillis()}uniqueRecord"
                        this.foodStatus = 0
                        this.menuId = updatedEntity.menu_id
                        this.menuQty = item.qty.toString()
                        this.varientId = item.variantId
                        this.productId = item.id
                        val productInfo = mainRepository.getProductDataAsync(this.productId ?: 0).await()
                        if (productInfo != null) {
                            this.taxId = productInfo.productTaxIds
                        }
               //         this.taxPercentage
                        this.isHalfAndHalf = "0"
                //        this.discountType
               //         this.itemDiscount
                    }
                    val x2modeList = ArrayList<Int>()
                    val modifierList = ArrayList<OrdersEntity.ModifierInfo>()
                    for (modif in item.modifiers) {
                        Log.d("searching", "Modifier with id ${modif.modId}")
                        val isa = modifierList.find { it.modifierId == modif.modId }
                        if (isa != null) {
                            Log.d("searching", "Modifier ${modif.modId} available")
                            continue
                        } else {
                            Log.d("searching", "Modifier ${modif.modId} not available")
                        }
                        var x2Mode = 0
                        val entityModifier = OrdersEntity.ModifierInfo()
                        val entitySubModifier = OrdersEntity.SubModifier()
                        val subModWhole = ArrayList<OrdersEntity.SubModifierWhole>()
                        val subModFHalf = ArrayList<OrdersEntity.SubModifierFirstHalf>()
                        val subModSHalf = ArrayList<OrdersEntity.SubModifierSecondHalf>()
                        for (subMods in modif.subMods) {
                            if (subMods.is2xMod == 1) {
                                x2Mode = 1
                            }
                            val subModInfo = mainRepository.getOneSubModifierDataAsync(subMods.id).await()
                            if (subModInfo != null) {
                                when (subMods.modeType) {
                                0 -> {
                                    val wholeSubModifier = OrdersEntity.SubModifierWhole().apply {
                                        this.subModifierId = subMods.id
                                        this.is2xMode = subMods.is2xMod.toString()
                                        this.subModifierPrice = subModInfo.normalModifierPrice
                                    }
                                    subModWhole.add(wholeSubModifier)
                                }
                                1 -> {
                                    val fHalfSubMod = OrdersEntity.SubModifierFirstHalf().apply {
                                        this.subModifierId = subMods.id
                                        this.is2xMode = subMods.is2xMod.toString()
                                        this.subModifierPrice = subModInfo.normalModifierPrice
                                    }
                                    subModFHalf.add(fHalfSubMod)
                                }
                                2 -> {
                                    val sHalfSubMod = OrdersEntity.SubModifierSecondHalf().apply {
                                        this.subModifierId = subMods.id
                                        this.is2xMode = subMods.is2xMod.toString()
                                        this.subModifierPrice = subModInfo.normalModifierPrice
                                    }
                                    subModSHalf.add(sHalfSubMod)
                                }
                                }
                                entitySubModifier.subModifierWhole = subModWhole
                                entitySubModifier.subModifierFirstHalf = subModFHalf
                                entitySubModifier.subModifierSecondHalf = subModSHalf
                            }
                        }
                        entityModifier.isHalfAndHalf = "1"
                        entityModifier.modifierId = modif.modId
                        entityModifier.subModifier = entitySubModifier
                        modifierList.add(entityModifier)
                        Log.d("ModifierEdit", "${entityModifier.modifierId}")
                        x2modeList.add(x2Mode)
                    }
                    newItemInfo.modifierInfo = modifierList
                    newItemInfo.is2xMod = x2modeList.joinToString(separator = ",")
                    val oldItems = updatedEntity.itemsInfo as ArrayList<OrdersEntity.ItemInfo>
                    oldItems.add(newItemInfo)
                    updatedEntity.itemsInfo = oldItems
                }
                if (!itemsInRequest.isNullOrEmpty()) {
                     itemsInRequest[0].apply {
                        this.addOnIds = addOnsId.joinToString()
                        this.addOnPrice = addOnsPrice.joinToString()
                        this.addOnUnitPrice = addOnsPrice.joinToString()
                        this.addOnQty = addOnsQty.joinToString()
                        this.addOnName = addOnsName.joinToString()
                    }
                    updateRequest.cartItems = itemsInRequest
                } else {
                    updateRequest.cartItems = ArrayList()
                }
                val eItems = updatedEntity.itemsInfo as ArrayList<OrdersEntity.ItemInfo>
                for (removed in removedWhileEdit) {
                    val toRemove = eItems.find { it.rowId == removed }
                    if (toRemove != null) {
                        eItems.remove(toRemove)
                    }
                }
                updatedEntity.itemsInfo = eItems
                MainActivity.orderRepository.updateOrder(ctx, updateRequest, updatedEntity)
            }
            return
        }
        val orderRequest = ProductOrderRequest()
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR_OF_DAY).toString()
        if (hour.length == 1) {
            hour = "0$hour"
        }
        var minute = cal.get(Calendar.MINUTE).toString()
        if (minute.length == 1) {
           minute = "0$minute"
        }
        var seconds = cal.get(Calendar.SECOND).toString()
        if (seconds.length == 1) {
            seconds = "0$seconds"
        }
        orderRequest.customerType = 1
        orderRequest.billTime = "$hour:$minute:$seconds"
        orderRequest.selectedCardType = ""
        orderRequest.driverUserId = ""
        orderRequest.orderDate = mainRepository.getTodayString()
        orderRequest.outletId = Constants.selectedOutletId
        orderRequest.orderAcceptDate = ""
        orderRequest.billDate = mainRepository.getTodayString()
        orderRequest.customerPaid = customerPaid
        orderRequest.table = selectedTableCompanion.toString()
        Log.d("Table", "${orderRequest.table}")
        orderRequest.accountNo = ""
        orderRequest.driverAssigned = 0
        orderRequest.cardType = 1
        orderRequest.discount = cartViewModel.discountPer.value
        orderRequest.billStatus = billStatus
        orderRequest.cookedTime = mainRepository.formatCookingTime(cookingTimeInMin)
        orderRequest.orderTime = "$hour:$minute:$seconds"
        orderRequest.tipAmount = tipAmount
        orderRequest.tipType = "0"
        orderRequest.createdBy = Constants.clientId
        val itemList = ArrayList<ProductOrderRequest.CartItem>()
        for (item in items) {
            val cartItem = ProductOrderRequest.CartItem()
            cartItem.productId = item.id
            if (item.isAddOn) {
                cartItem.addOnIds = item.id.toString()
                cartItem.addOnQty = item.qty
            } else {
                cartItem.addOnIds = ""
                cartItem.addOnQty = 0
            }
            cartItem.rowId = "${orderRequest.cookedTime}${orderRequest.totalAmount}${item.id}"
            cartItem.comboMainMenuId = ""
            cartItem.discountType = 1
            cartItem.foodStatus = 0
            cartItem.is2xMode = ""
            cartItem.itemDiscount = 0
            cartItem.itemMenuIdString =""
            cartItem.menuQty = item.qty
            val modifiers = ArrayList<String>()
            val subModifiers = ArrayList<String>()
            cartItem.modifierType = SUB_MODE_FULL.toString()
            val modifierTypeString = ArrayList<String>()
            for (modifier in item.modifiers) {
                val subMods = modifier.subMods
                for (subMod in subMods) {
                    subModifiers.add(subMod.id.toString())
                    modifierTypeString.add(subMod.modeType.toString())
                }
                modifiers.add(modifier.modId.toString())
            }
            val modStr = modifiers.joinToString(separator = ",")
            val subModStr = subModifiers.joinToString(separator = ",")
            val modTypeStr = modifierTypeString.joinToString(separator = ",")
            cartItem.modifierIdStr = modStr
            cartItem.modifierTypeString = modTypeStr
            cartItem.orderNotes = "Order from New Pos"
            cartItem.subModIds = subModStr
            cartItem.subModIdStr = subModStr
            var taxIds = ""
            var taxPercentages = ""
            if (!item.taxes.isNullOrEmpty()) {
                val taxes = item.taxes
                for (i in taxes.indices) {
                    if (i == item.taxes.size - 1) {
                        taxIds += "${taxes[i].id}"
                        taxPercentages += "${taxes[i].taxPercentage}"
                    } else {
                        taxIds += "${taxes[i].id},"
                        taxPercentages += "${taxes[i].taxPercentage},"
                    }
                }
                Log.d("Taxes", taxIds)
                cartItem.taxIds = taxIds
                cartItem.taxPercentages = taxPercentages
                cartItem.variantId = if (item.variantId == null) "" else (item.variantId.toString())
            }
            Log.d("items: ", "tax: ${cartItem.taxIds}; variant: ${cartItem.variantId}," +
                    "modifiers: ${cartItem.modifierIdStr}; SubModifiers: ${cartItem.subModIds}")
            itemList.add(cartItem)
            for (modifier in item.modifiers) {
                for (subModifier in modifier.subMods) {
                    val cartItem1 = ProductOrderRequest.CartItem()
                    cartItem1.productId = item.id
                    cartItem1.addOnIds = ""
                    cartItem1.addOnQty = 0
                    cartItem1.comboMainMenuId = ""
                    cartItem1.discountType = 1
                    cartItem1.foodStatus = 0
                    cartItem1.is2xMode = subModifier.is2xMod.toString()
                    cartItem1.itemDiscount = 0
                    cartItem1.itemMenuIdString =""
                    cartItem1.menuQty = 0
                    cartItem1.modifierQty = 1f
                    cartItem1.modifierIdStr = modifier.modId.toString()
                    cartItem1.modifierPriceStr = modifier.modPrice.toString()
                    cartItem1.subModIds = subModifier.id.toString()
                    cartItem1.modifierType = subModifier.modeType.toString()
                    cartItem1.modifierTypeString = null
                    itemList.add(cartItem1)
                }
            }
        }
        if (!itemList.isNullOrEmpty()) {
            orderRequest.cartItems = itemList
        }
        orderRequest.totalAmount = grandTotal
        orderRequest.paymentMethodId = paymentMethod
        orderRequest.tipValue = tipAmount
        orderRequest.waiterId = POSFragment.selectedWaiterId
        orderRequest.customerId = POSFragment.selectCustomerId
        orderRequest.receivedPaymentAmount = 0f
        orderRequest.selectedCard = "0"
        orderRequest.orderDeliveredOrNot = 0
        orderRequest.paymentReceivedOrNot = 0
        orderRequest.orderStatus = newOrderStatus
        MainActivity.orderRepository.placeOrderOnline(ctx, orderRequest)
    }

    fun insertInCart(categoryId: Int, product: ProductData) {
        productData = product
        categoryDiscount = 0f
        productDiscount = 0f
        categoryDiscounts = emptyList()
        productDiscounts = emptyList()
        if (POSFragment.currentDiscount.discountOnCategory.containsKey(categoryId)) {
   //         val discountHere = POSFragment.currentDiscount.discountOnCategory.get(categoryId)
            categoryDiscounts = POSFragment.currentDiscount.discountOnCategory[categoryId] as List<DiscountData>
   /*         categoryDiscountPercentage = discountHere?.discountType == 1
            categoryDiscount = discountHere?.discountPercentage?.toFloat() ?: 0f  */
        }
        if (POSFragment.currentDiscount.discountOnProduct.containsKey(product.productId)) {
            productDiscounts = POSFragment.currentDiscount.discountOnProduct[product.productId] as List<DiscountData>
    /*        productDiscountPercentage = discountHere?.discountType == 1
            productDiscount = discountHere?.discountPercentage?.toFloat() ?: 0f  */
        }
        if (items.isNullOrEmpty()) {
            if (!product.productVariants.isNullOrEmpty() || !product.productAddOns.isNullOrEmpty()
                || !product.productModifiers.isNullOrEmpty()) {
                subCart.insertInSubCart(product)
                return
            }
            CoroutineScope(Dispatchers.Main).launch {
                if (productData != null) {
                    items.add(
                        ItemInCart(productData?.productId, productData?.productName ?: "NA", null ,
                            ArrayList(), 1,
                            (productData?.productPrice ?: "0").toFloat(), categoryDiscounts, productDiscounts,
                            productData?.productTaxes,"TestingPOS", false))
                    itemAdapter.notifyItemInserted(items.size-1)
          //          MediaPlayer.create(ctx, R.raw.incart).start()
                } else {
                    Toast.makeText(ctx, "Product Data not found", Toast.LENGTH_SHORT).show()
                }
                updateTotalAmounts(true)
            }
            return
        }
        var itemFound = false
        for (item in items) {
            if (product.productId == item.id) {
                itemFound = true
                if (!product.productVariants.isNullOrEmpty() || !product.productAddOns.isNullOrEmpty()
                    || !product.productModifiers.isNullOrEmpty()) {
                        var dialog : AlertDialog? = null
                    val builder = AlertDialog.Builder(ctx).apply {
                        val dBinding = DialogRepearCartBinding.inflate(LayoutInflater.from(
                            ctx))
                        dBinding.closeBtn.setOnClickListener {
                            dialog?.dismiss()
                        }
                        dBinding.productNameText.text = item.name
                        dBinding.repeatBtn.setOnClickListener {
                            item.qty = (item.qty ?: 0) + 1
                            itemAdapter.notifyItemChanged(item.holderPosition)
                            updateTotalAmounts(true)
                            dialog?.dismiss()
                        }
                        dBinding.newBtn.setOnClickListener {
                            subCart.insertInSubCart(product)
                            dialog?.dismiss()
                        }
                        setView(dBinding.root)
                  /*      setMessage("Repeat the last set or create new?")
                        setPositiveButton("NEW") { _, _ ->
                            subCart.insertInSubCart(product)
                        }
                        setNegativeButton("REPEAT") { _, _ ->
                            item.qty = (item.qty ?: 0) + 1
                            itemAdapter.notifyItemChanged(item.holderPosition)
                            updateTotalAmounts(true)
                        } */
                    }
                    dialog = builder.create()
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.show()
                    break
                } else {
       //             MediaPlayer.create(ctx, R.raw.incart).start()
                    item.qty = (item.qty ?: 0) + 1
                    itemAdapter.notifyItemChanged(item.holderPosition)
                    updateTotalAmounts(true)
                    break
                }
            }
        }
        if (!itemFound) {
            if (!product.productVariants.isNullOrEmpty() || !product.productAddOns.isNullOrEmpty()
                || !product.productModifiers.isNullOrEmpty()) {
                subCart.insertInSubCart(product)
                return
            }
            CoroutineScope(Dispatchers.Main).launch {
                if (productData != null) {
                    items.add(
                        ItemInCart(productData?.productId, productData?.productName ?: "NA", null ,
                            ArrayList(), 1,
                            (productData?.productPrice ?: "0").toFloat(),categoryDiscounts, productDiscounts,
                            productData?.productTaxes, "TestingPOS", false))
                    itemAdapter.notifyItemInserted(items.size-1)
      //              MediaPlayer.create(ctx, R.raw.incart).start()
                } else {
                    Toast.makeText(ctx, "Product Data not found", Toast.LENGTH_SHORT).show()
                }
                updateTotalAmounts(true)
            }
        }

    /*    if (!product.productVariants.isNullOrEmpty() || !product.productAddOns.isNullOrEmpty()
            || !product.productModifiers.isNullOrEmpty()) {
            subCart.insertInSubCart(product)
            return
        }
        productData = product
        if (!items.isNullOrEmpty()) {
            var itemFound = false
            var atHolderPosition = 0
            for (item in items) {
                if (product.productId == item.id) {
                    var isOldConfiguration = false
                    if (!item.modifiers.isNullOrEmpty() || !item.subModifiers.isNullOrEmpty()) {
                        // show repeat dialog
                        val builder = AlertDialog.Builder(ctx).apply {
                            setMessage("Repeat the last set or create new?")
                            setPositiveButton("NEW") { _, _ ->
                                subCart.insertInSubCart(product)
                            }
                            setNegativeButton("REPEAT") { _, _ ->
                                isOldConfiguration = true
                            }
                        }
                        val dialog = builder.create()
                        dialog.show()
                    } else  {
                        isOldConfiguration = true
                    }
                    if (isOldConfiguration) {
                        item.qty = (item.qty ?: 0) + 1
                        itemFound = true
                        atHolderPosition = item.holderPosition
                    }
                    break
                }
            }
            if (itemFound) {
                itemAdapter.notifyItemChanged(atHolderPosition)
                updateTotalAmounts()
                return
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            if (productData != null) {
                items.add(
                    ItemInCart(productData?.productId, productData?.productName ?: "NA", null ,
                        ArrayList(), ArrayList(), qty,
                        (productData?.productPrice ?: "0").toFloat(), productData?.productTaxes))
                itemAdapter.notifyItemInserted(items.size-1)
            } else {
                Toast.makeText(ctx, "Product Data not found", Toast.LENGTH_SHORT).show()
            }
            updateTotalAmounts()
        }  */
    }

    fun insertModifiedProductInCart(fromSubCart : List<ItemInCart>) {
        for (newItem in fromSubCart) {
        /*    if (!items.isNullOrEmpty()) {
                var itemFound = false
                var atHolderPosition = 0
                for (item in items) {
                    if (item.id == newItem.id) {
                        if (newItem.variantId == item.variantId) {
                            item.qty = (item.qty ?: 0) + 1
                            itemFound = true
                            atHolderPosition = item.holderPosition
                            break
                        }
                    }
                }
                if (itemFound) {
                    itemAdapter.notifyItemChanged(atHolderPosition)
                    updateTotalAmounts()
                    return
                }
            }  */
            items.add(newItem)
            itemAdapter.notifyItemInserted(items.size -1)
        }
  //      MediaPlayer.create(ctx, R.raw.incart).start()
        updateTotalAmounts(true)
    }

    fun insertProductToUpdate(order: OrdersEntity) {
        removedWhileEdit.clear()
        binding.updateLayout.visibility = View.VISIBLE
        binding.updateOrderText.text = "Updating Order No. ${order.order_id}"
        binding.updateCancelBtn.setOnClickListener {
            exitEditingMode()
        }
        CoroutineScope(Dispatchers.Main).launch {
            val products = order.itemsInfo
            for (pro in (products ?: emptyList())) {
                val id = pro.productId ?: 0
                val prodInfo = mainRepository.getProductDataAsync(id).await()
                if (POSFragment.currentDiscount.discountOnCategory.containsKey(prodInfo?.categoryId)) {
                    categoryDiscounts = POSFragment.currentDiscount.discountOnCategory[prodInfo?.categoryId] as List<DiscountData>
                }
                if (POSFragment.currentDiscount.discountOnProduct.containsKey(id)) {
                    productDiscounts = POSFragment.currentDiscount.discountOnProduct[id] as List<DiscountData>
                }
                var prodName = prodInfo?.productName
                val variantId = pro.varientId
                var productPrice = (prodInfo?.productPrice ?: "0").toFloat()
                if (variantId != null && variantId > 0) {
                    if (!prodInfo?.productVariants.isNullOrEmpty()) {
                        val variantData = prodInfo?.productVariants?.find { it.variantId == variantId }
                        if (variantData != null) {
                            prodName += " ${variantData.variantName}"
                            productPrice = (variantData.variantPrice ?: "0").toFloat()
                        }
                    }
                }
                val modifiers = ArrayList<ModifierInCart>()
                for (modif in (pro.modifierInfo ?: emptyList())) {
                    val modifData = mainRepository.getOneModifierDataAsync((modif.modifierId ?: 0)).await()
                    if (modifData != null) {
                        prodName += "\n${modifData.modifierName}:"
                    }
                    val subModifPriceList = ArrayList<InvoiceFragment.SubMod>()
                    val subModifierList = ArrayList<SubModifierBrief>()
                    for (subModWhole in (modif.subModifier?.subModifierList ?: emptyList())) {
                        val subModInfo = mainRepository
                            .getOneSubModifierDataAsync(subModWhole.subModifierId ?: 0).await()
                        if (subModInfo != null) {
                            prodName += "(${subModInfo.subModifierName})"
                            subModifierList.add(SubModifierBrief(
                                subModWhole.subModifierId ?: 0, subModInfo.subModifierName ?: "NA",
                                if (subModWhole.is2xMode.isNullOrBlank()) 0 else (subModWhole.is2xMode ?: "0").toInt(),
                                0))
                            val subModPrice : Float = if (subModInfo.normalModifierPrice.isNullOrBlank()) {
                                    0f
                                } else {
                                    if ((pro.varientId ?: 0) > 0) {
                                        var subVariantPrice = 0f
                                        for (subVarPriceObj in (subModInfo.variantModifierPrice ?: emptyList())) {
                                            if (subVarPriceObj.variantId == pro.varientId) {
                                                subVariantPrice = if (subVarPriceObj.modifierPrice.isNullOrBlank()) 0f else
                                                    (subVarPriceObj.modifierPrice ?: "0").toFloat()
                                            }
                                        }
                                        subVariantPrice
                                    } else {
                                        (subModInfo.normalModifierPrice ?: "0").toFloat()
                                    }
                                }
                            subModifPriceList.add(
                                InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                subModPrice, 1f))
                            if (subModWhole.is2xMode == "1") {
                                subModifPriceList.add(
                                    InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                        subModPrice, 1f))
                            }
                        }
                    }
                    for (subModWhole in (modif.subModifier?.subModifierWhole ?: emptyList())) {
                        val subModInfo = mainRepository
                            .getOneSubModifierDataAsync(subModWhole.subModifierId ?: 0).await()
                        if (subModInfo != null) {
                            prodName += "(${subModInfo.subModifierName})"
                            subModifierList.add(SubModifierBrief(
                                subModWhole.subModifierId ?: 0, subModInfo.subModifierName ?: "NA",
                                if (subModWhole.is2xMode.isNullOrBlank()) 0 else (subModWhole.is2xMode ?: "0").toInt(),
                                0))
                            val subModPrice : Float = if (subModInfo.normalModifierPrice.isNullOrBlank()) {
                                0f
                            } else {
                                if ((pro.varientId ?: 0) > 0) {
                                    var subVariantPrice = 0f
                                    for (subVarPriceObj in (subModInfo.variantModifierPrice ?: emptyList())) {
                                        if (subVarPriceObj.variantId == pro.varientId) {
                                            subVariantPrice = if (subVarPriceObj.modifierPrice.isNullOrBlank()) 0f else
                                                (subVarPriceObj.modifierPrice ?: "0").toFloat()
                                        }
                                    }
                                    subVariantPrice
                                } else {
                                    (subModInfo.normalModifierPrice ?: "0").toFloat()
                                }
                            }
                            subModifPriceList.add(
                                InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                    subModPrice, 1f))
                            if (subModWhole.is2xMode == "1") {
                                subModifPriceList.add(
                                    InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                        subModPrice, 1f))
                            }
                        }
                    }
                    for (subModFHalf in (modif.subModifier?.subModifierFirstHalf ?: emptyList())) {
                        val subModInfo = mainRepository
                            .getOneSubModifierDataAsync(subModFHalf.subModifierId ?: 0).await()
                        if (subModInfo != null) {
                            prodName += "(${subModInfo.subModifierName} - Half)"
                            subModifierList.add(SubModifierBrief(
                                subModFHalf.subModifierId ?: 0, subModInfo.subModifierName ?: "NA",
                                if (subModFHalf.is2xMode.isNullOrBlank()) 0 else (subModFHalf.is2xMode ?: "0").toInt(),
                                0))
                            val subModPrice : Float = if (subModInfo.normalModifierPrice.isNullOrBlank()) {
                                0f
                            } else {
                                if ((pro.varientId ?: 0) > 0) {
                                    var subVariantPrice = 0f
                                    for (subVarPriceObj in (subModInfo.variantModifierPrice ?: emptyList())) {
                                        if (subVarPriceObj.variantId == pro.varientId) {
                                            subVariantPrice = (if (subVarPriceObj.modifierPrice.isNullOrBlank()) 0f else
                                                (subVarPriceObj.modifierPrice ?: "0").toFloat()) /2
                                        }
                                    }
                                    subVariantPrice
                                } else {
                                    (subModInfo.normalModifierPrice ?: "0").toFloat() /2
                                }
                            }
                            subModifPriceList.add(
                                InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                    subModPrice, 0.5f))
                            if (subModFHalf.is2xMode == "1") {
                                subModifPriceList.add(
                                    InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                        subModPrice, 0.5f))
                            }
                        }
                    }
                    for (subModSHalf in (modif.subModifier?.subModifierSecondHalf ?: emptyList())) {
                        val subModInfo = mainRepository
                            .getOneSubModifierDataAsync(subModSHalf.subModifierId ?: 0).await()
                        if (subModInfo != null) {
                            prodName += "(${subModInfo.subModifierName} - Half)"
                            subModifierList.add(SubModifierBrief(
                                subModSHalf.subModifierId ?: 0, subModInfo.subModifierName ?: "NA",
                                if (subModSHalf.is2xMode.isNullOrBlank()) 0 else (subModSHalf.is2xMode ?: "0").toInt(),
                                0))
                            val subModPrice : Float = if (subModInfo.normalModifierPrice.isNullOrBlank()) {
                                0f
                            } else {
                                if ((pro.varientId ?: 0) > 0) {
                                    var subVariantPrice = 0f
                                    for (subVarPriceObj in (subModInfo.variantModifierPrice ?: emptyList())) {
                                        if (subVarPriceObj.variantId == pro.varientId) {
                                            subVariantPrice = (if (subVarPriceObj.modifierPrice.isNullOrBlank()) 0f else
                                                (subVarPriceObj.modifierPrice ?: "0").toFloat()) /2
                                        }
                                    }
                                    subVariantPrice
                                } else {
                                    (subModInfo.normalModifierPrice ?: "0").toFloat() /2
                                }
                            }
                            subModifPriceList.add(
                                InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                    subModPrice, 0.5f))
                            if (subModSHalf.is2xMode == "1") {
                                subModifPriceList.add(
                                    InvoiceFragment.SubMod(subModInfo.subModifierName ?: "NA",
                                        subModPrice, 0.5f))
                            }
                        }
                    }
                    val modifierInCart = ModifierInCart(modif.modifierId, 0f, subModifierList)
                    modifiers.add(modifierInCart)
                    val includedModifiersCount : Int = modifData?.modifiersIncluded ?: 0
                    // add modifiers price in item price
                    val list = subModifPriceList.sortedWith(compareBy({it.price}, {it.weight}))
                    var payableModifierPrice = 0f
                    var freeModifier1 = includedModifiersCount.toFloat()
                    for (product in list) {
                        freeModifier1 -= product.weight
                        if (freeModifier1 >= 0) {
                            Log.d("Modifier0", "do nothing with ${product.name} : ${product.price}")
                        }
                        else {
                            Log.d("Modifier0", "added price of ${product.name} : ${product.price}")
                            payableModifierPrice += product.price
                        }
                    }
                    Log.d("EditOrder", "${modifData?.modifierName} - $payableModifierPrice")
                    productPrice += payableModifierPrice
                }
                // generate product tax list
                val taxIdList = pro.taxId?.split(",")
                val taxList = ArrayList<ProductTax>()
                for (tId in (taxIdList ?: emptyList())) {
                    if (tId.isNullOrBlank()) continue
                    val alr : ProductTax? = taxList.find { it.id == tId.toInt() }
                    if (alr == null) {
                        val taxData = mainRepository.getOneTaxDetailAsync(tId.toInt()).await()
                        if (taxData != null) {
                            taxList.add(taxData)
                        }
                    }

                }
                items.add(ItemInCart(id, prodName, variantId, modifiers,
                    (pro.menuQty ?: "0").toInt(), productPrice,
                    categoryDiscounts, productDiscounts, taxList, order.customer_note, false,
                    fromEdit = true, rowId = pro.rowId ,foodStatus = (pro.foodStatus ?: 0)
                ))
                itemAdapter.notifyItemInserted(items.size - 1)
            }
            val addOns = order.addOns
            for (addOn in (addOns ?: emptyList())) {
                val id = addOn.addOnId ?: 0
                val qty : Int = if (addOn.addOnQty.isNullOrBlank() || addOn.addOnQty.equals("NA")) {
                    0
                } else {
                    (addOn.addOnQty ?: "0").toInt()
                }
                val addOnInfo = mainRepository.getOneAddOnDataAsync(id).await()
                val productName = addOnInfo?.addOnName
                val productPrice: Float = if (addOnInfo?.addonPrice.isNullOrBlank() || addOnInfo?.addonPrice.equals("NA")) {
                    0f
                } else {
                    (addOnInfo?.addonPrice ?: "0").toFloat()
                }
                // generate addon Tax List
                val taxList = addOnInfo?.addOnTaxes
                items.add(ItemInCart(id, productName, null, ArrayList(), qty, productPrice, emptyList(), emptyList(),
                    taxList, order.customer_note, true, fromEdit = true
                ))
                itemAdapter.notifyItemInserted(items.size - 1)
            }
            // set up table
            selectedTableCompanion = order.table_no ?: 0
     //       val tableAdapter = binding.tableSpinner.adapter
            if (tableAdapter != null) {
                for (i in 0 until tableAdapter!!.count) {
                val tableData = tableAdapter!!.getItem(i) as TableData
                if (tableData.tableId == selectedTableCompanion) {
                    binding.tableSpinner.setSelection(i)
                    binding.tableBtn.performClick()
                    break
                }
            }
            }
            // set up discount
            cartViewModel.setDiscount((order.discount ?: "0").toFloat())
            // set up TipAmount
            tipAmount = if (order.tip_value.isNullOrBlank()) {
                0f
            } else {
                (order.tip_value ?: "0").toFloat()
            }
            binding.tipText.text = Constants.currencySign + tipAmount
            // set up cooking time
            Log.d("UpdateCookingTime", order.cookedtime.toString())
            val cookingHour = (order.cookedtime ?: "00:05:00").split(":")[0].toInt()
            val cookingMinute = (order.cookedtime ?: "00:05:00").split(":")[1].toInt()
            val totalMinutes = (cookingHour * 60) + cookingMinute
            binding.cookingTimeSeek.progress = totalMinutes
            binding.cookingTimeText.text = "$totalMinutes\nMin"
            inEditingMode = true
            orderToUpdate = order
            updateTotalAmounts(false)
        }
    }

    private fun getItemPriceWithDiscount(item: ItemInCart) : Float {
        val itemTotal = ((item.qty ?: 0) * (item.price ?: 0f))
        var catDiscount = 0f
        var prodDiscount = 0f
        if (!item.isAddOn) {
            if (!item.catDiscounts.isNullOrEmpty()) {
                for (disc in item.catDiscounts) {
                    val discount = if (disc.discountType == 1) {
                        itemTotal * (disc.discountPercentage ?: 0) / 100
                    } else {
                        itemTotal + ((item.qty ?: 0) * (disc.discountPercentage ?: 0))
                    }
                    catDiscount += discount
                }
            }
            if (!item.prodDiscount.isNullOrEmpty()) {
                for (disc in item.prodDiscount) {
                    val discount = if (disc.discountType == 1) {
                        itemTotal * (disc.discountPercentage ?: 0) / 100
                    } else {
                        itemTotal + ((item.qty ?: 0) * (disc.discountPercentage ?: 0))
                    }
                    prodDiscount += discount
                }
            }
        }

    /*    if (item.catDiscount > 0f) {
            catDiscount = if (item.catDiscountInPer) {
                itemTotal * item.catDiscount / 100
            } else {
                item.catDiscount * (item.qty ?: 1)
            }
        }
        if (item.prodDiscount > 0f) {
            prodDiscount = if (item.prodDiscountInPer) {
                itemTotal * item.prodDiscount / 100
            } else {
                item.prodDiscount * (item.qty ?: 1)
            }
        }  */
        return (itemTotal - catDiscount - prodDiscount)
    }

    private fun addTax(discountPercentage : Float) : Float {
        taxMap.clear()
        for (item in items) {
            if (!item.taxes.isNullOrEmpty()) {
                for (tax in item.taxes) {
                    if (tax.taxType == "1") {
                        val oldValue = taxMap["${tax.taxLabel}(+${tax.taxPercentage})"] ?: 0f
                        taxMap["${tax.taxLabel}(+${tax.taxPercentage})"] =
                            oldValue + ((tax.taxPercentage ?: "0").toFloat()) * (item.qty ?: 0)
                    } else {
                        val oldValue = taxMap["${tax.taxLabel}(${tax.taxPercentage}%)"] ?: 0f
                        val itemSubtotal = getItemPriceWithDiscount(item)
                        val itemPrice = itemSubtotal - (itemSubtotal * discountPercentage / 100)
                        taxMap["${tax.taxLabel}(${tax.taxPercentage}%)"] =
                            oldValue  + (((tax.taxPercentage ?: "0").toFloat()) * itemPrice  /100)
                        Log.d("tax:", "${tax.taxLabel}- ${tax.taxPercentage}% = $itemPrice ${item.qty}")
                    }
                }
            }
        }
        var totalTax = 0f
        val entries = taxMap.entries
        taxes.clear()
        for (entry in entries) {
            Log.d("Tax: ${entry.key}", entry.value.toString())
            taxes.add(TaxSummary(entry.key, entry.value))
            totalTax += entry.value
        }
        taxAdapter.notifyDataSetChanged()
        return totalTax
    }

    private fun updateTotalAmounts(playSound: Boolean) {
        var subtotal = 0f
        grandTotal = 0f
        for (item in items) {
            subtotal += getItemPriceWithDiscount(item)
        }
   /*     if (!categoryDiscountPercentage) {
            subtotal -= categoryDiscount
        } else {
            categoryDiscount = subtotal * categoryDiscount / 100
            subtotal -= categoryDiscount
        }  */
        val discountAmount = subtotal * (cartViewModel.discountPer.value ?: 0f)  / 100
        grandTotal += subtotal
        grandTotal -= discountAmount
        grandTotal += tipAmount
        binding.discountText.text = Constants.currencySign +" "+discountAmount.format()
        binding.subTotalText.text = Constants.currencySign +" " +subtotal.format()
        val tax = addTax(cartViewModel.discountPer.value ?: 0f)
        binding.taxTotalText.text = Constants.currencySign +" "+tax.format()
        grandTotal += tax
        if (grandTotal>0f && playSound) {
            MediaPlayer.create(ctx, R.raw.incart).start()
        }
        binding.totalText.text = Constants.currencySign +" "+grandTotal.format()
        // save cart details in storage
        CoroutineScope(Dispatchers.Main).launch {
       //     saveCartDataAsync().await()
        }
    }

 /*   private suspend fun saveCartDataAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                val cartFile = File(ctx.getExternalFilesDir(Constants.cartFile), Constants.cartFile)
                val gson = Gson()
                val tokenT = object : TypeToken<List<ItemInCart>>() { }
                val out = gson.toJson(items, tokenT.type)
                cartFile.outputStream().apply {
                    write(out.toByteArray())
                    flush()
                    close()
                }
            }
        }  */

    private suspend fun getCartDataAsync() : Deferred<ArrayList<ItemInCart>> =
        coroutineScope {
            async(Dispatchers.IO) {
                try {
                    val cartFile =
                        File(ctx.getExternalFilesDir(Constants.cartFile), Constants.cartFile)
                    val gson = Gson()
                    val tokenT = object : TypeToken<ArrayList<ItemInCart>>() {}
                    return@async gson.fromJson<ArrayList<ItemInCart>>(
                        cartFile.readText(),
                        tokenT.type
                    )
                } catch (e: Exception) {
                    return@async ArrayList()
                }
            }
        }

    private fun incrementQty(id: Int?) {
        var itemToReplace : ItemInCart? = null
        for (item in items) {
            if (item.id == id) {
                itemToReplace = item
                break
            }
        }
        if (itemToReplace != null) {
            if (inEditingMode) {
                itemToReplace.fromEdit = false
                removedWhileEdit.add(itemToReplace.rowId)
            }
     //       items.remove(itemToReplace)
            itemToReplace.qty = (itemToReplace.qty ?: 0) + 1
     //       items.add(itemToReplace)
            itemAdapter.notifyItemChanged(itemToReplace.holderPosition)
            updateTotalAmounts(true)
        }
    }

    private fun decrementQty(id: Int?) {
        var itemToReplace : ItemInCart? = null
        for (item in items) {
            if (item.id == id) {
                itemToReplace = item
                break
            }
        }
        if (itemToReplace != null) {
    //        items.remove(itemToReplace)
            itemToReplace.qty = (itemToReplace.qty ?: 0) - 1
            if ((itemToReplace.qty ?: 0) <= 0) {
                items.remove(itemToReplace)
                if (itemToReplace.fromEdit) {
                    removedWhileEdit.add(itemToReplace.rowId)
                }
                itemAdapter.notifyDataSetChanged()
            } else {
      //          items.add(itemToReplace)
                itemAdapter.notifyItemChanged(itemToReplace.holderPosition)
            }
            updateTotalAmounts(true)
        }
    }

    inner class CartProductAdapter(private val cartItems : ArrayList<ItemInCart>) : RecyclerView.Adapter<CartProductAdapter.MyHolder>() {

        inner class MyHolder(val cBinding : ItemCartProductBinding) :
                RecyclerView.ViewHolder(cBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(ItemCartProductBinding.inflate(
                LayoutInflater.from(ctx), parent, false))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val item = cartItems[position]
            item.holderPosition = holder.adapterPosition
            holder.cBinding.itemLabel.text = item.name
            holder.cBinding.itemQty.text = item.qty.toString()
            val itemPrice = (item.price ?: 0f) * (item.qty ?: 0)
            var catDiscount = 0f
            var prodDiscount = 0f
            if (!item.isAddOn) {
                if (!item.catDiscounts.isNullOrEmpty()) {
                for (disc in item.catDiscounts) {
                    val discount = if (disc.discountType == 1) {
                        itemPrice * (disc.discountPercentage ?: 0) / 100
                    } else {
                        itemPrice + ((item.qty ?: 0) * (disc.discountPercentage ?: 0))
                    }
                    catDiscount += discount
                }
            }
            if (!item.prodDiscount.isNullOrEmpty()) {
                for (disc in item.prodDiscount) {
                    val discount = if (disc.discountType == 1) {
                        itemPrice * (disc.discountPercentage ?: 0) / 100
                    } else {
                        itemPrice + ((item.qty ?: 0) * (disc.discountPercentage ?: 0))
                    }
                    prodDiscount += discount
                }
            }
            }
     /*       val catDiscount = if (item.catDiscountInPer) {
                itemPrice * item.catDiscount / 100
            } else {
                item.catDiscount
            }
            val prodDiscount = if (item.prodDiscountInPer) {
                itemPrice * item.prodDiscount / 100
            } else {
                item.prodDiscount
            } */
            if (catDiscount > 0 || prodDiscount > 0) {
                holder.cBinding.itemPriceStrike.visibility = View.VISIBLE
                holder.cBinding.itemPriceStrike.text = Constants.currencySign + " " + itemPrice.toString()
                holder.cBinding.itemPriceStrike.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.cBinding.itemPriceStrike.visibility = View.GONE
            }
            holder.cBinding.itemPrice.text = Constants.currencySign + " " +
                    (itemPrice - (catDiscount+prodDiscount)).toString()
            holder.cBinding.itemAdd.setOnClickListener {
                if (item.foodStatus == 1) {
                    MainActivity.progressDialogRepository.showErrorDialog(
                        "This item is served, Can not change this"
                    )
                } else {
                    incrementQty(item.id)
                }
            }
            holder.cBinding.itemRemove.setOnClickListener {
                if (item.foodStatus == 1) {
                    MainActivity.progressDialogRepository.showErrorDialog(
                        "This item is served, Can not change this")
                } else {
                    decrementQty(item.id)
                }
            }
        }

        override fun getItemCount(): Int = cartItems.size
    }

    inner class TaxAdapter(private val taxes: ArrayList<TaxSummary>) : RecyclerView.Adapter<TaxAdapter.MyHolder>() {

        inner class MyHolder(val binding: ItemCartTaxBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(ItemCartTaxBinding.inflate(LayoutInflater.from(
                ctx), parent, false))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            holder.binding.taxLabel.text = taxes[position].label
            holder.binding.taxAmount.text = Constants.currencySign +" " +
                    taxes[position].amount.format()
        }

        override fun getItemCount(): Int = taxes.size

    }

    private fun Float.format() = "%.2f".format(this)

    companion object {
        var selectedTableCompanion = 0
        var tableAdapter : SpinnerAdapter? = null
    }

    class ItemInCart(
        val id:Int?, val name:String?, val variantId: Int?, val modifiers: ArrayList<ModifierInCart>,
        var qty: Int?, val price: Float?,val catDiscounts : List<DiscountData>, val prodDiscount : List<DiscountData>,
        val taxes: List<ProductTax>?, val orderNote: String?, val isAddOn : Boolean, var fromEdit: Boolean= false, val foodStatus:Int = 0,
        val rowId: String? = null,
        var holderPosition:Int = 0)
    // missing field in cart modifierIds, orderNotes, SubModIds
    class ModifierInCart(val modId: Int?, val modPrice: Float?, val subMods : ArrayList<SubModifierBrief>)

    class SubModifierBrief(val id:Int, val name: String, val is2xMod: Int, val modeType: Int)

    class TaxSummary(val label: String, val amount: Float)

    class DiscountDialog : DialogFragment() {

        private lateinit var dBinding : DialogAddDiscountBinding
        private lateinit var ctx: Context
        private lateinit var cartViewModel: CartViewModel

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            dBinding = DialogAddDiscountBinding.inflate(
                inflater, container, false)
            dBinding.editText.setText(cartViewModel.discountPer.value.toString())
            dBinding.cancelBtn.setOnClickListener {
                this.dialog?.dismiss()
            }
            dBinding.closeButton2.setOnClickListener {
                this.dialog?.dismiss()
            }
            dBinding.acceptBtn.setOnClickListener {
                if (dBinding.editText.text.isNullOrBlank()) {
                    dBinding.editText.error = "value not set"
                    return@setOnClickListener
                }
                cartViewModel.setDiscount(dBinding.editText.text.toString().toFloat())
                this.dialog?.dismiss()
            }
            return dBinding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            super.onViewCreated(view, savedInstanceState)
        }

        companion object {

            fun getInstance(ctx: Context, viewModel: CartViewModel) : DiscountDialog {
                val dialog = DiscountDialog()
                dialog.ctx = ctx
                dialog.cartViewModel = viewModel
                return dialog
            }
        }
    }

    class CartViewModel : ViewModel() {

        private val _discountPer = MutableLiveData(0f)
        val discountPer : LiveData<Float> = _discountPer

        private val _selectedVariantId = MutableLiveData(0)
        val selectedVariantId : LiveData<Int> = _selectedVariantId

        fun setDiscount(per : Float) {
            _discountPer.value = per
        }
        fun setVariantId(vId: Int) {
            _selectedVariantId.value = vId
        }
    }

    inner class SubCart(val ctx: Context, private val binding: IncludeCartModifierBinding) {
        private val variantArray = ArrayList<VariantRadio>()
        private val addOnArray = ArrayList<ItemInCart>()
        private val selectedAddOns = ArrayList<Int>()
        private val modifiersArray = ArrayList<ProductModifier>()  // this array will show all available modifier in cart
        private val modifiersToShow = HashMap<Int, SelectedModifierDetails>() // this will only collect selected modifiers
        private var productName = ""
        private var variantName = ""
        private var selectedVariantPrice = 0f
  //      private var selectedVariantId :Int? = null
        private var selectedAddOnPrice = 0f
        private var productTotalPrice = 0f
        private var singlePrice = 0f
        private var productQty = 1
        private var id = 0
        private lateinit var productTax: List<ProductTax>

        private fun clearSubCart() {
            variantArray.clear()
            addOnArray.clear()
            selectedAddOns.clear()
            modifiersArray.clear()
            modifiersToShow.clear()
            productName = ""
            variantName = ""
            selectedVariantPrice = 0f
            selectedAddOnPrice = 0f
            productTotalPrice = 0f
            productQty = 1
            binding.qtyText.text = productQty.toString()
            binding.variantRadio.removeAllViews()
        }

        init {
     /*       cartOpenAnimation = ScaleAnimation(0f, 1f, 1f, 1f,
                binding.root.width.toFloat(), 0f)
            cartOpenAnimation.duration = Constants.subCartAnimDuration  */
            binding.qtyIncrBtn.setOnClickListener {
                productQty += 1
                binding.qtyText.text = productQty.toString()
                setTotalAmount()
            }
            binding.qtyDecrBtn.setOnClickListener {
                if (productQty > 1) {
                    productQty -= 1
                    binding.qtyText.text = productQty.toString()
                    setTotalAmount()
                }
            }
            binding.addToCartBtn.setOnClickListener {
                val newItems = ArrayList<ItemInCart>()
                var modName = ""
                val modifiersSentToCart = ArrayList<ModifierInCart>()
                var modCount = 0
                for (mod in modifiersToShow) {
               //     if (modName.contains(mod.value.name.toString())) continue
                    val modId = mod.key
                    val modInfo = mod.value
                    modName += if (modCount > 0) {
                        "\n${modInfo.name}:"
                    } else {
                        "${modInfo.name}:"
                    }
                    modCount += 1
                    val subMods = modInfo.subMods
                    for (subMod in subMods) {
                        modName += "(${subMod.name}"
                        if (subMod.modeType==1) {
                            modName += " half"
                        }
                        modName += if (subMod.is2xMod == 1) {
                            "-2x)"
                        } else {
                            ")"
                        }
                    }
                    modifiersSentToCart.add(ModifierInCart(modId, modInfo.totalPrice,
                        modInfo.subMods))
                }
          /*      val subModsSentToCart = ArrayList<Int>()
                for (mod in modifiersArray) {
                    val id = mod.modifierId
                    for (ent in modifiersToShow) {
                        if (id == ent.key) {
                            modInfo += "\n${mod.modifierName}:"
                            modifiersSentToCart.add(id)
                            for (sub in ent.value.subMods) {
                                modInfo += "${sub.name},"
                                subModsSentToCart.add(sub.id)
                            }
                        }
                    }
                }  */
                val thisItem = ItemInCart(
                    id, "$productName-$variantName\n$modName", cartViewModel.selectedVariantId.value,
                    modifiersSentToCart, productQty,
                    productTotalPrice, categoryDiscounts , productDiscounts,
                    productTax, "Testing POS", false)
                newItems.add(thisItem)
                val addONs = ArrayList<ItemInCart>()
                for (addOn in addOnArray) {
                    if (selectedAddOns.contains(addOn.id)) {
                        addONs.add(addOn)
                    }
                }
                newItems.addAll(addONs)
                insertModifiedProductInCart(newItems)
                val anim = ScaleAnimation(1f, 0f, 1f, 1f, binding.root.width.toFloat(), 0f)
                anim.duration = Constants.subCartAnimDuration
                binding.root.startAnimation(anim)
                Handler(Looper.getMainLooper()).postDelayed( {
                    binding.root.visibility = View.GONE
                    MainActivity.showActionBar()
                }, Constants.subCartAnimDuration)
            }
        }

        fun insertInSubCart(product: ProductData) {
            clearSubCart()
            binding.root.visibility = View.VISIBLE
            MainActivity.hideActionBar()
    //        binding.root.startAnimation(cartOpenAnimation)
            binding.productTitle.text = product.productName
            id = product.productId
            singlePrice = (product.productPrice ?: "0").toFloat()
            setTotalAmount()
            productName += "${product.productName}"
            productTax = product.productTaxes ?: ArrayList()
            if (!product.productVariants.isNullOrEmpty()) {
                binding.variantLayout.visibility = View.VISIBLE
                val sortedVariants = product.productVariants?.sortedBy { it.variantId }
                val layoutParams1 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                val px = 6.5 * ctx.resources.displayMetrics.density  // margin for variant name text
                layoutParams1.setMargins(0,px.toInt(),0,px.toInt())
                val typeFace = Typeface.createFromAsset(ctx.assets, "montserrat.ttf")
                binding.variantNames.removeAllViews()
                for (variant in sortedVariants!!) {
                    variantArray.add(
                        VariantRadio(
                            variant.variantId,
                            variant.variantName,
                            variant.variantPrice?.toFloat()
                        )
                    )
                    val textView = TextView(ctx).apply {
                        text = variant.variantName
                        layoutParams = layoutParams1
                        typeface = typeFace
                    }
                    val variantRadioBtn = RadioButton(ctx).apply {
                        id = variant.variantId ?: 0
                        text = "${variant.variantPrice} ${Constants.currencySign}"
                        typeface = typeFace
                    }
                    binding.variantNames.addView(textView)
                    binding.variantRadio.addView(variantRadioBtn)
                }
     //           selectedVariantPrice = variantArray[0].price ?: 0f
    //            selectedVariantId = variantArray[0].id
   //             cartViewModel.setVariantId(variantArray[0].id ?: 0)
    //            variantName = variantArray[0].label ?: "NA"
   //             setTotalAmount()
                binding.variantRadio.setOnCheckedChangeListener { _, i ->
                    for (vari in variantArray) {
                        if (vari.id == i) {
                            selectedVariantPrice = vari.price ?: 0f
                            cartViewModel.setVariantId(i)
                            variantName = vari.label ?: "NA"
                            Log.d("SelectVariant", "${vari.id} - $variantName")
                            setTotalAmount()
                            break
                        }
                    }
                }
                binding.variantRadio.clearCheck()
                binding.variantRadio.check(variantArray[0].id ?: 0)
            } else {
                binding.variantLayout.visibility = View.GONE
            }
            if (!product.productAddOns.isNullOrEmpty()) {
                binding.addOnLayout.visibility = View.VISIBLE
                addOnArray.clear()
                binding.addOnRecycler.layoutManager = LinearLayoutManager(ctx)
                binding.addOnRecycler.recycledViewPool.setMaxRecycledViews(0,0)
                binding.addOnRecycler.adapter = AddOnAdapter(product.productAddOns ?: emptyList())

          /*      binding.addOnGrid.removeAllViews()
                binding.addOnLayout.visibility = View.VISIBLE
                for (addOns in product.productAddOns!!) {
                    addOnArray.add(
                        ItemInCart(
                            addOns.addOnId, addOns.addOnName, null, ArrayList(),
                            1, addOns.addonPrice?.toFloat(), categoryDiscounts, productDiscounts,
                            addOns.addOnTaxes, "Testing POS", true
                        )
                    )
                    val checkbox = CheckBox(ctx).apply {
                        id = addOns.addOnId
                        text = addOns.addOnName
                    }
                    checkbox.setOnCheckedChangeListener { compoundButton, b ->
                        if (b) {
                            selectedAddOns.add(compoundButton.id)
                            selectedAddOnPrice += (addOns.addonPrice ?: "0").toFloat()
                        } else {
                            selectedAddOns.remove(compoundButton.id)
                            selectedAddOnPrice -= (addOns.addonPrice ?: "0").toFloat()
                        }
                        setTotalAmount()
                    }
                    binding.addOnGrid.addView(checkbox)
                }  */
            } else {
                binding.addOnLayout.visibility = View.GONE
            }
            if (!product.productModifiers.isNullOrEmpty()) {
                binding.modifierRecyclerView.visibility = View.VISIBLE
                modifiersArray.addAll(product.productModifiers!!)
                binding.modifierRecyclerView.layoutManager = LinearLayoutManager(ctx)
                binding.modifierRecyclerView.adapter = ModifierAdapter(modifiersArray)
            } else {
                binding.modifierRecyclerView.visibility = View.GONE
            }
        }

        private fun setTotalAmount() {
            productTotalPrice = singlePrice
            productTotalPrice += selectedVariantPrice
            for (entry in modifiersToShow) {
                productTotalPrice += (entry.value.totalPrice ?: 0f)
            }
            var catDiscount = 0f
            var prodDiscount = 0f
            if (!categoryDiscounts.isNullOrEmpty()) {
                for (disc in categoryDiscounts) {
                    val discount = if (disc.discountType ==1 ) {
                        productTotalPrice * (disc.discountPercentage ?: 0) / 100
                    } else {
                        productTotalPrice + (productQty * (disc.discountPercentage ?: 0))
                    }
                    catDiscount += discount
                }
            }
            if (!productDiscounts.isNullOrEmpty()) {
                for (disc in productDiscounts) {
                    val discount = if (disc.discountType == 1) {
                        productTotalPrice * (disc.discountPercentage ?: 0) / 100
                    } else {
                        productTotalPrice + (productQty * (disc.discountPercentage ?: 0))
                    }
                    prodDiscount += discount
                }
            }
            val totalPriceToShow = (productTotalPrice * productQty) + selectedAddOnPrice
            if ((catDiscount+prodDiscount) > 0) {
                val itemDiscount = (catDiscount + prodDiscount) * productQty
                val payAmount = totalPriceToShow - itemDiscount
                binding.realAmountCart.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                binding.realAmountCart.text = "(${Constants.currencySign} $totalPriceToShow) "
                binding.discountAmountCart.text = "(${Constants.currencySign} $payAmount)"
                binding.realAmountCart.visibility = View.VISIBLE
            } else {
                binding.discountAmountCart.text = "(${Constants.currencySign} $totalPriceToShow)"
                binding.realAmountCart.visibility = View.GONE
            }

        }

        fun addModifierDetails(id: Int, details: SelectedModifierDetails) {
            modifiersToShow[id] = details
            setTotalAmount()
            Log.d("ModifierUpdate", "$id, ${details.subMods}, ${details.totalPrice}")
        }


        inner class ModifierAdapter(val list: List<ProductModifier>) :
            RecyclerView.Adapter<ModifierAdapter.MyHolder>() {

            inner class MyHolder(val mBinding: ItemCartModifiersBinding) :
                RecyclerView.ViewHolder(mBinding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
                return MyHolder(
                    ItemCartModifiersBinding.inflate(
                        LayoutInflater.from(ctx),
                        parent,
                        false
                    )
                )
            }

            override fun onBindViewHolder(holder: MyHolder, position: Int) {
                val modifier: ProductModifier = list[position]
                holder.mBinding.modifierName.text = modifier.modifierName
                holder.mBinding.modifierName.isSelected = true
                holder.mBinding.modifierAvailableText.text =
                    "${modifier.modifiersIncluded} included / ${modifier.choosableModifiers} available"
                CoroutineScope(Dispatchers.Main).launch {
                    if (modifier.halfAndHalf == "1") {
                        if(modifier.available2x == 0) {
                            holder.mBinding.twoXTitle.visibility = View.INVISIBLE
                        } else {
                            holder.mBinding.twoXTitle.visibility = View.VISIBLE
                        }
                        holder.mBinding.recyclerSubModifier.layoutManager = LinearLayoutManager(ctx)
                        val adp1 = createSubModifierHalfAdapterAsync(modifier).await()
                        holder.mBinding.recyclerSubModifier.adapter = adp1
                        return@launch
                    }
                    holder.mBinding.halfAndHalfImages.visibility = View.GONE
                    holder.mBinding.recyclerSubModifier.layoutManager = LinearLayoutManager(ctx)
                    val adp2 = createSubModifierAdapterAsync(modifier).await()
                    holder.mBinding.recyclerSubModifier.adapter = adp2
                }
            }

            override fun getItemCount(): Int = list.size
        }

        suspend fun createSubModifierAdapterAsync(modifier: ProductModifier) : Deferred<SubModifierAdapter> =
            coroutineScope {
                async(Dispatchers.Main) {
                    return@async SubModifierAdapter(
                        modifier.modifierId ,modifier.modifierName, modifier.subModifiers ?: ArrayList(),
                        modifier.modifiersIncluded, modifier.choosableModifiers, modifier.available2x)
                }
            }

        suspend fun createSubModifierHalfAdapterAsync(modifier: ProductModifier) : Deferred<SubModHalfAndHalfAdapter2> =
            coroutineScope {
                async(Dispatchers.Main) {
                    return@async SubModHalfAndHalfAdapter2(modifier.modifierId, modifier.modifierName,
                        modifier.subModifiers ?: ArrayList(), modifier.modifiersIncluded,
                        modifier.choosableModifiers?.toFloat() ,modifier.available2x)
                }
            }

        inner class AddOnAdapter(private val addonList: List<ProductAddOns>) :
            RecyclerView.Adapter<AddOnAdapter.MyHolder>() {

            inner class MyHolder(val binding: ItemCartAddonBinding) : RecyclerView.ViewHolder(binding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
                return MyHolder(ItemCartAddonBinding.inflate(LayoutInflater.from(ctx), parent, false))
            }

            override fun onBindViewHolder(holder: MyHolder, position: Int) {
                val addon = addonList[position]
                var isSelected = false
                holder.binding.addOnName.text = addon.addOnName
                holder.binding.addOnQty.text = "1"
                holder.binding.addOnPrice.text = Constants.currencySign +" "+ addon.addonPrice
                addOnArray.add(ItemInCart(addon.addOnId, addon.addOnName, null, ArrayList(),
                    1, addon.addonPrice?.toFloat(), categoryDiscounts, productDiscounts, addon.addOnTaxes,
                    "" ,true))
                holder.binding.addOnCheck.setOnCheckedChangeListener { _, b ->
                    if (b) {
                        selectedAddOns.add(addon.addOnId)
                        selectedAddOnPrice += if (addon.addonPrice.isNullOrBlank()) {
                            0f
                        } else {
                            (addon.addonPrice ?: "0").toFloat()
                        }
                        isSelected = true
                    } else {
                        selectedAddOns.remove(addon.addOnId)
                        selectedAddOnPrice -= if (addon.addonPrice.isNullOrBlank()) {
                            0f
                        } else {
                                val currentAdd = addOnArray.find { it.id == addon.addOnId }
                            (addon.addonPrice ?: "0").toFloat()  * (currentAdd?.qty ?: 0)
                        }
                        holder.binding.addOnQty.text = "1"
                        holder.binding.addOnPrice.text = Constants.currencySign +" "+ addon.addonPrice
                        isSelected = false
                    }
                    setTotalAmount()
                }
                holder.binding.itemAdd.setOnClickListener {
                    if (isSelected) {
                        val addOnInArray = addOnArray.find { it.id == addon.addOnId }
                        addOnInArray?.qty = (addOnInArray?.qty ?: 0) + 1
                        holder.binding.addOnQty.text = addOnInArray?.qty.toString()
                        selectedAddOnPrice += if (addon.addonPrice.isNullOrBlank()) {
                            0f
                        } else {
                            (addon.addonPrice ?: "0").toFloat()
                        }
                        holder.binding.addOnPrice.text = Constants.currencySign + " "+
                            ((addon.addonPrice ?: "0").toFloat() * (addOnInArray?.qty ?: 0)).toString()
                        setTotalAmount()
                    }
                }
                holder.binding.itemRemove.setOnClickListener {
                    if (isSelected) {
                        val addOnInArray = addOnArray.find { it.id == addon.addOnId }
                        val addOnQty = addOnInArray?.qty ?: 0
                        if (addOnQty > 1) {
                            addOnInArray?.qty = (addOnInArray?.qty ?: 0) - 1
                            holder.binding.addOnQty.text = addOnInArray?.qty.toString()
                            selectedAddOnPrice -= if (addon.addonPrice.isNullOrBlank()) {
                                0f
                            } else {
                                (addon.addonPrice ?: "0").toFloat()
                            }
                            holder.binding.addOnPrice.text = Constants.currencySign + " "+
                                ((addon.addonPrice ?: "0").toFloat() * (addOnInArray?.qty ?: 0)).toString()
                            setTotalAmount()
                        }
                    }
                }
            }

            override fun getItemCount(): Int = addonList.size

        }


        inner class SubModHalfAndHalfAdapter2(
            private val mId : Int?,
            private val name: String?,
            private val listH: List<ProductSubModifier>,
            private val included: Int?,
            private val choosable: Float?,
            private val is2xMod: Int?
        ) : RecyclerView.Adapter<SubModHalfAndHalfAdapter2.MyHolder>() {

            private val subList = ArrayList<SelectedSubModifierHalf>()
            private var selected = 0f
            private var choosen = 0f
            private val subNames = ArrayList<SubModifierBrief>()
            inner class SubModifierCalc(val id: Int?, val price: Float?, val qty: Float?)

            private fun removeIncludedAmount(list: ArrayList<SubModifierCalc>) {
                subNames.clear()
                val sortedList = list.sortedWith(compareBy({it.price}, {it.qty}))
                var payableModifierPrice = 0f
                var freeModifier1 = (included ?: 0).toFloat()
                for (product in sortedList) {
                    if (product.qty == 0f) {
                        continue
                    }
                    freeModifier1 -= product.qty ?: 0f
                    if (freeModifier1 >= 0) {
                        Log.d("modifier", "Do nothing")
                    } else {
                        payableModifierPrice += product.price ?: 0f
                    }
                }
                for (subMod in subList) {
                    subNames.add(
                        SubModifierBrief(subMod.id ?: 0, subMod.name ?: "NA",
                            if (subMod.is2xMod) 1 else 0, if (subMod.qty == 0.5f) 1 else 0))
                }
                Log.d("payablePrice", "$payableModifierPrice")
                addModifierDetails(mId ?: 0, SelectedModifierDetails(name, subNames, payableModifierPrice))
            }
            private fun generateCalcList(list: ArrayList<SelectedSubModifierHalf>) {
                selected = 0f
                val calItems = ArrayList<SubModifierCalc>()
                for (item in list) {
                    calItems.add(SubModifierCalc(item.id, item.price, item.qty))
                    if (item.is2xMod) {
                        calItems.add(SubModifierCalc(item.id, item.price, item.qty))
                    }
                }
                removeIncludedAmount(calItems)
            }
            private fun addToSubList(subMod2: SelectedSubModifierHalf) {
                if (subList.isNullOrEmpty()) {
                    subList.add(subMod2)
                    generateCalcList(subList)
                    return
                }
                var toReplace : SelectedSubModifierHalf? = null
                for (temp in subList) {
                    if (temp.id == subMod2.id) {
                        toReplace = temp
                        break
                    }
                }
                if (toReplace != null) {
                    subList.remove(toReplace)
                    subList.add(subMod2)
                } else {
                    subList.add(subMod2)
                }
                generateCalcList(subList)
            }

            private fun removeFromList(id: Int) {
                var itemToRemove : SelectedSubModifierHalf? = null
                for (temp in subList) {
                    if (temp.id == id) {
                        itemToRemove = temp
                        break
                    }
                }
                if (itemToRemove != null) {
                    subList.remove(itemToRemove)
                }
                generateCalcList(subList)
            }

            private fun update2xMode(x2Mode: Boolean, id: Int) : Float {
                var updatedPrice = 0f
                var itemToUpdate : SelectedSubModifierHalf? = null
                for (temp in subList) {
                    if (temp.id == id) {
                        itemToUpdate = temp
                        break
                    }
                }
                if (itemToUpdate != null) {
                    subList.remove(itemToUpdate)
                    itemToUpdate.is2xMod = x2Mode
                    addToSubList(itemToUpdate)
                    updatedPrice = if (x2Mode) {
                        (itemToUpdate.price ?: 0f) * 2f
                    } else {
                        (itemToUpdate.price ?: 0f)
                    }
                }
                return updatedPrice
            }

            inner class MyHolder(val sBinding : ItemCartSubmodifierHalfAndHalfBinding) :
                    RecyclerView.ViewHolder(sBinding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
                return MyHolder(ItemCartSubmodifierHalfAndHalfBinding.inflate(LayoutInflater.from(ctx),
                    parent, false))
            }

            override fun onBindViewHolder(holder: MyHolder, position: Int) {
                val subInfo = listH[position]
                holder.sBinding.subModName.text = subInfo.subModifierName
                val check1 = holder.sBinding.checkFull
                val check2 = holder.sBinding.checkFHalf
                val check3 = holder.sBinding.checkSHalf
                val check2x = holder.sBinding.check2x
                var selectionActive = false
                var currentSelection = 0
                var is2xApplied = false
                if (is2xMod == 1) {
                    check2x.visibility = View.VISIBLE
                    // other 2x related operations will be here
                    check2x.setOnCheckedChangeListener { _, b ->
                        if (b) {
                            is2xApplied = true
                            when (currentSelection) {
                                1 -> {
                                    val newPrice = update2xMode(true, subInfo.subModifierId)
                                    holder.sBinding.finalPrice.text = Constants.currencySign + newPrice.format()
                                }
                                2 -> {
                                    val newPrice = update2xMode(true, subInfo.subModifierId)
                                    holder.sBinding.finalPrice.text = Constants.currencySign + newPrice.format()
                                }
                                3 -> {
                                    val newPrice =  update2xMode(true, subInfo.subModifierId)
                                    holder.sBinding.finalPrice.text = Constants.currencySign + newPrice.format()
                                }
                                else -> {
                                    check1.isChecked = true
                                }
                            }
                        } else {
                            is2xApplied = false
                            if (currentSelection == 0) {
                                return@setOnCheckedChangeListener
                            }
                            val newPrice = update2xMode(false, subInfo.subModifierId)
                            holder.sBinding.finalPrice.text = Constants.currencySign + newPrice.format()
                        }
                    }
                }
                check1.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if (choosen >= (choosable ?: 0f)) {
                           Toast.makeText(ctx, "Sorry, Only $choosable available", Toast.LENGTH_SHORT).show()
                            compoundButton.isChecked = false
                           return@setOnCheckedChangeListener
                        }
                        if (currentSelection == 2 || currentSelection == 3) {
                            selectionActive =  true
                            check2.isChecked = false
                            check3.isChecked = false
                            selectionActive = false
                        }
                        currentSelection = 1
                        choosen +=1f
                        var subModPrice : Float? = null
                        val selectedVariantId = cartViewModel.selectedVariantId.value
                        if (selectedVariantId != null) {
                            val variantPriceList = subInfo.variantModifierPrice
                            if (!variantPriceList.isNullOrEmpty() && !variantArray.isNullOrEmpty()) {
                                for (variant in variantPriceList) {
                                    if (variant.variantId == selectedVariantId) {
                                        subModPrice = if (variant.modifierPrice.isNullOrBlank()) {
                                            0f
                                        } else {
                                            variant.modifierPrice!!.toFloat()
                                        }
                                        break
                                    }
                                }
                                if (subModPrice == null) {
                                    subModPrice = subInfo.normalModifierPrice?.toFloat()
                                }
                            } else {
                                subModPrice = subInfo.normalModifierPrice?.toFloat()
                            }
                        } else {
                            subModPrice = subInfo.normalModifierPrice?.toFloat()
                        }
                        addToSubList(SelectedSubModifierHalf(
                            subInfo.subModifierId, subInfo.subModifierName, subModPrice, 1f,
                            is2xApplied
                        ))
                        holder.sBinding.finalPrice.visibility = View.VISIBLE
                        holder.sBinding.finalPrice.text =  Constants.currencySign + if (is2xApplied) {
                            ((subModPrice ?: 0f) * 2).format()
                        } else {
                            (subModPrice ?: 0f).format()
                        }

                    } else {
                        if (!selectionActive) {
                            currentSelection = 0
                            check2x.isChecked = false
                            holder.sBinding.finalPrice.visibility = View.INVISIBLE
                        }
                        removeFromList(subInfo.subModifierId)
                        choosen -= 1f
                    }
                }
                check2.setOnCheckedChangeListener { _, b ->
                    if (b) {
                        if (currentSelection == 1 || currentSelection == 3) {
                            selectionActive = true
                            check1.isChecked = false
                            check3.isChecked = false
                            selectionActive = false
                        }
                        currentSelection = 2
                        choosen += 0.5f
                        var subModPrice: Float? = null
                        val selectedVariantId = cartViewModel.selectedVariantId.value
                        if (selectedVariantId != null) {
                            val variantPriceList = subInfo.variantModifierPrice
                            if (!variantPriceList.isNullOrEmpty() && !variantArray.isNullOrEmpty()) {
                                for (variant in variantPriceList) {
                                    if (variant.variantId == selectedVariantId) {
                                        subModPrice = if (variant.modifierPrice.isNullOrBlank()) {
                                            0f
                                        } else {
                                            variant.modifierPrice!!.toFloat()
                                        }
                                        break
                                    }
                                }
                                if (subModPrice == null) {
                                    subModPrice = subInfo.normalModifierPrice?.toFloat()
                                }
                            } else {
                                subModPrice = subInfo.normalModifierPrice?.toFloat()
                            }
                        } else {
                            subModPrice = subInfo.normalModifierPrice?.toFloat()
                        }
                        addToSubList(
                            SelectedSubModifierHalf(
                                subInfo.subModifierId,
                                subInfo.subModifierName,
                                (subModPrice ?: 0f) / 2,
                                0.5f,
                                is2xApplied
                            )
                        )
                        holder.sBinding.finalPrice.visibility = View.VISIBLE
                        holder.sBinding.finalPrice.text =
                            Constants.currencySign + if (is2xApplied) {
                                (subModPrice ?: 0f).format()
                            } else {
                                ((subModPrice ?: 0f) / 2).format()
                            }
                        if (is2xApplied) {
                            check2x.isChecked = true
                        }
                    } else {
                        if (!selectionActive) {
                            currentSelection = 0
                            check2x.isChecked = false
                            holder.sBinding.finalPrice.visibility = View.INVISIBLE
                        }
                        removeFromList(subInfo.subModifierId)
                        choosen -= 0.5f
                    }
                }
                check3.setOnCheckedChangeListener { _, b ->
                    if (b) {
                        if (currentSelection == 1 || currentSelection == 2) {
                            selectionActive = true
                            check1.isChecked = false
                            check2.isChecked = false
                            selectionActive = false
                        }
                        currentSelection = 3
                        choosen += 0.5f
                        var subModPrice: Float? = null
                        val selectedVariantId = cartViewModel.selectedVariantId.value
                        if (selectedVariantId != null && !variantArray.isNullOrEmpty()) {
                            val variantPriceList = subInfo.variantModifierPrice
                            if (!variantPriceList.isNullOrEmpty()) {
                                for (variant in variantPriceList) {
                                    if (variant.variantId == selectedVariantId) {
                                        subModPrice = if (variant.modifierPrice.isNullOrBlank()) {
                                            0f
                                        } else {
                                            variant.modifierPrice!!.toFloat()
                                        }
                                        break
                                    }
                                }
                                if (subModPrice == null) {
                                    subModPrice = subInfo.normalModifierPrice?.toFloat()
                                }
                            } else {
                                subModPrice = subInfo.normalModifierPrice?.toFloat()
                            }
                        } else {
                            subModPrice = subInfo.normalModifierPrice?.toFloat()
                        }
                        addToSubList(
                            SelectedSubModifierHalf(
                                subInfo.subModifierId, subInfo.subModifierName,
                                (subModPrice ?: 0f) / 2, 0.5f,
                                is2xApplied
                            )
                        )
                        holder.sBinding.finalPrice.visibility = View.VISIBLE
                        holder.sBinding.finalPrice.text =
                            Constants.currencySign + if (is2xApplied) {
                                subModPrice?.format()
                            } else {
                                ((subModPrice ?: 0f) / 2).format()
                            }
                    } else {
                        if (!selectionActive) {
                            currentSelection = 0
                            check2x.isChecked = false
                            holder.sBinding.finalPrice.visibility = View.INVISIBLE
                        }
                        removeFromList(subInfo.subModifierId)
                        choosen -= 0.5f
                    }
                }
                cartViewModel.selectedVariantId.observe(ctx as AppCompatActivity, {
                    check1.isChecked = false
                    check2.isChecked = false
                    check3.isChecked = false
                    holder.sBinding.finalPrice.visibility = View.INVISIBLE
                } )
            }

            override fun getItemCount(): Int = listH.size

        }

        inner class SubModifierAdapter(
            private val mId: Int?,
            private val name: String?,
            val list: List<ProductSubModifier>,
            private val included: Int?,
            private val choosable: Int?,
            private val is2xMod: Int?,
        ) : RecyclerView.Adapter<SubModifierAdapter.MyHolder>() {

            private var selected = 0
            private var choosen = 0
            private val selectedList = ArrayList<SelectedSubModifier>()
            private val subModsNames = ArrayList<SubModifierBrief>()


            private fun allList(newList: ArrayList<SelectedSubModifier>) {
                selected = 0
                choosen = 0
                subModsNames.clear()
                val calcList = ArrayList<SelectedSubModifierCalc>()
                for (item in newList) {
                    subModsNames.add(SubModifierBrief(item.id ?: 0,item.name ?: "NA", item.is2xMode ?: 0, SUB_MODE_FULL))
                    Log.d("Inside all list", "${item.name}, ${item.price}, ${item.is2xMode}")
                    if (item.is2xMode == 1) {
                        selected += 2
                        choosen += 1
                        calcList.add(SelectedSubModifierCalc(item.id, item.price, 1f))
                        calcList.add(SelectedSubModifierCalc(item.id, item.price, 1f))
                    } else {
                        selected += 1
                        choosen += 1
                        calcList.add(SelectedSubModifierCalc(item.id, item.price, 1f))
                    }
                }
                Log.d("asd", "choosen:$choosen & selected: $selected")
                calculateTotal(calcList)
            }

            private fun calculateTotal(calList : ArrayList<SelectedSubModifierCalc>) {
                val sortedList = calList.sortedWith(compareBy({ it.price }, { it.qty }))
                var payableModifierPrice = 0f
                var freeModifier1 = (included ?: 0).toFloat()

                for (product in sortedList) {
                    if (product.qty == 0f)
                        continue
                    freeModifier1 -= product.qty ?: 1f
                    if (freeModifier1 >= 0) {
                        Log.d("Modifier0", "do nothing")
                    } else {
                        payableModifierPrice += (product.price ?: 0f)
                    }
                }
                addModifierDetails(mId ?: 0, SelectedModifierDetails(name, subModsNames, payableModifierPrice))
                Log.d("SubModifierTotal", "final price $payableModifierPrice")
            }


            private fun addToList(subMod: SelectedSubModifier) {
                selectedList.add(subMod)
                allList(selectedList)
            }

            private fun removeFromList(id: Int?) {
                var toRemove : SelectedSubModifier? = null
                for (item in selectedList) {
                    if (item.id == id) {
                        toRemove = item
                        break
                    }
                }
                if (toRemove != null) {
                    selectedList.remove(toRemove)
                } else {
                    Log.e("Remove", "Id not found")
                }
                allList(selectedList)
            }

            inner class MyHolder(val sBinding: ItemCartSubmodifiersBinding) :
                RecyclerView.ViewHolder(sBinding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
                return MyHolder(
                    ItemCartSubmodifiersBinding.inflate(
                        LayoutInflater.from(ctx),
                        parent,
                        false
                    )
                )
            }

            override fun onBindViewHolder(holder: MyHolder, position: Int) {
                val subMod = list[position]
                holder.sBinding.subModifierName.text = subMod.subModifierName
                var subModPrice : Float? = null
                if (!subMod.variantModifierPrice.isNullOrEmpty() && !variantArray.isNullOrEmpty()) {
                    for (varMod in subMod.variantModifierPrice!!) {
                        if (varMod.variantId == cartViewModel.selectedVariantId.value) {
                            subModPrice = if (varMod.modifierPrice.isNullOrBlank()) {
                                0f
                            } else {
                                varMod.modifierPrice?.toFloat()
                            }
                            break
                        }
                    }
                    if (subModPrice == null) {
                        subModPrice = subMod.normalModifierPrice?.toFloat()
                    }
                } else {
                    subModPrice = subMod.normalModifierPrice?.toFloat()
                }
                holder.sBinding.subModPrice.text = Constants.currencySign +
                        subModPrice?.format()
                val subModItem = SelectedSubModifier(subMod.subModifierId,
                    subMod.subModifierName, subModPrice, 0)
                if (is2xMod == 1) {
                    holder.sBinding.check2x.visibility = View.VISIBLE
                    holder.sBinding.check2x.setOnCheckedChangeListener { _, b ->
                        if (b) {
                            /*     if (choosen > (choosable ?: 1)) {
                                     Toast.makeText(ctx, "Sorry, Only $choosable is allowed", Toast.LENGTH_SHORT).show()
                              //       compoundButton.isChecked = false
                                     return@setOnCheckedChangeListener
                                 } */
                            subModItem.is2xMode = 1
                            if (holder.sBinding.subModCheck.isChecked) {
                                removeFromList(subMod.subModifierId)
                                holder.sBinding.subModPrice.text = Constants.currencySign +
                                        ((subModPrice ?: 0f) * 2f).format()
                                addToList(subModItem)
                            } else {
                                holder.sBinding.subModCheck.isChecked = true
                            }
                        } else {
                            subModItem.is2xMode = 0
                            if (holder.sBinding.subModCheck.isChecked) {
                                removeFromList(subMod.subModifierId)
                                holder.sBinding.subModPrice.text = Constants.currencySign +
                                        ((subModPrice ?: 0f)).format()
                                addToList(subModItem)
                            }
                        }
                    }
                }
                holder.sBinding.subModCheck.setOnCheckedChangeListener { btn, b1 ->
                    if (b1) {
                        if (choosen >= (choosable ?: 1)) {
                            Toast.makeText(ctx, "Sorry, Only $choosable is allowed", Toast.LENGTH_SHORT).show()
                            btn.isChecked = false
                            holder.sBinding.check2x.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                        if (is2xMod == 1) {
                            holder.sBinding.check2x.visibility = View.VISIBLE
                        }
                        holder.sBinding.subModPrice.visibility = View.VISIBLE
                        if (subModItem.is2xMode == 0) {
                            holder.sBinding.subModPrice.text = Constants.currencySign +
                                    (subModItem.price ?: 0f).format()
                        } else {
                            holder.sBinding.subModPrice.text = Constants.currencySign +
                                    ((subModItem.price ?: 0f) * 2).format()
                        }
                        addToList(subModItem)
                    } else {
                //        holder.sBinding.check2x.visibility = View.INVISIBLE
                        holder.sBinding.subModPrice.visibility = View.INVISIBLE
                        holder.sBinding.check2x.isChecked = false
                        removeFromList(subModItem.id)
                    }
                }
                cartViewModel.selectedVariantId.observe(ctx as AppCompatActivity, {
                    holder.sBinding.check2x.isChecked = false
                    holder.sBinding.subModCheck.isChecked = false
                })
            }

            override fun getItemCount(): Int = list.size
        }
    }


    private fun exitEditingMode() {
        clearTheCart()
        inEditingMode = false
        binding.updateLayout.visibility = View.GONE
    }

    // classes for sub cart
    class SelectedModifierDetails(val name: String?, val subMods: ArrayList<SubModifierBrief>, val totalPrice: Float?)
    class VariantRadio(val id:Int?, val label: String?, val price: Float?)
    class SelectedSubModifier(val id:Int?, val name : String?, val price:Float?, var is2xMode: Int?)
    class SelectedSubModifierHalf(
        val id: Int?,
        val name: String?,
        val price: Float?,
        val qty: Float?,
        var is2xMod: Boolean
    )
    class SelectedSubModifierCalc(val id: Int?, val price: Float?, val qty: Float?)
}