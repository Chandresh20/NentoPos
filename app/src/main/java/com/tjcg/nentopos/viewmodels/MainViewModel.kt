package com.tjcg.nentopos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _showProgress = MutableLiveData(false)
    val showProgress : LiveData<Boolean> = _showProgress

    private val _progressText = MutableLiveData<String>()
    val progressText : LiveData<String> = _progressText

    private val _showBackgroundProcess = MutableLiveData(false)
    val showBackgroundProcess : LiveData<Boolean> = _showBackgroundProcess

    private val _backgroundProgressText = MutableLiveData<String>()
    val backgroundProgressText : LiveData<String> = _backgroundProgressText

    fun toggleProgressBar(status : Boolean) {
        _showProgress.value = status
    }

    fun toggleBackgroundProgressView(status: Boolean) {
        _showBackgroundProcess.value = status
    }

    fun progressText(text: String) {
        _progressText.value = text
    }

    fun backgroundProgressText(text: String) {
        _backgroundProgressText.value = text
    }


}