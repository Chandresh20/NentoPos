package com.tjcg.nentopos.repositories

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.api.ApiService
import com.tjcg.nentopos.data.*
import com.tjcg.nentopos.fragments.POSFragment
import com.tjcg.nentopos.responses.*
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.lang.Exception
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class MainRepository(ctx : Context) {

    private var mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    val sharedPreferences: SharedPreferences = MainActivity.mainSharedPreferences
    private val productDatabase = ProductDatabase.getDatabase(ctx)
    private val productDao = productDatabase.getProductDao()
    private val userDatabase = UserDatabase.getDatabase(ctx)
    private val userDao = userDatabase.getUserDao()

    // API Operations
    fun loginSuperUser(ctx: Context, email: String, passwd: String, fToken: String) {
        if (!MainActivity.isInternetAvailable(ctx)) {
            MainActivity.progressDialogRepository.showErrorDialog("Internet Connection Required")
            return
        }
        val dialogId = MainActivity.progressDialogRepository.getProgressDialog("Logging In....")
        ApiService.apiService?.loginSuperUser(email, passwd, fToken, "android")
            ?.enqueue(object : Callback<LoginResponse?> {
                override fun onResponse(
                    call: Call<LoginResponse?>,
                    response: Response<LoginResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.status != null &&
                        response.body()?.status!!) {
                        Log.d("Login", "${response.body()?.message}")
                        // store authorization key
                        val authorization = response.body()?.userData?.authorization
                        Constants.authorization  = authorization ?: "-1"
                        Constants.clientId =
                            (response.body()?.userData?.userDetails?.clientId ?: "NA")
                        val oldClientId = sharedPreferences.getString(Constants.PREF_CLIENT_ID, "-1")
                        Log.d("ClientID", "${Constants.clientId} : $oldClientId")
                        Constants.isFromSubUser = false
                        if (Constants.clientId != oldClientId) {
                            mainScope.launch {
                                Constants.isNewLogin = true
                                sharedPreferences.edit().apply {
                                    putBoolean(Constants.PREF_IS_NEW_LOGIN, true)
                                    putString(Constants.PREF_HST_NUMBER, response.body()?.userData?.userDetails?.hstNo)
                                }.apply()
                                deleteAllProductsAsync().await()
                                val permissions = response.body()?.userData?.userPermissions
                                sharedPreferences.edit().apply {
                                    putString(Constants.PREF_SUPER_USER_EMAIL, email)
                                    putString(Constants.PREF_SUPER_USER_DOMAIN, response.body()?.userData?.userDetails?.domainName)
                                    putString(Constants.PREF_AUTHORIZATION, authorization ?: "-1")
                                    putString(Constants.PREF_CLIENT_ID, Constants.clientId)
                                    putInt(Constants.PREF_PERMISSION_POS, permissions?.permissionPOS ?: 0)
                                    putInt(Constants.PREF_PERMISSION_ALL_ORDERS, permissions?.permissionAllOrders ?: 0)
                                    putInt(Constants.PREF_PERMISSION_DASHBOARD, permissions?.permissionDashboard ?: 0)
                                    putInt(Constants.PREF_PERMISSION_KITCHEN, permissions?.permissionKitchenDisplay ?: 0)
                                    putInt(Constants.PREF_PERMISSION_COUNTER, permissions?.permissionCounterDisplay ?: 0)
                                    putInt(Constants.PREF_PERMISSION_RESERVATION, permissions?.permissionReservationAccess ?: 0)
                                    putInt(Constants.PREF_PERMISSION_CUSTOMER_LIST, permissions?.permissionCategoryList ?: 0)
                                    putInt(Constants.PREF_PERMISSION_REPORT_LIST, 1)
                                    putInt(Constants.PREF_PERMISSION_TABLE, 1)
                                    putInt(Constants.PREF_PERMISSION_MENU, permissions?.permissionMenuList ?: 0)
                                }.apply()
                                val outlets = response.body()?.userData?.outlets
                                if (!outlets.isNullOrEmpty()) {
                                    deleteAllOutletsAsync().await()
                                    insertOutletsInDatabaseAsync(outlets as ArrayList<OutletData>).await()
                                    loadOnFirstLogin(ctx, outlets)
                                    ctx.sendBroadcast(Intent(Constants.SUCCESS_BROADCAST))
                                    POSFragment.directLogin = false
                                }
                            }
                        } else {
                            POSFragment.directLogin = true
                            val permissions = response.body()?.userData?.userPermissions
                            sharedPreferences.edit().apply {
                                putString(Constants.PREF_AUTHORIZATION, authorization ?: "-1")
                                putString(Constants.PREF_CLIENT_ID, Constants.clientId)
                                putInt(Constants.PREF_PERMISSION_POS, permissions?.permissionPOS ?: 0)
                                putInt(Constants.PREF_PERMISSION_ALL_ORDERS, permissions?.permissionAllOrders ?: 0)
                                putInt(Constants.PREF_PERMISSION_DASHBOARD, permissions?.permissionDashboard ?: 0)
                                putInt(Constants.PREF_PERMISSION_KITCHEN, permissions?.permissionKitchenDisplay ?: 0)
                                putInt(Constants.PREF_PERMISSION_COUNTER, permissions?.permissionCounterDisplay ?: 0)
                                putInt(Constants.PREF_PERMISSION_RESERVATION, permissions?.permissionReservationAccess ?: 0)
                                putInt(Constants.PREF_PERMISSION_CUSTOMER_LIST, permissions?.permissionCategoryList ?: 0)
                                putInt(Constants.PREF_PERMISSION_REPORT_LIST, 1)
                                putInt(Constants.PREF_PERMISSION_TABLE, 1)
                                putInt(Constants.PREF_PERMISSION_MENU, permissions?.permissionMenuList ?: 0)
                            }.apply()
                            getNewProducts(ctx, Constants.selectedOutletId, "uniqueId",
                                MainActivity.deviceID)
                            ctx.sendBroadcast(Intent(Constants.SUCCESS_BROADCAST))
                        }
                    } else {
                        Log.e("Login", "Failure: ${response.body()?.message}")
                        val failureIntent = Intent(Constants.FAILURE_BROADCAST)
                        failureIntent.putExtra(Constants.error, response.body()?.message)
                        ctx.sendBroadcast(failureIntent)
                        MainActivity.progressDialogRepository.showErrorDialog("Error Login : ${response.body()?.message}")
                    }
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                }

                override fun onFailure(call: Call<LoginResponse?>, t: Throwable) {
                    Log.e("Login", "Failure: ${t.message}")
                    MainActivity.progressDialogRepository.showErrorDialog("Error Login : ${t.message}")
                    val failureIntent = Intent(Constants.FAILURE_BROADCAST)
                    failureIntent.putExtra(Constants.error, "Server Error: ${t.message}")
                    ctx.sendBroadcast(failureIntent)
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                }

            })
    }

    fun loadOnFirstLogin(ctx: Context, outlets: List<OutletData>) {
        for (outlet in outlets) {
            val isDefault : Boolean = if (outlets.size == 1) {
                Constants.selectedOutletId = outlet.outletId
                true
            } else {
                outlet.sDefault == 1
            }
            Log.d("Outlet", "${outlet.outletId} is default= $isDefault")
            Log.d("FirstLogin", "Loading data for default outlet ${outlet.outletId}")
            if (isDefault) {
                Constants.databaseBusy = true
                Log.d("Database---", "Set to busy")
            }
            loadProductData(ctx, outlet.outletId, outlet.uniqueId ?: "NA", MainActivity.deviceID, 1, isDefault)
            loadCustomerData(ctx, outlet.outletId, MainActivity.deviceID, 1, isDefault)
            loadSubUsersData(ctx, outlet.outletId, outlet.uniqueId ?: "NA", MainActivity.deviceID, 1)
            loadTableData(ctx, outlet.outletId, outlet.uniqueId ?: "NA", MainActivity.deviceID, 1, isDefault)
            loadDiscountData(ctx ,MainActivity.deviceID, 1, outlet.outletId, isDefault)
            loadCardTerminalData(outlet.outletId, MainActivity.deviceID, 1)
            MainActivity.orderRepository.getAllOrdersOnline(ctx, outlet.outletId, 1,isDefault)
        }
        loadCustomerTypes()
    }

    fun loginSubUser(ctx: Context, pin: String, domainName: String, deviceId: String) {
        val subUserIntent = Intent(Constants.SUB_USER_LOGIN_BROADCAST)
        val dialogId: Int
        if (MainActivity.isInternetAvailable(ctx)) {
            dialogId = MainActivity.progressDialogRepository.getProgressDialog("Logging in....")
      //      ApiService.apiService?.loginSubUser(superEmail, pin, domainName, deviceId,Constants.firebaseToken, "android")
            ApiService.apiService?.loginSubUser(pin, deviceId, domainName)
                ?.enqueue(object : Callback<SubUserLoginResponse?> {
                    override fun onResponse(
                        call: Call<SubUserLoginResponse?>,
                        response: Response<SubUserLoginResponse?>
                    ) {
                        if (response.isSuccessful && response.body()?.status != null &&
                                response.body()?.status!!) {
                            Log.d("SubUserLogin", "Login Successful")
                            Constants.authorization = response.body()?.userData?.Authorization ?: "NA"
                            val subUPermissions = response.body()?.userData?.sub_user_permission
                            val subUserDataFromApi = response.body()?.userData
                            val subUserData = SubUserData()
                            subUserData.id = subUserDataFromApi?.id ?: -1
                            Constants.loggedInSubUserId = subUserData.id
                            subUserData.firstname = subUserDataFromApi?.fullname ?: "null"
                            subUserData.email = subUserDataFromApi?.email ?: "null"
                            subUserData.pos = subUPermissions?.pos.toString()
                            subUserData.all_order = subUPermissions?.all_order.toString()
                            subUserData.dashboard_analytics = subUPermissions?.dashboard_analytics.toString()
                            subUserData.counter_display = subUPermissions?.counter_display.toString()
                            subUserData.kitchen_display = subUPermissions?.kitchen_display.toString()
                            subUserData.menu_management = subUPermissions?.menu_management.toString()
                            subUserData.management = subUPermissions?.management.toString()
                            subUserData.menus_list = subUPermissions?.menus_list.toString()
                            subUserData.category_list = subUPermissions?.category_list.toString()
                            subUserData.addons_list = subUPermissions?.addons_list.toString()
                            subUserData.variants_list = subUPermissions?.variants_list.toString()
                            subUserData.reservation_access = subUPermissions?.reservation_access.toString()
                            subUserData.reservation_management = subUPermissions?.reservation_management.toString()
                            subUserData.table_management = subUPermissions?.table_management.toString()
                            subUserData.waiting_list = subUPermissions?.waiting_list.toString()
                            subUserData.customer_management = subUPermissions?.customer_management.toString()
                            subUserData.customer_list = subUPermissions?.customer_list.toString()
                            subUserData.store_setup = subUPermissions?.store_setup.toString()
                            subUserData.report_list = subUPermissions?.report_list.toString()
                            subUserData.outletName = subUserDataFromApi?.outlet_name
                            val subUsers = ArrayList<SubUserData>()
                            subUsers.add(subUserData)
                            mainScope.launch {
                                insertSubUsersDataAsync(subUsers).await()
                            }
                            MainActivity.mainSharedPreferences.edit().apply {
                                putInt(Constants.PREF_PERMISSION_POS, subUPermissions?.pos ?: 0)
                                putInt(Constants.PREF_PERMISSION_ALL_ORDERS, subUPermissions?.all_order ?: 0)
                                putInt(Constants.PREF_PERMISSION_DASHBOARD, subUPermissions?.dashboard_analytics ?: 0)
                                putInt(Constants.PREF_PERMISSION_KITCHEN, subUPermissions?.kitchen_display ?: 0)
                                putInt(Constants.PREF_PERMISSION_COUNTER, subUPermissions?.counter_display ?: 0)
                                putInt(Constants.PREF_PERMISSION_RESERVATION, subUPermissions?.reservation_management ?: 0)
                                putInt(Constants.PREF_PERMISSION_CUSTOMER_LIST, subUPermissions?.customer_list ?: 0)
                                putInt(Constants.PREF_PERMISSION_REPORT_LIST, subUPermissions?.report_list ?: 0)
                                putInt(Constants.PREF_PERMISSION_TABLE, subUPermissions?.table_management ?: 0)
                                putInt(Constants.PREF_PERMISSION_MENU, subUPermissions?.menu_management ?: 0)
                            }.apply()
                            Constants.isFromSubUser = true
                            ctx.sendBroadcast(subUserIntent)
                        } else {
                            Log.e("SubUserLogin", "Failed:" +
                                    "status : ${response.body()?.status}, message: ${response.body()?.message}," +
                                    "data: ${response.body()?.userData}")
                            MainActivity.progressDialogRepository.showErrorDialog("Failed: ${response.body()?.message}")
                        }
                        MainActivity.progressDialogRepository.dismissDialog(dialogId)
                    }

                    override fun onFailure(call: Call<SubUserLoginResponse?>, t: Throwable) {
                        Log.e("SubUserLoginAPI", "Failed: ${t.message}")
                        MainActivity.progressDialogRepository.showErrorDialog("Failed: ${t.message}")
                        MainActivity.progressDialogRepository.dismissDialog(dialogId)
                    }

                })
        }
    }

    fun loadProductData(ctx: Context, outletId: Int, uniqueId: String,
                        deviceId: String, isAllData :Int, defaultOutlet : Boolean) {
        var dialogId = 0
        if (defaultOutlet) {
            dialogId =
                MainActivity.progressDialogRepository.getProgressDialog("Loading Product Data")
        } else {
            MainActivity.progressDialogRepository.showSmallProgressBar("Loading Product Data " +
                    "For Outlet id: $outletId")
        }
        ApiService.apiService?.getAllProducts(outletId, uniqueId, deviceId, isAllData,
            Constants.authorization)?.enqueue(object : Callback<MenuResponse?> {
            override fun onResponse(
                call: Call<MenuResponse?>,
                response: Response<MenuResponse?>
            ) {
                if (response.isSuccessful && response.body()?.status != null &&
                    response.body()?.status!!) {
                    val menus = response.body()?.menuData
                    if (defaultOutlet) {
                        Constants.databaseBusy = true
                        Log.d("Database---", "set to busy")
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        insertMenusInDatabaseAsync(menus ?: ArrayList()).await()
                        Log.d("AllProducts", "Menu Inserted in database ${menus?.size}")
                        if (!menus.isNullOrEmpty()) {
                            val categories = ArrayList<CategoryData>()
                            val products = ArrayList<ProductData>()
                            val variants = ArrayList<ProductVariants>()
                            val addons = ArrayList<ProductAddOns>()
                            val taxes = ArrayList<ProductTax>()
                            val modifiers = ArrayList<ProductModifier>()
                            val subModifiers = ArrayList<ProductSubModifier>()
                            for (menu in menus) {
                                categories.addAll(menu.categories as ArrayList<CategoryData>)
                                if (!menu.categories.isNullOrEmpty()) {
                                    for (cat in menu.categories!!) {
                                        products.addAll(cat.productSummaries as ArrayList<ProductData>)
                                        if (!products.isNullOrEmpty()) {
                                            for (pro in products) {
                                                taxes.addAll(pro.productTaxes as ArrayList<ProductTax>)
                                                variants.addAll(pro.productVariants as ArrayList<ProductVariants>)
                                                addons.addAll(pro.productAddOns as ArrayList<ProductAddOns>)
                                                if (!pro.productModifiers.isNullOrEmpty()) {
                                                    modifiers.addAll(pro.productModifiers as ArrayList<ProductModifier>)
                                                    for (modifier in pro.productModifiers!!) {
                                                        subModifiers.addAll(modifier.subModifiers as ArrayList<ProductSubModifier>)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (defaultOutlet) {
                                MainActivity.progressDialogRepository.dismissDialog(dialogId)
                                ctx.sendBroadcast(Intent(Constants.PRODUCT_UPDATE_BROADCAST))
                           //     d2Id = MainActivity.progressDialogRepository.getProgressDialog(
                          //          "Setting up Products data")
                            } else {
                                MainActivity.progressDialogRepository.closeSmallProgressBar()
                            }
                            insertCategoriesInDatabaseAsync(categories).await()
                            Log.d("AllProducts for $outletId","categories inserted in database: " +
                                    "${categories.size}")
                            insertProductsInDatabaseAsync(products).await()
                            Log.d("AllProducts", "Products inserted in database: " +
                                    "${products.size}")
                            insertVariantsInDatabaseAsync(variants).await()
                            Log.d("AllProducts", "Variants inserted in database: " +
                                    "${variants.size}")
                            insertAddOnsInDatabaseAsync(addons).await()
                            Log.d("AllProducts", "Addons inserted in database: " +
                                    "${variants.size}")
                            insertTaxesInDatabaseAsync(taxes).await()
                            Log.d("AllProduct", "Taxes data found ${taxes.size}")
                            insertModifiersInDatabaseAsync(modifiers).await()
                            Log.d("AllProduct", "Modifiers data : ${modifiers.size}")
                            insertSubModifiersInDatabaseAsync(subModifiers).await()
                            Log.d("AllProduct", "SubModifiers: ${subModifiers.size}")
                            if (defaultOutlet) {
                                Constants.databaseBusy = false
                                Log.d("Database---", "now free")
                            }
                        }
                    }
                } else {
                    if (response.body()?.message == "Invalid" || response.body()?.message == "invalid") {
                        sharedPreferences.edit().putString(Constants.PREF_AUTHORIZATION, "-1").apply()
                        Constants.authorization = "-1"
                        (ctx as MainActivity).finish()
                    }
                    Log.e("AllProducts", "Failure: ${response.body()?.message}")
                    if (defaultOutlet) {
                        val failureIntent = Intent(Constants.FAILURE_BROADCAST)
                        failureIntent.putExtra(Constants.error, response.body()?.message)
                        ctx.sendBroadcast(failureIntent)
                        MainActivity.progressDialogRepository.dismissDialog(dialogId)
                    } else {
                        MainActivity.progressDialogRepository.closeSmallProgressBar()
                    }
                    //                showErrorDialog(ctx, "Error: ${response.body()?.menuData}")
                    MainActivity.progressDialogRepository
                        .showErrorDialog("Error loading products ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<MenuResponse?>, t: Throwable) {
                Log.e("All Products", "API-Failure: ${t.message}")
                if (defaultOutlet) {
                    val failureIntent = Intent(Constants.FAILURE_BROADCAST)
                    failureIntent.putExtra(Constants.error, "Server Error: ${t.message}")
                    ctx.sendBroadcast(failureIntent)
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                } else {
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }
                MainActivity.progressDialogRepository.showErrorDialog(
                    "Error Loading Products: ${t.message}")
            }

        })
    }

    fun loadCustomerData(
        ctx: Context, outletId: Int, deviceId: String, isAllData: Int,
        defaultOutlet: Boolean
    ) {
        if (!MainActivity.isInternetAvailable(ctx)) {
            return
        }
        var dialogId = 0
        if (defaultOutlet) {
            dialogId =
                MainActivity.progressDialogRepository.getProgressDialog("Loading Customer Data")
        } else {
            MainActivity.progressDialogRepository.showSmallProgressBar("Loading Customer Data for id: $outletId")
        }
        ApiService.apiService?.getCustomerList(outletId,deviceId, isAllData,
            Constants.authorization)?.enqueue(object : Callback<CustomerListResponse?> {
            override fun onResponse(
                call: Call<CustomerListResponse?>,
                response: Response<CustomerListResponse?>
            ) {
                if (response.isSuccessful && response.body()?.status != null &&
                    response.body()?.status!!) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val customers = response.body()?.customers
                        insertCustomerDataAsync(customers as List<CustomerData>).await()
                        Log.d("CustomerData", "Loaded: ${customers.size}")
                        MainActivity.orderViewModel.setNewCustomers(customers)
                    }
                }
                if (!response.body()?.status!! && response.body()?.message != null &&
                    (response.body()?.message?.contains("Invalid")!! ||
                            response.body()?.message?.contains("invalid")!!)) {
                    MainActivity.logOutNow(ctx)
                }
            /*        MainActivity.progressDialogRepository.showErrorDialog(
                      "Error Loading Customer data for outlet : $outletId\n${response.body()?.message}")  */
                if (defaultOutlet) {
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                } else {
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }
            }

            override fun onFailure(call: Call<CustomerListResponse?>, t: Throwable) {
                //            showErrorDialog(ctx, "Error Loading Customer data\n${t.message}")
                if (defaultOutlet) {
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                } else {
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }
                MainActivity.progressDialogRepository.showErrorDialog(
                    "Error Loading Customer data for outlet : $outletId\n${t.message}")
            }

        })
    }

    fun loadTableData(ctx: Context, outletId: Int, uniqueId: String, deviceId: String,
                      isAllData: Int, defaultOutlet: Boolean) {
        var dialogId = 0
    /*    if (defaultOutlet) {
            dialogId = MainActivity.progressDialogRepository
                .getProgressDialog("Loading Table Data")
        } else {
            MainActivity.progressDialogRepository
                .showSmallProgressBar("Loading Table Data for id : $outletId")
        }  */
        ApiService.apiService?.getTableList(outletId, uniqueId, deviceId, isAllData,
            Constants.authorization)?.enqueue(object : Callback<TableResponse?> {
            override fun onResponse(
                call: Call<TableResponse?>,
                response: Response<TableResponse?>
            ) {
                if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                    val tables = response.body()?.tableData
                    val tables2 = ArrayList<TableData>()
                    if (!tables.isNullOrEmpty()) {
                        for (table in tables) {
                            table.outletId = outletId
                            tables2.add(table)
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            insertTableDataAsync(tables2).await()
                            downloadTableDataAsync(ctx, tables2).await()
                            ctx.sendBroadcast(Intent(Constants.TABLE_LOADED_BROADCAST))
                            Log.d("Tables", "Inserted: ${tables2.size} for Id: $outletId")
                        }
                    }
                }
                if (response.body()?.message?.lowercase()?.contains("invalid") == true) {
                    MainActivity.logOutNow(ctx)
                }
          /*      if (defaultOutlet) {
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                } else {
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }  */
            }

            override fun onFailure(call: Call<TableResponse?>, t: Throwable) {
                //          showErrorDialog(ctx, "Error Loading Table Data: ${t.message}")
                MainActivity.progressDialogRepository.showErrorDialog(
                    "Error loading table data for outlet: $outletId\n${t.message}")
                if (defaultOutlet) {
                    MainActivity.progressDialogRepository.dismissDialog(dialogId)
                } else {
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }
            }

        })
    }

    private suspend fun downloadTableDataAsync(ctx: Context, tables : ArrayList<TableData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                Constants.tableImagesReady = false
                val tableDir = File(ctx.getExternalFilesDir(Constants.TABLE_IMAGE_DIR), Constants.TABLE_IMAGE_DIR)
                if (tableDir.exists()) {
                    tableDir.delete()
                }
                tableDir.mkdirs()
                for (table in tables) {
                    try {
                        val tbFile = File(tableDir, "${table.tableId}.jpg")
                        val tbImageURL = URL(table.tableIcon)
                        val tbStream = tbImageURL.openStream()
                        var read: Int
                        var buff : ByteArray
                        val outStream = tbFile.outputStream()
                        while (true) {
                            buff = ByteArray(1024)
                            read = tbStream.read(buff)
                            if (read <= 0) break
                            outStream.write(buff, 0, read)
                        }
                        Log.d("TableIMage", "${table.tableId} downloaded")
                    } catch (e: Exception) {
                        Log.e("TableIMage", "${table.tableId} failed--$e")
                    }
                }
            }
        }


    private fun loadCustomerTypes() {
        ApiService.apiService?.getCustomerTypes(Constants.authorization)
            ?.enqueue(object : Callback<CustomerTypeResponse> {
                override fun onResponse(
                    call: Call<CustomerTypeResponse>,
                    response: Response<CustomerTypeResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status!= null && response.body()?.status!!) {
                        mainScope.launch {
                            insertCustomerTypesAsync(response.body()?.types ?: emptyList()).await()
                        }
                    }
                }

                override fun onFailure(call: Call<CustomerTypeResponse>, t: Throwable) {
                    Log.d("CustomerTypes", "Failed to get Customer types")
                }

            })
    }

    fun loadSubUsersData(ctx: Context, outletId: Int, uniqueId: String, deviceId: String, isAllData: Int) {
        MainActivity.progressDialogRepository.showSmallProgressBar("Loading SubUsers Data for $outletId")
        Log.d("SubUserReq", "$outletId, $uniqueId, $deviceId, $isAllData, ${Constants.authorization}")
        ApiService.apiService?.getSubUsers(outletId, uniqueId, deviceId, isAllData, Constants.authorization)
            ?.enqueue(object : Callback<SubUserResponse?> {
                override fun onResponse(
                    call: Call<SubUserResponse?>,
                    response: Response<SubUserResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                        CoroutineScope(Dispatchers.Main).launch {
                            insertSubUsersDataAsync(response.body()?.subUsers as List<SubUserData>).await()
                            Log.d("Inserted SubUsers", "${response.body()?.subUsers?.size}")
                        }
                    } else {
                        if (!response.body()?.status!! && response.body()?.message != null &&
                            (response.body()?.message?.contains("Invalid")!! ||
                                    response.body()?.message?.contains("invalid")!!)) {
                            MainActivity.logOutNow(ctx)
                        }
                        Log.e("SubUsersError: ", "${response.body()?.message}")
                    }
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }

                override fun onFailure(call: Call<SubUserResponse?>, t: Throwable) {
                    Log.e("SubUsersError: ", "${t.message}")
                    MainActivity.progressDialogRepository.closeSmallProgressBar()
                }

            })
    }

    private fun loadDiscountData(ctx: Context, deviceId: String, isAllData: Int, outletId: Int, defaultOutlet: Boolean) {
        var dialogId = 0
        if (defaultOutlet) {
            dialogId =
                MainActivity.progressDialogRepository.getProgressDialog("Loading Discount Data")
        } else {
            MainActivity.progressDialogRepository.showSmallProgressBar("Loading Discount Data " +
                    "For Outlet id: $outletId")
        }
        ApiService.apiService?.getDiscountList(deviceId, isAllData, outletId, Constants.authorization)
            ?.enqueue(object : Callback<DiscountResponse> {
                override fun onResponse(
                    call: Call<DiscountResponse>,
                    response: Response<DiscountResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status != null &&
                            response.body()?.status!!) {
                        mainScope.launch {
                            insertDiscountDataAsync(response.body()?.discounts as List<DiscountData>).await()
                            if (defaultOutlet) {
                                MainActivity.progressDialogRepository.dismissDialog(dialogId)
                            } else {
                                MainActivity.progressDialogRepository.closeSmallProgressBar()
                            }
                        }
                    } else {
                    /*    MainActivity.progressDialogRepository.showErrorDialog(
                            "Error loading Discounts for outlet : $outletId, ${response.body()?.message}") */
                        if (!response.body()?.status!! && response.body()?.message != null &&
                            (response.body()?.message?.contains("Invalid")!! ||
                                    response.body()?.message?.contains("invalid")!!)) {
                            MainActivity.logOutNow(ctx)
                        }
                        if (defaultOutlet) {
                            MainActivity.progressDialogRepository.dismissDialog(dialogId)
                        } else {
                            MainActivity.progressDialogRepository.closeSmallProgressBar()
                        }
                    }
                }

                override fun onFailure(call: Call<DiscountResponse>, t: Throwable) {
                    MainActivity.progressDialogRepository.showErrorDialog(
                        "Error loading Discounts for outlet : $outletId, ${t.message}")
                    if (defaultOutlet) {
                        MainActivity.progressDialogRepository.dismissDialog(dialogId)
                    } else {
                        MainActivity.progressDialogRepository.closeSmallProgressBar()
                    }
                }

            })
    }

    private fun loadCardTerminalData(outletId: Int, deviceId: String, isAllData: Int) {
        ApiService.apiService?.getCardTerminals(outletId, deviceId, isAllData, Constants.authorization)
            ?.enqueue(object : Callback<CardTerminalResponse> {
                override fun onResponse(
                    call: Call<CardTerminalResponse>,
                    response: Response<CardTerminalResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status != null && response.body()?.status!!) {
                        mainScope.launch {
                            insertCardTerminalDataAsync((response.body()?.cardData?.cardTypes ?: emptyList())).await()
                        }
                    } else {
                        MainActivity.progressDialogRepository.showErrorDialog(
                            "Error Loading card terminal data for outlet: $outletId, ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<CardTerminalResponse>, t: Throwable) {
                    MainActivity.progressDialogRepository.showErrorDialog(
                        "Error Loading card terminal data for outlet: $outletId, ${t.message}")
                }

            })
    }

    /*   fun loadProductDataOfOtherOutlets(ctx: Context, outlets: List<OutletData>, deviceId: String) {
           for (outlet in outlets) {
               MainActivity.mainViewModel.toggleBackgroundProgressView(true)
               MainActivity.mainViewModel.backgroundProgressText("Loading Products of ${outlet.outletName}")
               loadProductData(ctx, outlet.outletId, outlet.uniqueId ?: "NA", deviceId, "1")
               val successReceiver = object : BroadcastReceiver() {
                   override fun onReceive(p0: Context?, p1: Intent?) {
                       if (p1?.action == Constants.SUCCESS_BROADCAST) {
                           Toast.makeText(ctx, "Data loaded for ${outlet.outletName}", Toast.LENGTH_SHORT).show()
                       } else {
                           Toast.makeText(ctx, "Failed to load data for ${outlet.outletName}", Toast.LENGTH_SHORT).show()
                       }
                       MainActivity.mainViewModel.toggleBackgroundProgressView(false)
                       ctx.unregisterReceiver(this)
                   }
               }
               ctx.registerReceiver(successReceiver, IntentFilter(Constants.SUCCESS_BROADCAST))
               ctx.registerReceiver(successReceiver, IntentFilter(Constants.FAILURE_BROADCAST))
           }
       }  */

    fun getNewProducts(ctx: Context, outletId: Int, uniqueId: String, deviceId: String) {
        Log.d("NewProduct", "API call")
        ApiService.apiService?.getAllProducts(outletId, uniqueId, deviceId, 0, Constants.authorization)
            ?.enqueue(object : Callback<MenuResponse?> {
                override fun onResponse(
                    call: Call<MenuResponse?>,
                    response: Response<MenuResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.status != null &&
                            response.body()?.status!!) {
                        Log.d("NewProduct", "API response : ${response.body()?.message}")
                        mainScope.launch {
                            val menuData = response.body()?.menuData ?: emptyList()
                            for (menu in menuData) {
                                val cats = menu.categories
                                if (!cats.isNullOrEmpty()) {
                                    for (cat in cats) {
                                        val newProducts = cat.productSummaries
                                        Log.d("NewProduct", "ProductInCategory${cat.categoryId} - $newProducts")
                                        if (!newProducts.isNullOrEmpty()) {
                                            //add this new product in category
                                                Log.d("NewProduct", "updating category: ${cat.categoryId}")
                                            val menuData1 = getAllProductDataAsync(Constants.selectedOutletId).await() ?: emptyList()
                                            for (menu1 in menuData1) {
                                                Log.d("comparingMenuId", "${menu1.menuId} : ${menu.menuId}")
                                                if (menu1.menuId == menu.menuId) {
                                                    Log.d("NewProduct",
                                                        "Found Menu: ${menu1.menuId}")
                                                    for (cat1 in (menu1.categories
                                                        ?: emptyList())) {
                                                        if (cat.categoryId == cat1.categoryId) {
                                                            val productData =
                                                                cat1.productSummaries as ArrayList<ProductData>
                                                            Log.d(
                                                                "NewProduct",
                                                                "Adding new ${newProducts.size}"
                                                            )
                                                            productData.addAll(newProducts)
                                                            Log.d(
                                                                "NewProduct",
                                                                "Updating new product"
                                                            )
                                                            updateMenuDataAsync(menu1).await()
                                                            for (pro in newProducts) {
                                                                insertOneProductDataAsync(pro).await()
                                                            }
                                                            Log.d(
                                                                "NewProduct",
                                                                "Inserted in category : ${cat1.categoryId}"
                                                            )
                                                            ctx.sendBroadcast(Intent(Constants.PRODUCT_UPDATE_BROADCAST))
                                                        }
                                                    }
                                                    break
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e("NewProduct", "Error: ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<MenuResponse?>, t: Throwable) {
                    Log.e("NewProduct", "Error: ${t.message}")
                }
            })
    }

    fun startCustomerSync(ctx: Context) {
        mainScope.launch {
            val allOffCustomer = getAllOfflineCustomersAsync().await()
            Log.d("CustomerSyn","Available: ${allOffCustomer?.size}")
            if (allOffCustomer.isNullOrEmpty()) {
                MainActivity.orderRepository.mapOfflineCustomers(ctx, null)
                return@launch
            }
            val req = convertCustomerReqToJson(allOffCustomer)
            Log.d("customerReq", req)
            ApiService.apiService?.syncOfflineCustomers(req, Constants.authorization)
                ?.enqueue(object : Callback<CustomerSyncResponse> {
                    override fun onResponse(
                        call: Call<CustomerSyncResponse>,
                        response: Response<CustomerSyncResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status != null &&
                                response.body()?.status!!) {
                            val mapping = response.body()?.customersMap
                            Log.d("CustomerSyn","Mapping : ${mapping?.size}")
                            if (!mapping.isNullOrEmpty()) {
                                MainActivity.orderRepository.mapOfflineCustomers(ctx, mapping)
                            }
                        }
                    }

                    override fun onFailure(call: Call<CustomerSyncResponse>, t: Throwable) {
                        Log.e("CustomerSyn", "API error: ${t.message}")
                        val intent = Intent(Constants.SYNC_COMPLETE_BROADCAST)
                        intent.putExtra(Constants.IS_SUCCESS, false)
                        ctx.sendBroadcast(intent)
                    }
                })
        }
    }


    private fun convertCustomerReqToJson(allCustomer: List<CustomerOffline>) : String {
        val gson = Gson()
        val typeT = object : TypeToken<List<CustomerOffline>>() { }
        return gson.toJson(allCustomer, typeT.type)
    }

    fun logOutSubUser(ctx: Context, deviceId: String) {
        if (MainActivity.isInternetAvailable(ctx)) {
            ApiService.apiService?.logoutFromDevice(
                deviceId, Constants.authorization)
                ?.enqueue(object : Callback<String> {
                    override fun onResponse(
                        call: Call<String>,
                        response: Response<String>
                    ) {
                        Log.d("LogOutAPI", "response : $response")
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Log.e("LogOutAPI", "failed: ${t.message}")
                    }

                })
        }
    }

    //user database operations
    suspend fun insertOutletsInDatabaseAsync(outlets : List<OutletData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                Log.d("InsertedOutlets", "${outlets.size}")
                userDao.insertAllOutletData(outlets)
            }
        }


    // product database operation
    suspend fun insertMenusInDatabaseAsync(menus: List<MenuData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                Log.d("InsertedMenus", "${menus.size}")
                productDao.insertAllMenuData(menus)
            }
        }

    suspend fun insertCategoriesInDatabaseAsync(categories : List<CategoryData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                productDao.insertAllCategoryData(categories)
            }
        }

    suspend fun insertProductsInDatabaseAsync(products : List<ProductData>) =
        coroutineScope {
            async (Dispatchers.IO) {
                productDao.insertAllProductsData(products)
            }
        }

    suspend fun insertVariantsInDatabaseAsync(variants : List<ProductVariants>) =
        coroutineScope {
            async(Dispatchers.IO) {
                productDao.insertAllProductVariants(variants)
            }
        }

    suspend fun insertAddOnsInDatabaseAsync(addOns : List<ProductAddOns>) =
        coroutineScope {
            async(Dispatchers.IO) {
                productDao.insertAllProductAddOns(addOns)
            }
        }

    suspend fun insertTaxesInDatabaseAsync(taxes: List<ProductTax>) =
        coroutineScope {
            async (Dispatchers.IO) {
                return@async productDao.insertAllTaxesData(taxes)
            }
        }

    suspend fun insertModifiersInDatabaseAsync(modifiers: List<ProductModifier>) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.insertAllModifiers(modifiers)
            }
        }

    suspend fun insertSubModifiersInDatabaseAsync(subModifier: List<ProductSubModifier>) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.insertAllSubModifiers(subModifier)
            }
        }

    suspend fun insertDiscountDataAsync(discounts : List<DiscountData>) =
        coroutineScope {
            async (Dispatchers.IO) {
                productDao.insertAllDiscounts(discounts)
            }
        }

    suspend fun insertCustomerDataAsync(customers: List<CustomerData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.insertAllCustomerData(customers)
            }
        }

    suspend fun insertOneCustomerDataAsync(customer: CustomerData) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.insertOneCustomer(customer)
            }
        }

    suspend fun insertOneOfflineCustomerAsync(customer: CustomerOffline) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.insertOneOfflineCustomer(customer)
            }
        }

    suspend fun insertCustomerTypesAsync(customerTypes: List<CustomerTypeData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                userDao.insertCustomerTypes(customerTypes)
            }
        }

    suspend fun insertTableDataAsync(tables : List<TableData>) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.insertAllTablesData(tables)
            }
        }

    suspend fun insertSubUsersDataAsync(subUsers : List<SubUserData>) =
        coroutineScope {
            async (Dispatchers.IO) {
                userDao.insertSubUsersData(subUsers)
            }
        }

    suspend fun insertCardTerminalDataAsync(cardData: List<CardTerminalData>) =
        coroutineScope {
            async (Dispatchers.IO) {
                userDao.insertCardTerminalData(cardData)
            }
        }

    suspend fun insertOneProductDataAsync(productData: ProductData) =
        coroutineScope {
            async (Dispatchers.IO) {
                productDao.insertOneProductData(productData)
            }
        }

    suspend fun updateOneTableDataAsync(table: TableData) =
        coroutineScope {
            async(Dispatchers.IO) {
                userDao.updateOneTable(table)
            }
        }

    suspend fun getMenuNameAsync(id: Int) =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getMenuName(id)
            }
        }

    suspend fun updateMenuDataAsync(menuData: MenuData) =
        coroutineScope {
            async (Dispatchers.IO) {
                productDao.updateMenuData(menuData)
            }
        }

