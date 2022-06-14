package com.tjcg.nentopos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.adapters.OnlineOrderAdapter
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.FragmentOnlineOrderBinding
import kotlin.collections.ArrayList

class OnlineOrderFragment : Fragment() {

    private lateinit var binding : FragmentOnlineOrderBinding
    private lateinit var ctx: Context
    private var currentTab = 0
    private var activeFutureOrders = emptyList<OrdersEntity>()
    private var onlineOrders = emptyList<OrdersEntity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        MainActivity.orderViewModel.setAllOrders()
        binding = FragmentOnlineOrderBinding.inflate(inflater)
        binding.newOrder.setOnClickListener {
            currentTab = 0
            selectTab()
        }
        binding.accepted.setOnClickListener {
            currentTab = 1
            selectTab()
        }
        binding.completed.setOnClickListener {
            currentTab = 2
            selectTab()
        }
        binding.cancelled.setOnClickListener {
            currentTab = 3
            selectTab()
        }
        binding.futureORder.setOnClickListener {
            currentTab = 4
            selectTab()
        }
        setupObservers()
        selectTab()
        return binding.root
    }

    private fun setupObservers() {
        MainActivity.orderViewModel.outletName.observe(
            viewLifecycleOwner, { name ->
                binding.textViewOutlet.text = name
            }
        )
        MainActivity.orderViewModel.onlineOrders.observe(
            viewLifecycleOwner, { list ->
                onlineOrders = list
                updateOnlineOrdersList()
            }
        )
        MainActivity.orderViewModel.acceptOrders.observe(
            viewLifecycleOwner, { list ->
                val sortedList= list.sortedByDescending { it.order_id.toInt() }
                binding.productRecyclerViewAccept.layoutManager = LinearLayoutManager(ctx)
                binding.productRecyclerViewAccept.adapter = OnlineOrderAdapter(ctx, sortedList, findNavController())
                selectTab()
            }
        )
        MainActivity.orderViewModel.allCompletedOrders.observe(
            viewLifecycleOwner, { list ->
                val sortedList= list.sortedByDescending { it.order_id }
                binding.productRecyclerViewCompleted.layoutManager = LinearLayoutManager(ctx)
                binding.productRecyclerViewCompleted.adapter = OnlineOrderAdapter(ctx, sortedList, findNavController())
                selectTab()
            }
        )
        MainActivity.orderViewModel.allCancelledOrders.observe(
            viewLifecycleOwner, { list ->
                val sortedList= list.sortedByDescending { it.order_id }
                binding.productRecyclerViewCancel.layoutManager = LinearLayoutManager(ctx)
                binding.productRecyclerViewCancel.adapter = OnlineOrderAdapter(ctx, sortedList, findNavController())
                selectTab()
            }
        )
        MainActivity.orderViewModel.activeFutureOrders.observe(
            viewLifecycleOwner, {
                activeFutureOrders = it
                updateOnlineOrdersList()
            }
        )
        MainActivity.orderViewModel.steadyFutureOrders.observe(
            viewLifecycleOwner, { list ->
                val filteredList= list.sortedByDescending { it.order_id }
                binding.productRecyclerFuture.layoutManager = LinearLayoutManager(ctx)
                binding.productRecyclerFuture.adapter = OnlineOrderAdapter(ctx, filteredList, findNavController())
                selectTab()
            }
        )
    }

    private fun updateOnlineOrdersList() {
        val mainList = ArrayList<OrdersEntity>()
        mainList.addAll(onlineOrders)
        val sortedList = mainList.sortedByDescending { it.order_id }
        binding.productRecyclerView.layoutManager = LinearLayoutManager(ctx)
        binding.productRecyclerView.adapter = OnlineOrderAdapter(ctx, sortedList, findNavController())
        selectTab()
    }

    private fun showOnlineOrders() {
        if (onlineOrders.isNullOrEmpty() && activeFutureOrders.isNullOrEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            return
        } else {
            binding.noDataFound.visibility = View.GONE
            binding.productRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAcceptedOrders() {
        if (MainActivity.orderViewModel.acceptOrders.value.isNullOrEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            return
        }
        binding.noDataFound.visibility = View.GONE
        binding.productRecyclerViewAccept.visibility = View.VISIBLE

    }

    private fun showCompletedOrders() {
        if (MainActivity.orderViewModel.allCompletedOrders.value.isNullOrEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            return
        }
        binding.noDataFound.visibility = View.GONE
        binding.productRecyclerViewCompleted.visibility = View.VISIBLE
    }

    private fun showCancelledOrders() {
        if (MainActivity.orderViewModel.allCancelledOrders.value.isNullOrEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            return
        }
        binding.noDataFound.visibility = View.GONE
        binding.productRecyclerViewCancel.visibility = View.VISIBLE
    }

    private fun showFutureOrders() {
        if (MainActivity.orderViewModel.steadyFutureOrders.value.isNullOrEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            return
        }
        binding.noDataFound.visibility = View.GONE
        binding.productRecyclerFuture.visibility = View.VISIBLE
    }

    private fun selectTab() {
        binding.newOrder.isSelected = false
        binding.accepted.isSelected = false
        binding.completed.isSelected = false
        binding.cancelled.isSelected = false
        binding.futureORder.isSelected = false
        binding.productRecyclerView.visibility = View.GONE
        binding.productRecyclerViewAccept.visibility = View.GONE
        binding.productRecyclerViewCompleted.visibility = View.GONE
        binding.productRecyclerViewCancel.visibility = View.GONE
        binding.productRecyclerFuture.visibility = View.GONE
        when(currentTab) {
            0 -> {
                binding.newOrder.isSelected = true
                showOnlineOrders()
            }
            1 -> {
                binding.accepted.isSelected = true
                showAcceptedOrders()
            }
            2 -> {
                binding.completed.isSelected = true
                showCompletedOrders()
            }
            3 -> {
                binding.cancelled.isSelected = true
                showCancelledOrders()
            }
            4 -> {
                binding.futureORder.isSelected = true
                showFutureOrders()
            }
        }
    }
}