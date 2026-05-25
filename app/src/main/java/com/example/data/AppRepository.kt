package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val appDao: AppDao) {

    // Seeder
    suspend fun seedDatabaseIfEmpty() {
        // No seeding by default as per user request to avoid demo accounts and dummy data.
    }

    // Register dynamic company and super admin (Superuser)
    suspend fun registerCompanyAndAdmin(
        projectId: String,
        companyName: String,
        adminId: String,
        adminName: String,
        adminEmail: String,
        password: String
    ): Boolean {
        val existingCompany = appDao.getCompanyById(projectId)
        if (existingCompany != null) return false

        // Insert new Company
        appDao.insertCompany(
            Company(
                projectId = projectId,
                companyName = companyName,
                status = "active"
            )
        )

        // Insert new administrator (Super Admin)
        appDao.insertUser(
            User(
                userId = adminId,
                projectId = projectId,
                name = adminName,
                email = adminEmail,
                role = "super_admin",
                supervisorId = null,
                deviceId = null,
                password = password
            )
        )
        return true
    }

    // Auth & Validation
    suspend fun getCompany(id: String): Company? = appDao.getCompanyById(id)
    suspend fun getUser(id: String): User? = appDao.getUserById(id)
    suspend fun getUserByEmail(email: String): User? = appDao.getUserByEmail(email)
    suspend fun saveUser(user: User) = appDao.insertUser(user)

    fun getUsersFlow(projectId: String): Flow<List<User>> = appDao.getUsersFlow(projectId)
    fun getSalesmenBySupervisorFlow(spvId: String): Flow<List<User>> = appDao.getSalesmenBySupervisorFlow(spvId)

    // Store operations
    suspend fun getStoreById(storeId: String): Store? = appDao.getStoreById(storeId)
    fun getStoresFlow(projectId: String): Flow<List<Store>> = appDao.getStoresFlow(projectId)
    suspend fun insertStore(store: Store) = appDao.insertStore(store)

    // Product operations
    suspend fun getProductById(skuId: String): Product? = appDao.getProductById(skuId)
    fun getAllProductsFlow(): Flow<List<Product>> = appDao.getAllProductsFlow()
    suspend fun insertProduct(product: Product) = appDao.insertProduct(product)
    suspend fun deleteProduct(skuId: String) = appDao.deleteProduct(skuId)

    // Attendance visit operations
    suspend fun getVisitById(visitId: String): AttendanceVisit? = appDao.getVisitById(visitId)
    fun getVisitsBySalesmanFlow(salesmanId: String): Flow<List<AttendanceVisit>> = appDao.getVisitsBySalesmanFlow(salesmanId)
    fun getAllVisitsFlow(): Flow<List<AttendanceVisit>> = appDao.getAllVisitsFlow()
    suspend fun getLatestVisitToday(salesmanId: String, startOfDay: Long): List<AttendanceVisit> = appDao.getLatestVisitToday(salesmanId, startOfDay)
    suspend fun insertVisit(visit: AttendanceVisit) = appDao.insertVisit(visit)

    // Order operations
    suspend fun getOrdersByStore(storeId: String): List<Order> = appDao.getOrdersByStore(storeId)
    fun getOrdersBySalesmanFlow(salesmanId: String): Flow<List<Order>> = appDao.getOrdersBySalesmanFlow(salesmanId)
    fun getAllOrdersFlow(): Flow<List<Order>> = appDao.getAllOrdersFlow()
    suspend fun insertOrder(order: Order) = appDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = appDao.updateOrder(order)
}
