package com.tjcg.nentopos.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.adapters.KitchenAdapter
import com.tjcg.nentopos.databinding.FragmentKitchenDisplayBinding

class KitchenDisplayFragment : Fragment() {

    private lateinit var binding: FragmentKitchenDisplayBinding
    private lateinit var ctx : Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = FragmentKitchenDisplayBinding.inflate(
            inflater, container, false)
//        OrderRepository1(ctx)
        val permission = MainActivity.mainSharedPreferences
            .getInt(Constants.PREF_PERMISSION_KITCHEN, 0)
        Log.d("Kitchen", "Permission: $permission")
        if (permission == 1) {
            binding.noaccess.visibility = View.GONE
            binding.recyclerViewKitchenOrder.visibility = View.VISIBLE
            binding.recyclerViewKitchenOrder.layoutManager =
                GridLayoutManager(ctx, 3)
            setupObservers()
        } else {
            binding.noaccess.visibility = View.VISIBLE
            binding.recyclerViewKitchenOrder.visibility = View.GONE
            MainActivity.orderViewModel.outletName.observe( viewLifecycleOwner, { name ->
                binding.textViewOutlet.text = name
            })
        }
        return binding.root
    }

    private fun setupObservers() {
        MainActivity.orderViewModel.outletName.observe( viewLifecycleOwner, { name ->
                binding.textViewOutlet.text = name
            })
        MainActivity.orderViewModel.kitchenOrdersToday.observe(viewLifecycleOwner,
            Observer { orders ->
                Log.d("KitchenView", "Orders :${orders.size}")
                if (orders.isEmpty()) {
                    binding.noDataFound.visibility = View.VISIBLE
                    binding.recyclerViewKitchenOrder.visibility = View.GONE
                    return@Observer
                }
                val sortedOrders = orders.sortedByDescending { it.order_id }
                binding.noDataFound.visibility = View.GONE
                binding.recyclerViewKitchenOrder.visibility = View.VISIBLE
                binding.recyclerViewKitchenOrder.adapter =
                    KitchenAdapter(ctx, sortedOrders)
            })
    }
}