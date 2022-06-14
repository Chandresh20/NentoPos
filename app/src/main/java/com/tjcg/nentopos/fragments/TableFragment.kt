package com.tjcg.nentopos.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.DISPLAY_SERVICE
import android.content.Context.WINDOW_SERVICE
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.Cart
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.data.TableData
import com.tjcg.nentopos.databinding.DialogTableInfoBinding
import com.tjcg.nentopos.databinding.ItemTableOrderBinding
import com.tjcg.nentopos.databinding.TableLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val MARGIN_UNSCALLED = 35
class TableFragment : Fragment(){

    private lateinit var binding: TableLayoutBinding
    private lateinit var ctx: Context

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = TableLayoutBinding.inflate(inflater, container, false)
        if (Constants.displayWidth == 0) {
            Constants.displayWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val winManager = ctx.getSystemService(WINDOW_SERVICE) as WindowManager
                val winMetrics = winManager.currentWindowMetrics
                val bounds = winMetrics.bounds
                bounds.width()
            } else {
                val displayManager = ctx.getSystemService(DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.displays
                val point = Point()
                display[0].getSize(point)
                point.x
            }
        }
        val scalingFactor : Float = Constants.displayWidth.toFloat() / 800
        val tableDir = File(ctx.getExternalFilesDir(Constants.TABLE_IMAGE_DIR), Constants.TABLE_IMAGE_DIR)
        binding.tableGrid.columnCount = 6
        CoroutineScope(Dispatchers.Main).launch {
            val allTables = MainActivity.mainRepository.getTablesForOutletAsync(Constants.selectedOutletId).await()
            if (!allTables.isNullOrEmpty()) {
                var position = 0
                for (table in allTables) {
                    val relativeLayout = RelativeLayout(ctx)
                    val tableImage = ImageView(ctx)
                    tableImage.layoutParams = ViewGroup.LayoutParams(
                        200, 200)
                    if (table.assignedOrNot == 1) {
                        tableImage.background = ResourcesCompat.getDrawable(
                            ctx.resources, R.drawable.table_border_red, ctx.resources.newTheme())
                        tableImage.setOnClickListener {
                            val dialog = TableOrders.getInstance(ctx, table.relatedOrders, table.tableId ?: 0)
                            dialog.show(MainActivity.fManager, "table")
                        }
                    } else {
                        tableImage.background = ResourcesCompat.getDrawable(
                            ctx.resources, R.drawable.table_border_green, ctx.resources.newTheme())
                    }
                    tableImage.setPadding(20)
                    val tableImageFile = File(tableDir, "${table.tableId}.jpg")
                    Log.d("TableIMage", "Searching for $tableImageFile")
                    if (tableImageFile.exists()) {
                        val tbMap = BitmapFactory.decodeFile(tableImageFile.toString())
                        tableImage.setImageBitmap(tbMap)
                    }
                    val positionStr = table.iconPosition ?: "0; 0px; 0px"
                    Log.d("POSITION",positionStr)
                    val leftPosStr = positionStr.split(";")[1]
                    var indexOfSp = leftPosStr.lastIndexOf(" ")
                    var indexOfPx = leftPosStr.indexOf("px")
                    val leftPos = leftPosStr.substring(indexOfSp, indexOfPx)
                    val topPosStr = positionStr.split(";")[2]
                    indexOfSp = topPosStr.lastIndexOf(" ")
                    indexOfPx = topPosStr.indexOf("px")
                    val topPos = topPosStr.substring(indexOfSp, indexOfPx)
                    Log.d("LEFT", leftPos)
                    Log.d("TOP", topPos)
                    relativeLayout.translationX = leftPos.toFloat() * scalingFactor
                    relativeLayout.translationY = topPos.toFloat() * scalingFactor
                    relativeLayout.addView(tableImage)
                    val textParams : RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT ,ViewGroup.LayoutParams.WRAP_CONTENT)
                    val textView = TextView(ctx).apply {
                        this.layoutParams = textParams
                            this.setTextColor(Color.WHITE)
                        this.text = "Table:${table.tableName}\nCapacity:${table.personCapacity}"
                    }
                    relativeLayout.addView(textView)
                    binding.tableGrid.addView(relativeLayout)
                    val marginLayout: ViewGroup.MarginLayoutParams = relativeLayout.layoutParams as ViewGroup.MarginLayoutParams
                    marginLayout.setMargins(
                        (MARGIN_UNSCALLED*scalingFactor).toInt(),
                        (MARGIN_UNSCALLED*scalingFactor).toInt(),
                        (MARGIN_UNSCALLED*scalingFactor).toInt(), (MARGIN_UNSCALLED*scalingFactor).toInt())
                    position += 1
                }
            }
        }
        return binding.root
    }

    class TableOrders : DialogFragment() {

        private lateinit var tBinding : DialogTableInfoBinding
        lateinit var ctx: Context
        lateinit var relatedOrders : List<TableData.RelatedOrderInfo>
        var tableId : Int = 0
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            tBinding = DialogTableInfoBinding.inflate(inflater, container, false)
            tBinding.tableInfoRecycler.layoutManager = LinearLayoutManager(ctx)
            tBinding.tableInfoRecycler.adapter = RelatedOrderAdapter(relatedOrders)
            tBinding.createOrderBtn.setOnClickListener {
                Cart.selectedTableCompanion = tableId
                findNavController().navigate(R.id.navigation_pos)
                this.dialog?.dismiss()
            }
            tBinding.closeButton2.setOnClickListener {
                this.dialog?.dismiss()
            }
            return tBinding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            super.onViewCreated(view, savedInstanceState)
        }

        companion object {
            fun getInstance(ctx: Context, relatedOrders : List<TableData.RelatedOrderInfo>?,
                tableId: Int) : TableOrders {
                val dialog = TableOrders()
                dialog.ctx = ctx
                dialog.relatedOrders = relatedOrders ?: emptyList()
                dialog.tableId = tableId
                return dialog
            }
        }

        inner class RelatedOrderAdapter(private val relatedOrders1 : List<TableData.RelatedOrderInfo>) : RecyclerView.Adapter<RelatedOrderAdapter.RHolder>() {

            inner class RHolder(val binding: ItemTableOrderBinding) : RecyclerView.ViewHolder(binding.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RHolder =
                RHolder(ItemTableOrderBinding.inflate(LayoutInflater.from(ctx), parent, false))

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: RHolder, position: Int) {
                val order = relatedOrders1[position]
                holder.binding.orderNo.text = order.orderId.toString()
                holder.binding.waiterName.text = "${order.firstName} ${order.lastName}"
                holder.binding.status.text = Constants.getOrderStatus(order.orderStatus ?: 0)
                holder.binding.payment.text = if (order.billStatus == 1) {
                    "PAID"
                } else {
                    "UNPAID"
                }
                holder.binding.orderEdit.setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (Constants.databaseBusy) {
                            MainActivity.progressDialogRepository.showAlertDialog(
                                "Database is busy, please try again in few minutes")
                            return@launch
                        }
                        val orderEntity = MainActivity.orderRepository.getSingleOrderAsync(
                            Constants.selectedOutletId, order.orderId ?: 0L).await()
                        POSFragment.orderToUpdate = orderEntity
                            findNavController().navigate(R.id.navigation_pos)
                        this@TableOrders.dialog?.dismiss()
                    }
                }
            }

            override fun getItemCount(): Int = relatedOrders1.size
        }
    }
}