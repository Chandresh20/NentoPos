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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.DialogCompleteOrderBinding
import com.tjcg.nentopos.databinding.DialogSendSmsBinding
import com.tjcg.nentopos.databinding.ItemOngoingOrderBinding
import com.tjcg.nentopos.dialog.OrderCancelDialog
import com.tjcg.nentopos.dialog.PaymentDialog
import com.tjcg.nentopos.fragments.InvoiceFragment
import com.tjcg.nentopos.fragments.POSFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OngoingOrderAdapter(val ctx: Context, val list:List<OrdersEntity>, val navController: NavController)
    : RecyclerView.Adapter<OngoingOrderAdapter.MyHolder>()  {

    inner class MyHolder(val binding: ItemOngoingOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemOngoingOrderBinding.inflate(
            LayoutInflater.from(ctx), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val ongoingOrder = list[position]
        if (ongoingOrder.billInfo?.billStatus == 1) {
            holder.binding.paidTick.visibility = View.VISIBLE
            holder.binding.editOrder.visibility = View.GONE
        }
        holder.binding.editOrder.setOnClickListener {
            if (Constants.databaseBusy) {
                MainActivity.progressDialogRepository.showAlertDialog(
                    "Database is busy, please try again in few minutes")
                return@setOnClickListener
            }
            POSFragment.orderToUpdate = ongoingOrder
            navController.navigate(R.id.navigation_pos)
        }
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.tableNo.text = "Table No: " + if (ongoingOrder.table_no == null) {
                "NA"
            } else {
                val tableData = MainActivity.mainRepository.getOneTableDataAsync(ongoingOrder.table_no ?: 0).await()
                if (tableData != null) {
                    tableData.tableName
                } else {
                    "NA"
                }
            }
            holder.binding.waiterName.text = "Waiter: " + if (ongoingOrder.waiter_id == null) {
                "NA"
            } else {
                val subUserData = MainActivity.mainRepository.getOneSubUserDetailAsync(ongoingOrder.waiter_id ?: 0).await()
                if (subUserData != null) {
                    "${subUserData.firstname} ${subUserData.lastname}"
                } else {
                    "NA"
                }
            }
        }
        holder.binding.orderNo.text = "Order No: ${ongoingOrder.order_id}"
        holder.binding.orderStatus.text =
            when (ongoingOrder.order_status) {
                Constants.ORDER_STATUS_PROCESSING -> {
                    "Processing"
                }
                Constants.ORDER_STATUS_READY -> {
                    "Ready"
                }
                else -> {
                    "Status: ${ongoingOrder.order_status}"
                }
            }
        holder.binding.completeOrder.setOnClickListener {
            if (ongoingOrder.billInfo?.billStatus == 1) {
                var alertDialog : AlertDialog? = null
                val dialogCloseReceiver = object : BroadcastReceiver() {
                    override fun onReceive(p0: Context?, p1: Intent?) {
                        alertDialog?.dismiss()
                        ctx.unregisterReceiver(this)
                    }
                }
                ctx.registerReceiver(dialogCloseReceiver, IntentFilter(Constants.CLOSE_DIALOG_BROADCAST))
                val builder = AlertDialog.Builder(ctx).apply {
                    val dBinding = DialogCompleteOrderBinding.inflate(LayoutInflater.from(ctx))
                    dBinding.noBtn.setOnClickListener {
                        alertDialog?.dismiss()
                    }
                    dBinding.yesBtn.setOnClickListener {
                        MainActivity.orderRepository.completeOrderWithPayment(ctx,
                            (ongoingOrder.customerpaid ?: "0").toFloat(), ongoingOrder.order_id,
                            ongoingOrder.outlet_id ?: Constants.selectedOutletId)
                    }
                    setView(dBinding.root)
                }
                alertDialog = builder.create()
                alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                alertDialog.show()
                return@setOnClickListener
            }
            var paymentDialog : PaymentDialog? = null
            val orderCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    val dId = p1?.getIntExtra(Constants.ID_DIALOG, 0)
                    if (dId == Constants.ID_DIALOG_COMPLETE_ORDER) {
                        if (paymentDialog != null) { paymentDialog?.dismiss() }
                        val isSuccess = p1.getBooleanExtra(Constants.IS_SUCCESS, false)
                        if (isSuccess) {
                            val builder = AlertDialog.Builder(ctx).apply {
                                setMessage("Order completed successfully, Would you like to print the invoice?")
                                setPositiveButton("Print") { dialog, _ ->
                                    dialog.dismiss()
                                    InvoiceFragment.orderId = ongoingOrder.order_id
                                    navController.navigate(R.id.navigation_invoice)
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
            paymentDialog = PaymentDialog(ctx, ongoingOrder.totalamount ?: 0f,
                PaymentDialog.FROM_ONGOING_ORDERS)
            paymentDialog.show(MainActivity.fManager, "payment")
            val paymentReceiver = object: BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    val customerPaid = p1?.getFloatExtra(PaymentDialog.paymentAmount, 0f) ?: 0f
                    MainActivity.orderRepository.completeOrderWithPayment(ctx, customerPaid, ongoingOrder.order_id,
                        ongoingOrder.outlet_id ?: 0)
                    ctx.unregisterReceiver(this)
                }
            }
            ctx.registerReceiver(paymentReceiver, IntentFilter(Constants.PAYMENT_DONE_BROADCAST))
        /*    if(ongoingOrder.billInfo?.billStatus.equals("1")){
                OrderRepository1.changeOrderStatusOnline(ctx, Constants.selectedOutletId,
                    ongoingOrder.order_id, Constants.ORDER_STATUS_SERVED)
                val closeReceiver = object : BroadcastReceiver() {
                    override fun onReceive(p0: Context?, p1: Intent?) {
                        val success = p1?.getBooleanExtra(SharedPreferencesKeys.IS_SUCCESS, false)
                        if (success!=null && success) {
                            val builder = AlertDialog.Builder(ctx).apply {
                                setMessage("Order completed successfully, Would you like to print the invoice?")
                                setPositiveButton("Print") { dialog, _ ->
                                    (ctx as MainActivity).setFragment(InvoiceFragment1.getInstance(ongoingOrder.order_id))
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
                        lottieProgressDialog.cancelDialog()
                        ctx.unregisterReceiver(this)
                    }
                }
                ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG))
                /*    val optionReceiptDialog = OptionReceiptDialog(object: OptionReceiptDialog.ClickListener {
                        override fun onPrint(order_status: String) {
                            Toast.makeText(ctx, "yet to implement", Toast.LENGTH_SHORT).show()
                        }

                        override fun onSkip(order_status: String) {
                            Toast.makeText(ctx, "yet to implement", Toast.LENGTH_SHORT).show()
                        }

                    },"",ongoingOrder.order_id)
                    optionReceiptDialog.show((ctx as AppCompatActivity).supportFragmentManager, "")  */
            } else {
                val tipDialog = PaymentMethodDialog(object: PaymentMethodDialog.ClickListener {
                    override fun paymentMethodClick(type: String, orderId: String) {
                        when (type) {
                            Constants.PAYMENT_METHOD_CASH -> {
                                val calcDialog = CalculatorDialog(object : CalculatorDialog.ClickListener {
                                    override fun onCalculatorNext(customerPaidAmount: String) {
                                        Log.d("OngoingAdapter", "calling complete API")
                                        lottieProgressDialog.showDialog()
                                        OrderRepository1.completeOrderWithPayment(ctx, customerPaidAmount.toFloat(),
                                            ongoingOrder.order_id, (ongoingOrder.outlet_id ?: "1"))
                                    }
                                }, if (ongoingOrder.totalAmount.isNullOrBlank()) "0" else ongoingOrder.totalAmount!!)
                                calcDialog.show((ctx as AppCompatActivity).supportFragmentManager, "tag")
                                val closeReceiver = object : BroadcastReceiver() {
                                    override fun onReceive(p0: Context?, p1: Intent?) {
                                        val success = p1?.getBooleanExtra(SharedPreferencesKeys.IS_SUCCESS, false)
                                        if (success!=null && success) {
                                            val builder = AlertDialog.Builder(ctx).apply {
                                                setMessage("Order completed successfully, Would you like to print the invoice?")
                                                setPositiveButton("Print") { dialog, _ ->
                                                    (ctx as MainActivity).setFragment(InvoiceFragment1.getInstance(ongoingOrder.order_id))
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
                                }, ongoingOrder.order_id)
                                orderDialog.show((ctx as AppCompatActivity).supportFragmentManager, "tag")
                            }
                            Constants.PAYMENT_METHOD_CREDIT -> {
                                val cardDialog = CardTerminalDialog(object : CardTerminalDialog.creditCard {
                                    override fun onCardSelect(
                                        orderId: String,
                                        selectedCardId: String
                                    ) {

                                    }
                                }, ongoingOrder.order_id)
                                cardDialog.show((ctx as AppCompatActivity).supportFragmentManager, "tag")
                            }
                        }
                    }
                },ongoingOrder.order_id)
                tipDialog.show((ctx as AppCompatActivity).supportFragmentManager, "Payment Method")
                val closeReceiver = object : BroadcastReceiver() {
                    override fun onReceive(p0: Context?, p1: Intent?) {
                        tipDialog.dismiss()
                    }
                }
                ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG))
            }  */
            /*      OrderRepository1.changeOrderStatusOnline(ctx, (ongoingOrder.outlet_id ?: Constants.selectedOutletId) ,
                      ongoingOrder.order_id, Constants.ORDER_STATUS_SERVED) */
        }
        holder.binding.cancelOrder.setOnClickListener {
            val dialog = OrderCancelDialog.getInstance(ctx, ongoingOrder)
            dialog.show(MainActivity.fManager, "cancelDialog")
        }
        holder.binding.invoicePrint.setOnClickListener {
            InvoiceFragment.directPrint = true
            InvoiceFragment.orderId = ongoingOrder.order_id
            navController.navigate(R.id.navigation_invoice)
        }
        holder.binding.getInvoice.setOnClickListener {
            InvoiceFragment.orderId = ongoingOrder.order_id
            navController.navigate(R.id.navigation_invoice)
        }
        holder.binding.sendSms.setOnClickListener {
            val smsDialog = SMSDialog.getInstance(ongoingOrder.customer_id ?: 0L, ctx)
            smsDialog.show(MainActivity.fManager, "sms")
        }
    }

    override fun getItemCount(): Int = list.size

    class SMSDialog : DialogFragment() {

        private lateinit var dBinding: DialogSendSmsBinding
        private lateinit var ctx: Context
        private var customerId : Long = 0L
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            dBinding = DialogSendSmsBinding.inflate(
                inflater, container, false)
            CoroutineScope(Dispatchers.Main).launch {
                val custData = MainActivity.mainRepository.getOneCustomerDataAsync(customerId).await()
                if (custData != null) {
                    dBinding.customerName.text = custData.customerName
                    var customerNumber = ""
                    if (!custData.customerPhone.isNullOrBlank()) {
                        if (!custData.countryCode.isNullOrBlank()) {
                            customerNumber += "+(${custData.countryCode}) "
                        }
                        customerNumber += MainActivity.formatPhone(custData.customerPhone ?: "0000")
                    } else {
                        customerNumber = "NA"
                    }
                    dBinding.mobileNumber.text = customerNumber
                }
            }

            dBinding.sendSms.setOnClickListener {
                if (dBinding.messageEd.text.isNullOrBlank()) {
                    Toast.makeText(ctx, "Sms Field Should not be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val message = dBinding.messageEd.text.toString()
                MainActivity.orderRepository.sendSmsOnline(ctx, Constants.selectedOutletId, customerId, message)
                val closeReceiver = object : BroadcastReceiver() {
                    override fun onReceive(p0: Context?, p1: Intent?) {
                        val dId = p1?.getIntExtra(Constants.ID_DIALOG, 0)
                        if (dId == Constants.ID_DIALOG_SEND_SMS) {
                            this@SMSDialog.dialog?.dismiss()
                        }
                    }
                }
                ctx.registerReceiver(closeReceiver, IntentFilter(Constants.CLOSE_DIALOG_BROADCAST))
            }
            dBinding.closeButton.setOnClickListener {
                this.dialog?.dismiss()
            }
            dBinding.closeButton2.setOnClickListener {
                this.dialog?.dismiss()
            }
            return dBinding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            super.onViewCreated(view, savedInstanceState)
        }
        companion object {
            fun getInstance(custId: Long, ctx: Context) : SMSDialog {
                val dialog = SMSDialog()
                dialog.customerId = custId
                dialog.ctx = ctx
                return dialog
            }
        }
    }
}