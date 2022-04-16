package com.tjcg.nentopos.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.databinding.DialogErrorBinding

class ErrorDialog(private val ctx: Context,private val message: String) : DialogFragment() {

    private lateinit var dBinding: DialogErrorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dBinding = DialogErrorBinding.inflate(LayoutInflater.from(ctx),
            container, false)
        dBinding.errorText.text = message
        dBinding.dismissBtn.setOnClickListener {
            this.dialog?.dismiss()
        }
        return dBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}