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
    private var selectedTable = 0
    private var billStatus = 0
    private var categoryDiscounts : List<DiscountData> = emptyList()
    private var productDiscounts : List<DiscountData> = emptyList()
    private var categoryDiscount = 0f
    private var productDiscount = 0f
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
        binding.tableBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val tables = mainRepository
                    .getTablesForOutletAsync(Constants.selectedOutletId).await()
                Log.d("Available Tables for ${Constants.selectedOutletId}", "${tables?.size}")
                if (!tables.isNullOrEmpty()) {
                    val arrayAdapter = ArrayAdapter(ctx, R.layout.item_light_spinner, R.id.light_spinner_text, tables)
                    binding.tableSpinner.adapter = arrayAdapter
                    binding.tableSpinner.visibility = View.VISIBLE
                    binding.tableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            p0: AdapterView<*>?,
                            p1: View?,
                            p2: Int,
                            p3: Long
                        ) {
                            selectedTable = tables[p2].tableId
                            Log.d("Selected Table", "${tables[p2].tableId}")
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) { }
                    }
                    binding.tableBtn.visibility = View.GONE
                } else {
                    Toast.makeText(ctx, "Tables data not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.addTipBtn.setOnClickListener {
            var tipDialog : AlertDialog? = null
            val builder = AlertDialog.Builder(ctx).apply {
                val tipBinding = DialogTipBinding.inflate(LayoutInflater.from(ctx))
                var tipAmount1 = 0f
                var inPercentage = false
                tipBinding.radioTipType.setOnCheckedChangeListener { _, i ->
                    when (i) {
                        R.id.radioAmount -> {
                            inPercentage = false
                            if (!tipBinding.tipEd.text.isNullOrBlank()) {
                                tipAmount1 = tipBinding.tipEd.text.toString().toFloat()
                                tipBinding.totalTip.text = "Tip ( ${Constants.currencySign} ${tipBinding.tipEd.text} )"
                            }
                            tipBinding.tipType.text = "$"
                        }
                        R.id.radioPercentage -> {
                            inPercentage = true
                            if (!tipBinding.tipEd.text.isNullOrBlank()) {
                                tipAmount1 = (grandTotal * tipBinding.tipEd.text.toString().toFloat() / 100)
                                tipBinding.totalTip.text = "Tip ( ${Constants.currencySign} ${tipAmount1.format()} )"
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
                        tipBinding.totalTip.text = "Tip ( ${Constants.currencySign} ${tipAmount1.format()} )"
                        return@doOnTextChanged
                    }
                    tipAmount1 = text.toString().toFloat()
                    tipBinding.totalTip.text = "Tip ( ${Constants.currencySign} ${tipAmount1.format()} )"
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
        ctx.registerReceiver(outletChangeReceiver, IntentFilter(Constants.OUTLET_CHANGE_BROADCAST))
    }

    fun clearTheCart() {
        tipAmount = 0f
        binding.cookingTimeSeek.progress = 5
        binding.tipText.text = Constants.currencySign +"0"
        items.clear()
        itemAdapter.notifyDataSetChanged()
        cartViewModel.setDiscount(0)
        updateTotalAmounts(false)
    }

    fun checkoutCart(items: List<ItemInCart>) {
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
        orderRequest.table = selectedTable.toString()
        Log.d("Table", "${orderRequest.table}")
        orderRequest.accountNo = ""
        orderRequest.driverAssigned = 0
        orderRequest.cardType = 1
        orderRequest.discount = cartViewModel.discountPer.value?.toFloat()
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

    private fun addTax(discountPercentage : Int) : Float {
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
        val discountAmount = subtotal * (cartViewModel.discountPer.value ?: 0)  / 100
        grandTotal += subtotal
        grandTotal -= discountAmount
        grandTotal += tipAmount
        binding.discountText.text = Constants.currencySign +" "+discountAmount.format()
        binding.subTotalText.text = Constants.currencySign +" " +subtotal.format()
        val tax = addTax(cartViewModel.discountPer.value ?: 0)
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
                incrementQty(item.id)
            }
            holder.cBinding.itemRemove.setOnClickListener {
                decrementQty(item.id)
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

    class ItemInCart(
        val id:Int?, val name:String?, val variantId: Int?, val modifiers: ArrayList<ModifierInCart>,
        var qty: Int?, val price: Float?,val catDiscounts : List<DiscountData>, val prodDiscount : List<DiscountData>,
        val taxes: List<ProductTax>?, val orderNote: String?, val isAddOn : Boolean,
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
                cartViewModel.setDiscount(dBinding.editText.text.toString().toInt())
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

        private val _discountPer = MutableLiveData(0)
        val discountPer : LiveData<Int> = _discountPer

        private val _selectedVariantId = MutableLiveData(0)
        val selectedVariantId : LiveData<Int> = _selectedVariantId

        fun setDiscount(per : Int) {
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
                        id = variant.variantId
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
                holder.binding.addOnCheck.setOnCheckedChangeListener { compoundButton, b ->
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
                            /*        holder.sBinding.subModCheck.isChecked = true
                                    removeFromList(subMod.subModifierId)
                                    holder.sBinding.subModPrice.text = Constants.currencySign +
                                        ((subModPrice ?: 0f) * 2f).format()
                                    addToList(subModItem)  */
                        } else {
                            subModItem.is2xMode = 0
                            /*       holder.sBinding.subModPrice.text = Constants.currencySign +
                                           subModPrice?.format()
                                   removeFromList(subModItem.id)
                                   addToList(subModItem)  */
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