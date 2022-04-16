package com.tjcg.nentopos.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.api.ApiService
import com.tjcg.nentopos.data.CustomerData
import com.tjcg.nentopos.data.CustomerOffline
import com.tjcg.nentopos.data.CustomerTypeData

import com.tjcg.nentopos.databinding.DialogAddCustomerBinding
import com.tjcg.nentopos.databinding.DialogAlertBinding
import com.tjcg.nentopos.responses.AddCustomerResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.util.*

class AddCustomerDialog(val ctx: Context) : DialogFragment() {

    private lateinit var dBinding : DialogAddCustomerBinding
    private var selectedCustomerType = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dBinding = DialogAddCustomerBinding.inflate(inflater, container, false)
        CoroutineScope(Dispatchers.Main).launch {
            val customerTypeArray = MainActivity.mainRepository.getCustomerTypesAsync().await() as List<CustomerTypeData>
            dBinding.customerTypeSpinner.adapter = ArrayAdapter(ctx, R.layout.item_light_spinner, R.id.light_spinner_text, customerTypeArray)
            dBinding.customerTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    selectedCustomerType = customerTypeArray[p2].customerType
                }

                override fun onNothingSelected(p0: AdapterView<*>?) { }

            }
        }
        dBinding.closeButton.setOnClickListener {
            this.dialog?.dismiss()
        }
        dBinding.closeButton2.setOnClickListener {
            this.dialog?.dismiss()
        }
        dBinding.addCustomerBtn.setOnClickListener {
            if (dBinding.customerNameEt.text.isNullOrEmpty()) {
                dBinding.customerNameEt.error = "Name Required"
                return@setOnClickListener
            }
            if (dBinding.emailAddressEt.text.isNullOrEmpty()) {
                dBinding.emailAddressEt.error = "Email Address Required"
                return@setOnClickListener
            }
            if (dBinding.mobileNumberEt.text.isNullOrEmpty()) {
                dBinding.mobileNumberEt.error = "Phone Number Required"
                return@setOnClickListener
            }
            if (dBinding.addressEt.text.isNullOrEmpty()) {
                dBinding.addressEt.error = "Address required"
                return@setOnClickListener
            }
            if (MainActivity.isInternetAvailable(ctx)) {
                val dId = MainActivity.progressDialogRepository.getProgressDialog("Adding Customer ....")
                val userId = Constants.clientId
                val outletId = Constants.selectedOutletId
                val menuId = 63
                val customerName = dBinding.customerNameEt.text.toString()
                selectedCustomerType // - fetched for customer category
                val customerEmail = dBinding.emailAddressEt.text.toString()
                val countryCode = dBinding.countryCodePicker.selectedCountryCode
                val customerPhone = dBinding.mobileNumberEt.text.toString()
                val customerAddress = dBinding.addressEt.text.toString()
                val customerNote = dBinding.remarkEt.text.toString()
                Log.d("CustomerParameter", "auth: ${Constants.authorization}, " +
                        "userId: $userId, outlet: $outletId, menu: $menuId, custName: $customerName," +
                        "type: $selectedCustomerType, email: $customerEmail, country: $countryCode, " +
                        "phone: $customerPhone, address: $customerAddress, note: $customerNote")
                ApiService.apiService?.addCustomer(Constants.authorization, userId, outletId, menuId,
                customerName, selectedCustomerType, null, customerEmail, countryCode, customerPhone,
                customerAddress, customerNote)?.enqueue(object : retrofit2.Callback<AddCustomerResponse> {
                    @SuppressLint("SetTextI18n")
                    override fun onResponse(
                        call: Call<AddCustomerResponse>,
                        response: Response<AddCustomerResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                            // TODO: add customer to local database
                                val customers = response.body()?.customerData?.customerList
                            if (!customers.isNullOrEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    MainActivity.mainRepository.insertCustomerDataAsync(
                                        customers).await()
                                }
                                ctx.sendBroadcast(Intent(Constants.CUSTOMER_ADDED_BROADCAST))
                            }
                            var alertDialog : AlertDialog? = null
                            val builder = AlertDialog.Builder(ctx).apply {
                                val dBinding = DialogAlertBinding.inflate(LayoutInflater.from(ctx))
                                dBinding.infoText.text = "Customer Added successfully"
                                dBinding.closeBtn.setOnClickListener {
                                    alertDialog?.dismiss()
                                }
                                setView(dBinding.root)
                            }
                            alertDialog = builder.create()
                            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                            alertDialog.show()
                            this@AddCustomerDialog.dialog?.dismiss()
                        } else {
                            MainActivity.progressDialogRepository.showErrorDialog(
                                "Error Creating Customer : ${response.body()?.message}")
                        }
                        MainActivity.progressDialogRepository.dismissDialog(dId)
                    }

                    override fun onFailure(call: Call<AddCustomerResponse>, t: Throwable) {
                        MainActivity.progressDialogRepository.showErrorDialog(
                            "Error Creating Customer : ${t.message}")
                        MainActivity.progressDialogRepository.dismissDialog(dId)
                    }
                })
            } else {
                // do for offline ---
                val customerName = dBinding.customerNameEt.text.toString()
                selectedCustomerType // - fetched for customer category
                val customerEmail = dBinding.emailAddressEt.text.toString()
                val countryCode = dBinding.countryCodePicker.selectedCountryCode
                val customerPhone = dBinding.mobileNumberEt.text.toString()
                val customerAddress = dBinding.addressEt.text.toString()
                val customerNote = dBinding.remarkEt.text.toString()
                val tmpId = Calendar.getInstance().timeInMillis
                val customerData = CustomerData().apply {
                    this.customerId = tmpId
                    this.customerName = customerName
                    this.customerEmail = customerEmail
                    this.customerAddress = customerAddress
                    this.countryCode = countryCode
                    this.customerPhone = customerPhone
                    this.note = customerNote
                    this.customerCategory = selectedCustomerType.toString()
                }
                CoroutineScope(Dispatchers.Main).launch {
                    MainActivity.mainRepository.insertOneCustomerDataAsync(
                        customerData).await()
                    val offlineCustomer = CustomerOffline().apply {
                        this.customerTmpId = tmpId
                        this.customerName = customerName
                        this.customerEmail = customerEmail
                        this.customerAddress = customerAddress
                        this.countryCode = countryCode
                        this.customerPhone = customerPhone
                        this.note = customerNote
                        this.customerCategory = selectedCustomerType.toString()
                    }
                    MainActivity.mainRepository.insertOneOfflineCustomerAsync(offlineCustomer).await()
                    ctx.sendBroadcast(Intent(Constants.CUSTOMER_ADDED_BROADCAST))
                    this@AddCustomerDialog.dialog?.dismiss()
                }
            }
        }
        return dBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        super.onViewCreated(view, savedInstanceState)
    }

}