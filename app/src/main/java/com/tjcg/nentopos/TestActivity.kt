package com.tjcg.nentopos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tjcg.nentopos.data.ProductSubModifier
import com.tjcg.nentopos.databinding.ActivityTestBinding
import com.tjcg.nentopos.databinding.ItemCartSubmodifierHalfAndHalfBinding
import com.tjcg.nentopos.databinding.ItemCartSubmodifiersBinding

class TestActivity : AppCompatActivity() {

    lateinit var binding : ActivityTestBinding
    private var available = 4
    private var selected =0f
    private var included = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.testRecycler.layoutManager = LinearLayoutManager(this)
        val list = ArrayList<SubMods>()
        list.add(SubMods(1, "MY product", 30f, 0))
        list.add(SubMods(2, "gas", 7.5f, 0))
        list.add(SubMods(3, "diesel", 55f, 0))
        list.add(SubMods(4, "nihatjh", 11f, 0))
        list.add(SubMods(5, "fasks", 89f, 0))
        binding.testRecycler.adapter = SubModHalfAndHalfAdapter(list, 4, 1)
    }

    class CalcItem(val id: Int?,val price: Float?,val qty: Float?)

    fun caluclateTotal(calList : ArrayList<CalcItem>) {
        val sortedList = calList.sortedWith(compareBy({ it.price }, { it.qty }))
        var payableModifierPrice = 0f
        var freeModifier1 = (included ?: 0).toFloat()

        for (product in sortedList) {
            if (product.qty == 0f)
                continue
            freeModifier1 -= product.qty ?: 1f
            if (freeModifier1 >= 0) {
                Log.d("Modifier0", "do nothing")
            } else {
                payableModifierPrice += (product.price ?: 0f)
            }
        }
        Log.d("InvoiceFragModifier", "final price $payableModifierPrice")
    }

    fun showTotal(list: ArrayList<SubMod2>) {
        selected = 0f
        val calcItems = ArrayList<CalcItem>()
        for (item in list) {
            if (item.is2xMod) {
                calcItems.add(CalcItem(item.id, item.price, item.qty))
                calcItems.add(CalcItem(item.id, item.price, item.qty))
                selected += ((item.qty ?: 0f) * 2)
            } else {
                calcItems.add(CalcItem(item.id, item.price, item.qty))
                selected += item.qty ?: 0f
            }
        }
        Log.d("selected", "$selected")
        caluclateTotal(calcItems)

    /*    var qty = 0f
        var total =0f
        for (item in list) {
            if (item.is2xMod) {
                qty += (item.qty ?: 0f) * 2
                total += (item.price ?: 0f) * 2
            } else {
                qty += (item.qty ?: 0f)
                total += (item.price ?: 0f)
            }
            Log.d("InList", "${item.label} : ${item.qty} : ${item.price}")
        }
        binding.totalText.text = "$total : $qty"  */
    }

    inner class SubModHalfAndHalfAdapter(
        val listH: List<SubMods>,
        val choosable: Int?,
        val is2xMod: Int?
    ) :
        RecyclerView.Adapter<SubModHalfAndHalfAdapter.MyHolder>() {

        inner class MyHolder(val sBinding: ItemCartSubmodifierHalfAndHalfBinding) :
            RecyclerView.ViewHolder(sBinding.root)

        val tempList = ArrayList<SubMod2>()

        fun addToList(subMod2 : SubMod2) {
            if (tempList.isNullOrEmpty()) {
                tempList.add(subMod2)
                showTotal(tempList)
                return
            }
            var toReplace :SubMod2? = null
            for (temp in tempList) {
                if (temp.id == subMod2.id) {
                    toReplace = temp
                    break
                }
            }
            if (toReplace != null) {
                tempList.remove(toReplace)
                tempList.add(subMod2)
            } else {
                tempList.add(subMod2)
            }
            showTotal(tempList)
        }

        fun removeFromList(id: Int) {
            var itemToRemove : SubMod2? = null
            for (temp in tempList) {
                if (temp.id == id) {
                    itemToRemove = temp
                    break
                }
            }
            if (itemToRemove != null) {
                tempList.remove(itemToRemove)
            }
            showTotal(tempList)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(
                ItemCartSubmodifierHalfAndHalfBinding.inflate(
                   layoutInflater, parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            var currentSelection = 0
            var is2xApplied = false
            val finalPriceText = holder.sBinding.finalPrice
            val subMod = if (!listH.isNullOrEmpty()) {
                listH[position]
            } else {
                null
            }
            if (is2xMod == 1) {
                holder.sBinding.check2x.visibility = View.VISIBLE
                holder.sBinding.check2x.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if (selected >= available) {
                            Toast.makeText(this@TestActivity, "Sorry, only $available available", Toast.LENGTH_SHORT).show()
                            compoundButton.isChecked= false
                            return@setOnCheckedChangeListener
                        }
                    }
                    is2xApplied = b
                    if (currentSelection == 1) {
                        addToList(SubMod2(subMod?.id, subMod?.label, ((subMod?.price ?: 0f)) , 1f, is2xApplied))
                        finalPriceText.text = if (is2xApplied) {
                            ((subMod?.price ?: 0f) * 2f).toString()
                        } else {
                            subMod?.price.toString()
                        }
                    } else if (currentSelection == 2 || currentSelection == 3) {
                        addToList(SubMod2(subMod?.id, subMod?.label, ((subMod?.price ?: 0f) / 2) , 0.5f, is2xApplied))
                        finalPriceText.text = if (is2xApplied) {
                            (subMod?.price).toString()
                        } else {
                            ((subMod?.price ?: 0f) / 2).toString()
                        }
                    }
                }
            }
            holder.sBinding.subModName.text = subMod?.label
            val check1 = holder.sBinding.checkFull
            val check2 = holder.sBinding.checkFHalf
            val check3 = holder.sBinding.checkSHalf
            check1.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    if ((selected + 0.5)  >= available) {
                        Toast.makeText(this@TestActivity, "Sorry, only $available available", Toast.LENGTH_SHORT).show()
                        compoundButton.isChecked= false
                        return@setOnCheckedChangeListener
                    }
                    currentSelection = 1
                    check2.isChecked = false
                    check3.isChecked = false
                    addToList(SubMod2(subMod?.id, subMod?.label, ((subMod?.price ?: 0f)) , 1f, is2xApplied))
                    finalPriceText.text = if (is2xApplied) {
                        ((subMod?.price ?: 0f) * 2f).toString()
                    } else {
                        subMod?.price.toString()
                    }
                } else {
                    if (currentSelection == 1) {
                        removeFromList(subMod?.id ?: 0)
                        currentSelection = 0
                        finalPriceText.text = "0"
                        selected -= 1
                    }
                }
            }
            check2.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    if (selected >= available) {
                        Toast.makeText(this@TestActivity, "Sorry, only $available available", Toast.LENGTH_SHORT).show()
                        compoundButton.isChecked= false
                        return@setOnCheckedChangeListener
                    }
                    check1.isChecked = false
                    check3.isChecked = false
                    currentSelection = 2
                    addToList(SubMod2(subMod?.id, subMod?.label, ((subMod?.price ?: 0f) / 2) , 0.5f, is2xApplied))
                    finalPriceText.text = if (is2xApplied) {
                        (subMod?.price).toString()
                    } else {
                        ((subMod?.price ?: 0f) / 2).toString()
                    }
                } else {
                    if (currentSelection == 2) {
                        removeFromList(subMod?.id ?: 0)
                        currentSelection = 0
                        finalPriceText.text = "0"
                    }
                }
            }
            check3.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    if ( selected >= available) {
                        Toast.makeText(this@TestActivity, "Sorry, only $available available", Toast.LENGTH_SHORT).show()
                        compoundButton.isChecked= false
                        return@setOnCheckedChangeListener
                    }
                    check1.isChecked = false
                    check2.isChecked = false
                    currentSelection = 3
                    addToList(SubMod2(subMod?.id, subMod?.label, ((subMod?.price ?: 0f) / 2) , 0.5f, is2xApplied))
                    finalPriceText.text = if (is2xApplied) {
                        (subMod?.price).toString()
                    } else {
                        ((subMod?.price ?: 0f) / 2).toString()
                    }
                } else {
                    if (currentSelection == 3) {
                        removeFromList(subMod?.id ?: 0)
                        currentSelection = 0
                        finalPriceText.text = "0"
                    }
                }
            }
        }

        override fun getItemCount(): Int = listH.size
    }

    class SubMods(val id:Int, val label: String, val price: Float, val is2xMod: Int)
    class SubMod2(val id:Int?, val label: String?, val price: Float?, val qty: Float?, val is2xMod: Boolean)
}