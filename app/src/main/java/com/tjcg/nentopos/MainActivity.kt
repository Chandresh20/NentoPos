package com.tjcg.nentopos

import android.annotation.SuppressLint
import android.content.*
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.tjcg.nentopos.databinding.ActivityMainBinding
import com.tjcg.nentopos.databinding.BelowActionBarBinding
import com.tjcg.nentopos.databinding.DialogOutletsBinding
import com.tjcg.nentopos.databinding.DrowerMainBinding
import com.tjcg.nentopos.dialog.ProgressDialog
import com.tjcg.nentopos.dialog.ProgressDialogRepository
import com.tjcg.nentopos.java.MainActivitySetting
import com.tjcg.nentopos.receiver.ScreenReceiver
import com.tjcg.nentopos.repositories.MainRepository
import com.tjcg.nentopos.repositories.OrderRepository
import com.tjcg.nentopos.viewmodels.MainViewModel
import com.tjcg.nentopos.viewmodels.OrderViewModel
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val NAV_POS = 0
const val NAV_ALL_ORDERS = 1
const val NAV_KITCHEN = 2
const val NAV_COUNTER = 3
const val NAV_ONLINE = 4
const val NAV_ONGOING = 5
const val NAV_TABLES = 6

class MainActivity : AppCompatActivity() {

    private lateinit var mainDrawerBinding: DrowerMainBinding
    private lateinit var binding : ActivityMainBinding
    private var progressDialog : ProgressDialog? = null
    private lateinit var animator: Animator
    private var currentNavigation = NAV_POS
    private lateinit var posBtn : Button
    private lateinit var kitchenBtn : Button
    private lateinit var counterBtn: Button
    private lateinit var allOrderBtn : Button
    private lateinit var settingsBtn : Button

