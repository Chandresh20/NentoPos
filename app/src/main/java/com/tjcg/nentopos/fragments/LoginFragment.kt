package com.tjcg.nentopos.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.tjcg.nentopos.Constants
import com.tjcg.nentopos.MainActivity
import com.tjcg.nentopos.R
import com.tjcg.nentopos.databinding.ActivityLoginBinding

class LoginFragment : Fragment() {

    private lateinit var binding : ActivityLoginBinding
    private lateinit var ctx : Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = findNavController().context
        binding = ActivityLoginBinding.inflate(inflater, container, false)
        val isLoggedIn = MainActivity.mainSharedPreferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false)
        //clear all old data on newInstall
        if (isLoggedIn) {
            Constants.authorization =
                MainActivity.mainRepository.sharedPreferences.getString(Constants.PREF_AUTHORIZATION, "-1") ?: "-1"
            Constants.clientId = MainActivity.mainRepository.sharedPreferences.getString(Constants.PREF_CLIENT_ID, "-1") ?: "-1"
            POSFragment.directLogin = true
            findNavController().navigate(R.id.action_navigation_login_to_navigation_pos)
            Handler(Looper.getMainLooper()).postDelayed( {
                ctx.sendBroadcast(Intent(Constants.TABLE_LOADED_BROADCAST))
            }, 2000)
            Constants.databaseBusy = false
            return binding.root
        }
        val isNewLogin = MainActivity.mainSharedPreferences.getBoolean(Constants.PREF_IS_NEW_LOGIN, false)
        if (isNewLogin) {
            binding.loginSubUserBtn.visibility = View.VISIBLE
            binding.loginSubUserBtn1.setOnClickListener {
                findNavController().navigate(R.id.navigation_subUser_login)
            }
        }
        getFirebaseToken()
        binding.loginBtn.setOnClickListener {
            if (binding.emailEditText.text.isNullOrBlank() ||
                binding.passwordEditText.text.isNullOrBlank()) {
                Toast.makeText(ctx, "Need all data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.errorText.visibility = View.GONE
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            ctx.registerReceiver(responseReceiver, IntentFilter(Constants.SUCCESS_BROADCAST))
            ctx.registerReceiver(responseReceiver, IntentFilter(Constants.FAILURE_BROADCAST))
            MainActivity.mainRepository.loginSuperUser(ctx, email, password, Constants.firebaseToken)
            Log.d("LoginToken", Constants.firebaseToken)
        }
        return binding.root
    }

    private val responseReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1?.action == Constants.SUCCESS_BROADCAST) {
                MainActivity.mainSharedPreferences.edit()
                    .putBoolean(Constants.PREF_IS_LOGGED_IN, true).apply()
                findNavController().navigate(R.id.action_navigation_login_to_navigation_pos)
                ctx.unregisterReceiver(this)
            }
            if (p1?.action == Constants.FAILURE_BROADCAST) {
                val error = p1.getStringExtra(Constants.error)
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = error
            }
        }
    }

    private fun getFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d("Firebase token", it)
            Constants.firebaseToken = it
        }
    }
}