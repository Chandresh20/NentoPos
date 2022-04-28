package com.tjcg.nentopos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = REPLACE)
    fun insertAllOutletData(outlets: List<OutletData>)

    @Insert(onConflict = REPLACE)
    fun insertAllCustomerData(customers: List<CustomerData>)

    @Insert(onConflict = REPLACE)
    fun insertAllTablesData(tables: List<TableData>)

    @Insert(onConflict = REPLACE)
    fun insertSubUsersData(subUsers : List<SubUserData>)

    @Insert(onConflict = REPLACE)
    fun insertCustomerTypes(customerTypes : List<CustomerTypeData>)

    @Insert(onConflict = REPLACE)
    fun insertCardTerminalData(cardTerminalData: List<CardTerminalData>)

    @Insert(onConflict = REPLACE)
    fun insertOneCustomer(customerData: CustomerData)

    @Insert(onConflict = REPLACE)
    fun insertOneOfflineCustomer(customer: CustomerOffline)

    @Query("select * from Outlets")
    fun getAllOutlets() : List<OutletData>

    @Query("select * from Outlets where outletId=:outletId")
    fun getOutletDetails(outletId: Int) : OutletData

    @Query("select * from Customers")
    fun getAllCustomers() : List<CustomerData>?

    @Query("select * from Customers where customerId=:id")
    fun getOneCustomerDetails(id: Int) : CustomerData?

    @Query("select * from Customers where customerId=:id")
    fun getOneCustomer(id : Long) : CustomerData?

    @Query("select * from CustomerOffline")
    fun getAllOfflineCustomer() : List<CustomerOffline>?


    @Query("select * from CustomerType")
    fun getCustomerTypes() : List<CustomerTypeData>?

    @Query("select * from Tables where tableId=:id")
    fun getOneTableData(id: Int) : TableData?

    @Query("select * from Tables where outletId=:outletId")
    fun getTableDataForOutlet(outletId: Int) : List<TableData>

    @Query("select * from SubUsers where outletId=:outletId")
    fun getAllSubUsers(outletId: Int) : List<SubUserData>?

    @Query("select * from SubUsers where id=:sId")
    fun getOneSubUser(sId : Int) : SubUserData?

    @Query("select * from CardTerminals where outletId=:outletId")
    fun getCardTerminals(outletId: Int) : List<CardTerminalData>?

    @Query("delete from CustomerOffline")
    fun deleteAllOfflineCustomers()

    @Query("delete from Outlets")
    fun deleteAllOutletData()

    @Query("delete from Customers")
    fun deleteAllCustomers()

    @Query("delete from Tables")
    fun deleteAllTableData()

    @Query("delete from SubUsers")
    fun deleteAllSubUsers()
}