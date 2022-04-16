package com.tjcg.nentopos.dialog

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.tjcg.nentopos.databinding.DialogAlertBinding
import java.lang.Exception

class ProgressDialogRepository(private val ctx: Context, private val fManager: FragmentManager) {

    private var currentId = 0
    private lateinit var textView: TextView
    private lateinit var smallLayout : LinearLayout
    private var syncDialog = SyncDialog.getDialog(ctx)

    private val dialogs = ArrayList<ProgressDialogMain>()
    fun getProgressDialog(text:String) : Int {
        val pDialog = ProgressDialogMain(text)
        pDialog.isCancelable = false
        pDialog.show(fManager, "$currentId")
        dialogs.add(currentId, pDialog)
        currentId += 1
        return (currentId - 1)
    }

    fun dismissDialog(id: Int) {
        try {
            val pDialog = dialogs.get(id)
            if (pDialog.isVisible) {
                pDialog.dismiss()
            }
        } catch (e: Exception) {
            Log.e("DialogException", "Invalid Broadcast Received")
        }

    }

    fun setSmallProgressBar(textView: TextView, layout: LinearLayout) {
        this.textView = textView
        smallLayout = layout
    }

    fun showSmallProgressBar(text: String) {
        textView.text = text
        smallLayout.visibility = View.VISIBLE
    }

    fun closeSmallProgressBar() {
        smallLayout.visibility = View.GONE
    }

    fun showErrorDialog(error: String) {
        val eDialog = ErrorDialog(ctx, error)
        eDialog.show(fManager, "error")
    }

    fun showSyncDialog() {
        syncDialog = SyncDialog.getDialog(ctx)
        syncDialog.show(fManager, "$currentId:Sync")
    }

    fun dismissSyncDialog() {
        if (syncDialog.isVisible)
            syncDialog.dismiss()
    }

    fun showAlertDialog(msg : String) {
        var aDialog : AlertDialog? = null
        val builder = AlertDialog.Builder(ctx).apply {
            val aBinding = DialogAlertBinding.inflate(
                LayoutInflater.from(ctx))
            aBinding.infoText.text = msg
            aBinding.closeBtn.setOnClickListener {
                aDialog?.dismiss()
            }
            setView(aBinding.root)
        }
        aDialog = builder.create()
        aDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        aDialog?.show()
    }
}