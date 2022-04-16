package com.tjcg.nentopos.dialog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.DialogCancelOrderBinding

class OrderCancelDialog : DialogFragment() {

    private lateinit var binding: DialogCancelOrderBinding
    private lateinit var ctx: Context
    private lateinit var order: OrdersEntity
    private var dId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCancelOrderBinding.inflate(
            inflater, container, false
        )
        binding.orderId.text = order.order_id.toString()
        binding.closeButton.setOnClickListener {
            this.dismiss()
        }
        binding.closeButton2.setOnClickListener {
            this.dismiss()
        }
        binding.confirm.setOnClickListener {
            if (binding.cancelReasonEt.text.isNullOrBlank()) {
                Toast.makeText(ctx, "Please Provide a reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dId = MainActivity.progressDialogRepository.getProgressDialog("Rejecting Order...")
            MainActivity.orderRepository.acceptRejectOrderOnline(
                ctx,
                order.outlet_id!!,
                order.order_id,
                order.order_status!!,
                Constants.REJECT_ORDER,
                0,
                binding.cancelReasonEt.text.toString()
            )
        }
        this.ctx.registerReceiver(
            closeDialogReceiver,
            IntentFilter(Constants.CLOSE_DIALOG_BROADCAST)
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private val closeDialogReceiver = object : BroadcastReceiver() {

        override fun onReceive(p0: Context?, p1: Intent?) {
            MainActivity.progressDialogRepository.dismissDialog(dId)
            this@OrderCancelDialog.dismiss()
        }

    }

    companion object {
        fun getInstance(ctx: Context, order: OrdersEntity): OrderCancelDialog {
            val dialog = OrderCancelDialog()
            dialog.ctx = ctx
            dialog.order = order
            return dialog
        }
    }
}