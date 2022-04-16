package com.tjcg.nentopos.dialog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.DialogAcceptOrderBinding

class OrderAcceptDialog : DialogFragment() {
    private lateinit var dBinding : DialogAcceptOrderBinding
    private lateinit var ctx: Context
    private lateinit var order: OrdersEntity
    private val minCookingTime = 5
    private var cookingTimeInMinutes : Int = minCookingTime
    private var progressId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dBinding = DialogAcceptOrderBinding.inflate(
            inflater, container, false)
        dBinding.btnclose.setOnClickListener {
            this.dismiss()
        }
        dBinding.confirmOrderBtn.setOnClickListener {
            progressId = MainActivity.progressDialogRepository.getProgressDialog("Accepting Order...")
            MainActivity.orderRepository.acceptRejectOrderOnline(ctx, order.outlet_id ?: 0, order.order_id,
                Constants.ORDER_STATUS_PROCESSING, Constants.ACCEPT_ORDER, cookingTimeInMinutes, "null")
        }
        dBinding.closeButton.setOnClickListener {
            this.dismiss()
        }
        dBinding.seekBar1.max = 120
        dBinding.seekBar1.progress = 5
        dBinding.seekBar1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                cookingTimeInMinutes = if (p0?.progress != null && p0.progress <= minCookingTime) {
                    minCookingTime
                } else {
                    p0?.progress!!
                }
                dBinding.cookingTime.text = cookingTimeInMinutes.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) { }

        })
        this.ctx.registerReceiver(closeDialogReceiver, IntentFilter(Constants.CLOSE_DIALOG_BROADCAST))
        return dBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private val closeDialogReceiver = object : BroadcastReceiver() {

        override fun onReceive(p0: Context?, p1: Intent?) {
            val id = p1?.getIntExtra(Constants.ID_DIALOG, 0)
            if (id == Constants.ID_DIALOG_ACCEPT_ORDER) {
                MainActivity.progressDialogRepository.dismissDialog(progressId)
                this@OrderAcceptDialog.dismiss()
            }
        }

    }

    companion object {
        fun getInstance(ctx: Context, order: OrdersEntity) : OrderAcceptDialog {
            val dialog = OrderAcceptDialog()
            dialog.ctx = ctx
            dialog.order = order
            return dialog
        }
    }
}