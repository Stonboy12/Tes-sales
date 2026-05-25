package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class Company(
    @PrimaryKey val projectId: String,
    val companyName: String,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val projectId: String,
    val name: String,
    val email: String,
    val role: String, // super_admin, supervisor, salesman
    val supervisorId: String?, // Null for admin/supervisor
    var deviceId: String?, // Tied permanently on first login
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val password: String = "12345" // For simulated security
)

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey val storeId: String,
    val projectId: String,
    val storeName: String,
    val ownerName: String,
    val category: String, // Minimarket, Warung, Supermarket, Horeca
    val potential: String, // Besar, Sedang, Kecil
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val operatingHours: String = "08:00 - 21:00",
    val registeredBy: String,
    val lastPhotoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_visits")
data class AttendanceVisit(
    @PrimaryKey val visitId: String,
    val projectId: String,
    val salesmanId: String,
    val storeId: String,
    val checkInTime: Long,
    var checkOutTime: Long? = null,
    var durationMinutes: Int = 0,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkInPhoto: String, // Base64 or local path string
    var checkOutPhoto: String? = null,
    var visitStatus: String = "Order", // Order, Toko Tutup, Stok Banyak, Owner Tidak Ada
    val fraudAlert: Boolean = false,
    val fraudReason: String = ""
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderId: String,
    val projectId: String,
    val salesmanId: String,
    val supervisorId: String?,
    val storeId: String,
    val itemsJson: String, // JSON representation of List<OrderItem>
    val totalOmset: Int,
    val paymentStatus: String, // Lunas, Sebagian, Belum Bayar
    val totalPaid: Int,
    val remainingPiutang: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val skuId: String,
    val projectId: String,
    val name: String,
    val price: Int,
    val maxOrderLimit: Int,
    val stock: Int
)

// Convenient helper data class for order items
data class OrderItem(
    val sku: String,
    val name: String,
    val qty: Int,
    val price: Int,
    val subtotal: Int
)
