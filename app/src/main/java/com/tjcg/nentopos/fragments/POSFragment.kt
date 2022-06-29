package com.tjcg.nentopos.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.*
import kotlinx.coroutines.*

import com.tjcg.nentopos.MainActivity.Companion.mainRepository
import com.tjcg.nentopos.MainActivity.Companion.mainSharedPreferences
import com.tjcg.nentopos.data.*
import com.tjcg.nentopos.databinding.*
import com.tjcg.nentopos.dialog.AddCustomerDialog
import java.lang.Runnable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val sch_MONDAY = "Monday"
const val sch_TUESDAY = "Tuesday"
const val sch_WEDNESDAY = "Wednesday"
const val sch_THURSDAY = "Thursday"
const val sch_FRIDAY = "Friday"
const val sch_SATURDAY = "Saturday"
const val sch_SUNDAY = "Sunday"


class POSFragment : Fragment() {

    private lateinit var binding : FragmentPosBinding
    private lateinit var ctx: Context
    private var categorySelected = 0
    private lateinit var cart : Cart

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        MainActivity.onPOSFragment = true
        binding = FragmentPosBinding.inflate(inflater, container, false)
        MainActivity.showActionBar()
        MainActivity.collapseActionBar()
        MainActivity.orderViewModel.outletName.observe(viewLifecycleOwner, {
            binding.outletText.text = it
        })
        if (Constants.isFromSubUser) {
            binding.includedCart.changeOutletBtn.visibility = View.GONE
            MainActivity.hideChangeLocationBtnAction()
            CoroutineScope(Dispatchers.Main).launch {
                val subInfo = mainRepository.getOneSubUserDetailAsync(Constants.loggedInSubUserId).await()
                if (subInfo != null) {
                    MainActivity.orderViewModel.setOutletName("${subInfo.firstname}(${subInfo.outletName})")
                } else {
                    MainActivity.orderViewModel.setOutletName("Error")
                }
            }
        } else {
            binding.includedCart.changeOutletBtn.visibility = View.VISIBLE
            MainActivity.showChangeLocationBtnAction()
            CoroutineScope(Dispatchers.Main).launch {
                val outletData = mainRepository.getOutletDataAsync(Constants.selectedOutletId).await()
                MainActivity.orderViewModel.setOutletName(outletData?.outletName ?: "MenuOnline")
            }
        }
        binding.syncInPOS.setOnClickListener {
            if (!MainActivity.isInternetAvailable(ctx)) {
                Toast.makeText(ctx, "Internet Not Available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (MainActivity.orderRepository.syncRequired) {
                MainActivity.progressDialogRepository.showSyncDialog()
                return@setOnClickListener
            }
            val synDialogBuilder = AlertDialog.Builder(ctx).apply {
                setMessage("Select any option")
                setPositiveButton("Refresh Orders") { _, _ ->
                    MainActivity.orderRepository.getAllOrdersOnline(ctx, Constants.selectedOutletId,0, true)
                }
                setNegativeButton("Reload All Orders") { _, _ ->
                    //       mainSharedPreferences.edit().putInt(Constants.PREF_IS_ALL_DATA, 1).apply()
                    MainActivity.orderRepository.getAllOrdersOnline(ctx, Constants.selectedOutletId,1, true)
                }
                setNeutralButton("Reload All Products") { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val thisOutlet = mainRepository.getOutletDataAsync(Constants.selectedOutletId).await()
                        if (thisOutlet != null) {
                            val listOfOutlet = ArrayList<OutletData>()
                            listOfOutlet.add(thisOutlet)
                            mainRepository.loadOnFirstLogin(ctx, listOfOutlet)
                        }
                    }
                }
            }
            val synDialog = synDialogBuilder.create()
            synDialog.show()
        }
        val posPermission = mainSharedPreferences.getInt(
            Constants.PREF_PERMISSION_POS, 0)
        if (posPermission == 0) {
            binding.noAccess.visibility = View.VISIBLE
            binding.includedCart.logoutBtn.setOnClickListener {
                MainActivity.logOutNow(ctx)
            }
            return binding.root
        }
        binding.reloadBtn.setOnClickListener {
        }
        binding.posBackground.setOnClickListener {
            hideSubCart()
        }
        binding.productRecycler.setOnTouchListener { _, p1 ->
            when (p1?.action) {
                MotionEvent.ACTION_DOWN -> {
                    hideSubCart()
                    true
                }
                else -> false
            }
        }
        MainActivity.orderRepository.getAllOrdersOnline(ctx, Constants.selectedOutletId, 0,false)
        if (MainActivity.isInternetAvailable(ctx)) {
            mainRepository.loadTableData(ctx, Constants.selectedOutletId, MainActivity.deviceID,
                MainActivity.deviceID, 1, false)
        }
   /*     if (directLogin) {
            updateProductShowcase()
            directLogin = false
        }  */
        if (binding.categoryRecycler.size == 0) {
            updateProductShowcase()
        }
        val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1?.action == Constants.CLEAR_CART_BROADCAST) {
                    if (orderToUpdate == null) {
                        binding.includedCart.cartClearBtn.performClick()
                    }
                    binding.posAutoComplete.setText("")
                    binding.userSpinner.setSelection(0, true)
                }
                if (p1?.action == Constants.PRODUCT_UPDATE_BROADCAST ||
                        p1?.action == Constants.CUSTOMER_ADDED_BROADCAST) {
                    updateProductShowcase()
                }
                if (p1?.action == Constants.OUTLET_CHANGE_BROADCAST) {
                    updateProductShowcase()
                    binding.includedCart.cartClearBtn.performClick()
                    binding.posAutoComplete.setText("")
                }
            }
        }
        val iFilter = IntentFilter().apply {
            addAction(Constants.PRODUCT_UPDATE_BROADCAST)
            addAction(Constants.OUTLET_CHANGE_BROADCAST)
            addAction(Constants.CLEAR_CART_BROADCAST)
            addAction(Constants.CUSTOMER_ADDED_BROADCAST)
        }
        ctx.registerReceiver(updateReceiver, iFilter)
        cart = Cart(ctx, binding.includedCart, binding.includedModifier)

        binding.relodbtn.setOnClickListener {
       //     MainActivity.orderRepository.getAllOrdersOnline(ctx, Constants.selectedOutletId, true)
        }
        binding.reloadAll.setOnClickListener {
            mainSharedPreferences.edit()
                .putInt(Constants.PREF_IS_ALL_DATA, 1).apply()
     //       MainActivity.orderRepository.getAllOrdersOnline(ctx, Constants.selectedOutletId, true)
        }
        binding.testSentry.setOnClickListener {
            throw Exception("Throwing Exception to test sentry")
        }

        binding.addCustBtn.setOnClickListener {
            val dialog = AddCustomerDialog(ctx)
            dialog.show(MainActivity.fManager, "customer")
        }
        val syncTimingHandler = Handler(Looper.getMainLooper())
        val syncTimingR = object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                val interval = Calendar.getInstance().timeInMillis -
                        (MainActivity.orderViewModel.lastSyncTiming.value ?: 0L)
                if (interval > 3600000) {
                    binding.syncText.text = "NA"
                } else {
                    val tx = when (val mins = interval / 60000) {
                        0L -> {
                            "few sec ago"
                        }
                        1L -> {
                            "1 min ago"
                        }
                        else -> {
                            "$mins mins ago"
                        }
                    }
                    binding.syncText.text = tx
                    syncTimingHandler.postDelayed(this, 30000)
                }
            }

        }
        MainActivity.orderViewModel.lastSyncTiming.observe(viewLifecycleOwner, {
            syncTimingHandler.removeCallbacks(syncTimingR)
            syncTimingHandler.post(syncTimingR)
        })
        if (MainActivity.orderViewModel.lastSyncTiming.value == null) {
            val lastSync = mainSharedPreferences.getLong(Constants.PREF_SYN_TIME, 0)
            MainActivity.orderViewModel.lastSyncTiming.value = lastSync
        }
        if (orderToUpdate != null) {
            cart.insertProductToUpdate(orderToUpdate!!)
        }
        return binding.root
    }

    private fun hideSubCart() {
        MainActivity.showActionBar()
        if (binding.includedModifier.root.visibility == View.VISIBLE) {
            val toHide = binding.includedModifier.root
            if (toHide.visibility == View.VISIBLE) {
                val anim = ScaleAnimation(1f, 0f, 1f, 1f, toHide.width.toFloat(), 0f).apply {
                    duration = Constants.subCartAnimDuration
                }
                toHide.startAnimation(anim)
                Handler(Looper.getMainLooper()).postDelayed( {
                    toHide.visibility = View.GONE
                }, Constants.subCartAnimDuration)
            }
        }
    }

    private fun updateProductShowcase() {
        CoroutineScope(Dispatchers.Main).launch {
            val allProductData = mainRepository
                .getAllProductDataAsync(Constants.selectedOutletId).await()
            if (allProductData.isNullOrEmpty()) {
                val outlet = mainRepository.getOutletDataAsync(Constants.selectedOutletId).await()
                if (outlet != null) {
                    Log.d("ProductForDefault", "loading...")
                    mainRepository.loadProductData(ctx,
                        Constants.selectedOutletId,
                        outlet.uniqueId ?: "NA", "0",
                        1, true)
                    Log.d("CustomerForDefault", "loading...")
                    mainRepository.loadCustomerData(
                        ctx, Constants.selectedOutletId,
                        "0", 1, true
                    )
                    Log.d("TablesForDefault", "loading...")
                    mainRepository.loadTableData(ctx, Constants.selectedOutletId,
                        outlet.uniqueId ?: "NA", "0", 1, true)
                    mainRepository.loadSubUsersData(ctx, Constants.selectedOutletId,
                        outlet.uniqueId ?: "NA", MainActivity.deviceID, 1)
                    return@launch
                } else {
                    Log.e("Loading Products", "Outlet Details not found for id: ${Constants.selectedOutletId}")
                }
            }
            Log.d("Menus", "${allProductData?.size}")
            val menuList = ArrayList<MenuToShow>()
            for (pro in allProductData!!) {
                if (pro.onPos == 1) {
                    val categoriesToShow = ArrayList<CategoriesToShow>()
                    if (!pro.categories.isNullOrEmpty()) {
                        for (cat in pro.categories!!) {
                            categoriesToShow.add(CategoriesToShow(cat.categoryId, cat.categoryName ?: "0",
                                cat.productSummaries ?: ArrayList()))
                        }
                    }
                    menuList.add(MenuToShow(
                        pro.menuId, pro.menuName ?: "NA",
                        pro.position ?: 0, categoriesToShow
                    ))
                }
            }
            menuList.sortBy { it.position }
            val arrayAdapter = ArrayAdapter(ctx, R.layout.item_menu_spinner, R.id.menu_spinner_text, menuList)
            binding.menuSpinner.adapter = arrayAdapter
            binding.menuSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if (orderToUpdate == null) {
                        cart.clearTheCart()
                    } else {
                        orderToUpdate = null
                    }
                    categorySelected = 0
                    selectedMenuId = menuList[p2].Id
                    binding.noProductText.visibility = View.VISIBLE
                    binding.categoryRecycler.layoutManager = LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false)
                    binding.categoryRecycler.recycledViewPool.setMaxRecycledViews(0,0)
                    binding.categoryRecycler.adapter = CategoryAdapter(menuList[p2].categories)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) { }
            }
            val subUsers = mainRepository.getAllSubUsersAsync(Constants.selectedOutletId).await() as List<SubUserData>
            if (!subUsers.isNullOrEmpty()) {
                val waiterList = ArrayList<SubUserData>()
                for (sub in subUsers) {
                    if (sub.role_name?.lowercase() == "waiter") {
                        waiterList.add(sub)
                    }
                }
                val waiterAdapter = ArrayAdapter(ctx, R.layout.item_menu_spinner, R.id.menu_spinner_text, waiterList)
                binding.userSpinner.adapter = waiterAdapter
                binding.userSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        selectedWaiterId = waiterList[p2].id
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) { }

                }
            }
            val customerList = mainRepository.getAllCustomersAsync().await() as List<CustomerData>
            val userAdapter2 =
                ArrayAdapter(ctx, R.layout.item_light_spinner, R.id.light_spinner_text, customerList)
            binding.posAutoComplete.setAdapter(userAdapter2)
            binding.posAutoComplete.setOnItemClickListener { adapterView, _, i, _ ->
                val selectedCustomer = adapterView.adapter.getItem(i) as CustomerData
                selectCustomerId = selectedCustomer.customerId
                Log.d("selectedCustomer", "$selectCustomerId")
            }
            getCurrentDiscounts()
        }
    }


    private fun getCurrentDiscounts() {
        CoroutineScope(Dispatchers.Main).launch {
            val discounts = mainRepository.getAllDiscountsAsync().await()
            val discountCategory = HashMap<Int,ArrayList<DiscountData>>()
            val discountProduct = HashMap<Int, ArrayList<DiscountData>>()
            if (!discounts.isNullOrEmpty()) {
                for (discount in discounts) {
                    if (isDiscountNow(discount)) {
                        val appliesTo = discount.appliedIds?.split(",")
                        if (appliesTo != null) {
                            for (ids in appliesTo) {
                                if (discount.discountOn == 1) {
                                    var discountArray = discountCategory[ids.toInt()]
                                    if (discountArray.isNullOrEmpty()) {
                                        discountArray = ArrayList()
                                    }
                                    discountArray.add(discount)
                                    discountCategory[ids.toInt()] = discountArray
                                }
                                if (discount.discountOn == 2) {
                                    var discountArray = discountProduct[ids.toInt()]
                                    if (discountArray.isNullOrEmpty()) {
                                        discountArray = ArrayList()
                                    }
                                    discountArray.add(discount)
                                    discountProduct[ids.toInt()] = discountArray
                                }
                            }
                        }

                    }
                }
            }
            var discount = ""
            for (entry in discountCategory) {
                discount += "cat ${entry.key} :"
                for (disc in entry.value) {
                    discount += "${disc.discountPercentage},"
                }
            }
            for (entry in discountProduct) {
                discount += "product ${entry.key} : ${entry.value}\n"
            }
            currentDiscount = CurrentDiscounts(discountCategory, discountProduct)
            Log.d("CurrentDiscount", discount)
        }
    }

    private fun isDiscountNow(discount: DiscountData) : Boolean {
        var available = false
        if (discount.discountApplyService == 1 || discount.discountApplyService == 3) {
            if (discount.discountSchedule == 1) {
                available = true
            }
        /*    else {
                // TODO("discount will be available on particular")
            } */
        }
        return available
    }

    companion object{

        var directLogin = true
        var selectedMenuId = 0
        lateinit var currentDiscount : CurrentDiscounts
        var selectedWaiterId = 0
        var selectCustomerId = 0L
        var orderToUpdate : OrdersEntity? = null

        class CurrentDiscounts(val discountOnCategory : HashMap<Int,ArrayList<DiscountData>>,
                               val discountOnProduct : HashMap<Int,ArrayList<DiscountData>>)

        fun getInstance(ctx: Context) : POSFragment {
            val fragment = POSFragment()
            fragment.ctx = ctx
            return POSFragment()
        }
    }

    class MenuToShow(
        val Id: Int,
        private val label: String,
        val position: Int,
        val categories: List<CategoriesToShow>) {
        override fun toString(): String {
            return label
        }
    }
    class CategoriesToShow(val id: Int, val label : String, val products : List<ProductData>)

    inner class CategoryAdapter(private val catList: List<CategoriesToShow>) : RecyclerView.Adapter<CategoryAdapter.MyHolder>() {

        inner class MyHolder(val cBinding : ItemCategoryBinding) :
                RecyclerView.ViewHolder(cBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(ItemCategoryBinding.inflate(
                LayoutInflater.from(ctx), parent, false))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val catText = holder.cBinding.categoryText
            catText.text = catList[position].label
            catText.setOnClickListener {
                val lastCategorySelected = categorySelected
                categorySelected = holder.adapterPosition
                binding.noProductText.visibility = View.VISIBLE
                notifyItemChanged(lastCategorySelected)
                notifyItemChanged(position)
            }
            catText.isSelected = position == categorySelected
            if (catText.isSelected) {
                catText.setTextColor(Color.WHITE)
                val productsToShow = ArrayList<ProductData>()
                for (pro in catList[position].products) {
                    if (pro.sDisplayInPOS == 1) {
                        productsToShow.add(pro)
                    }
                }
          /*      //added multiple for scrolling test
                for (pro in catList[position].products) {
                    if (pro.sDisplayInPOS == 1) {
                        productsToShow.add(pro)
                    }
                }
                // end here  */

                if (!productsToShow.isNullOrEmpty()) {
                    binding.noProductText.visibility = View.GONE
                }
                Log.d("Products:", "${productsToShow.size}")
                binding.productRecycler.layoutManager = GridLayoutManager(ctx, 4)
                binding.productRecycler.adapter = ProductAdapter(catList[position].id,productsToShow)
            //    Toast.makeText(ctx, "selected Cat: ${catList[position].id}", Toast.LENGTH_SHORT).show()
            } else {
                catText.setTextColor(Color.BLACK)
            }
        }
        override fun getItemCount(): Int = catList.size
    }

    inner class ProductAdapter(private val catId: Int, private val proList: List<ProductData>) : RecyclerView.Adapter<ProductAdapter.MyHolder>() {

        inner class MyHolder(val pBinding: ItemProductBinding) :
                RecyclerView.ViewHolder(pBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(
                ItemProductBinding.inflate(LayoutInflater.from(ctx), parent, false))
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val product = proList[position]
            var available = true
            var scheduleInfo = ""
            holder.pBinding.productLabel.text = product.productName
            if (product.sScheduledItem == 1) {
                available = false
                scheduleInfo += "${product.scheduledDay}\n"
                scheduleInfo += "From ${product.scheduledStartTime}\n"
                scheduleInfo += "To: ${product.scheduledEndTime}"
                val days = product.scheduledDay?.split(",")
                if (!days.isNullOrEmpty()) {
                    for (day in days) {
                        available = isProductAvailableToday(day, product.scheduledStartTime ?: "NA", product.scheduledEndTime ?: "NA")
                        if (available) {
                            break
                        }
                    }
                }
                holder.pBinding.availableTExt.text = scheduleInfo
                if (available) {
                    holder.pBinding.productLayout.background =
                        ResourcesCompat.getDrawable(ctx.resources,
                            R.drawable.product_selector_reverse, ctx.theme)
                } else {
                    holder.pBinding.productLayout.visibility = View.GONE
                }
            }
            holder.pBinding.productPrice.text = if (product.productPrice.isNullOrBlank() || product.productPrice == "0") {
                ""
            } else {
                "${Constants.currencySign} ${proList[position].productPrice}"
            }
            holder.pBinding.productLayout.setOnClickListener {
                if (!available) {
                    MainActivity.progressDialogRepository.showErrorDialog("Sorry, Item is only available on $scheduleInfo")
                    return@setOnClickListener
                }
                cart.insertInCart(catId, product)
            }
        }
        override fun getItemCount(): Int = proList.size
    }

    fun isProductAvailableToday(day: String, from: String, to : String) : Boolean {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var available = false
        when (day) {
            sch_SUNDAY -> {
                if (today == 1) {
                    available = isProductAvailableNow(from, to)
                }
            }
            sch_MONDAY -> {
                if (today == 2) {
                    available = isProductAvailableNow(from, to)
                }
            }
            sch_TUESDAY -> {
                if (today == 3) {
                    available = isProductAvailableNow(from, to)
                }
            }
            sch_WEDNESDAY -> {
                if (today == 4) {
                    available = isProductAvailableNow(from, to)
                }
            }
            sch_THURSDAY -> {
                if (today == 5) {
                    available = isProductAvailableNow(from, to)
                }
            }
            sch_FRIDAY -> {
                if (today == 6) {
                    available = isProductAvailableNow(from, to)
                }
            }
            sch_SATURDAY -> {
                if (today == 7) {
                    available = isProductAvailableNow(from, to)
                }
            }
        }
        return available
    }

    private fun isProductAvailableNow(from: String, to : String) : Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val hour1 = from.split(":")[0].toInt()
        val min1 = from.split(":")[1].toInt()
        val hour2 = to.split(":")[0].toInt()
        val min2 = to.split(":")[1].toInt()
        Log.d("Product Searching :", "$hour1:$min1, $hour:$min, $hour2:$min2")
        var available = false
        if (hour in (hour1 + 1) until hour2) {
            available = true
        } else if (hour == hour2) {
            if (min < min2) {
                available = true
            }
        }
        return available
    }
}