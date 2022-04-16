package com.tjcg.nentopos.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.adapters.OrdersAdapter
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.FragmentAllOrdersBinding

class AllOrderFragment : Fragment() {

    private lateinit var binding : FragmentAllOrdersBinding
    private lateinit var ctx: Context
    private var currentTab = 0

    private var allOrderList = emptyList<OrdersEntity>()
    private var allPendingList = emptyList<OrdersEntity>()
    private var allProcessList = emptyList<OrdersEntity>()
    private var allReadyList = emptyList<OrdersEntity>()
    private var allCompletedList = emptyList<OrdersEntity>()
    private var allCancelledList = emptyList<OrdersEntity>()
 //   private val orderRepository = MainActivity.orderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        MainActivity.onPOSFragment = false
        MainActivity.orderViewModel.setAllOrders()
        binding = FragmentAllOrdersBinding.inflate(inflater, container, false)
        val permission = MainActivity.mainSharedPreferences
            .getInt(Constants.PREF_PERMISSION_ALL_ORDERS, 0)
        if (permission == 1) {
            binding.noaccess.visibility = View.GONE
            binding.recyclerViewAllOrder.visibility = View.VISIBLE
            binding.recyclerViewAllOrder.layoutManager = LinearLayoutManager(ctx)
            binding.recyclerViewAllOrder.recycledViewPool.setMaxRecycledViews(0,0)
            binding.allOrderBtn.setOnClickListener {
                currentTab = 0
                selectTab()
            }
            binding.pendingBtn.setOnClickListener {
                currentTab = 1
                selectTab()
            }
            binding.inProgressBtn.setOnClickListener {
                currentTab = 2
                selectTab()
            }
            binding.readyBtn.setOnClickListener {
                currentTab = 3
                selectTab()
            }
            binding.completedBtn.setOnClickListener {
                currentTab = 4
                selectTab()
            }
            binding.cancelledBtn.setOnClickListener {
                currentTab =5
                selectTab()
            }
            setupViewModels()
        } else {
            binding.noaccess.visibility = View.VISIBLE
            binding.recyclerViewAllOrder.visibility = View.GONE
            MainActivity.orderViewModel.outletName.observe(
                viewLifecycleOwner, { name ->
                    binding.textViewOutlet.text = name
                })
        }
        return binding.root
    }

    private fun setupViewModels() {
        MainActivity.orderViewModel.outletName.observe(
            viewLifecycleOwner, { name ->
                binding.textViewOutlet.text = name
            })
        MainActivity.orderViewModel.allOrders.observe(
            viewLifecycleOwner, { list ->
                allOrderList = list.sortedByDescending { it.order_id }
                Log.d("AllOrders", "AllOrders : ${list.size}")
                selectTab()
            }
        )
        MainActivity.orderViewModel.allPendingOrders.observe(
            viewLifecycleOwner, { list ->
                allPendingList = list.sortedByDescending { it.order_id }
                selectTab()
            }
        )
        MainActivity.orderViewModel.allProcessingOrders.observe(
            viewLifecycleOwner, { list ->
                allProcessList = list.sortedByDescending { it.order_id }
                selectTab()
            }
        )
        MainActivity.orderViewModel.allReadyOrders.observe(
            viewLifecycleOwner, { list ->
                allReadyList = list.sortedByDescending { it.order_id }
                selectTab()
            }
        )
        MainActivity.orderViewModel.allCompletedOrders.observe(
            viewLifecycleOwner, { list ->
                allCompletedList = list.sortedByDescending { it.order_id }
                selectTab()
            }
        )
        MainActivity.orderViewModel.allCancelledOrders.observe(
            viewLifecycleOwner, { list ->
                allCancelledList = list.sortedByDescending { it.order_id }
                selectTab()
            }
        )
    }

    private fun selectTab() {
        binding.allOrderBtn.isSelected = false
        binding.pendingBtn.isSelected = false
        binding.inProgressBtn.isSelected = false
        binding.readyBtn.isSelected = false
        binding.completedBtn.isSelected = false
        binding.cancelledBtn.isSelected = false
        when(currentTab) {
            0 -> {
                binding.allOrderBtn.isSelected = true
                if (allOrderList.isNullOrEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewAllOrder.visibility = View.GONE
                    return
                }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewAllOrder.visibility = View.VISIBLE
                binding.recyclerViewAllOrder.adapter = OrdersAdapter(ctx, allOrderList, findNavController())
            }
            1 -> {
                binding.pendingBtn.isSelected = true
                if (allPendingList.isNullOrEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewAllOrder.visibility = View.GONE
                    return
                }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewAllOrder.visibility = View.VISIBLE
                binding.recyclerViewAllOrder.adapter = OrdersAdapter(ctx, allPendingList, findNavController())
            }
            2 -> {
                binding.inProgressBtn.isSelected = true
                if (allProcessList.isNullOrEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewAllOrder.visibility = View.GONE
                    return
                }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewAllOrder.visibility = View.VISIBLE
                binding.recyclerViewAllOrder.adapter = OrdersAdapter(ctx, allProcessList, findNavController())
            }
            3 -> {
                binding.readyBtn.isSelected = true
                if (allReadyList.isNullOrEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewAllOrder.visibility = View.GONE
                    return
                }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewAllOrder.visibility = View.VISIBLE
                binding.recyclerViewAllOrder.adapter = OrdersAdapter(ctx, allReadyList, findNavController())
            }
            4 -> {
                binding.completedBtn.isSelected = true
                if (allCompletedList.isNullOrEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewAllOrder.visibility = View.GONE
                    return
                }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewAllOrder.visibility = View.VISIBLE
                binding.recyclerViewAllOrder.adapter = OrdersAdapter(ctx, allCompletedList, findNavController())
            }
            5 -> {
                binding.cancelledBtn.isSelected = true
                if (allCancelledList.isNullOrEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewAllOrder.visibility = View.GONE
                    return
                }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewAllOrder.visibility = View.VISIBLE
                binding.recyclerViewAllOrder.adapter = OrdersAdapter(ctx, allCancelledList, findNavController())
            }
        }
    }
}