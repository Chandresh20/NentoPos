package com.tjcg.nentopos.adapters

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.DialogCompleteOrderBinding
import com.tjcg.nentopos.databinding.ItemAllOrderBinding
import com.tjcg.nentopos.dialog.OrderAcceptDialog
import com.tjcg.nentopos.dialog.OrderCancelDialog
import com.tjcg.nentopos.dialog.PaymentDialog
import com.tjcg.nentopos.fragments.InvoiceFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrdersAdapter(val ctx: Context, val list: List<OrdersEntity>, private val navController: NavController) :
    RecyclerView.Adapter<OrdersAdapter.MyHolder>() {

    private val mainScope = CoroutineScope(Dispatchers.Main)
    inner class MyHolder(val binding: ItemAllOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersAdapter.MyHolder {
        return MyHolder(ItemAllOrderBinding.inflate(LayoutInflater.from(ctx), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: OrdersAdapter.MyHolder, position: Int) {
        val onlineOrderData = list[position]
        holder.binding.orderId.text = onlineOrderData.order_id.toString()
        holder.binding.gridLayout.removeView(holder.binding.editOrder)
        if (onlineOrderData.futureOrderType == Constants.FUTURE_ORDER_FAR) {
            holder.binding.gridLayout.removeView(holder.binding.editOrder)
            holder.binding.gridLayout.removeView(holder.binding.acceptOrder)
            holder.binding.gridLayout.removeView(holder.binding.cancelOrder)
            holder.binding.gridLayout.removeView(holder.binding.proccedPayment)
        }
   /*     when (onlineOrderData.futureOrderType) {
            Constants.FUTURE_ORDER_NULL -> {
                holder.binding.orderDateTime.setTextColor(Color.BLUE)
            }
            Constants.FUTURE_ORDER_NEAR -> {
                holder.binding.orderDateTime.setTextColor(Color.RED)
            }
            Constants.FUTURE_ORDER_FAR -> {
                holder.binding.orderDateTime.setTextColor(Color.GREEN)
                holder.binding.cancelOrder.visibility = View.GONE
                holder.binding.acceptOrder.visibility = View.GONE
                holder.binding.proccedPayment.visibility = View.GONE
            }
            else -> {
                holder.binding.orderDateTime.setTextColor(Color.MAGENTA)
            }
        }  */
        holder.binding.orderDateTime.text = "${onlineOrderData.order_date} ${onlineOrderData.order_time}"
        holder.binding.futureOrderDate.text = if (onlineOrderData.future_order_date.isNullOrEmpty()
            && onlineOrderData.future_order_time.isNullOrEmpty()) {
            "NA"
        } else {
            onlineOrderData.future_order_date + " " + onlineOrderData.future_order_time
        }
        mainScope.launch {
            val customerData = MainActivity.mainRepository
                .getOneCustomerDataAsync(onlineOrderData.customer_id ?: 0).await()
            holder.binding.customerName.text = if (customerData != null) {
                customerData.customerName
            } else {
                "NA"
            }
            holder.binding.tableName.text = if (onlineOrderData.table_no == null) {
                "NA"
            } else {
                var tableName = "NA"
                val tableData = MainActivity.mainRepository.getOneTableDataAsync(onlineOrderData.table_no ?: 0).await()
                if (tableData!= null) {
                    tableName = (tableData.tableName ?: "NA")
                }
                tableName
            }
            holder.binding.serverName.text = if (onlineOrderData.waiter_id == null) {
                ctx.resources.getString(R.string.na)
            } else {
                val subUser = MainActivity.mainRepository.getOneSubUserDetailAsync(onlineOrderData.waiter_id!!).await()
                val waiterName= if (subUser != null) {
                    "${subUser.firstname} ${subUser.lastname}"
                } else {
                    "NA"
                }
                waiterName
            }
        }


        holder.binding.orderStatus.text = Constants.getOrderStatus(onlineOrderData.order_status ?: 11)
//        holder.binding.paymentStatus.text = onlineOrderData.pis_payment_received
        holder.binding.totalAmount.text = "$ " + onlineOrderData.totalamount
//        holder.binding.editOrder.visibility = View.VISIBLE
//        holder.binding.acceptOrder.visibility = View.VISIBLE
//        holder.binding.cancelOrder.visibility = View.VISIBLE
 //       holder.binding.proccedPayment.visibility = View.VISIBLE
        holder.binding.posInvoice.visibility = View.VISIBLE
        holder.binding.printInvoice.visibility = View.VISIBLE
        Log.d("AllOrderAdapter", "Order: ${onlineOrderData.order_id}, payment: ${onlineOrderData.bill_status}")
        holder.binding.paymentStatus.text = when (onlineOrderData.billInfo?.billStatus) {
            Constants.STATUS_NOT_PAID -> ctx.resources.getString(R.string.not_paid)
            Constants.STATUS_PAID -> ctx.resources.getString(R.string.paid)
            else -> ctx.resources.getString(R.string.unknown)
        }
        holder.binding.accountNumber.text = if (onlineOrderData.account_number.isNullOrBlank()) {
            ctx.resources.getString(R.string.na)
        } else {
            onlineOrderData.account_number
        }
        holder.binding.posInvoice.setOnClickListener {
            if (Constants.databaseBusy) {
                var dialog : AlertDialog? = null
                val builder = AlertDialog.Builder(ctx).apply {
                    setMessage("Database is busy, Please try again after few minutes")
                    setPositiveButton("Dismiss") { _, _ -> }
                }
                dialog = builder.create()
                dialog.show()
                return@setOnClickListener
            }
            InvoiceFragment.orderId = onlineOrderData.order_id
            navController.navigate(R.id.navigation_invoice)
        }
        holder.binding.printInvoice.setOnClickListener {

        }
        when (onlineOrderData.order_status) {
            Constants.ORDER_STATUS_CANCELED -> {
                holder.binding.gridLayout.removeView(holder.binding.editOrder)
                holder.binding.gridLayout.removeView(holder.binding.acceptOrder)
                holder.binding.gridLayout.removeView(holder.binding.cancelOrder)
                holder.binding.gridLayout.removeView(holder.binding.proccedPayment)
            }
            Constants.ORDER_STATUS_SERVED -> {
                holder.binding.gridLayout.removeView(holder.binding.editOrder)
                holder.binding.gridLayout.removeView(holder.binding.acceptOrder)
                holder.binding.gridLayout.removeView(holder.binding.cancelOrder)
                if (onlineOrderData.billInfo?.billStatus == 1) {
                    holder.binding.gridLayout.removeView(holder.binding.proccedPayment)
                }
            }
            Constants.ORDER_STATUS_PENDING -> {
                holder.binding.gridLayout.removeView(holder.binding.proccedPayment)
                if (onlineOrderData.futureOrderType == Constants.FUTURE_ORDER_NULL ||
                        onlineOrderData.futureOrderType == Constants.FUTURE_ORDER_NEAR) {
                    holder.binding.acceptOrder.visibility = View.VISIBLE
                    holder.binding.cancelOrder.visibility = View.VISIBLE
                }
         /*       holder.binding.acceptOrder.visibility = View.GONE
                holder.binding.cancelOrder.visibility = View.GONE
                holder.binding.proccedPayment.visibility = View.GONE
                holder.binding.editOrder.visibility = View.GONE
                if (onlineOrderData.pis_frontend_order == "1") {
                    holder.binding.acceptOrder.visibility = View.VISIBLE
                    holder.binding.cancelOrder.visibility = View.VISIBLE
                }
                else{
                    holder.binding.acceptOrder.visibility = View.GONE
                }  */
            }
            Constants.ORDER_STATUS_PROCESSING -> {
       //         holder.binding.acceptOrder.visibility = View.GONE
                holder.binding.gridLayout.removeView(holder.binding.acceptOrder)
                holder.binding.gridLayout.removeView(holder.binding.editOrder)
           /*     if (onlineOrderData.billInfo?.billStatus == 1) {
                    holder.binding.gridLayout.removeView(holder.binding.cancelOrder)
          //          holder.binding.editOrder.visibility = View.GONE
         //           holder.binding.cancelOrder.visibility = View.GONE
                }  */
            }
            Constants.ORDER_STATUS_READY -> {
                holder.binding.gridLayout.removeView(holder.binding.acceptOrder)
      //          holder.binding.gridLayout.removeView(holder.binding.cancelOrder)
            }
        }
        holder.binding.editOrder.setOnClickListener {
            //    ctx?.setFragment(PosTabFragment.newInstance())
        }
        holder.binding.acceptOrder.setOnClickListener {
            val dialog = OrderAcceptDialog.getInstance(ctx, onlineOrderData)
            dialog.show(MainActivity.fManager, "dialog")
        }
        holder.binding.cancelOrder.setOnClickListener {
            val dialog = OrderCancelDialog.getInstance(ctx, onlineOrderData)
            dialog.show(MainActivity.fManager, "cancelDialog")
        }
        holder.binding.proccedPayment.setOnClickListener {
            if (onlineOrderData.billInfo?.billStatus == 1) {
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
                            (onlineOrderData.customerpaid ?: "0").toFloat(), onlineOrderData.order_id,
                            onlineOrderData.outlet_id ?: Constants.selectedOutletId)
                    }
                    setView(dBinding.root)
                }
                alertDialog = builder.create()
                alertDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
                                    InvoiceFragment.orderId = onlineOrderData.order_id
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
            paymentDialog = PaymentDialog(ctx, onlineOrderData.totalamount ?: 0f,
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
        }
    }

    override fun getItemCount(): Int = list.size

    // codes for printInvoice
 /*   fun getInvoiceData(outletId: String, orderId: String) {
        val orderData = OrderRepository1.getSingleOrder(outletId, orderId)
        val itemInfo = orderData.itemsInfo
        val billInfo = orderData.billInfo
        val customerInfo = OrderRepository1.getCustomer((orderData.customer_id ?: "0"))
        val addOns = orderData.addOns
    }  */

}