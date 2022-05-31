package com.tjcg.nentopos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update

@Dao
interface OrdersDao {

    @Insert(onConflict = REPLACE)
    fun insertOrdersData(onlineOrderData: List<OrdersEntity>)

    @Insert(onConflict = REPLACE)
    fun insertSingleOrder(order: OrdersEntity)

    @Insert(onConflict = REPLACE)
    fun insertEditedOrders(eOrders : OrderUpdateSync)

    @Query("delete from orders where outlet_id=:outletId")
    fun deleteAllOrders(outletId: Int)
    /*   @Query("update orders set orderItems=:details where outlet_id=:outletId and order_id=:orderId")
       fun addOrderItems(outletId: String, orderId: String, details : List<OrdersEntity.OrderItems>)  */

    @Query("select * from orders where outlet_id=:outletId and order_id=:orderId")
    fun getSingleOrder(outletId: Int, orderId: Long) : OrdersEntity

    @Update(onConflict = REPLACE)
    fun updateSingleOrder(order: OrdersEntity)

    @Update(onConflict = REPLACE)
    fun updateSingleOfflineOrder(offlineOrder2: OfflineOrder2)


    /*   @Query("select orderItems from orders where outlet_id=:outletId and order_id=:orderId")
       fun getSingleOrderItems(outletId:String, orderId: String) : String  */

    @Query("select * from orders where outlet_id=:outletId")
    fun getOrdersData(outletId: Int) : List<OrdersEntity>?

    @Query("select * from orders where outlet_id=:outletId and syncOrNot=0")
    fun getUnSyncOrders(outletId: Int) : List<OrdersEntity>?

    @Query("select * from Edited_Orders where outletId=:outletId")
    fun getAllEditedOrders(outletId: Int) : OrderUpdateSync?

    @Query("select * from orders where outlet_id=:outletId and order_status=:statusInt")
    fun getOrdersByStatus(outletId: Int, statusInt: Int) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_date=:date or future_order_date=:date")
    fun getOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_date=:date and order_status=2")
    fun getKitchenOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_accept_date is not null")
    fun getAcceptedOrders(outletId: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_status=1 and order_date=:date and future_order_date is null")
    fun getOnlineOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_accept_date is not null and order_status=2 and order_date=:date")
    fun getAcceptedOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_status=4 and order_date=:date")
    fun getCompletedOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_date=:date and order_status=5")
    fun getCancelledOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_date=:date and order_status=4")
    fun getServedOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and order_date=:date and order_status=3")
    fun getReadyOrdersByDate(outletId: Int, date: String) : List<OrdersEntity>

    @Query("select * from orders where outlet_id=:outletId and future_order_date is not null and order_status=1")
    fun getFutureOrders(outletId: Int) : List<OrdersEntity>

    // for offline orders
  /*  @Insert
    fun insertNewOfflineOrder(offlineOrder : OfflineOrder)

    @Query("select * from OfflineOrders where outletId=:outletId")
    fun getAllOfflineOrders(outletId: Int) : List<OfflineOrder>?

    @Query("delete from OfflineOrders where outletId=:outletId")
    fun deleteAllOfflineOrders(outletId: Int)  */

    @Insert
    fun insertNewOffline2Order(offlineOrder2 : OfflineOrder2)

    @Query("select * from OfflineOrder2 where outletId=:outletId")
    fun getAllOffline2Orders(outletId: Int) : List<OfflineOrder2>?

    @Query("select * from OfflineOrder2 where outletId=:outletId and tmpOrderId=:tmpId")
    fun getOneOffline2Order(outletId: Int, tmpId: Long) : OfflineOrder2?

    @Update(onConflict = REPLACE)
    fun updateOneOffline2Order(offlineOrder2: OfflineOrder2)

    @Query("delete from OfflineOrder2 where tmpOrderId=:tmpId")
    fun deleteOneOfflineOrder(tmpId: Long)

    @Query("delete from OfflineOrder2 where outletId=:outletId")
    fun deleteAllOffline2Orders(outletId: Int)

    @Query("delete from Edited_Orders where outletId=:outletId")
    fun deleteEditedOrders(outletId: Int)

    @Query("delete from orders")
    fun deleteAllOrders()

    @Query("delete from OfflineOrder2")
    fun deleteAllOfflineOrders()
}