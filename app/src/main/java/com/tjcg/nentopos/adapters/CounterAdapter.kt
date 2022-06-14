package com.tjcg.nentopos.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.data.OrdersEntity
import com.tjcg.nentopos.databinding.ItemCounterDisplayBinding
import com.tjcg.nentopos.fragments.CounterDisplayFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CounterAdapter(private val ctx: Context, private val list : List<OrdersEntity>) :
    RecyclerView.Adapter<CounterAdapter.MyHolder>() {

    inner class MyHolder(val binding: ItemCounterDisplayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemCounterDisplayBinding.inflate(
            LayoutInflater.from(ctx), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val counterData = list[position]
        CoroutineScope(Dispatchers.Main).launch {
            holder.binding.tableNo.text = if (counterData.table_no == null) {
                "NA"
            } else {
                val tableData = MainActivity.mainRepository.getOneTableDataAsync(counterData.table_no ?: 0).await()
                if (tableData != null) {
                    tableData.tableName
                } else {
                    "NA"
                }
            }
            val menuName = MainActivity.mainRepository.getMenuNameAsync(counterData.menu_id ?: 0).await()
            holder.binding.orderNo.text = if (menuName.isNullOrBlank()) {
                counterData.order_id.toString()
            } else {
                "${counterData.order_id} ($menuName)"
            }
        }
        val totalItems = (counterData.itemsInfo?.size ?: 0) + (counterData.addOns?.size ?: 0)
        var completedItems = 0
        if (!counterData.itemsInfo.isNullOrEmpty()) {
            for (item in counterData.itemsInfo!!) {
                if (item.foodStatus == 1) {
                    completedItems += 1
                }
            }
        }
        if (!counterData.addOns.isNullOrEmpty()) {
            for (addOn in counterData.addOns!!) {
                if (addOn.foodStatus ==1) {
                    completedItems += 1
                }
            }
        }
        holder.binding.completedItems.text = "$completedItems / $totalItems"
        val formattedDate = getTimeWithAMPMFromTime(counterData.order_time)
        holder.binding.orderTime.text = formattedDate
        holder.binding.orderStatus.text = Constants.getOrderStatus(counterData.order_status!!)
        if (counterData.order_status == Constants.ORDER_STATUS_READY) {
            holder.binding.remainingTime.text = "NA"
            holder.binding.completedItems.text = "-"
        } else {
            if (counterData.remainedTime != CounterDisplayFragment.timeOverLong) {
                startTimer(holder.binding.remainingTime, counterData.remainedTime)
            } else {
                holder.binding.remainingTime.text = ctx.resources.getString(R.string.time_over)
            }
        }
    }

    private fun startTimer(textView: TextView, remainedTime: Long) {
        if (remainedTime <= 0) {
            textView.text = ctx.resources.getString(R.string.time_over)
            return
        }
        val cal = Calendar.getInstance()
        object : CountDownTimer(remainedTime, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(p0: Long) {
                cal.timeInMillis = p0 - cal.timeZone.rawOffset
                val hour = "${cal.get(Calendar.HOUR_OF_DAY)}"
                var minutes = "${cal.get(Calendar.MINUTE)}"
                if (minutes.length == 1) {
                    minutes = "0$minutes"
                }
                var sec = "${cal.get(Calendar.SECOND)}"
                if (sec.length == 1) {
                    sec = "0$sec"
                }
                textView.text = "$hour:$minutes:$sec"
            }

            override fun onFinish() {
                textView.text = ctx.resources.getString(R.string.time_over)
            }
        }.start()
    }

    @SuppressLint("SimpleDateFormat")
    private fun getTimeWithAMPMFromTime(dt: String?): String? {
        return try {
            val inFormat = SimpleDateFormat("HH:mm:ss")
            val date = inFormat.parse(dt ?: "0")
            val outFormat = SimpleDateFormat("hh:mm a")
            outFormat.format(date ?: Date())
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }

    override fun getItemCount(): Int = list.size
    }