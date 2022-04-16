package com.tjcg.nentopos.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Discounts")
class DiscountData {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    var discountId : Int = 0

    @SerializedName("outlet_id")
    var outletId : Int? = null

    @SerializedName("discount_name")
    var discountName : String? = null

    @SerializedName("discount_type")
    var discountType : Int? = null

    @SerializedName("discount_percentage")
    var discountPercentage : Int? = null

    @SerializedName("discount_on")
    var discountOn : Int? = null

    @SerializedName("applied_ids")
    var appliedIds : String? = null

    @SerializedName("discount_apply_service")
    var discountApplyService : Int? = null

    @SerializedName("discount_schedule")
    var discountSchedule : Int? = null

    @SerializedName("discount_start_date_time")
    var discountStartTime : String? = null

    @SerializedName("discount_end_date_time")
    var discountEndDateTime : String? = null

    @SerializedName("is_sunday_open")
    var sSundayOpen : Int? = null

    @SerializedName("sunday_start_time")
    var sundayStartTime : String? = null

    @SerializedName("sunday_end_time")
    var sundayEndTime : String? = null

    @SerializedName("is_monday_open")
    var sMondayOpen : Int? = null

    @SerializedName("monday_start_time")
    var mondayStartTime : String? = null

    @SerializedName("monday_end_time")
    var mondayEndTime : String? = null

    @SerializedName("is_tuesday_open")
    var sTuesdayOpen : Int? = null

    @SerializedName("tuesday_start_time")
    var tuesdayStartTime : String? = null

    @SerializedName("tuesday_end_time")
    var tuesdayEndTime : String? = null

    @SerializedName("is_wednesday_open")
    var sWednesdayOpen : Int? = null

    @SerializedName("wednesday_start_time")
    var wednesdayStartTime : String? = null

    @SerializedName("wednesday_end_time")
    var wednesdayEndTime : String? = null

    @SerializedName("is_thursday_open")
    var sThursdayOpen : Int? = null

    @SerializedName("thursday_start_time")
    var thursdayStartTime : String? = null

    @SerializedName("thursday_end_time")
    var thursdayEndTime : String? = null

    @SerializedName("is_friday_open")
    var sFridayOpen : Int? = null

    @SerializedName("friday_start_time")
    var fridayStartTime : String? = null

    @SerializedName("friday_end_time")
    var fridayEndTime : String? = null

    @SerializedName("is_saturday_open")
    var sSaturdayOpen : Int? = null

    @SerializedName("saturday_start_time")
    var saturdayStartTime : String? = null

    @SerializedName("saturday_end_time")
    var saturdayEndTime : String? = null

    @SerializedName("is_deleted")
    var sDeleted : Int? = null
}