package com.dzworks.locator42.map

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocatorViewModel
@Inject constructor(
) : ViewModel() {

    val latitude: MutableLiveData<String> = MutableLiveData("Latitude 0.0000")
    val longitude: MutableLiveData<String> = MutableLiveData("Longitude 0.000")

}