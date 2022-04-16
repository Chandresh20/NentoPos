package com.tjcg.nentopos.adapters

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.DialogAsignDriverBinding
import com.tjcg.nentopos.databinding.ItemOnlineOrderBinding
import com.tjcg.nentopos.dialog.OrderAcceptDialog
import com.tjcg.nentopos.dialog.OrderCancelDialog
import com.tjcg.nentopos.dialog.PaymentDialog
import com.tjcg.nentopos.fragments.InvoiceFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class OnlineOrderAdapter(val ctx: Context, val list: List<OrdersEntity>, val navController: NavController) :
    RecyclerView.Adapter<OnlineOrderAdapter.MyHolder>() {

    inner class MyHolder(val binding: ItemOnlineOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemOnlineOrderBinding.inflate(
            LayoutInflater.from(ctx), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val onlineOrderData = list[position]
        holder.binding.orderId.text = onlineOrderData.order_id.toString()
        holder.binding.totalAmount.text = "${onlineOrderData.totalamount}"
        if (onlineOrderData.order_status != Constants.ORDER_STATUS_PENDING) {
            holder.binding.acceptOrder.visibility = View.GONE
            if (onlineOrderData.order_status != Constants.ORDER_STATUS_PROCESSING) {
                holder.binding.cancelOrder.visibility = View.GONE
                holder.binding.assignDriver.visibility = View.GONE
            }
        }
        if (onlineOrderData.order_status == Constants.ORDER_STATUS_PROCESSING) {
            holder.binding.completeOrder.visibility = View.VISIBLE
        }
        if (onlineOrderData.futureOrderType == Constants.FUTURE_ORDER_FAR) {
            holder.binding.acceptOrder.visibility = View.GONE
            holder.binding.assignDriver.visibility = View.GONE
        }
  /*      if (!onlineOrderData.future_order_date.isNullOrBlank()) {

            holder.binding.acceptOrder.visibility = View.GONE
            holder.binding.assignDriver.visibility = View.GONE
            // look for future order date
            val fDate = onlineOrderData.future_order_date?.split("-")
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (!fDate?.get(0).isNullOrBlank() && fDate?.get(0)?.toInt() == year) {
                if (!fDate[1].isBlank() && fDate[1].toInt() == month) {
                    if (!fDate[2].isBlank() && fDate[2].toInt() == day) {
                        holder.binding.acceptOrder.visibility = View.VISIBLE
                        holder.binding.assignDriver.visibility = View.VISIBLE
                    }
                }
            }
        }  */

        holder.binding.acceptOrder.setOnClickListener {
            val dialog = OrderAcceptDialog.getInstance(ctx, onlineOrderData)
            dialog.show(MainActivity.fManager, "dialog")
        }
        holder.binding.orderTime.text = "${onlineOrderData.order_date}\n${onlineOrderData.order_time}"
        holder.binding.paymentStatus.text =
            when (onlineOrderData.billInfo?.billStatus) {
                Constants.STATUS_NOT_PAID -> ctx.resources.getString(R.string.not_paid)
                Constants.STATUS_PAID -> ctx.resources.getString(R.string.paid)
                else -> ctx.resources.getString(R.string.unknown)
            }
        holder.binding.completeOrder.setOnClickListener {
            val paymentDialog = PaymentDialog(ctx, onlineOrderData.totalamount ?: 0f,
                PaymentDialog.FROM_ONGOING_ORDERS)
            paymentDialog.show(MainActivity.fManager, "payment")
            val paymentReceiver = object: BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    val customerPaid = p1?.getFloatExtra(PaymentDialog.paymentAmount, 0f) ?: 0f
                    MainActivity.orderRepository.completeOrderWithPayment(ctx, customerPaid, onlineOrderData.order_id,
                        onlineOrderData.outlet_id ?: 0)
                    ctx.unregisterReceiver(this)
                }
            }
            ctx.registerReceiver(paymentReceiver, IntentFilter(Constants.PAYMENT_DONE_BROADCAST))
            val orderCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    val dId = p1?.getIntExtra(Constants.ID_DIALOG, 0)
                    if (dId == Constants.ID_DIALOG_COMPLETE_ORDER) {
                        paymentDialog.dismiss()
                        val isSuccess = p1.getBooleanExtra(Constants.IS_SUCCESS, false)
                        if (isSuccess) {
                            val builder = AlertDialog.Builder(ctx).apply {
                                setMessage("Order completed successfully, Would you like to print the invoice?")
                                setPositiveButton("Print") { dialog, _ ->
                                    InvoiceFragment.orderId = onlineOrderData.order_id
                                    navController.navigate(R.id.navigation_invoice)
                                    dialog.dismiss()
                                }
                                setNegativeButton("Skip") { _, _ -> }
                            }
                            val dialog = builder.create()
                            dialog.show()
                        } else {
                            val builder = AlertDialog.Builder(ctx).apply {
                                setMessage("An Error Occurred7, please try again..")
                                setPositiveButton("Dismiss") { dialog, _ ->
                                    dialog.dismiss()
                                }
                            }
                            val dialog = builder.create()
                            dialog.show()
                        }
                        ctx.unregisterReceiver(this)
                    }
                }

            }
            ctx.registerReceiver(orderCompleteReceiver, IntentFilter(Constants.CLOSE_DIALOG_BROADCAST))
        /*    val tipDialog = PaymentMethodDialog(object: PaymentMethodDialog.ClickListener {
                override fun paymentMethodClick(type: String, orderId: String) {
                    when (type) {
                        Constants.PAYMENT_METHOD_CASH -> {
                            val calcDialog = CalculatorDialog(object : CalculatorDialog.ClickListener {
                                override fun onCalculatorNext(customerPaidAmount: String) {
                                    Log.d("OngoingAdapter", "calling complete API")
                                    lottieProgressDialog.showDialog()
                                    OrderRepository1.completeOrderWithPayment(ctx, customerPaidAmount.toFloat(),
                                        onlineOrderData.order_id, (onlineOrderData.outlet_id ?: "1"))
                                }
                            }, if (onlineOrderData.totalAmount.isNullOrBlank()) "0" else onlineOrderData.totalAmount!!)
                            calcDialog.show((ctx as AppCompatActivity).supportFragmentManager, "tag")
                            val closeReceiver = object : BroadcastReceiver() {
                                override fun onReceive(p0: Context?, p1: Intent?) {
                                    val success = p1?.getBooleanExtra(SharedPreferencesKeys.IS_SUCCESS, false)
                                    if (success!=null && success) {
                                        val builder = AlertDialog.Builder(ctx).apply {
                                            setMessage("Order completed successfully, Would you like to print the invoice?")
                                            setPositiveButton("Print") { dialog, _ ->
                                                (ctx as MainActivity).setFragment(InvoiceFragment1.getInstance(onlineOrderData.order_id))
                                                dialog.dismiss()
                                            }
                                            setNegativeButton("Skip") { _, _ -> }
                                        }
                                        val dialog = builder.create()
                                        dialog.show()
                                    } else {
                                        val builder = AlertDialog.Builder(ctx).apply {
                                            setMessage("An Error Occurred7, please try again..")
                                            setPositiveButton("Dismiss") { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                        }
                                        val dialog = builder.create()
                                        dialog.show()
                                    }
                                    calcDialog.dismiss()
                                    lottieProgressDialog.cancelDialog()
                                    ctx.unregisterReceiver(this)
                                }
                            }
                            ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG))
                        }
                        Constants.PAYMENT_METHOD_DEBIT -> {
                            val orderDialog = OrderStatusDialog(object : OrderStatusDialog.ClickListener {
                                override fun onNext(order_status: String, orderId: String) {

                                }
                            }, onlineOrderData.order_id)
                            orderDialog.show((ctx as AppCompatActivity).supportFragmentManager, "tag")
                        }
                        Constants.PAYMENT_METHOD_CREDIT -> {
                            val cardDialog = CardTerminalDialog(object : CardTerminalDialog.creditCard {
                                override fun onCardSelect(
                                    orderId: String,
                                    selectedCardId: String
                                ) {

                                }
                            }, onlineOrderData.order_id)
                            cardDialog.show((ctx as AppCompatActivity).supportFragmentManager, "tag")
                        }
                    }
                }
            },onlineOrderData.order_id)
            tipDialog.show((ctx as AppCompatActivity).supportFragmentManager, "Payment Method")
            val closeReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    tipDialog.dismiss()
                }
            }
            ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG))  */
        }
        holder.binding.cancelOrder.setOnClickListener {
            val dialog = OrderCancelDialog.getInstance(ctx, onlineOrderData)
            dialog.show(MainActivity.fManager, "cancelDialog")
        }
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.tableName.text = if (onlineOrderData.table_no == null) {
                "NA"
            } else {
                val tableData = MainActivity.mainRepository.getOneTableDataAsync(onlineOrderData.table_no ?: 0).await()
                if (tableData != null) {
                    "${tableData.tableName}"
                } else {
                    "NA"
                }
            }
            holder.binding.serverName.text = if (onlineOrderData.waiter_id == null) {
                "NA"
            } else {
                val subUserData = MainActivity.mainRepository
                    .getOneSubUserDetailAsync(onlineOrderData.waiter_id ?: 0).await()
                if (subUserData != null) {
                    "${subUserData.firstname} ${subUserData.lastname}"
                } else {
                    "NA"
                }
            }
            if (onlineOrderData.customer_id == null || (onlineOrderData.customer_id ?: 0)  <= 0) {
                holder.binding.customerName.text = "Guest"
            } else {
                val customerInfo = MainActivity.mainRepository
                .getOneCustomerDataAsync(onlineOrderData.customer_id ?: 0).await()
            holder.binding.customerName.text =
                if (customerInfo != null) {
                    customerInfo.customerName
                } else {
                    MainActivity.orderViewModel.newCustomersData.observe(
                        ctx as MainActivity,  { list ->
                            if (list.isNotEmpty()) {
                                for (customer in list) {
                                    if (customer.customerId == onlineOrderData.customer_id) {
                                        holder.binding.customerName.text = customer.customerName
                                    }
                                }
                            }
                        })
                    val outletData = MainActivity.mainRepository.getOutletDataAsync(Constants.selectedOutletId).await()
                    MainActivity.mainRepository.loadCustomerData(
                        ctx, outletData?.outletId ?: 0, MainActivity.deviceID,
                        1, true
                    )
                    "loading"
                }
            }
            holder.binding.accountNumber.text = if (onlineOrderData.account_number.isNullOrBlank()) {
                "NA"
            } else {
                onlineOrderData.account_number
            }
            holder.binding.futureOrderDate.text = if (onlineOrderData.future_order_date.isNullOrBlank()) {
                "NA"
            } else {
                "${onlineOrderData.future_order_date}\n${onlineOrderData.future_order_time}"
            }
            val orderType = MainActivity.mainRepository.getMenuNameAsync(onlineOrderData.menu_id ?: 0).await()
            holder.binding.orderType.text = orderType ?: "NA"
        }

        holder.binding.onlineOrderInvoice.setOnClickListener {
            InvoiceFragment.orderId = onlineOrderData.order_id
            navController.navigate(R.id.navigation_invoice)
        }
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.assignedDriver.text = if (onlineOrderData.driver_user_id == null) {
                "NA"
            } else {
                val subUserData = MainActivity.mainRepository
                    .getOneSubUserDetailAsync(onlineOrderData.driver_user_id ?: 0).await()
                if (subUserData != null) {
                    val orderDelivered = if (onlineOrderData.pis_order_delivered == 1) {
                        "(Delivered)"
                    } else {
                        "(Not Delivered)"
                    }
                    val orderPaid = if (onlineOrderData.billInfo?.billStatus == 1) {
                        "(Paid)"
                    } else {
                        "(Unpaid)"
                    }
                    "${subUserData.firstname} ${subUserData.lastname}\n $orderDelivered $orderPaid"
                } else {
                    "NA"
                }
            }
        }

        holder.binding.viewDetails.setOnClickListener {
            InvoiceFragment.orderId = onlineOrderData.order_id
            navController.navigate(R.id.navigation_invoice)
        }
        holder.binding.pickupTime.text = if (onlineOrderData.order_pickup_at.isNullOrBlank()) {
            "NA"
        } else {
            onlineOrderData.order_pickup_at
        }
        holder.binding.assignDriver.setOnClickListener {
            val dialog = DriverDialog.getInstance(ctx, onlineOrderData.order_id)
            dialog.show(MainActivity.fManager, "none")
        }
    }

    override fun getItemCount(): Int = list.size

    class DriverDialog : DialogFragment() {

        lateinit var dBinding : DialogAsignDriverBinding
        private lateinit var ctx : Context
        private var orderId: Long = 0
        private var selectedDriverId = -1

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            dBinding = DialogAsignDriverBinding.inflate(
                inflater, container, false
            )
            dBinding.closeButton.setOnClickListener {
                this.dialog?.dismiss()
            }
            dBinding.closeButton2.setOnClickListener {
                this.dialog?.dismiss()
            }
            CoroutineScope(Dispatchers.Main).launch {
                val driverArray = ArrayList<DriverClass>()
                val subUsers = MainActivity.mainRepository.getAllSubUsersAsync(Constants.selectedOutletId).await()
                if (!subUsers.isNullOrEmpty()) {
                    for (subUser in subUsers) {
                        if (subUser.role_name?.lowercase(Locale.ROOT) == getString(R.string.driver)) {
                            driverArray.add(DriverClass(subUser.id, (subUser.firstname ?: "NA"),
                                    subUser.lastname ?: "NA"))
                        }
                    }
                }
                if (!driverArray.isNullOrEmpty()) {
                    val driverAdapter = ArrayAdapter(
                        ctx,
                        R.layout.item_light_spinner,
                        R.id.light_spinner_text,
                        driverArray
                    )
                    dBinding.driverSpinner.adapter = driverAdapter
                    dBinding.driverSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
                        ) {
                            Toast.makeText(ctx, "selected ${driverArray[p2]}", Toast.LENGTH_SHORT).show()
                            selectedDriverId = driverArray[p2].userId
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) { }

                    }
                } else {
                    dBinding.driverSpinner.visibility = View.GONE
                }
            }
            dBinding.assignButton.setOnClickListener {
                if (selectedDriverId == -1) {
                    this.dialog?.dismiss()
                    Toast.makeText(ctx,
                        "Person not assigned", Toast.LENGTH_SHORT).show()
                } else {
                    MainActivity.orderRepository.assignDriverOnline(ctx, orderId, selectedDriverId)
                    val closeReceiver = object : BroadcastReceiver() {
                        override fun onReceive(p0: Context?, p1: Intent?) {
                            val dId = p1?.getIntExtra(Constants.ID_DIALOG, 0)
                            if (dId == Constants.ID_DIALOG_DRIVER_ASSIGN) {
                                this@DriverDialog.dialog?.dismiss()
                            }
                        }
                    }
                    ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG_BROADCAST))
                }
                /*      if (dBinding.driverSpinner.selectedIndex == -1) {
                    this.dialog?.dismiss()
                    Toast.makeText(ctx,
                        "Person not assigned", Toast.LENGTH_SHORT).show()
                } else {
                    val assignedName = selectedPersonName.split(" ")
                    var assignedId : String? = null
                    for (subUser in subUsers) {
                        if (subUser?.firstname == assignedName[0] && subUser.lastname == assignedName[1]) {
                            assignedId = subUser.id
                        }
                    }
                    lottieProgressDialog.showDialog()
                    OrderRepository1.assignDriverOnline(ctx, orderId, assignedId ?: "0")
                    val closeReceiver = object : BroadcastReceiver() {
                        override fun onReceive(p0: Context?, p1: Intent?) {
                            lottieProgressDialog.cancelDialog()
                            this@DriverDialog.dialog?.dismiss()
                        }
                    }
                    ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG))  */
            }
            return dBinding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        class DriverClass(val userId: Int, private val fName: String, private val lName: String) {
            override fun toString(): String = "$fName $lName"
        }

        companion object {
            fun getInstance(ctx: Context, orderId: Long) : DriverDialog {
                val dialog = DriverDialog()
                dialog.ctx = ctx
                dialog.orderId = orderId
                return dialog
            }
        }
    }
}