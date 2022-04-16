package com.tjcg.nentopos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.adapters.CounterAdapter
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.FragmentCounterDisplayBinding
import java.util.*

class CounterDisplayFragment : Fragment() {

    private lateinit var binding: FragmentCounterDisplayBinding
    private lateinit var ctx: Context
    private var inProcessingList = emptyList<OrdersEntity>()
    private var readyList = emptyList<OrdersEntity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = FragmentCounterDisplayBinding.inflate(
            inflater, container, false)
        val permission = MainActivity.mainSharedPreferences.getInt(Constants.PREF_PERMISSION_COUNTER, 0)
        if (permission == 1) {
            binding.noaccess.visibility = View.GONE
            binding.recyclerViewCounterDisplay.visibility = View.VISIBLE
            setupObservers()
        } else {
            binding.noaccess.visibility = View.VISIBLE
            binding.recyclerViewCounterDisplay.visibility = View.GONE
            MainActivity.orderViewModel.outletName.observe(viewLifecycleOwner,
                androidx.lifecycle.Observer { name ->
                    binding.textViewOutlet.text = name
                })
        }
        return binding.root
    }

    private fun setupObservers() {
        MainActivity.orderViewModel.outletName.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { name ->
                binding.textViewOutlet.text = name
            })
        MainActivity.orderViewModel.kitchenOrdersToday.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { counterList ->
                if (counterList.isEmpty()) {
                    updateRecyclers(inProcessingList, readyList)
                    return@Observer
                }
                for (i in counterList.iterator()) {
                    // check for order accept data(if it is an online order)
                    val remainedTime = if (i.order_accept_date != null) {
                        val separationIndex = i.order_accept_date?.indexOf(" ")
                        val acceptDate = i.order_accept_date?.substring(0, separationIndex ?: 0)
                        val acceptTime = i.order_accept_date?.substring((separationIndex ?: 0) +1)
                        getRemainedTime((acceptDate ?: "01:01:2000"), (acceptTime ?: "00:00:00"), (i.cookedtime ?: "00:00:00"))
                    } else { // it is null if not an online order
                        getRemainedTime((i.order_date ?: "01:01:2000"), (i.order_time ?: "00:00:00"), (i.cookedtime ?: "00:00:00"))
                    }
                    i.remainedTime = if (remainedTime <= 0) {
                        timeOverLong
                    } else {
                        remainedTime
                    }
                }
                val sortedList = counterList.sortedBy { it.remainedTime }
                inProcessingList = sortedList
                updateRecyclers(inProcessingList, readyList)
                /*     binding.noDataFound.visibility = View.GONE
                     binding.recyclerViewCounterDisplay.visibility = View.VISIBLE
                     binding.recyclerViewCounterDisplay.layoutManager = LinearLayoutManager(ctx)
                     binding.recyclerViewCounterDisplay.adapter = CounterAdapter1(ctx, sortedList)*/
            })
        MainActivity.orderViewModel.readyOrdersToday.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { readyList ->
                this.readyList = readyList
                updateRecyclers(inProcessingList, this.readyList)
            })
    }

    private fun updateRecyclers(inProcessing : List<OrdersEntity>, ready: List<OrdersEntity>) {
        if (inProcessing.isNullOrEmpty() && ready.isNullOrEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            binding.recyclerViewCounterDisplay.visibility = View.VISIBLE
            binding.recyclerViewCounterReady.visibility = View.VISIBLE
            return
        }
        binding.noDataFound.visibility = View.GONE
        binding.recyclerViewCounterDisplay.visibility = View.VISIBLE
        binding.recyclerViewCounterReady.visibility = View.VISIBLE
        binding.recyclerViewCounterDisplay.layoutManager = LinearLayoutManager(ctx)
        binding.recyclerViewCounterReady.layoutManager = LinearLayoutManager(ctx)
        binding.recyclerViewCounterDisplay.adapter = CounterAdapter(ctx, inProcessingList)
        binding.recyclerViewCounterReady.adapter = CounterAdapter(ctx, readyList)
    }

    private fun getRemainedTime(orderDate: String, orderTime: String, cookedTime: String): Long {
        val orderPlacedString = orderTime.split(":".toRegex()).toTypedArray()
        val orderDateString = orderDate.split("-".toRegex()).toTypedArray()
        val orderCalendar = Calendar.getInstance()
        orderCalendar.set(Calendar.YEAR, orderDateString[0].toInt())
        orderCalendar.set(Calendar.MONTH, (orderDateString[1].toInt()) - 1)
        orderCalendar.set(Calendar.DAY_OF_MONTH, orderDateString[2].toInt())
        orderCalendar.set(Calendar.HOUR_OF_DAY, orderPlacedString[0].toInt())
        orderCalendar.set(Calendar.MINUTE, orderPlacedString[1].toInt())
        orderCalendar.set(Calendar.SECOND, orderPlacedString[2].toInt())

        val orderPlacedInMill = orderCalendar.timeInMillis
        val cookedTimeString = cookedTime.split(":".toRegex()).toTypedArray()
        val hour = cookedTimeString[0].toInt()
        val minutes = cookedTimeString[1].toInt()
        val seconds = cookedTimeString[2].toInt()

        val timeOverMill = orderPlacedInMill + ((hour*3600 + minutes*60 + seconds) * 1000)
        val currentTime = Calendar.getInstance().timeInMillis
        return timeOverMill - currentTime
    }

    companion object {
        const val timeOverLong : Long = 18000000
    }

}