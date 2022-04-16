package com.tjcg.nentopos.adapters

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.MainActivity.Companion.mainRepository
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.DialogOrderDetailsBinding
import com.tjcg.nentopos.databinding.ItemKitchenBinding
import com.tjcg.nentopos.databinding.ItemOrderDetailsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KitchenAdapter(private val ctx: Context, private val list : List<OrdersEntity>) :
    RecyclerView.Adapter<KitchenAdapter.MyHolder>() {

    inner class MyHolder(val binding: ItemKitchenBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemKitchenBinding.inflate(LayoutInflater.from(ctx), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val order = list[position]
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.tableNo.text = "Table No. "+ if (order.table_no == null) {
                "NA"
            } else {
                val tableInfo = mainRepository.getOneTableDataAsync(order.table_no ?: 0).await()
                if (tableInfo != null) {
                    "${tableInfo.tableName}"
                } else {
                    "NA"
                }
            }
            holder.binding.waiterName.text = "Waiter: " + if (order.waiter_id == null) {
                "NA"
            } else {
                val subUserData = mainRepository.getOneSubUserDetailAsync(order.waiter_id!!).await()
                if (subUserData != null) {
                    "${subUserData.firstname} ${subUserData.lastname}"
                } else {
                    "NA"
                }
            }
        }

        holder.binding.tokenNo.text = "Token No. " + if (order.tokenno.isNullOrBlank()) {
            "NA"
        } else {
            order.tokenno
        }
        holder.binding.cookingTime.text = "Cooking Time : ${order.cookedtime}"
        holder.binding.orderNo.text = "Order Id : ${order.order_id}"
        holder.binding.viewOrder.setOnClickListener {
            val dialog1 = OrderDetailsDialog1.getInstance(ctx, order.order_id)
            dialog1.show(MainActivity.fManager, "dialog")
        }
    }

    override fun getItemCount(): Int = list.size

    companion object {
        class KitchenOperation(val orderId: Long,val menuId: Int?,val foodStatus: Int,val rowId: String, val addOnId: Int?)
    }

    class OrderDetailsDialog1 : DialogFragment() {

        private lateinit var dBinding : DialogOrderDetailsBinding
        private lateinit var ctx : Context
        private var orderId: Long = 0
        private val kitchenOperation = ArrayList<KitchenOperation>()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            dBinding = DialogOrderDetailsBinding.inflate(
                inflater, container, false)
            dBinding.recyclerViewOrderDetails.layoutManager = LinearLayoutManager(ctx)
            dBinding.productAddonsRecyclerView.layoutManager = LinearLayoutManager(ctx)
            CoroutineScope(Dispatchers.Main).launch {
                val order : OrdersEntity? = MainActivity.orderRepository
                    .getSingleOrderAsync(Constants.selectedOutletId, orderId).await()
                if (order == null) {
                    Toast.makeText(ctx, "Data not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val itemList = order.itemsInfo
                val addOns = order.addOns
                dBinding.recyclerViewOrderDetails.adapter = OrderDetailsAdapter(ctx, itemList ?: emptyList())
                dBinding.productAddonsRecyclerView.adapter = AddOnDetailsAdapter(ctx, addOns ?: emptyList())
                dBinding.orderCompleteButton.setOnClickListener {
                    MainActivity.orderRepository.completeAllOrderItems(ctx, orderId)
                }
            }

            dBinding.applyBtn.setOnClickListener {
                MainActivity.orderRepository.changeKitchenOrderStatusMulti(ctx, kitchenOperation)
         /*       for (operation in kitchenOperation) {
                    Log.d("RowIds", operation.rowId)
                    MainActivity.orderRepository.changeKitchenOrderStatusOnline(ctx, Constants.selectedOutletId,
                        operation.orderId, operation.menuId ?: 0, operation.orderStatus, operation.rowId, operation.addOnId,
                        Constants.authorization)
                } */
            }
            dBinding.closeButton.setOnClickListener {
                this.dismiss()
            }
            val closeReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    val dId = p1?.getIntExtra(Constants.ID_DIALOG, 0)
                    if (dId == Constants.ID_DIALOG_KITCHEN_ITEM_DETAILS) {
                        this@OrderDetailsDialog1.dismiss()
                    }
                }
            }
            ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG_BROADCAST))
            return dBinding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            super.onViewCreated(view, savedInstanceState)
        }


        companion object {

            fun getInstance(ctx: Context, orderId: Long) : OrderDetailsDialog1 {
                val dialog = OrderDetailsDialog1()
                dialog.ctx = ctx
                dialog.orderId = orderId
                return dialog
            }
        }

        inner class OrderDetailsAdapter(val ctx: Context, private val itemList:List<OrdersEntity.ItemInfo>) : RecyclerView.Adapter<OrderDetailsAdapter.MyHolder>() {

            inner class MyHolder(val binding: ItemOrderDetailsBinding) :
                RecyclerView.ViewHolder(binding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
                return MyHolder(ItemOrderDetailsBinding.inflate(
                    LayoutInflater.from(ctx), parent, false))
            }

            override fun onBindViewHolder(holder: MyHolder, position: Int) {
                val item = itemList[position]
                CoroutineScope(Dispatchers.Main).launch {
                    val productData = mainRepository.getProductDataAsync(item.productId ?: 0).await()
                    if (productData != null) {
                        var productName = productData.productName
                        if (item.varientId != null) {
                            val variant = mainRepository.getOneVariantDataAsync((item.varientId ?: 0)).await()
                            if (variant?.variantName != null) {
                                productName += " - ${variant.variantName}"
                            }
                        }
                        holder.binding.productName.text = productName
                    }
                }

                holder.binding.productQuantity.text = item.menuQty
                // get the list of modifiers
                if (!item.modifierInfo.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        for (modInfo in item.modifierInfo!!) {
                            val mod = mainRepository.getOneModifierDataAsync(modInfo.modifierId ?: 0).await()
                            val textView = TextView(ctx)
                            if (mod != null) {
                                var modText = "-${mod.modifierName} :"
                                if (modInfo.subModifier != null) {
                                    if (!modInfo.subModifier?.subModifierWhole.isNullOrEmpty()) {
                                        for (subMod in modInfo.subModifier?.subModifierWhole!!) {
                                            val subModInfo = mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                            if (subModInfo != null) {
                                                modText += "${subModInfo.subModifierName},"
                                            }
                                        }
                                    }
                                    if (!modInfo.subModifier?.subModifierFirstHalf.isNullOrEmpty()) {
                                        for (subMod in modInfo.subModifier?.subModifierFirstHalf!!) {
                                            val subModInfo = mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                            if (subModInfo != null) {
                                                modText += "${subModInfo.subModifierName},"
                                            }
                                        }
                                    }
                                    if (!modInfo.subModifier?.subModifierSecondHalf.isNullOrEmpty()) {
                                        for (subMod in modInfo.subModifier?.subModifierSecondHalf!!) {
                                            val subModInfo = mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                            if (subModInfo != null) {
                                                modText += "${subModInfo.subModifierName},"
                                            }
                                        }
                                    }
                                    if (!modInfo.subModifier?.subModifierList.isNullOrEmpty()) {
                                        for (subMod in modInfo.subModifier?.subModifierList!!) {
                                            val subModInfo = mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                            if (subModInfo != null) {
                                                modText += "${subModInfo.subModifierName},"
                                            }
                                        }
                                    }
                                }
                                holder.binding.modifierItemLayout.addView(textView)
                                textView.text = modText
                                textView.setTextColor(Color.RED)
                            }

                        }
                    }

                }

                if (item.foodStatus == 1) {
                    holder.binding.checkbox.isChecked = true
                }
                holder.binding.customerNote.text = if (item.orderNote.isNullOrBlank()) {
                    "NA"
                } else {
                    item.orderNote
                }

                holder.binding.checkbox.setOnCheckedChangeListener { _, b ->
                    dBinding.applyBtn.visibility = View.VISIBLE
                    val orderStatus : Int= if (b) 1 else 0
                    var replaceOperation = false
                    var toReplace : KitchenOperation? = null
                    if (!kitchenOperation.isNullOrEmpty()) {
                        for (operation in kitchenOperation) {
                            if (operation.rowId == item.rowId) {
                                replaceOperation = true
                                toReplace = operation
                            }
                        }
                        if (replaceOperation && toReplace!=null) {
                            kitchenOperation.remove(toReplace)
                            kitchenOperation.add(KitchenOperation(item.orderId ?: 0L, item.menuId ?: 0
                                , orderStatus, item.rowId ?: "0", null))
                        } else {
                            kitchenOperation.add(KitchenOperation(item.orderId ?: 0L, item.menuId ?: 0
                                , orderStatus, item.rowId ?: "0", null))
                        }
                    } else {
                        kitchenOperation.add(KitchenOperation(item.orderId ?: 0, item.menuId ?: 0
                            , orderStatus, item.rowId ?: "0", null))
                    }
                    Log.d("KitchenOperation", "operations: ${kitchenOperation.size}")
                    for (order in kitchenOperation) {
                        Log.d("operation: ", "${order.rowId}, ${order.orderId}, ${order.foodStatus}")
                    }
                    /*   val orderStatus : String = if (b) "1" else "0"
                       OrderRepository1.changeKitchenOrderStatusOnline(ctx, Constants.selectedOutletId, (item.orderId ?: "0"),
                           (item.menuId ?: "0") , orderStatus, (item.rowId ?: "0") ,Constants.Authorization)  */
                }
            }

            override fun getItemCount(): Int = itemList.size
        }

        inner class AddOnDetailsAdapter(val ctx: Context, val addOnList: List<OrdersEntity.AddOn>) :
                RecyclerView.Adapter<AddOnDetailsAdapter.MyHolder>() {

                    inner class MyHolder(val binding: ItemOrderDetailsBinding) :
                            RecyclerView.ViewHolder(binding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
                return MyHolder(ItemOrderDetailsBinding.inflate(
                    LayoutInflater.from(ctx), parent, false))
            }

            override fun onBindViewHolder(holder: MyHolder, position: Int) {
                val addOn = addOnList[position]
                CoroutineScope(Dispatchers.Main).launch {
                    val addOnInfo = mainRepository.getOneAddOnDataAsync(addOn.addOnId ?: 0).await()
                    if (addOnInfo != null) {
                        holder.binding.productName.text = addOnInfo.addOnName
                        holder.binding.productQuantity.text = addOn.addOnQty
                    }
                    holder.binding.checkbox.isChecked = addOn.foodStatus == 1
                    holder.binding.checkbox.setOnCheckedChangeListener { _, b ->
                        dBinding.applyBtn.visibility = View.VISIBLE
                        val orderStatus : Int = if (b) 1 else 0
                        var replaceOperation = false
                        var toReplace : KitchenOperation? = null
                        if (!kitchenOperation.isNullOrEmpty()) {
                            for (operation in kitchenOperation) {
                                if (operation.rowId == addOn.rowId) {
                                    replaceOperation = true
                                    toReplace = operation
                                }
                            }
                            if (replaceOperation && toReplace!= null) {
                                kitchenOperation.remove(toReplace)
                                kitchenOperation.add(KitchenOperation(orderId, 0, orderStatus,
                                addOn.rowId ?: "0", addOn.addOnId))
                            } else {
                                kitchenOperation.add(KitchenOperation(orderId, 0, orderStatus,
                                addOn.rowId ?: "0", addOn.addOnId))
                            }
                        } else {
                            kitchenOperation.add(
                                KitchenOperation(orderId, -1, orderStatus,
                                    addOn.rowId ?: "0", addOn.addOnId))
                        }
                    }
                }
            }

            override fun getItemCount(): Int = addOnList.size
        }
    }
}