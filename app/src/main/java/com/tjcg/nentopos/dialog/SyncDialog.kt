package com.tjcg.nentopos.dialog

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.databinding.DialogSyncBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncDialog : DialogFragment() {

    lateinit var syncBinding : DialogSyncBinding
    private var pDialogId = -1
    lateinit var ctx: Context
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isShowing = true
        isCancelable = false
        syncBinding = DialogSyncBinding.inflate(inflater, container, false)
        val synCompleteReceiver = object: BroadcastReceiver(){
            @SuppressLint("SetTextI18n")
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.d("newSync", "broadcast received: ${p1?.action}")
                if (p1?.action == Constants.SYNC_COMPLETE_BROADCAST) {
                    val success = p1.getBooleanExtra(Constants.IS_SUCCESS, false)
                    Log.d("newSync", "Success : $success")
                    if (success) {
                        syncBinding.synText.text = "Sync Completed Successfully"
                        syncBinding.synNowBtn.visibility = View.GONE
                        MainActivity.mainSharedPreferences.edit()
                            .putBoolean(Constants.PREF_IS_SYNC_REQUIRES, false).apply()
                        MainActivity.orderRepository.syncRequired = false
                        MainActivity.orderRepository.markAllOrdersAsSynchronized()
                    } else {
                        syncBinding.synText.text = "An Error Occurred, Please try again"
                    }
                    MainActivity.progressDialogRepository.dismissDialog(pDialogId)
                    ctx.unregisterReceiver(this)
                }
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            val offlineOrders = MainActivity.orderRepository.getOffline2OrdersAsync(Constants.selectedOutletId).await()
            Log.d("OfflineOrders", "Avaialble: ${offlineOrders?.size}")
        }
        syncBinding.closeBtn.setOnClickListener {
            isShowing = false
            this.dismiss()
        }
        syncBinding.synNowBtn.setOnClickListener {
            MainActivity.orderRepository.startSync(ctx)
            MainActivity.orderRepository.startUploadingOfflineOrders(ctx)
            pDialogId = MainActivity.progressDialogRepository.getProgressDialog("Sync in progress")
            ctx.registerReceiver(synCompleteReceiver, IntentFilter(Constants.SYNC_COMPLETE_BROADCAST))
        }
        return syncBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        var isShowing = false
        fun getDialog(ctx: Context) : SyncDialog {
            val dialog = SyncDialog()
            dialog.ctx = ctx
            return dialog
        }
    }
}