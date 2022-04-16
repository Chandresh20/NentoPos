package com.tjcg.nentopos.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.databinding.DialogProgressBinding

class ProgressDialog : DialogFragment() {

    private lateinit var dBinding: DialogProgressBinding
    private lateinit var ctx : Context
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dBinding = DialogProgressBinding.inflate(
            LayoutInflater.from(ctx), container, false)
        MainActivity.mainViewModel.progressText.observe(ctx as AppCompatActivity, {
            dBinding.textView6.text = it ?: ctx.getString(R.string.pleaseWait)
        })
        return dBinding.root
    }


    companion object {

        fun getInstance(ctx : Context) : ProgressDialog {
            val dialog = ProgressDialog()
            dialog.ctx = ctx
            return dialog
        }
    }
}