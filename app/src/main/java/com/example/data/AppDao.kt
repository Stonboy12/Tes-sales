package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Company operations
    @Query("SELECT * FROM companies WHERE projectId = :id LIMIT 1")
    suspend fun getCompanyById(id: String): Company?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: Company)

    // User operations
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE projectId = :projectId")
    fun getUsersFlow(projectId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE supervisorId = :supervisorId")
    fun getSalesmenBySupervisorFlow(supervisorId: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Store operations
    @Query("SELECT * FROM stores WHERE storeId = :storeId LIMIT 1")
    suspend fun getStoreById(storeId: String): Store?

    @Query("SELECT * FROM stores WHERE projectId = :projectId")
    fun getStoresFlow(projectId: String): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: Store)

    // Attendance visit operations
    @Query("SELECT * FROM attendance_visits WHERE visitId = :visitId LIMIT 1")
    suspend fun getVisitById(visitId: String): AttendanceVisit?

    @Query("SELECT * FROM attendance_visits WHERE salesmanId = :salesmanId ORDER BY checkInTime DESC")
    fun getVisitsBySalesmanFlow(salesmanId: String): Flow<List<AttendanceVisit>>

    @Query("SELECT * FROM attendance_visits ORDER BY checkInTime DESC")
    fun getAllVisitsFlow(): Flow<List<AttendanceVisit>>

    @Query("SELECT * FROM attendance_visits WHERE salesmanId = :salesmanId AND checkInTime >= :startOfDay ORDER BY checkInTime DESC")
    suspend fun getLatestVisitToday(salesmanId: String, startOfDay: Long): List<AttendanceVisit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: AttendanceVisit)

    // Order operations
    @Query("SELECT * FROM orders WHERE storeId = :storeId")
    suspend fun getOrdersByStore(storeId: String): List<Order>

    @Query("SELECT * FROM orders WHERE salesmanId = :salesmanId ORDER BY createdAt DESC")
    fun getOrdersBySalesmanFlow(salesmanId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    // SKU / Product operations
    @Query("SELECT * FROM products WHERE skuId = :skuId LIMIT 1")
    suspend fun getProductById(skuId: String): Product?

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("DELETE FROM products WHERE skuId = :skuId")
    suspend fun deleteProduct(skuId: String)
}
