package com.tjcg.nentopos.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.api.ApiService
import com.tjcg.nentopos.databinding.DialogSendEmailBinding
import com.tjcg.nentopos.fragments.InvoiceFragment
import com.tjcg.nentopos.responses.SimpleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SendEmailDialog(val orderId: Long) : DialogFragment() {

    private lateinit var binding : DialogSendEmailBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSendEmailBinding.inflate(inflater, container, false)
        binding.radioEmail.setOnCheckedChangeListener { radioGroup, i ->
            if (i == R.id.emailYesRadio) {
                binding.emailEditText.visibility = View.VISIBLE
                binding.sendEmailBtn.visibility = View.VISIBLE
            } else {
                binding.emailEditText.visibility = View.INVISIBLE
                binding.sendEmailBtn.visibility = View.GONE
            }
        }
        binding.sendEmailBtn.setOnClickListener {
            if (binding.emailEditText.text.isNullOrBlank()) {
                binding.emailEditText.error = "Please enter email here"
                return@setOnClickListener
            }
            ApiService.apiService?.sendOrderEmail(Constants.selectedOutletId,
                orderId.toInt(), binding.emailEditText.text.toString(), Constants.authorization)
                    ?.enqueue(object : Callback<SimpleResponse> {
                        override fun onResponse(
                            call: Call<SimpleResponse>,
                            response: Response<SimpleResponse>
                        ) {
                            if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                                this@SendEmailDialog.dialog?.dismiss()
                            } else {
                                MainActivity.progressDialogRepository.showErrorDialog(
                                    "Error Sending Email : ${response.body()?.message}"
                                )
                                this@SendEmailDialog.dialog?.dismiss()
                            }
                        }

                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                            MainActivity.progressDialogRepository.showErrorDialog(
                                "Error Sending Email : ${t.message}"
                            )
                            this@SendEmailDialog.dialog?.dismiss()
                        }

                    })
        }
        binding.printBtn.setOnClickListener {
            InvoiceFragment.orderId = orderId
            findNavController().navigate(R.id.navigation_invoice)
            this.dialog?.dismiss()
        }
        binding.skipBtn.setOnClickListener {
            this.dialog?.dismiss()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        super.onViewCreated(view, savedInstanceState)
    }

}