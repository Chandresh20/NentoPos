package com.tjcg.nentopos.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.data.TableData
import com.tjcg.nentopos.databinding.FragmentTableBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TableFragment : Fragment() {

    lateinit var binding: FragmentTableBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTableBinding.inflate(layoutInflater)
        CoroutineScope(Dispatchers.Main).launch {
            val allTableData =
                MainActivity.mainRepository.getTablesForOutletAsync(Constants.selectedOutletId).await()
            if (!allTableData.isNullOrEmpty()) {
                var iconPositions = ""
                for (table in allTableData) {
                    iconPositions+= "${table.tableName} : ${table.iconPosition}\n"
                }
                Log.d("IconPositions", iconPositions)
            }
        }
        return binding.root
    }
}