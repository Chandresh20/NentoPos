package com.tjcg.nentopos.dialog

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.databinding.DialogCalculatorBinding
import com.tjcg.nentopos.databinding.DialogCardTerminalBinding
import com.tjcg.nentopos.databinding.DialogNewOrderStatusBinding
import com.tjcg.nentopos.databinding.DialogPaymentMethodsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PaymentDialog(private val ctx: Context, private val totalAmount: Float, private val shownFrom : Int) : DialogFragment() {

    lateinit var paymentBinding : DialogPaymentMethodsBinding
    lateinit var calcBinding : DialogCalculatorBinding
    lateinit var orderBinding : DialogNewOrderStatusBinding
    lateinit var cardTerminalBinding : DialogCardTerminalBinding
    private var cashString = ""
    private var isPointTaken = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        paymentBinding = DialogPaymentMethodsBinding.inflate(inflater, container, false)
        paymentBinding.closeBtn.setOnClickListener {
            this.dialog?.dismiss()
        }
        paymentBinding.paymentCash.setOnClickListener {
            calcBinding = DialogCalculatorBinding.inflate(inflater, container, false)
            this.dialog?.setContentView(calcBinding.root)
            calcBinding.totalAmount.text = totalAmount.format()
            calcBinding.closeButton.setOnClickListener {
                this.dialog?.dismiss()
            }
            calcBinding.btnC.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                cashString = "0"
                isPointTaken = false
                calculateAmount()
            }
            calcBinding.btn1.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("1")
            }
            calcBinding.btn2.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("2")
            }
            calcBinding.btn3.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("3")
            }
            calcBinding.btn4.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("4")
            }
            calcBinding.btn5.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("5")
            }
            calcBinding.btn6.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("6")
            }
            calcBinding.btn7.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("7")
            }
            calcBinding.btn8.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("8")
            }
            calcBinding.btn9.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("9")
            }
            calcBinding.btn0.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("0")
            }
            calcBinding.btn00.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString("00")
            }
            calcBinding.btn55.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCash(5)
            }
            calcBinding.btn1010.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCash(10)
            }
            calcBinding.btn20.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCash(20)
            }
            calcBinding.btn50.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCash(50)
            }
            calcBinding.btn100.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCash(100)
            }
            calcBinding.btn1000.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCash(1000)
            }
            calcBinding.btnPoint.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                addToCashString(".")
            }
            calcBinding.next.setOnClickListener {
                MediaPlayer.create(ctx, R.raw.btnclick).start()
                val cash = cashString.toFloat()
                if (cash < totalAmount) {
                    Log.d("CalcDialog", "Invalid Amount")
                    Toast.makeText(ctx, "Invalide Amount", Toast.LENGTH_LONG).show()
                } else {
                    if (shownFrom == FROM_ONGOING_ORDERS) {
                        val intent = Intent(Constants.PAYMENT_DONE_BROADCAST)
                        intent.putExtra(paymentMethod, Constants.PAYMENT_METHOD_CASH)
                        intent.putExtra(paymentAmount, cashString.toFloat())
                        ctx.sendBroadcast(intent)
                        this.dialog?.dismiss()
                        return@setOnClickListener
                    }
                    orderBinding = DialogNewOrderStatusBinding.inflate(inflater, container, false)
                    this.dialog?.setContentView(orderBinding.root)
                    var orderStatus1 = Constants.ORDER_STATUS_SERVED
                    orderBinding.orderRadio.setOnCheckedChangeListener { _, i ->
                        when(i) {
                            R.id.radioCompleted -> {
                                orderStatus1 = Constants.ORDER_STATUS_SERVED
                            }
                            R.id.radioOnGoing -> {
                                orderStatus1 = Constants.ORDER_STATUS_PROCESSING
                            }
                        }
                    }
                    orderBinding.nextBtn.setOnClickListener {
                        val intent = Intent(Constants.PAYMENT_DONE_BROADCAST)
                        intent.putExtra(orderStatus, orderStatus1)
                        intent.putExtra(paymentMethod, Constants.PAYMENT_METHOD_CASH)
                        intent.putExtra(paymentAmount, cashString.toFloat())
                        ctx.sendBroadcast(intent)
                        this.dialog?.dismiss()
                    }
                }
            }
        }
        paymentBinding.paymentDebit.setOnClickListener {
            orderBinding = DialogNewOrderStatusBinding.inflate(inflater, container, false)
            this.dialog?.setContentView(orderBinding.root)
            var orderStatus1 = Constants.ORDER_STATUS_SERVED
            orderBinding.orderRadio.setOnCheckedChangeListener { _, i ->
                when(i) {
                    R.id.radioCompleted -> {
                        orderStatus1 = Constants.ORDER_STATUS_SERVED
                    }
                    R.id.radioOnGoing -> {
                        orderStatus1 = Constants.ORDER_STATUS_PROCESSING
                    }
                }
            }
            orderBinding.nextBtn.setOnClickListener {
                val intent = Intent(Constants.PAYMENT_DONE_BROADCAST)
                intent.putExtra(orderStatus, orderStatus1)
                intent.putExtra(paymentMethod, Constants.PAYMENT_METHOD_CASH)
                intent.putExtra(paymentAmount, totalAmount)
                ctx.sendBroadcast(intent)
                this.dialog?.dismiss()
            }
        }
        paymentBinding.paymentCredit.setOnClickListener {
            cardTerminalBinding = DialogCardTerminalBinding.inflate(inflater, container, false)
            this.dialog?.setContentView(cardTerminalBinding.root)
            cardTerminalBinding.closeBtn.setOnClickListener {
                this.dialog?.dismiss()
            }
            CoroutineScope(Dispatchers.Main).launch {
                val cardTerminals =
                    MainActivity.mainRepository.getCardTerminalsAsync(Constants.selectedOutletId).await()
                if (!cardTerminals.isNullOrEmpty()) {
                    val terminalArray = ArrayList<String>()
                    terminalArray.add("Select Option")
                    for (card in cardTerminals) {
                        terminalArray.add(card.terminalName ?: "NA")
                    }
                    val arrayAdapter = ArrayAdapter(ctx, R.layout.item_light_spinner, R.id.light_spinner_text, terminalArray)
                    cardTerminalBinding.spinnerCardTerminal.adapter = arrayAdapter
                    cardTerminalBinding.nextBtn.setOnClickListener {
                        orderBinding = DialogNewOrderStatusBinding.inflate(inflater, container, false)
                        this@PaymentDialog.dialog?.setContentView(orderBinding.root)
                        var orderStatus1 = Constants.ORDER_STATUS_SERVED
                        orderBinding.orderRadio.setOnCheckedChangeListener { _, i ->
                            when(i) {
                                R.id.radioCompleted -> {
                                    orderStatus1 = Constants.ORDER_STATUS_SERVED
                                }
                                R.id.radioOnGoing -> {
                                    orderStatus1 = Constants.ORDER_STATUS_PROCESSING
                                }
                            }
                        }
                        orderBinding.nextBtn.setOnClickListener {
                            val intent = Intent(Constants.PAYMENT_DONE_BROADCAST)
                            intent.putExtra(orderStatus, orderStatus1)
                            intent.putExtra(paymentMethod, Constants.PAYMENT_METHOD_CASH)
                            intent.putExtra(paymentAmount, totalAmount)
                            ctx.sendBroadcast(intent)
                            this@PaymentDialog.dialog?.dismiss()
                        }
                    }
                }
            }

        }
        return paymentBinding.root
    }

    private fun addToCashString(digit: String) {
        if (digit == ".") {
            if (!isPointTaken) {
                cashString = "$cashString$digit"
                calculateAmount()
                isPointTaken = true
            }
            return
        }
        cashString = "$cashString$digit"
        calculateAmount()
    }

    private fun addToCash(amount : Int) {
        var cash = if (!cashString.isBlank()) {
            cashString.toFloat()
        } else 0f
        cash += amount
        calcBinding.customerPaid.text = cash.format()
        val due = totalAmount - cash
        calcBinding.balanceDue.text = due.format()
        cashString = cash.toString()
    }

    private fun calculateAmount() {
        val cash = cashString.toFloat()
        calcBinding.customerPaid.text = cash.format()
        val due = totalAmount - cash
        calcBinding.balanceDue.text = (due*-1).format()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun Float.format() =
        "%.2f".format(this)


    companion object {
        const val paymentMethod = "paymentMethod"
        const val paymentAmount = "paymentAmount"
        const val orderStatus = "orderStatus"
        const val FROM_ONGOING_ORDERS = 0
        const val FROM_CHECKOUT = 1
    }
}