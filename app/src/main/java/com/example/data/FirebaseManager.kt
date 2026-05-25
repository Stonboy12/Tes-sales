package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Checks if Firebase is initialized and available
    fun isInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // Try starting Firebase dynamically using direct connection string credentials (programmatic Web/Android options)
    fun initializeDynamically(
        context: Context,
        apiKey: String,
        appId: String,
        projectId: String
    ): Boolean {
        return try {
            if (isInitialized(context)) {
                // Remove previous app to reinitialize if credentials change
                val currentApp = FirebaseApp.getInstance()
                currentApp.delete()
            }
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey.trim())
                .setApplicationId(appId.trim())
                .setProjectId(projectId.trim())
                .build()
            FirebaseApp.initializeApp(context.applicationContext, options)
            Log.d(TAG, "Firebase initialized dynamically with projectId: $projectId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase dynamically", e)
            false
        }
    }

    // AUTH ACTIONS: Sign-up
    suspend fun firebaseSignUp(email: String, password: String): String? {
        return try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Sign-Up error", e)
            throw e
        }
    }

    // AUTH ACTIONS: Log-in
    suspend fun firebaseLogin(email: String, password: String): String? {
        return try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Login error", e)
            throw e
        }
    }

    // FIRESTORE ACTIONS: Sync User Profile
    suspend fun syncUserToFirestore(user: User) {
        if (!isFirebaseReady()) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.userId)
                .set(user, SetOptions.merge())
                .await()
            Log.d(TAG, "User ${user.userId} successfully synced to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user ${user.userId}", e)
        }
    }

    // FIRESTORE ACTIONS: Sync Company
    suspend fun syncCompanyToFirestore(company: Company) {
        if (!isFirebaseReady()) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("companies").document(company.projectId)
                .set(company, SetOptions.merge())
                .await()
            Log.d(TAG, "Company ${company.projectId} successfully synced to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync company ${company.projectId}", e)
        }
    }

    // FIRESTORE ACTIONS: Sync Store
    suspend fun syncStoreToFirestore(store: Store) {
        if (!isFirebaseReady()) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("stores").document(store.storeId)
                .set(store, SetOptions.merge())
                .await()
            Log.d(TAG, "Store ${store.storeId} successfully synced to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync store ${store.storeId}", e)
        }
    }

    // FIRESTORE ACTIONS: Sync Visit
    suspend fun syncVisitToFirestore(visit: AttendanceVisit) {
        if (!isFirebaseReady()) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("visits").document(visit.visitId)
                .set(visit, SetOptions.merge())
                .await()
            Log.d(TAG, "Visit ${visit.visitId} successfully synced to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync visit ${visit.visitId}", e)
        }
    }

    // FIRESTORE ACTIONS: Sync Order
    suspend fun syncOrderToFirestore(order: Order) {
        if (!isFirebaseReady()) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("orders").document(order.orderId)
                .set(order, SetOptions.merge())
                .await()
            Log.d(TAG, "Order ${order.orderId} successfully synced to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync order ${order.orderId}", e)
        }
    }

    // FIRESTORE ACTIONS: Fetch user by email (For central Auth login mapping)
    suspend fun fetchUserFromFirestoreByEmail(email: String): User? {
        if (!isFirebaseReady()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            val doc = querySnapshot.documents.firstOrNull()
            if (doc != null) {
                User(
                    userId = doc.getString("userId") ?: doc.id,
                    projectId = doc.getString("projectId") ?: "",
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "salesman",
                    supervisorId = doc.getString("supervisorId"),
                    deviceId = doc.getString("deviceId"),
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    password = doc.getString("password") ?: "12345"
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user by email: $email", e)
            null
        }
    }

    // FIRESTORE ACTIONS: Fetch company details
    suspend fun fetchCompanyFromFirestore(projectId: String): Company? {
        if (!isFirebaseReady()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("companies").document(projectId).get().await()
            if (doc.exists()) {
                Company(
                    projectId = doc.getString("projectId") ?: doc.id,
                    companyName = doc.getString("companyName") ?: "Company",
                    status = doc.getString("status") ?: "active",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch company: $projectId", e)
            null
        }
    }

    // Download/Sync all project collections (Stores, Products, Orders, Visits) when Logging in
    suspend fun downloadProjectDataFromFirestore(projectId: String, dbHelper: AppDao) {
        if (!isFirebaseReady()) return
        try {
            val db = FirebaseFirestore.getInstance()
            
            // Sync Stores
            val storesSnapshot = db.collection("stores").whereEqualTo("projectId", projectId).get().await()
            for (doc in storesSnapshot.documents) {
                val store = Store(
                    storeId = doc.getString("storeId") ?: doc.id,
                    projectId = projectId,
                    storeName = doc.getString("storeName") ?: "Un-named Store",
                    ownerName = doc.getString("ownerName") ?: "",
                    category = doc.getString("category") ?: "Warung",
                    potential = doc.getString("potential") ?: "Sedang",
                    address = doc.getString("address") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    operatingHours = doc.getString("operatingHours") ?: "08:00 - 21:00",
                    registeredBy = doc.getString("registeredBy") ?: "",
                    lastPhotoUrl = doc.getString("lastPhotoUrl"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                dbHelper.insertStore(store)
            }

            // Sync Orders
            val ordersSnapshot = db.collection("orders").whereEqualTo("projectId", projectId).get().await()
            for (doc in ordersSnapshot.documents) {
                val order = Order(
                    orderId = doc.getString("orderId") ?: doc.id,
                    projectId = projectId,
                    salesmanId = doc.getString("salesmanId") ?: "",
                    supervisorId = doc.getString("supervisorId"),
                    storeId = doc.getString("storeId") ?: "",
                    itemsJson = doc.getString("itemsJson") ?: "[]",
                    totalOmset = (doc.getLong("totalOmset") ?: 0L).toInt(),
                    paymentStatus = doc.getString("paymentStatus") ?: "Lunas",
                    totalPaid = (doc.getLong("totalPaid") ?: 0L).toInt(),
                    remainingPiutang = (doc.getLong("remainingPiutang") ?: 0L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                dbHelper.insertOrder(order)
            }

            // Sync Visits
            val visitsSnapshot = db.collection("visits").whereEqualTo("projectId", projectId).get().await()
            for (doc in visitsSnapshot.documents) {
                val visit = AttendanceVisit(
                    visitId = doc.getString("visitId") ?: doc.id,
                    projectId = projectId,
                    salesmanId = doc.getString("salesmanId") ?: "",
                    storeId = doc.getString("storeId") ?: "",
                    checkInTime = doc.getLong("checkInTime") ?: System.currentTimeMillis(),
                    checkOutTime = doc.getLong("checkOutTime"),
                    durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt(),
                    checkInLatitude = doc.getDouble("checkInLatitude") ?: 0.0,
                    checkInLongitude = doc.getDouble("checkInLongitude") ?: 0.0,
                    checkInPhoto = doc.getString("checkInPhoto") ?: "",
                    checkOutPhoto = doc.getString("checkOutPhoto"),
                    visitStatus = doc.getString("visitStatus") ?: "Order",
                    fraudAlert = doc.getBoolean("fraudAlert") ?: false,
                    fraudReason = doc.getString("fraudReason") ?: ""
                )
                dbHelper.insertVisit(visit)
            }

            Log.d(TAG, "Completed full master-data pull from Firebase Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving project collections", e)
        }
    }

    private fun isFirebaseReady(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }
}
