package com.tjcg.nentopos.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.databinding.DialogProgressBinding

class ProgressDialogMain(val text: String) : DialogFragment() {

    private lateinit var dBinding : DialogProgressBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dBinding = DialogProgressBinding.inflate(inflater, container, false)
        dBinding.textView6.text = text
        return dBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}