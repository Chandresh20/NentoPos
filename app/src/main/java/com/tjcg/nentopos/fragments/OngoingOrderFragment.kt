package com.tjcg.nentopos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.adapters.OngoingOrderAdapter
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.FragmentOngoingOrderBinding

class OngoingOrderFragment : Fragment() {

    private lateinit var binding : FragmentOngoingOrderBinding
    private lateinit var ctx: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = FragmentOngoingOrderBinding.inflate(
            inflater, container, false)
        setupObservers()
        return binding.root
    }

    private fun setupObservers() {
        MainActivity.orderViewModel.outletName.observe(
            viewLifecycleOwner, { name ->
                binding.textViewOutlet.text = name
            })
        MainActivity.orderViewModel.readyOrdersToday.observe(
            viewLifecycleOwner, { updateList() }
        )
        MainActivity.orderViewModel.kitchenOrdersToday.observe(
            viewLifecycleOwner, { updateList() }
        )
    }

    private fun updateList() {
        val listKitchen = if (MainActivity.orderViewModel.kitchenOrdersToday.value.isNullOrEmpty()) {
            ArrayList()
        } else {
            MainActivity.orderViewModel.kitchenOrdersToday.value as List<OrdersEntity>
        }
        val listReady = if (MainActivity.orderViewModel.readyOrdersToday.value.isNullOrEmpty()) {
            ArrayList()
        } else {
            MainActivity.orderViewModel.readyOrdersToday.value as List<OrdersEntity>
        }
        val list = ArrayList<OrdersEntity>()
        list.addAll(listKitchen)
        list.addAll(listReady)
        if (list.isEmpty()) {
            binding.noDataFound.visibility = View.VISIBLE
            binding.recyclerViewOngoingOrder.visibility = View.GONE
            return
        }
        val sortedList = list.sortedByDescending { it.order_id.toInt() }
        binding.noDataFound.visibility = View.GONE
        binding.recyclerViewOngoingOrder.visibility = View.VISIBLE
        binding.recyclerViewOngoingOrder.layoutManager = GridLayoutManager(ctx, 3)
        binding.recyclerViewOngoingOrder.adapter = OngoingOrderAdapter(ctx, sortedList, findNavController())
    }
}