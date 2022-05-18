package com.tjcg.nentopos.fragments

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.data.DiscountData
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.data.ProductTax
import net.posprinter.posprinterface.IMyBinder
import com.tjcg.nentopos.databinding.FragmentInvoiceBinding
import com.tjcg.nentopos.databinding.ItemInvoiceMainBinding
import com.tjcg.nentopos.databinding.ItemInvoiceTaxBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.posprinter.posprinterface.TaskCallback
import net.posprinter.service.PosprinterService
import net.posprinter.utils.DataForSendToPrinterPos80
import net.posprinter.utils.StringUtils
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val PRINT_SUBTOTAL = "subtotal"
const val PRINT_DISCOUNT = "discount"
const val PRINT_TIP = "tip"
const val PRINT_STORE_CHARGE = "store_charge"
const val PRINT_GRAND_TOTAL = "grand_total"
const val PRINT_CUSTOMER_PAID = "customer_paid"
const val PRINT_CHANGE_DUE = "change_due"
const val PRINT_CURRENCY = "$"

class InvoiceFragment : Fragment() {

    private lateinit var binding : FragmentInvoiceBinding
    private lateinit var myBinder : IMyBinder
    private var isPrinterConnected = false
    private lateinit var ctx: Context
    private var subTotal = 0f
    private var grandTotal = 0f
    private lateinit var prefixStr : String
    private val manageTaxes = ManageTaxes()
    private val itemPrints = ArrayList<ItemPrint>()
    private val taxPrints = HashMap<String,Float>()
    private val amountPrints = HashMap<String, Float>()
    private var reloadCustomerData = false
    private var mainDiscountPrec = 0f
    private val mServiceConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            myBinder = p1 as IMyBinder
        }
        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.e("myBinder", "disconnect")
        }
    }

    companion object {
        var directPrint = false
        var orderId: Long = -1
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = FragmentInvoiceBinding.inflate(
            inflater, container, false)
        MainActivity.expandActionBar()
        val intent = Intent((ctx as AppCompatActivity), PosprinterService::class.java)
        (ctx).bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        prefixStr = getString(R.string.amount_prefix)
        binding.printBtn.setOnClickListener {

            // test printing value
            /*       var str = ""
                   for (tax in taxPrints) {
                       str += "${tax.key} : ${tax.value}"
                   }
                   Toast.makeText(ctx, str, Toast.LENGTH_LONG).show()
                   str += "Subtotal : $PRINT_CURRENCY ${amountPrints[PRINT_SUBTOTAL]}\n"
                   str += "Discount : $PRINT_CURRENCY ${amountPrints[PRINT_DISCOUNT]}\n"
                   str += "Tip : $PRINT_CURRENCY ${amountPrints[PRINT_TIP]}\n"
                   str += "StoreCharge : $PRINT_CURRENCY ${amountPrints[PRINT_STORE_CHARGE]}\n"
                   str += "GrandTotal : $PRINT_CURRENCY ${amountPrints[PRINT_GRAND_TOTAL]}\n"
                   str += "Customer Paid : $PRINT_CURRENCY ${amountPrints[PRINT_CUSTOMER_PAID]}\n"
                   str += "Change Due : $PRINT_CURRENCY ${amountPrints[PRINT_CHANGE_DUE]}\n"

                   Toast.makeText(ctx, str, Toast.LENGTH_LONG).show()  */


            initializePrinting()
        }
        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        CoroutineScope(Dispatchers.Main).launch {
            val order : OrdersEntity? = MainActivity.orderRepository
                .getSingleOrderAsync(Constants.selectedOutletId, orderId).await()
            if (order == null) {
                Toast.makeText(ctx, "Data not found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            binding.textViewInvoiceno.text = "Invoice No.: ${order.order_id}"
            //       binding.textViewStatus.text = "Order Status: ${order.order_status}"
            binding.textViewBilldate.text = "Billing Date: ${MainActivity.orderRepository.getTodayString()}"
            val orderType = MainActivity.mainRepository.getMenuNameAsync(order.menu_id ?: 0).await()
            binding.textViewOrdertype.text = if (orderType == null) {
                "Order Type : NA"
            } else {
                "Order Type : $orderType"
            }
            amountPrints[PRINT_CUSTOMER_PAID] = (if (order.customerpaid.isNullOrBlank()) "0" else
                order.customerpaid)?.toFloat() ?: 0f
            binding.textVieCustomerPaidAmount.text =
                "$prefixStr "+ amountPrints[PRINT_CUSTOMER_PAID]?.format()

            // bill from
            val outletDetails = MainActivity.mainRepository.getOutletDataAsync(Constants.selectedOutletId).await()
            if (outletDetails != null) {
                binding.textViewCustnamefrom.text = "${outletDetails.outletName}"
                binding.textViewAddressfrom.text = "${outletDetails.address}"
                binding.textViewMobnofrom.text = "Mobile: ${outletDetails.phoneNo}"
                binding.textViewEmailfrom.text = "Email id: ${outletDetails.email}"
                val hstNumber = MainActivity.mainSharedPreferences.getString(Constants.PREF_HST_NUMBER, "NA")
                binding.textViewHstFrom.text = "HST No. : $hstNumber"
            }

            // billing to
            setUpBillingToInfo(order)

            //testing data
            val itemInfo = order.itemsInfo
            if (!itemInfo.isNullOrEmpty()) {
                // get Item Name, variant name and modifiers
                binding.rvInvoiceItem.layoutManager = LinearLayoutManager(ctx)
                binding.rvInvoiceItem.adapter = MainListAdapter(ctx, itemInfo)
            }
            if (!order.addOns.isNullOrEmpty()) {
                binding.rvAddonItems.layoutManager = LinearLayoutManager(ctx)
                binding.rvAddonItems.adapter = AddOnAdapter(ctx, order.addOns!!)
            }
            mainDiscountPrec = if (order.discount.isNullOrBlank()) 0f else order.discount?.toFloat()!!
            Handler(ctx.mainLooper).postDelayed( {
                val taxArrayList = manageTaxes.taxList
                var totalTax = 0f
                if (!taxArrayList.isNullOrEmpty()) {
                    for (tax in taxArrayList.iterator()) {
                        totalTax += (manageTaxes.getTaxValue(tax.id) ?: 0f)
                    }
                }

                val tipAmount = if (order.tip_value.isNullOrBlank()) 0f else order.tip_value?.toFloat()!!
                totalTax += tipAmount
                val mainDiscount = (subTotal * mainDiscountPrec /100)
                totalTax -= mainDiscount
                val storeCharge = if (order.billInfo?.serviceCharge.isNullOrBlank()) {
                    0f
                } else {
                    order.billInfo?.serviceCharge?.toFloat()
                }
                binding.textViewDiscount.text = "$prefixStr ${mainDiscount.format()}"
                binding.textVieTip.text = "$prefixStr " + tipAmount.format()
                binding.textVieStoreChargePackingExtra.text = "$prefixStr ${storeCharge?.format()}"
                totalTax += storeCharge ?: 0f
                amountPrints[PRINT_SUBTOTAL] = subTotal
                amountPrints[PRINT_DISCOUNT] = mainDiscount
                amountPrints[PRINT_TIP] = tipAmount
                amountPrints[PRINT_STORE_CHARGE] = storeCharge ?: 0f
                updateGrandTotal(totalTax)
            }, 1000)
        }
        if (directPrint) {
            val prepareDialogId = MainActivity.progressDialogRepository.getProgressDialog(
                "Please Wait")
            Handler(Looper.getMainLooper()).postDelayed( {
                MainActivity.progressDialogRepository.dismissDialog(prepareDialogId)
                binding.printBtn.performClick()
                directPrint = false
            }, 3000)
        }
        return binding.root
    }

    fun updateTaxesData() {
        val taxArrayList = ArrayList<TaxObject>()
        if (!manageTaxes.taxList.isNullOrEmpty()) {
            for (tax in manageTaxes.taxList) {
                val taxValue = manageTaxes.getTaxValue(tax.id)
                if ((taxValue  ?: 0f) > 0f) {
                    taxArrayList.add(
                        TaxObject((tax.taxLabel ?: "NO Label"), (tax.taxPercentage ?: "0"),
                            tax.taxType ?: "NA", MainActivity.orderRepository.roundFloatValue(taxValue ?: 0f)))
                }
            }
            binding.rvTax.layoutManager = LinearLayoutManager(ctx)
            binding.rvTax.adapter = TaxListAdapter(ctx, taxArrayList)
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateGrandTotal(num: Float) {
        grandTotal += num
        amountPrints[PRINT_GRAND_TOTAL] = grandTotal
        val changeDue = (amountPrints[PRINT_CUSTOMER_PAID] ?: 0f) - grandTotal
        binding.textVieChangedue.text = "$prefixStr ${changeDue.format()}"
        amountPrints[PRINT_CHANGE_DUE] = changeDue
        binding.textVieGrandTotal.text = "$prefixStr ${grandTotal.format()}"
    }

    @SuppressLint("SetTextI18n")
    private suspend fun setUpBillingToInfo(order : OrdersEntity) {
        if (order.customer_id == null || (order.customer_id ?: 0)  <= 0) {
            binding.textViewCustname.text = "Guest Customer"
            binding.textViewMobno.visibility = View.GONE
            binding.textViewEmail.visibility = View.GONE
            binding.textViewAddress1.visibility = View.GONE
            binding.textViewCustomerNote.visibility = View.GONE
            binding.noteCaption.visibility = View.GONE
            return
        }
        val customer =
            MainActivity.mainRepository.getOneCustomerDataAsync(order.customer_id ?: 0).await()
        if (customer != null) {
            val nameText = "<b>Customer Name: </b> ${customer.customerName}"
            binding.textViewCustname.text = HtmlCompat.fromHtml(nameText, FROM_HTML_MODE_LEGACY)
            binding.textViewMobno.text = if (customer.customerPhone.isNullOrBlank()) {
                ""
            } else {
                "Mobile: + (${customer.countryCode ?: ""}) "+
                        MainActivity.formatPhone(customer.customerPhone ?: "0000")
            }
            binding.textViewEmail.text = if (customer.customerEmail.isNullOrBlank()) {
                ""
            } else {
                "Email: ${customer.customerEmail}"
            }
            binding.textViewAddress1.text = if (customer.customerAddress.isNullOrBlank()) {
                ""
            } else {
                "Address: ${customer.customerAddress}"
            }
//            binding.textViewAddress2.text = "Address2: Pending"
            binding.textViewCustomerNote.text = if (order.customer_note.isNullOrBlank()) {
                binding.customerNoteLayout.visibility = View.GONE
                ""
            } else {
                binding.customerNoteLayout.visibility = View.VISIBLE
                "${order.customer_note}"
            }
        } else {
            if (!reloadCustomerData) {
                MainActivity.orderViewModel.newCustomersData.observe(
                    ctx as MainActivity, {
                        CoroutineScope(Dispatchers.Main).launch {
                            setUpBillingToInfo(order)
                        }
                    })
                val outletData = MainActivity.mainRepository.getOutletDataAsync(Constants.selectedOutletId).await()
                if (outletData != null) {
                    MainActivity.mainRepository.loadCustomerData(
                        ctx, outletData.outletId, MainActivity.deviceID,
                        1, true
                    )
                }
                reloadCustomerData = true
            }
        }
    }

    class SubMod(val name:String, val price:Float, val weight:Float)

    private fun initializePrinting() {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("com.tjcg.nentopos",Context.MODE_PRIVATE)

        val btAddress: String = sharedPreferences.getString("address", "").toString()

        if (btAddress.isBlank()) {
            Toast.makeText(ctx.applicationContext, ctx.getString(R.string.con_failed), Toast.LENGTH_SHORT).show()
        } else {
            myBinder.ConnectBtPort(btAddress, object : TaskCallback {
                override fun OnSucceed() {
                    isPrinterConnected = true
                    startPrinting()
                }
                override fun OnFailed() {
                    isPrinterConnected = false
                    Toast.makeText(ctx, ctx.getString(R.string.con_failed), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun startPrinting() {
        if (isPrinterConnected) {
            myBinder.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {
                    Toast.makeText(ctx, ctx.getString(R.string.con_success), Toast.LENGTH_SHORT).show()
                }
                override fun OnFailed() {
                    Toast.makeText(ctx, ctx.getString(R.string.con_failed), Toast.LENGTH_SHORT).show()
                }
            }) {
                generatePrintData()
            }
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.connect_first), Toast.LENGTH_SHORT).show()
        }
    }

    private fun generatePrintData() : java.util.ArrayList<ByteArray> {
        val list : ArrayList<ByteArray> = ArrayList()
        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectAlignment(1))
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        try{
            list.add(StringUtils.strTobytes(binding.textViewCustnamefrom.text.toString()))
        }catch (ex : Exception){
            list.add(StringUtils.strTobytes("NULL"))
        }
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        try{
            list.add(StringUtils.strTobytes(binding.textViewAddressfrom.text.toString()))
        }catch (ex : Exception){
            list.add(StringUtils.strTobytes("NULL"))
        }
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        try{
            list.add(StringUtils.strTobytes(binding.textViewMobnofrom.text.toString()))
        }catch (ex : Exception){
            list.add(StringUtils.strTobytes("NULL"))
        }
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add(StringUtils.strTobytes("================================================"))
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        //generate customer info
        try{
            list.add(StringUtils.strTobytes(binding.textViewCustname.text.toString()))
        }catch (ex : Exception){
            list.add(StringUtils.strTobytes("NULL"))
        }
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        try{
            list.add(StringUtils.strTobytes(binding.textViewAddress1.text.toString() + " | " + binding.textViewMobno.text.toString() + ""))
        }catch (ex : Exception){
            list.add(StringUtils.strTobytes("NULL" + " | " + "NULL" + ""))
        }
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        try{
            list.add(StringUtils.strTobytes(binding.textViewBilldate.text.toString()))
        }catch (ex : Exception){
            list.add(StringUtils.strTobytes("Date: NULL" ))
        }
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        val fmt1 = Formatter()
        fmt1.format("%-3s %-25s %-8s %9s\n", "Q", "Item", "Price", "Total")
        fmt1.format("%-48s\n", "================================================")

        for (item in itemPrints) {
            val res = splitString(item.label, 25)
            for (i in 0 until res.size) {
                if (i == 0) {
                    fmt1.format("%-3s %-25s %-8s %9s\n", item.qty.toString(), res[i].trim(), item.price.toString(), item.total.toString())
                } else {
                    fmt1.format("%-3s %-25s %-8s %9s\n", "", res[i].trim(), "", "")
                }
            }
        }

        fmt1.format("%-48s\n", "================================================")
        fmt1.format("%-37s %10s\n", "Subtotal", "$PRINT_CURRENCY ${amountPrints[PRINT_SUBTOTAL]?.format()}")
        fmt1.format("%-37s %10s\n", "Discount", "$PRINT_CURRENCY ${amountPrints[PRINT_DISCOUNT]?.format()}")

        // tax print is here
        val taxEntries = taxPrints.entries
        for (entry in taxEntries) {
            fmt1.format("%-37s %10s\n", entry.key, "$PRINT_CURRENCY " + entry.value.format())
        }

        fmt1.format("%-37s %10s\n", "Tip", "$PRINT_CURRENCY ${amountPrints[PRINT_TIP]?.format()}")
        fmt1.format("%-48s\n", "================================================")
        fmt1.format("%-37s %10s\n", "Grand Total", "$PRINT_CURRENCY ${amountPrints[PRINT_GRAND_TOTAL]?.format()}")
        fmt1.format("%-48s\n", "================================================")
        fmt1.format("%-37s %10s\n", "Customer Paid Amount", "$PRINT_CURRENCY ${amountPrints[PRINT_CUSTOMER_PAID]?.format()}")
        fmt1.format("%-37s %10s\n", "Change due", "$PRINT_CURRENCY ${amountPrints[PRINT_CHANGE_DUE]?.format()}")
        fmt1.format("%-48s\n", "================================================")
        list.add(StringUtils.strTobytes(fmt1.toString()))
        return list
    }

    private fun splitString(msg: String?, lineSize: Int): java.util.ArrayList<String> {
        val res = java.util.ArrayList<String>()
        val p: Pattern = Pattern.compile("\\b.{1," + (lineSize - 1) + "}\\b\\W?")
        val m: Matcher = p.matcher(msg.toString())
        while (m.find()) {
            res.add(m.group())
        }
        return res
    }

    private fun removeIncluded3(subModList: ArrayList<SubMod>, included: Int) : Float {
        val list = subModList.sortedWith(compareBy({it.price}, {it.weight}))
        var payableModifierPrice = 0f
        var freeModifier1 = included.toFloat()

        for (product in list) {
            freeModifier1 -= product.weight
            if (freeModifier1 >= 0) {
                Log.d("Modifier0", "do nothing with ${product.name} : ${product.price}")
            }
            else {
                Log.d("Modifier0", "added price of ${product.name} : ${product.price}")
                payableModifierPrice += product.price
            }
        }
        Log.d("InvoiceFragModifier", "final price $payableModifierPrice")
        return payableModifierPrice
    }

    inner class MainListAdapter(val ctx: Context, private val itemList: List<OrdersEntity.ItemInfo>) : RecyclerView.Adapter<MainListAdapter.MyHolder>() {

        inner class MyHolder(val binding: ItemInvoiceMainBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder =
            MyHolder(ItemInvoiceMainBinding.inflate(LayoutInflater.from(ctx), parent, false))

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val item = itemList[position]
            var variantName = ""
            var modStr = ""
            var productPrice = 0f
            if (!item.orderNote.isNullOrBlank()) {
                holder.binding.orderNoteLayout.visibility = View.VISIBLE
                holder.binding.orderNoteText.text = item.orderNote
            }
            CoroutineScope(Dispatchers.Main).launch {
                val productData = MainActivity.mainRepository.getProductDataAsync(item.productId ?: 0).await()
                if (productData == null) {
                    Toast.makeText(ctx, "An Error Occurred", Toast.LENGTH_SHORT).show()
                    (ctx as MainActivity).onBackPressed()
                    return@launch
                }
                if (!productData.productPrice.isNullOrBlank() && productData.productPrice == "0") {
                    // if product price is 0 in database then search for variant price
                    val variantData = MainActivity.mainRepository.getOneVariantDataAsync(item.varientId ?: 0).await()
                    if (variantData != null) {
                        variantName = variantData.variantName ?: "Unknown"
                        productPrice += (variantData.variantPrice ?: "0").toFloat()
                        Log.d("InvoiceProductPrice", "${variantData.variantName} : ${variantData.variantPrice}")
                    }
                } else {
                    productPrice += (productData.productPrice ?: "0").toFloat()
                }
                val productDetails = MainActivity.mainRepository.getProductDataAsync(item.productId ?: 0).await()
                if (productDetails != null) {
                    holder.binding.titleText.text = "${productDetails.productName} - $variantName"
                    holder.binding.qtyText.text  = item.menuQty
                    val modifiers = item.modifierInfo
                    var modifiersAmount = 0f
                    if (!modifiers.isNullOrEmpty()) {
                        for (modifier in modifiers) {
                            val modifierDetails = MainActivity.mainRepository.getOneModifierDataAsync(modifier.modifierId ?: 0).await()
                                ?: return@launch
                            val tst =
                                "${modifierDetails.modifierName}: included ${modifierDetails.modifiersIncluded} / aval: ${modifierDetails.totalModifiers}"
                            Log.d("modifiers",tst)
                            modStr += "\n-${modifierDetails.modifierName}: "
                            val subArray = ArrayList<SubMod>()
                            if (modifier.isHalfAndHalf == "1") {
                                val wholeArray = modifier.subModifier?.subModifierWhole
                                if (!wholeArray.isNullOrEmpty()) {
                                    modStr += "\nWhole: "
                                    for (i in wholeArray.indices) {
                                        val subMod = wholeArray[i]
                                        val sepStr = if (i == wholeArray.size - 1) {
                                            ""
                                        } else {
                                            ","
                                        }
                                        val subModData = MainActivity.mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                /*        if (subModData != null) {
                                            modStr += if (subMod.is2xMode == "1") {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                "2x ${subModData.subModifierName}$sepStr"
                                            } else {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                "${subModData.subModifierName}$sepStr"
                                            }
                                        }  */
                                        if (subModData != null) {
                                            modStr += if (subMod.is2xMode == "1") {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                "2x ${subModData.subModifierName}$sepStr"
                                            } else {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                "${subModData.subModifierName}$sepStr"
                                            }
                                        }
                                    }
                                }
                                val firstHalfArray = modifier.subModifier?.subModifierFirstHalf
                                if (!firstHalfArray.isNullOrEmpty()) {
                                    modStr += "\nFirst Half : "
                                    for (i in firstHalfArray.indices) {
                                        val subMod = firstHalfArray[i]
                                        val sepStr = if (i == firstHalfArray.size -1) {
                                            ""
                                        } else {
                                            ","
                                        }
                                        val subModData = MainActivity.mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                        if (subModData != null) {
                                            modStr += if (subMod.is2xMode == "1") {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    ((if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat() ) / 2, 0.5f))
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    ((if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat() ) / 2, 0.5f))
                                                "2x ${subModData.subModifierName}$sepStr"
                                            } else {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    ((if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat() ) / 2, 0.5f))
                                                "${subModData.subModifierName}$sepStr"
                                            }
                                        }
                                    }
                                }
                                val secondHalfArray = modifier.subModifier?.subModifierSecondHalf
                                if (!secondHalfArray.isNullOrEmpty()) {
                                    modStr += "\nSecond Half : "
                                    for (i in secondHalfArray.indices) {
                                        val subMod = secondHalfArray[i]
                                        val sepStr = if (i == secondHalfArray.size -1) {
                                            ""
                                        } else {
                                            ","
                                        }
                                        val subModData = MainActivity.mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                        if (subModData != null) {
                                            modStr += if (subMod.is2xMode == "1") {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    ((if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat() ) / 2, 0.5f))
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    ((if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat() ) / 2, 0.5f))
                                                "2x ${subModData.subModifierName}$sepStr"
                                            } else {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    ((if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat() ) / 2, 0.5f))
                                                "${subModData.subModifierName}$sepStr"
                                            }
                                        }
                                    }
                                }
                            }
                            if (modifier.isHalfAndHalf == "0") {
                                val subList = modifier.subModifier?.subModifierList
                                if (!subList.isNullOrEmpty()) {
                                    for (i in subList.indices) {
                                        val subMod = subList[i]
                                        val sepStr = if (i == subList.size -1) {
                                            ""
                                        } else {
                                            ","
                                        }
                                        val subModData = MainActivity.mainRepository.getOneSubModifierDataAsync(subMod.subModifierId ?: 0).await()
                                        if (subModData != null) {
                                            modStr += if (subMod.is2xMode == "1") {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                        subMod.subModifierPrice)!!.toFloat(), 1f))
                                                "2x ${subModData.subModifierName}$sepStr"
                                            } else {
                                                subArray.add(SubMod(subModData.subModifierName ?: "NA",
                                                    (if (subMod.subModifierPrice.isNullOrBlank()) "0" else
                                                            subMod.subModifierPrice)!!.toFloat(), 1f))
                                                "${subModData.subModifierName}$sepStr"
                                            }
                                        }
                                    }
                                }
                            }
                            modifiersAmount += removeIncluded3(subArray, (modifierDetails.modifiersIncluded ?: 0).toInt())
                            Log.d("Total amount ${modifierDetails.modifierName}", modifiersAmount.toString())
                        }
                    }
                    productPrice += modifiersAmount

                    holder.binding.strikeText.text = "$prefixStr ${productPrice.format()}"
                    holder.binding.strikeText.visibility = View.VISIBLE
                    holder.binding.strikeText.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG

                    // set product price by adding discount
                    val catDiscountMap = POSFragment.currentDiscount.discountOnCategory
                    val prodDiscountMap = POSFragment.currentDiscount.discountOnProduct
                    var catDiscount = 0f
                    var prodDiscount = 0f

                    val product = MainActivity.mainRepository
                        .getProductDataAsync(item.productId ?: 0).await()
                    if (product != null) {
                        if (catDiscountMap.containsKey(product.categoryId)) {
                            val catDiscountInfo : ArrayList<DiscountData> =
                                catDiscountMap[product.categoryId] ?: ArrayList()
                            for (disc in catDiscountInfo) {
                                val discount : Float = if (disc.discountType == 1) {
                                    productPrice * (disc.discountPercentage ?: 0) / 100
                                } else {
                                    disc.discountPercentage?.toFloat() ?: 0f
                                }
                                catDiscount += discount
                            }
                        }
                    }
                    if (catDiscountMap.containsKey(item.productId))
                    Log.d("InvoiceProduct", "${item.productId}")
                    if (prodDiscountMap.containsKey(item.productId)) {
                        val prodDiscountInfo: ArrayList<DiscountData> = prodDiscountMap[item.productId] ?: ArrayList()
                        for (disc in prodDiscountInfo) {
                            val discount : Float = if (disc.discountType == 1) {
                                productPrice * (disc.discountPercentage ?: 0) / 100
                            } else {
                                disc.discountPercentage?.toFloat() ?: 0f
                            }
                            prodDiscount += discount
                        }
                /*        prodDiscount = if (discountInfo?.discountType == 1) {
                            (discountInfo.discountPercentage ?: 0).toFloat() * productPrice / 100
                        } else {
                            (discountInfo?.discountPercentage ?: 0).toFloat()
                        }  */
                    }
             /*       for (cat in catDiscountMap) {
                        val categoryData =
                            MainActivity.mainRepository.getAllProductFromCategoryAsync(cat.key).await()
                        val categoryProducts = categoryData?.productSummaries
                        //is available product in the category
                        if (!categoryProducts.isNullOrEmpty()) {
                            for (prod in categoryProducts) {
                                if (prod.productId == item.productId) {
                                    val discountInfo = cat.value
                                    for (disc in discountInfo) {
                                        val catDiscount1 = if (disc.discountType == 1) {
                                            (disc.discountPercentage ?: 0).toFloat() * productPrice /100
                                        } else {
                                            (disc.discountPercentage ?: 0).toFloat()
                                        }
                                        catDiscount += catDiscount1
                                    }
                                    break
                                }
                            }
                        }
                    }  */

                    if (prodDiscount ==0f && catDiscount == 0f) {
                        holder.binding.strikeText.visibility = View.GONE
                    }
                    productPrice -= (prodDiscount + catDiscount)
                    holder.binding.priceText.text = "$prefixStr ${productPrice.format()}"
                    val totalPrice = productPrice * (item.menuQty ?: "0").toFloat()
                    holder.binding.totalText.text = "$prefixStr ${totalPrice.format()}"
                    holder.binding.modifierText.text = modStr
                    subTotal += totalPrice
                    updateGrandTotal(totalPrice)
                    binding.textViewSubtotal.text = "$prefixStr ${subTotal.format()}"

                    // add print data here
                    itemPrints.add(ItemPrint(if (item.menuQty.isNullOrBlank()) 0 else item.menuQty!!.toInt(),
                        "${holder.binding.titleText.text}\n${holder.binding.modifierText.text}",
                        productPrice, totalPrice))

                    // calculate taxes here
                    var taxIds = item.taxId
                    val taxArray = ArrayList<Int>()
                    // convert ids in string in to array
                    if (!taxIds.isNullOrBlank()) {
                        while (taxIds!!.contains(",")) {
                            val sepIndex = taxIds.indexOf(",")
                            val tId = taxIds.substring(0, sepIndex)
                            taxIds = taxIds.substring(sepIndex+1)
                            if (!taxArray.contains(tId.toInt())) {
                                taxArray.add(tId.toInt())
                            }
                        }
                        // for last id
                        if (!taxArray.contains(taxIds.toInt())) {
                            taxArray.add(taxIds.toInt())
                        }
                    }
                    // calculate each tax as per tax id
                    for (taxId in taxArray) {
                        val taxDetail = MainActivity.mainRepository.getOneTaxDetailAsync(taxId).await()
                        if (taxDetail != null) {
                            val taxValue = if (taxDetail.taxType == Constants.TAX_IN_PERCENT) {
                                    val mainDiscount = totalPrice * mainDiscountPrec / 100
                                ((totalPrice - mainDiscount) * (taxDetail.taxPercentage ?: "0").toFloat() / 100)
                            } else {
                                Log.d("TAX ${taxDetail.taxPercentage}", "${item.menuId}")
                                (taxDetail.taxPercentage ?: "0").toFloat() * (item.menuQty ?: "0").toFloat()
                            }
                            val currentTax = manageTaxes.getTaxValue(taxId)
                            manageTaxes.updateTaxInfo(taxId, ((currentTax ?: 0f) + taxValue))
                        }
                    }
                    updateTaxesData()
                }
            }
        }

        override fun getItemCount(): Int = itemList.size
    }

    inner class AddOnAdapter(val ctx: Context, val addOns: List<OrdersEntity.AddOn>) : RecyclerView.Adapter<AddOnAdapter.MyHolder>() {

        inner class MyHolder(val binding: ItemInvoiceMainBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder =
            MyHolder(ItemInvoiceMainBinding.inflate(LayoutInflater.from(ctx), parent, false))

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val addOn = addOns[position]
            holder.binding.qtyText.text = addOn.addOnQty
            CoroutineScope(Dispatchers.Main).launch {
                val addOnData = MainActivity.mainRepository.getOneAddOnDataAsync(addOns[position].addOnId ?: 0).await()
                if (addOnData != null) {
                    holder.binding.titleText.text = addOnData.addOnName
                    holder.binding.priceText.text = "$prefixStr ${addOnData.addonPrice?.format()}"
                    val totalPrice = (addOn.addOnQty ?: "0").toFloat() * (addOnData.addonPrice ?: "0").toFloat()
                    holder.binding.totalText.text = "$prefixStr ${totalPrice.format()}"
                    holder.binding.modifierText.visibility = View.GONE
                    subTotal += totalPrice
                    updateGrandTotal(totalPrice)
                    binding.textViewSubtotal.text = "$prefixStr ${subTotal.format()}"

                    // calculate Taxes here
                    var taxIds = addOn.taxIds
                    val taxArray = ArrayList<Int>()
                    // convert ids in string in to array
                    if (!taxIds.isNullOrBlank()) {
                        while (taxIds!!.contains(",")) {
                            val sepIndex = taxIds.indexOf(",")
                            val tId = taxIds.substring(0, sepIndex)
                            taxIds = taxIds.substring(sepIndex+1)
                            taxArray.add(tId.toInt())
                        }
                        // for last id
                        taxArray.add(taxIds.toInt())
                    }
                    // calculate each tax as per tax id
                    for (taxId in taxArray) {
                        val taxDetail = MainActivity.mainRepository.getOneTaxDetailAsync(taxId).await()
                        if (taxDetail != null) {
                            val taxValue = if (taxDetail.taxType == Constants.TAX_IN_PERCENT) {
                                (totalPrice * (taxDetail.taxPercentage ?: "0").toFloat() / 100)
                            } else {
                                    Log.d("TAXADD ${taxDetail.taxPercentage}", "${addOn.addOnId}")
                                (taxDetail.taxPercentage ?: "0").toFloat() * (addOn.addOnQty ?: "0").toFloat()
                            }
                            val currentTax = manageTaxes.getTaxValue(taxId)
                            manageTaxes.updateTaxInfo(taxId, ((currentTax ?: 0f) + taxValue))
                        }
                    }
                    updateTaxesData()
                }
            }
        }

        override fun getItemCount(): Int = addOns.size
    }

    class TaxObject(val label: String, val per: String, val type: String, val amount: Float)
    class ItemPrint(val qty: Int, val label: String, val price: Float, val total : Float)

    inner class TaxListAdapter(val ctx: Context, private val taxes : ArrayList<TaxObject>) : RecyclerView.Adapter<TaxListAdapter.TaxHolder>() {

        inner class TaxHolder(val binding : ItemInvoiceTaxBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxHolder =
            TaxHolder(ItemInvoiceTaxBinding.inflate(LayoutInflater.from(ctx), parent, false))

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: TaxHolder, position: Int) {
            val tax = taxes[position]
            val taxPrintLabel : String
            if (tax.type == Constants.TAX_IN_PERCENT) {
                holder.binding.taxTitle.text = "${tax.label} (${tax.per}%)"
                taxPrintLabel = "${tax.label}(${tax.per}%)"
            } else {
                holder.binding.taxTitle.text = "${tax.label} (${tax.per})"
                taxPrintLabel = "${tax.label}(+${tax.per})"
            }
            holder.binding.taxAmount.text = "$prefixStr ${taxes[position].amount}"
            // add data for printing
            taxPrints[taxPrintLabel] = tax.amount
        }

        override fun getItemCount(): Int = taxes.size
    }

    class ManageTaxes {

        private val taxManageMap = HashMap<Int, Float>()
        var taxList = emptyList<ProductTax>()

        init {
            CoroutineScope(Dispatchers.Main).launch {
                taxList = MainActivity.mainRepository.getAllTaxesDetailsAsync().await() ?: emptyList()
                if (!taxList.isNullOrEmpty()) {
                    for (taxInfo in taxList) {
                        taxManageMap[taxInfo.id] = 0f
                    }
                }
            }
        }

        fun updateTaxInfo(id: Int, amount: Float) {
            taxManageMap[id] = amount
        }

        fun getTaxValue(id: Int) : Float? {
            return taxManageMap[id]
        }
    }

    private fun Float.format() = "%.2f".format(this)
}