/*    suspend fun getAllProductFromCategoryAsync(catId: Int) : Deferred<CategoryData?> =
        coroutineScope {
            async (Dispatchers.IO) {
                return@async productDao.getAllProductFromCategory(catId)
            }
        }  */

    suspend fun getAllProductDataAsync(outletId: Int) : Deferred<List<MenuData>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getAllMenuData(outletId)
            }
        }

    suspend fun getProductDataAsync(id: Int) : Deferred<ProductData?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getProductData(id)
            }
        }

    suspend fun getOneVariantDataAsync(variantId: Int) : Deferred<ProductVariants?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getOneVariantData(variantId)
            }
        }

    suspend fun getOneModifierDataAsync(modId: Int) : Deferred<ProductModifier?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getOneModifierData(modId)
            }
        }

    suspend fun getOneSubModifierDataAsync(subModId: Int) : Deferred<ProductSubModifier?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getOneSubModifierDetails(subModId)
            }
        }

    suspend fun getOneAddOnDataAsync(addId : Int) : Deferred<ProductAddOns?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getOneAddOnDetails(addId)
            }
        }

    suspend fun getAllOutletDataAsync() : Deferred<List<OutletData>?> =
        coroutineScope {
            async (Dispatchers.IO) {
                return@async userDao.getAllOutlets()
            }
        }

    suspend fun getOutletDataAsync(id:Int) : Deferred<OutletData?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getOutletDetails(id)
            }
        }

    suspend fun getCardTerminalsAsync(outletId: Int) =
        coroutineScope {
            async (Dispatchers.IO) {
                return@async userDao.getCardTerminals(outletId)
            }
        }

    suspend fun getOneCustomerDataAsync(id: Long) : Deferred<CustomerData?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getOneCustomer(id)
            }
        }

    suspend fun getCustomerTypesAsync() : Deferred<List<CustomerTypeData>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getCustomerTypes()
            }
        }


    suspend fun getAllCustomersAsync() : Deferred<List<CustomerData>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getAllCustomers()
            }
        }

    private suspend fun getAllOfflineCustomersAsync() : Deferred<List<CustomerOffline>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getAllOfflineCustomer()
            }
        }

    suspend fun getOneTableDataAsync(id: Int) : Deferred<TableData?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getOneTableData(id)
            }
        }

    suspend fun getTablesForOutletAsync(outletId: Int) : Deferred<List<TableData>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getTableDataForOutlet(outletId)
            }
        }

 /*   suspend fun getCustomerDetailsAsync(id: Int) : Deferred<CustomerData?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getOneCustomerDetails(id)
            }
        }  */

    suspend fun getAllSubUsersAsync(outletId: Int) : Deferred<List<SubUserData>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getAllSubUsers(outletId)
            }
        }

    suspend fun getOneSubUserDetailAsync(id: Int) : Deferred<SubUserData?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async userDao.getOneSubUser(id)
            }
        }

    suspend fun getAllTaxesDetailsAsync() : Deferred<List<ProductTax>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getAllTaxesData()
            }
        }


    suspend fun getOneTaxDetailAsync(taxId: Int) : Deferred<ProductTax?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getOneTaxData(taxId)
            }
        }

    suspend fun getAllDiscountsAsync() : Deferred<List<DiscountData>?> =
        coroutineScope {
            async(Dispatchers.IO) {
                return@async productDao.getAllDiscounts()
            }
        }

    suspend fun deleteOfflineCustomersAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                userDao.deleteAllOfflineCustomers()
            }
        }

    suspend fun deleteAllOutletsAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                userDao.deleteAllOutletData()
                userDao.deleteAllCustomers()
                userDao.deleteAllTableData()
                userDao.deleteAllSubUsers()
            }
        }

    suspend fun deleteAllProductsAsync() =
        coroutineScope {
            async(Dispatchers.IO) {
                productDao.deleteAllMenus()
                productDao.deleteAllCategories()
                productDao.deleteAllProducts()
                productDao.deleteAllTaxes()
                productDao.deleteAllVariants()
                productDao.deleteAllAddOns()
                productDao.deleteAllModifiers()
                productDao.deleteAllSubModifiers()
                productDao.deleteAllDiscountData()
            }
        }

    fun getTodayString() : String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val monthInt = cal.get(Calendar.MONTH)
        var month = (monthInt+1).toString()
        if (month.length == 1) {
            month = "0$month"
        }
        var date = cal.get(Calendar.DATE).toString()
        if (date.length == 1) {
            date = "0$date"
        }
        return "$year-$month-$date"
    }

    fun formatCookingTime(minutes: Int) : String {
        val hour = minutes / 60
        var hourStr = hour.toString()
        if (hourStr.length == 1) {
            hourStr = "0$hourStr"
        }
        val reminder = minutes % 60
        var remStr = reminder.toString()
        if (remStr.length == 1) {
            remStr = "0$remStr"
        }
        Log.d("cookingTime", "$hourStr:$remStr:00" )
        return "$hourStr:$remStr:00"
    }

}