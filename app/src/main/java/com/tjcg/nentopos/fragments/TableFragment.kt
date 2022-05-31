package com.tjcg.nentopos.fragments

import android.content.Context
import android.content.Context.DISPLAY_SERVICE
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.databinding.TableLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val MARGIN_UNSCALLED = 35
class TableFragment : Fragment(){

    private lateinit var binding: TableLayoutBinding
    private lateinit var ctx: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = TableLayoutBinding.inflate(inflater, container, false)
        if (Constants.displayWidth == 0) {
            val displayManager = ctx.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.displays
            val point = Point()
            display[0].getSize(point)
            Constants.displayWidth = point.x
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
}