    @SuppressLint("SetTextI18n", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainSharedPreferences = getSharedPreferences(Constants.PREFS_MAIN, MODE_PRIVATE)
        mainDrawerBinding = DrowerMainBinding.inflate(layoutInflater)
        binding = mainDrawerBinding.mainLayout
        setContentView(mainDrawerBinding.root)
        deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("DeviceId", deviceID)
        dummyViewActionBar = binding.dummyView
        Constants.isNewLogin = mainSharedPreferences.getBoolean(
            Constants.PREF_IS_NEW_LOGIN, true)
        mainRepository = MainRepository(this)
        orderRepository = OrderRepository(this)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        orderViewModel = ViewModelProvider(this).get(OrderViewModel::class.java)
        fManager = supportFragmentManager
        mainDrawerLayout = mainDrawerBinding.drawerMain
        progressDialogRepository = ProgressDialogRepository(this, fManager)
        progressDialogRepository.setSmallProgressBar(binding.backgroundProgressText,
            binding.backgroundOperations)
        progressDialog = ProgressDialog.getInstance(this)
        mainActionBar = binding.mainActionBar
        binding.mainActionBar.navMenuBtn.setOnClickListener {
            mainDrawerBinding.drawerMain.openDrawer(GravityCompat.START)
        }

        val dHeader = mainDrawerBinding.posNavigation.getHeaderView(0)
        posBtn = dHeader.findViewById(R.id.nav_pos)
        posBtn.isSelected = true
        allOrderBtn = dHeader.findViewById(R.id.nav_all_orders)
        kitchenBtn = dHeader.findViewById(R.id.nav_kitchen)
        counterBtn = dHeader.findViewById(R.id.nav_counter)
        settingsBtn = dHeader.findViewById(R.id.nav_settings)
        posBtn.setOnClickListener {
            navigateTo(NAV_POS)
        }
        allOrderBtn.setOnClickListener {
            navigateTo(NAV_ALL_ORDERS)
        }
        kitchenBtn.setOnClickListener {
            navigateTo(NAV_KITCHEN)
        }
        counterBtn.setOnClickListener {
            navigateTo(NAV_COUNTER)
        }
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, MainActivitySetting::class.java))
        }
        Constants.selectedOutletId = mainSharedPreferences.getInt(
            Constants.PREF_SELECTED_OUTLET, 1 )
        switchLocationBtn = binding.mainActionBar.changeOutlet
        binding.mainActionBar.changeOutlet.setOnClickListener {
            var outletDialog : AlertDialog? = null
            val builder = AlertDialog.Builder(this).apply {
                val dBinding = DialogOutletsBinding.inflate(layoutInflater)
                CoroutineScope(Dispatchers.Main).launch {
                    val outlets = mainRepository.getAllOutletDataAsync().await()
                    if (!outlets.isNullOrEmpty()) {
                        for (outlet in outlets) {
                            val radioButton = RadioButton(this@MainActivity).apply {
                                text = outlet.outletName
                                textSize = 32f
                                id = outlet.outletId
                                if (outlet.outletId == Constants.selectedOutletId) {
                                    this.isChecked = true
                                }
                            }
                            dBinding.outletRadio.addView(radioButton)
                        }
                    }
                    var newId = Constants.selectedOutletId
                    dBinding.outletRadio.setOnCheckedChangeListener { _, i ->
                        newId = i
                    }
                    dBinding.outletBtn.setOnClickListener {
                        Constants.selectedOutletId = newId
                        mainSharedPreferences.edit().putInt(
                            Constants.PREF_SELECTED_OUTLET, newId).apply()
                        outletDialog?.dismiss()
                        CoroutineScope(Dispatchers.Main).launch {
                            val outletData = mainRepository.getOutletDataAsync(newId).await()
                            orderViewModel.setOutletName(outletData?.outletName ?: "MenuOnline")
                        }
                        orderRepository.getAllOrdersOnline(this@MainActivity,
                            Constants.selectedOutletId,0, true)
                        sendBroadcast(Intent(Constants.OUTLET_CHANGE_BROADCAST))
                    }
                }
                setView(dBinding.root)
            }
            outletDialog= builder.create()
            outletDialog.show()
        }
        changeOutletBtn = binding.mainActionBar.changeOutlet
        binding.mainActionBar.navPosBtn.setOnClickListener {
            navigateTo(NAV_POS)
        }
        binding.mainActionBar.navOnlineOrders.setOnClickListener {
            navigateTo(NAV_ONLINE)
        }
        binding.mainActionBar.navOngoingOrders.setOnClickListener {
            navigateTo(NAV_ONGOING)
        }
        binding.mainActionBar.navTables.setOnClickListener {
            navigateTo(NAV_TABLES)
        }
        val logoutReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                try {
                    findNavController(binding.fragmentContainerView).navigate(R.id.navigation_login)
                } catch (e: Exception) {
                    Log.e("ErrorSignOut", "$e")
                    Sentry.captureMessage("SignOutError : $e")
                }
            }
        }
        registerReceiver(logoutReceiver, IntentFilter(Constants.LOG_OUT_NOW_BROADCAST))
        animator = Animator()
        mediaPlayer = MediaPlayer.create(this, R.raw.alarmtone)
        orderViewModel.onlineWithFutureOrderCount.observe( this, { count ->
            binding.mainActionBar.onlineOrderText.text = "ONLINE ORDERS ( $count )"
            if (count > 0) {
                if(mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer = MediaPlayer.create(this, R.raw.alarmtone)
                mediaPlayer.isLooping = true
                mediaPlayer.start()
                binding.mainActionBar.onlineOutline.visibility = View.VISIBLE
                animator.startBlinking(binding.mainActionBar.onlineOutline, 300)
            } else {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                binding.mainActionBar.onlineOutline.visibility = View.GONE
                animator.stopBlinking()
            }
        })
        orderViewModel.ongoingOrdersCount.observe( this, { count ->
            binding.mainActionBar.navOngoingOrders.text = "ONGOING ORDERS ( $count )"
        })
        val internetChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (isInternetAvailable(this@MainActivity) && Constants.authorization != "-1") {
                    orderRepository.getAllOrdersOnline(this@MainActivity,
                        Constants.selectedOutletId, 0,false)
                }
            }
        }
        val iFilter = IntentFilter().apply {
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        }
        registerReceiver(internetChangeReceiver, iFilter)
        orderViewModel.lastSyncTiming.observe(this,  { time ->
            mainSharedPreferences.edit().putLong(Constants.PREF_SYN_TIME ,time).apply()
        })
        val idleReceiver = ScreenReceiver()
        val iFilter2 = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(idleReceiver, iFilter2)
    }

    private fun navigateTo(nav: Int) {
        currentNavigation = nav
        posBtn.isSelected = false
        kitchenBtn.isSelected = false
        counterBtn.isSelected = false
        allOrderBtn.isSelected = false
        mainDrawerBinding.drawerMain.closeDrawer(GravityCompat.START)
        val navController = findNavController(binding.fragmentContainerView)
        when(nav) {
            NAV_POS -> {
                posBtn.isSelected = true
                navController.navigate(R.id.navigation_pos)
                collapseActionBar()
            }
            NAV_ALL_ORDERS -> {
                allOrderBtn.isSelected = true
                navController.navigate(R.id.navigation_all_orders)
                expandActionBar()
            }
            NAV_KITCHEN -> {
                kitchenBtn.isSelected = true
                navController.navigate(R.id.navigation_kitchen)
                expandActionBar()
            }
            NAV_COUNTER -> {
                counterBtn.isSelected = true
                navController.navigate(R.id.navigation_counter)
                expandActionBar()
            }
            NAV_ONLINE -> {
                navController.navigate(R.id.navigation_online_orders)
                expandActionBar()
            }
            NAV_ONGOING -> {
                navController.navigate(R.id.navigation_ongoing_orders)
                expandActionBar()
            }
            NAV_TABLES -> {
                navController.navigate(R.id.navigation_table)
                expandActionBar()
            }
        }
    }

    override fun onBackPressed() {
        if (currentNavigation == NAV_POS) {
            finishAffinity()
        } else {
            super.onBackPressed()
        }
    }
    companion object {
        var deviceID: String = ""
        var mediaPlayer = MediaPlayer()
        lateinit var mainRepository: MainRepository
        lateinit var orderRepository : OrderRepository
        lateinit var mainViewModel : MainViewModel
        lateinit var orderViewModel : OrderViewModel
        lateinit var fManager : FragmentManager
        lateinit var mainDrawerLayout: DrawerLayout
        lateinit var mainActionBar : BelowActionBarBinding
        lateinit var mainSharedPreferences: SharedPreferences
        @SuppressLint("StaticFieldLeak")
        lateinit var progressDialogRepository: ProgressDialogRepository
        @SuppressLint("StaticFieldLeak")
        lateinit var changeOutletBtn : ImageView
        @SuppressLint("StaticFieldLeak")
        lateinit var dummyViewActionBar : View
        @SuppressLint("StaticFieldLeak")
        lateinit var switchLocationBtn : ImageView

        var onPOSFragment = false

        fun hideActionBar() {
            mainActionBar.root.visibility = View.GONE
        }

        fun showActionBar() {
            mainActionBar.root.visibility = View.VISIBLE
        }

        fun expandActionBar() {
            dummyViewActionBar.visibility = View.GONE
        }

        fun collapseActionBar()  {
            dummyViewActionBar.visibility = View.VISIBLE
        }

        fun hideChangeLocationBtnAction() {
            switchLocationBtn.visibility = View.INVISIBLE
        }

        fun showChangeLocationBtnAction() {
            switchLocationBtn.visibility = View.VISIBLE
        }

        fun isInternetAvailable(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val n = cm.activeNetwork
                if (n != null) {
                    val nc = cm.getNetworkCapabilities(n)
                    return nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                }
                return false
            } else {
                val netInfo = cm.activeNetworkInfo
                return netInfo != null && netInfo.isConnectedOrConnecting
            }
        }

        fun formatPhone(num: String) : String {
            var returnStr = ""
            if (num.length <= 4) {
                returnStr = num
            } else {
                val revereStr = StringBuffer(num).reverse().toString()
                val group4 = revereStr.substring(0, 4)
                returnStr += "$group4-"
                var pendingStr = revereStr.substring(4)
                while (pendingStr.length > 3) {
                    val group3 = pendingStr.substring(0, 3)
                    returnStr += "$group3-"
                    pendingStr = pendingStr.substring(3)
                }
                returnStr += pendingStr
                returnStr = StringBuffer(returnStr).reverse().toString()
            }
            return returnStr

        }

        fun logOutNow(ctx : Context) {
            Constants.authorization = "-1"
            mainSharedPreferences.edit().apply {
      //          putString(Constants.PREF_AUTHORIZATION, Constants.authorization)
                putBoolean(Constants.PREF_IS_LOGGED_IN, false)
                putInt(Constants.PREF_IS_ALL_DATA, 1)
                putInt(Constants.PREF_SELECTED_OUTLET, 1)
            }.apply()
            val cartFile = File(ctx.getExternalFilesDir(Constants.cartFile), Constants.cartFile)
            if (cartFile.exists()) {
                cartFile.delete()
            }
            hideActionBar()
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            ctx.sendBroadcast(Intent(Constants.LOG_OUT_NOW_BROADCAST))
        }

    }
}