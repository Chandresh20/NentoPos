package com.tjcg.nentopos.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.databinding.FragmentSubuserLoginBinding

class SubUserLoginFragment : Fragment() {

    private lateinit var binding: FragmentSubuserLoginBinding
    private lateinit var ctx: Context
    private var pin = ""
    private var pinToShow = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        ctx = findNavController().context
        binding = FragmentSubuserLoginBinding.inflate(inflater, container, false)
        binding.adminLoginButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btn0.setOnClickListener {
            addToPin("0")
        }
        binding.btn1.setOnClickListener {
            addToPin("1")
        }
        binding.btn2.setOnClickListener {
            addToPin("2")
        }
        binding.btn3.setOnClickListener {
            addToPin("3")
        }
        binding.btn4.setOnClickListener {
            addToPin("4")
        }
        binding.btn5.setOnClickListener {
            addToPin("5")
        }
        binding.btn6.setOnClickListener {
            addToPin("6")
        }
        binding.btn7.setOnClickListener {
            addToPin("7")
        }
        binding.btn8.setOnClickListener {
            addToPin("8")
        }
        binding.btn9.setOnClickListener {
            addToPin("9")
        }
        binding.clear.setOnClickListener {
            clearPin()
        }
        binding.next.setOnClickListener {
            val superEmail = MainActivity.mainSharedPreferences.getString(
                Constants.PREF_SUPER_USER_EMAIL, "")
            val domainName = MainActivity.mainSharedPreferences.getString(
                Constants.PREF_SUPER_USER_DOMAIN, "")
            val authorization = MainActivity.mainSharedPreferences.getString(
                Constants.PREF_AUTHORIZATION, "")
            Log.d("forSubUsers","$superEmail : $domainName : $pin : $authorization")
            val loginReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    findNavController().navigate(R.id.navigation_pos)
                    ctx.unregisterReceiver(this)
                }
            }
            ctx.registerReceiver(loginReceiver, IntentFilter(Constants.SUB_USER_LOGIN_BROADCAST))
            MainActivity.mainRepository.loginSubUser(ctx, superEmail ?: "", pin, domainName ?: "")
        }
        return binding.root
    }

    private fun addToPin(pin1: String) {
        pin += pin1
        pinToShow += "*"
        binding.pinEditText.text = pinToShow
    }

    private fun clearPin() {
        pin = ""
        pinToShow = ""
        binding.pinEditText.text = pinToShow
    }
}