package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "CustomerOffline")
class CustomerOffline {

    @PrimaryKey
    @NonNull
    @SerializedName("customer_id")
    var customerTmpId : Long = 0

    @SerializedName("customer_name")
    var customerName : String? = null

    @SerializedName("customer_lastname")
    var customerLastName : String? = null

    @SerializedName("customer_email")
    var customerEmail : String? = null

    @SerializedName("customer_address")
    var customerAddress : String? = null

    @SerializedName("country_code")
    var countryCode : String? = null

    @SerializedName("customer_phone")
    var customerPhone : String? = null

    @SerializedName("note")
    var note: String? = null

    @SerializedName("customer_category")
    var customerCategory : String? = null
}