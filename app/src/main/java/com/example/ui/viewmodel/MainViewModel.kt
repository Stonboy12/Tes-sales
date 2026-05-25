package com.example.ui.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainViewModel(
    private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    // Dynamic Firebase credentials setup at runtime
    var firebaseApiKey = MutableStateFlow("")
    var firebaseAppId = MutableStateFlow("")
    var firebaseProjectId = MutableStateFlow("")
    var firebaseSetupSuccess = MutableStateFlow<String?>(null)
    var firebaseSetupError = MutableStateFlow<String?>(null)

    fun setupFirebaseDynamically() {
        val apiKey = firebaseApiKey.value.trim()
        val appId = firebaseAppId.value.trim()
        val pId = firebaseProjectId.value.trim()
        
        if (apiKey.isEmpty() || appId.isEmpty() || pId.isEmpty()) {
            firebaseSetupError.value = "Semua field config Firebase harus diisi!"
            firebaseSetupSuccess.value = null
            return
        }
        
        val success = FirebaseManager.initializeDynamically(context, apiKey, appId, pId)
        if (success) {
            firebaseSetupSuccess.value = "Koneksi Firebase Berhasil Dikonfigurasi!"
            firebaseSetupError.value = null
        } else {
            firebaseSetupError.value = "Konfigurasi gagal! Silakan periksa kembali API Key / App ID."
            firebaseSetupSuccess.value = null
        }
    }

    // Login/Auth inputs
    var inputProjectId = MutableStateFlow("")
    var inputUserId = MutableStateFlow("")
    var inputPassword = MutableStateFlow("")
    var authError = MutableStateFlow<String?>(null)

    // Registration inputs for new Company & Admin (SaaS multi-tenant mode list)
    var regProjectId = MutableStateFlow("")
    var regCompanyName = MutableStateFlow("")
    var regAdminId = MutableStateFlow("")
    var regAdminName = MutableStateFlow("")
    var regAdminEmail = MutableStateFlow("")
    var regAdminPassword = MutableStateFlow("")
    var regError = MutableStateFlow<String?>(null)
    var regSuccess = MutableStateFlow<String?>(null)
    var isRegisterMode = MutableStateFlow(false)

    // Logged in user info
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentCompany = MutableStateFlow<Company?>(null)
    val currentCompany: StateFlow<Company?> = _currentCompany.asStateFlow()

    // Simulated states for easy demo & anti-fraud testing
    val devSimulateFakeGps = MutableStateFlow(false)
    val devSimulateDeviceId = MutableStateFlow("")
    val devCustomLatitude = MutableStateFlow(-6.2088) // default Jakarta
    val devCustomLongitude = MutableStateFlow(106.8456)

    val actualDeviceId: String by lazy {
        try {
            Settings.Secure.getString(context.applicationContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "MOCK_DEV_1234"
        } catch (e: Exception) {
            "MOCK_DEV_1234"
        }
    }

    // Active Salesman visit
    private val _currentActiveVisit = MutableStateFlow<AttendanceVisit?>(null)
    val currentActiveVisit: StateFlow<AttendanceVisit?> = _currentActiveVisit.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()
    private var countdownJob: Job? = null

    // Shopping Cart for active visit
    private val _shoppingCart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val shoppingCart: StateFlow<Map<Product, Int>> = _shoppingCart.asStateFlow()

    // Dynamic Lists & flows (Multi-tenant scoped)
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    private val _allStores = MutableStateFlow<List<Store>>(emptyList())
    val allStores: StateFlow<List<Store>> = _allStores.asStateFlow()

    private val _allVisits = MutableStateFlow<List<AttendanceVisit>>(emptyList())
    val allVisits: StateFlow<List<AttendanceVisit>> = _allVisits.asStateFlow()

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders.asStateFlow()

    // Filtered lists for specific roles
    private val _salesmenList = MutableStateFlow<List<User>>(emptyList())
    val salesmenList: StateFlow<List<User>> = _salesmenList.asStateFlow()

    private val _supervisorsList = MutableStateFlow<List<User>>(emptyList())
    val supervisorsList: StateFlow<List<User>> = _supervisorsList.asStateFlow()

    // Current screen navigation
    val currentScreen = MutableStateFlow("login") // login, dashboard

    // Active client store for checkout or orders or payments
    val activeStore = MutableStateFlow<Store?>(null)

    init {
        try {
            // Initialize simulator device ID to the hardware device's actual ID
            devSimulateDeviceId.value = actualDeviceId
        } catch (e: Exception) {
            android.util.Log.e("SmartSales", "Error setting device ID in init", e)
            devSimulateDeviceId.value = "MOCK_DEV_1234"
        }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("SmartSales", "Executing database seeding check...")
                repository.seedDatabaseIfEmpty()
                android.util.Log.d("SmartSales", "Database seeding finished successfully")
            } catch (e: Exception) {
                android.util.Log.e("SmartSales", "DB seeding failed with exception", e)
            }
        }

        // Extremely robust, multi-tenant flow collection that dynamically updates whenever user is logged in
        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    // Update Products for logged-in project
                    launch {
                        repository.getAllProductsFlow().collectLatest { products ->
                            _allProducts.value = products.filter { it.projectId == user.projectId }
                        }
                    }
                    // Update Stores for logged-in project
                    launch {
                        repository.getStoresFlow(user.projectId).collectLatest { stores ->
                            _allStores.value = stores
                        }
                    }
                    // Update Visits for logged-in project
                    launch {
                        repository.getAllVisitsFlow().collectLatest { visits ->
                            _allVisits.value = visits.filter { it.projectId == user.projectId }
                        }
                    }
                    // Update Orders for logged-in project
                    launch {
                        repository.getAllOrdersFlow().collectLatest { orders ->
                            _allOrders.value = orders.filter { it.projectId == user.projectId }
                        }
                    }
                } else {
                    _allProducts.value = emptyList()
                    _allStores.value = emptyList()
                    _allVisits.value = emptyList()
                    _allOrders.value = emptyList()
                }
            }
        }
    }

    fun setScreen(screen: String) {
        currentScreen.value = screen
    }

    // Dynamic Registration of Company and Super Admin (Multi-tenant)
    fun registerCompany() {
        viewModelScope.launch {
            try {
                regError.value = null
                regSuccess.value = null
                val pId = regProjectId.value.trim()
                val cName = regCompanyName.value.trim()
                val aId = regAdminId.value.trim()
                val aName = regAdminName.value.trim()
                val aEmail = regAdminEmail.value.trim()
                val aPass = regAdminPassword.value

                if (pId.isEmpty() || cName.isEmpty() || aId.isEmpty() || aName.isEmpty() || aEmail.isEmpty() || aPass.isEmpty()) {
                    regError.value = "Format input registrasi tidak boleh kosong!"
                    return@launch
                }

                val success = repository.registerCompanyAndAdmin(
                    projectId = pId,
                    companyName = cName,
                    adminId = aId,
                    adminName = aName,
                    adminEmail = aEmail,
                    password = aPass
                )

                if (success) {
                    // Try to register in FirebaseAuth and Firestore
                    if (FirebaseManager.isInitialized(context)) {
                        try {
                            FirebaseManager.firebaseSignUp(aEmail, aPass)
                            val company = Company(projectId = pId, companyName = cName)
                            val adminUser = User(
                                userId = aId,
                                projectId = pId,
                                name = aName,
                                email = aEmail,
                                role = "super_admin",
                                supervisorId = null,
                                deviceId = null,
                                password = aPass
                            )
                            FirebaseManager.syncCompanyToFirestore(company)
                            FirebaseManager.syncUserToFirestore(adminUser)
                        } catch (fe: Exception) {
                            android.util.Log.e("SmartSales", "Firebase Registration failed", fe)
                        }
                    }

                    regSuccess.value = "Perusahaan '$cName' & Super Admin '$aName' berhasil terdaftar! Silakan Login."
                    // Populate login fields for easy accessibility
                    inputProjectId.value = pId
                    inputUserId.value = aId
                    inputPassword.value = aPass
                    
                    // Switch back to login form 
                    isRegisterMode.value = false
                    
                    // Reset inputs
                    regProjectId.value = ""
                    regCompanyName.value = ""
                    regAdminId.value = ""
                    regAdminName.value = ""
                    regAdminEmail.value = ""
                    regAdminPassword.value = ""
                } else {
                    regError.value = "ID Project '$pId' sudah terdaftar! Gunakan ID unik lain."
                }
            } catch (e: Exception) {
                android.util.Log.e("SmartSales", "Error in registerCompany", e)
                regError.value = "Gagal mendaftar: ${e.localizedMessage ?: "database error"}"
            }
        }
    }

    // Login logic
    fun checkLogin() {
        viewModelScope.launch {
            try {
                authError.value = null
                val projectId = inputProjectId.value.trim()
                val userId = inputUserId.value.trim()
                val password = inputPassword.value

                android.util.Log.d("SmartSales", "checkLogin called with Project ID: '$projectId', User ID: '$userId'")

                if (projectId.isEmpty() || userId.isEmpty() || password.isEmpty()) {
                    authError.value = "Format input tidak boleh kosong!"
                    return@launch
                }

                // If Firebase is initialized, attempt authentication and data syncing from Firebase
                if (FirebaseManager.isInitialized(context)) {
                    try {
                        // Support logging in via either userId or email
                        val targetEmail = if (userId.contains("@")) {
                            userId
                        } else {
                            // Fetch local user to get email matching, or check Firestore
                            val localUser = repository.getCompany(projectId)?.let { repository.getUser(userId) }
                            localUser?.email ?: ""
                        }

                        if (targetEmail.isNotEmpty()) {
                            val fbUid = FirebaseManager.firebaseLogin(targetEmail, password)
                            if (fbUid != null) {
                                // Down-sync user profile & project tables
                                val fsUser = FirebaseManager.fetchUserFromFirestoreByEmail(targetEmail)
                                if (fsUser != null) {
                                    // Save/Sync user locally if missing
                                    if (repository.getUser(fsUser.userId) == null) {
                                        repository.saveUser(fsUser)
                                    }
                                    
                                    // Fetch & save company profile locally if missing
                                    val fsComp = FirebaseManager.fetchCompanyFromFirestore(projectId)
                                    if (fsComp != null && repository.getCompany(projectId) == null) {
                                        repository.registerCompanyAndAdmin(
                                            projectId = fsComp.projectId,
                                            companyName = fsComp.companyName,
                                            adminId = fsUser.userId,
                                            adminName = fsUser.name,
                                            adminEmail = fsUser.email,
                                            password = fsUser.password
                                        )
                                    }

                                    // Pull master collections (stores, orders, visits)
                                    FirebaseManager.downloadProjectDataFromFirestore(projectId, AppDatabase.getDatabase(context).appDao())
                                }
                            }
                        }
                    } catch (fe: Exception) {
                        android.util.Log.w("SmartSales", "Firebase Authentication fallback to offline Room: ${fe.message}")
                    }
                }

                // 1. Verify Company Exists
                val company = repository.getCompany(projectId)
                android.util.Log.d("SmartSales", "Company query result: $company")
                if (company == null) {
                    authError.value = "Kecurangan Terdeteksi: ID Project '$projectId' tidak valid!"
                    return@launch
                }

                // 2. Fetch User Profile
                val user = if (userId.contains("@")) repository.getUserByEmail(userId) else repository.getUser(userId)
                android.util.Log.d("SmartSales", "User query result: $user")
                if (user == null || user.projectId != projectId) {
                    authError.value = "Kredensial salah atau user tidak terdaftar pada project ini!"
                    return@launch
                }

                // 3. Match passwords
                if (user.password != password) {
                    authError.value = "Password yang Anda masukkan salah!"
                    return@launch
                }

                // 4. Device ID Lock (Core Anti-Kecurangan Engine)
                val currentEnvDeviceId = devSimulateDeviceId.value.trim()
                android.util.Log.d("SmartSales", "Device binding validation. Registered: [${user.deviceId}], Current: [$currentEnvDeviceId]")
                if (user.deviceId == null || user.deviceId!!.isEmpty()) {
                    // If unset, bind device ID permanently
                    user.deviceId = currentEnvDeviceId
                    repository.saveUser(user)
                    // Sync up to Firebase
                    if (FirebaseManager.isInitialized(context)) {
                        try {
                            FirebaseManager.syncUserToFirestore(user)
                        } catch (fe: Exception) {
                            android.util.Log.e("SmartSales", "Failed to sync device ID up to Firestore", fe)
                        }
                    }
                    android.util.Log.d("SmartSales", "Tethered device ID [$currentEnvDeviceId] successfully to user [${user.userId}]")
                } else if (user.deviceId != currentEnvDeviceId) {
                    // If bound and mismatched, lock access immediately
                    authError.value = """
                        AKSES DIBATALKAN (Device Mismatch):
                        Hardware terikat: [${user.deviceId}]
                        Hardware login: [$currentEnvDeviceId]
                        Silakan hubungi Super Admin untuk reset perangkat.
                    """.trimIndent()
                    return@launch
                }

                // Login successful
                _currentUser.value = user
                _currentCompany.value = company
                
                // Reload user lists if Supervisor/Admin
                refreshUserLists(projectId, user.role)

                // Auto-restore any incomplete active visits today
                try {
                    restoreIncompleteVisit(user.userId)
                } catch (ex: Exception) {
                    android.util.Log.e("SmartSales", "Failing to restore incomplete visits safely", ex)
                }

                currentScreen.value = "dashboard"
                android.util.Log.d("SmartSales", "Auth success, navigate user [${user.userId}] to dashboard")
            } catch (e: Exception) {
                android.util.Log.e("SmartSales", "Unhandled error inside checkLogin", e)
                authError.value = "Gagal masuk: ${e.localizedMessage ?: "database error"}"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentCompany.value = null
        _currentActiveVisit.value = null
        _shoppingCart.value = emptyMap()
        inputPassword.value = ""
        currentScreen.value = "login"
    }

    private fun refreshUserLists(projectId: String, role: String) {
        viewModelScope.launch {
            if (role == "super_admin") {
                // Load all users for admin
                repository.getUsersFlow(projectId).collectLatest { list ->
                    _supervisorsList.value = list.filter { it.role == "supervisor" }
                    _salesmenList.value = list.filter { it.role == "salesman" }
                }
            } else if (role == "supervisor") {
                // Load all salesmen under supervisor
                repository.getSalesmenBySupervisorFlow(_currentUser.value?.userId ?: "").collectLatest { list ->
                    _salesmenList.value = list
                }
            }
        }
    }

    private suspend fun restoreIncompleteVisit(salesmanId: String) {
        val startOfDay = System.currentTimeMillis() - 24 * 60 * 60 * 1000L // last 24 hours
        val latestVisits = repository.getLatestVisitToday(salesmanId, startOfDay)
        val incomplete = latestVisits.firstOrNull { it.checkOutTime == null }
        if (incomplete != null) {
            _currentActiveVisit.value = incomplete
            activeStore.value = repository.getStoreById(incomplete.storeId)
            startCheckoutCountdown(incomplete.checkInTime)
        }
    }

    // Interactive Check-In
    fun checkInStore(store: Store, photoBase64: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000L))

            // 1. Fake GPS Anti-Cheat Lockout (BASED ON MOCK LOCATION PARAM CHECK)
            if (devSimulateFakeGps.value) {
                authError.value = "ABSENSI DITOLAK (Fake GPS): Manipulasi koordinat terdeteksi!"
                return@launch
            }

            // 2. Impossible Speed Tracker (Haversine Formula Comparison)
            var isFraud = false
            var fraudReason = ""
            val yesterdaysOrTodaysVisits = repository.getLatestVisitToday(user.userId, startOfDay)
            val lastVisitCheckedOut = yesterdaysOrTodaysVisits.firstOrNull { it.checkOutTime != null }

            val currentLat = devCustomLatitude.value
            val currentLon = devCustomLongitude.value

            if (lastVisitCheckedOut != null) {
                val lastStoreId = lastVisitCheckedOut.storeId
                val lastStore = repository.getStoreById(lastStoreId)
                if (lastStore != null) {
                    val distanceKm = calculateHaversineDistance(
                        lastStore.latitude, lastStore.longitude,
                        currentLat, currentLon
                    )
                    
                    val timeDiffMs = System.currentTimeMillis() - lastVisitCheckedOut.checkOutTime!!
                    val timeDiffHours = timeDiffMs.toDouble() / (1000.0 * 60.0 * 60.0)

                    if (timeDiffHours > 0.0) {
                        val calculatedSpeedKmh = distanceKm / timeDiffHours
                        if (calculatedSpeedKmh > 80.0) {
                            isFraud = true
                            fraudReason = "Perpindahan Lokasi Mustahil (${String.format("%.1f", calculatedSpeedKmh)} km/h dari toko sebelumnya)"
                        }
                    }
                }
            }

            // Create Attendance entry
            val visit = AttendanceVisit(
                visitId = "VISIT-${UUID.randomUUID().toString().substring(0, 8).uppercase()}",
                projectId = user.projectId,
                salesmanId = user.userId,
                storeId = store.storeId,
                checkInTime = System.currentTimeMillis(),
                checkInLatitude = currentLat,
                checkInLongitude = currentLon,
                checkInPhoto = photoBase64,
                fraudAlert = isFraud,
                fraudReason = fraudReason
            )

            repository.insertVisit(visit)
            _currentActiveVisit.value = visit
            activeStore.value = store
            
            if (FirebaseManager.isInitialized(context)) {
                try {
                    FirebaseManager.syncVisitToFirestore(visit)
                } catch (fe: Exception) {
                    android.util.Log.e("SmartSales", "Firestore sync visit failed", fe)
                }
            }
            
            // Trigger 5-minute checkout countdown
            startCheckoutCountdown(visit.checkInTime)
        }
    }

    // Fast-forward countdown cheat (for live review testing)
    fun cheatFastForwardCheckInTime() {
        val active = _currentActiveVisit.value ?: return
        viewModelScope.launch {
            // Subtract 5 minutes from checkInTime in local state & DB to allow instant check-out
            val cheatedTime = active.checkInTime - (6 * 60 * 1000L)
            val updated = active.copy(checkInTime = cheatedTime)
            repository.insertVisit(updated)
            _currentActiveVisit.value = updated
            if (FirebaseManager.isInitialized(context)) {
                try {
                    FirebaseManager.syncVisitToFirestore(updated)
                } catch (fe: Exception) {
                    android.util.Log.e("SmartSales", "Firestore sync visit failed", fe)
                }
            }
            startCheckoutCountdown(cheatedTime)
        }
    }

    private fun startCheckoutCountdown(checkInTime: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val elapsedSeconds = (System.currentTimeMillis() - checkInTime) / 1000
                val remaining = (300 - elapsedSeconds).toInt() // 5 minutes = 300 seconds
                if (remaining <= 0) {
                    _countdownSeconds.value = 0
                    break
                } else {
                    _countdownSeconds.value = remaining
                }
                delay(1000)
            }
        }
    }

    // Interactive Check-Out
    fun checkOutStore(photoBase64: String, visitStatus: String) {
        viewModelScope.launch {
            val active = _currentActiveVisit.value ?: return@launch
            
            // Duration Guard
            val elapsedSeconds = (System.currentTimeMillis() - active.checkInTime) / 1000
            if (elapsedSeconds < 300) {
                // Should be blocked by UI but just in case:
                return@launch
            }

            val updated = active.copy(
                checkOutTime = System.currentTimeMillis(),
                checkOutPhoto = photoBase64,
                durationMinutes = ((System.currentTimeMillis() - active.checkInTime) / (60000L)).toInt().coerceAtLeast(1),
                visitStatus = visitStatus
            )

            repository.insertVisit(updated)
            _currentActiveVisit.value = null
            activeStore.value = null
            _shoppingCart.value = emptyMap()
            countdownJob?.cancel()
            _countdownSeconds.value = 0

            if (FirebaseManager.isInitialized(context)) {
                try {
                    FirebaseManager.syncVisitToFirestore(updated)
                } catch (fe: Exception) {
                    android.util.Log.e("SmartSales", "Firestore sync visit failed", fe)
                }
            }
        }
    }

    // Shopping Cart Operations
    fun addToCart(product: Product) {
        val currentMap = _shoppingCart.value.toMutableMap()
        val currentQty = currentMap[product] ?: 0
        if (currentQty < product.stock && currentQty < product.maxOrderLimit) {
            currentMap[product] = currentQty + 1
            _shoppingCart.value = currentMap
        }
    }

    fun removeFromCart(product: Product) {
        val currentMap = _shoppingCart.value.toMutableMap()
        val currentQty = currentMap[product] ?: 0
        if (currentQty > 0) {
            if (currentQty == 1) {
                currentMap.remove(product)
            } else {
                currentMap[product] = currentQty - 1
            }
            _shoppingCart.value = currentMap
        }
    }

    fun getCartTotal(): Int {
        return _shoppingCart.value.entries.sumOf { it.key.price * it.value }
    }

    // Submit Order containing Firestore transaction logic simulation (deducting counts, stock, logging turnovers)
    fun submitActiveOrder() {
        viewModelScope.launch {
            val active = _currentActiveVisit.value ?: return@launch
            val salesman = _currentUser.value ?: return@launch
            val cart = _shoppingCart.value
            if (cart.isEmpty()) return@launch

            val itemsArray = JSONArray()
            var totalOmset = 0

            cart.forEach { (product, qty) ->
                val subtotal = product.price * qty
                totalOmset += subtotal

                val itemJson = JSONObject().apply {
                    put("sku", product.skuId)
                    put("name", product.name)
                    put("qty", qty)
                    put("price", product.price)
                    put("subtotal", subtotal)
                }
                itemsArray.put(itemJson)

                // Cut warehousing stock directly
                val updatedProduct = product.copy(stock = (product.stock - qty).coerceAtLeast(0))
                repository.insertProduct(updatedProduct)
            }

            val orderId = "ORD-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
            val order = Order(
                orderId = orderId,
                projectId = salesman.projectId,
                salesmanId = salesman.userId,
                supervisorId = salesman.supervisorId,
                storeId = active.storeId,
                itemsJson = itemsArray.toString(),
                totalOmset = totalOmset,
                paymentStatus = "Belum Bayar",
                totalPaid = 0,
                remainingPiutang = totalOmset,
                createdAt = System.currentTimeMillis()
            )

            repository.insertOrder(order)
            _shoppingCart.value = emptyMap()
            if (FirebaseManager.isInitialized(context)) {
                try {
                    FirebaseManager.syncOrderToFirestore(order)
                } catch (fe: Exception) {
                    android.util.Log.e("SmartSales", "Firestore sync order failed", fe)
                }
            }
        }
    }

    // Installment / Full balance collections
    fun collectInvoicePayment(order: Order, amountPaid: Int, receiptPhotoBase64: String) {
        viewModelScope.launch {
            val newTotalPaid = order.totalPaid + amountPaid
            val newRemaining = (order.totalOmset - newTotalPaid).coerceAtLeast(0)
            val newStatus = if (newRemaining == 0) "Lunas" else "Sebagian"

            val updatedOrder = order.copy(
                totalPaid = newTotalPaid,
                remainingPiutang = newRemaining,
                paymentStatus = newStatus
            )

            repository.updateOrder(updatedOrder)
            if (FirebaseManager.isInitialized(context)) {
                try {
                    FirebaseManager.syncOrderToFirestore(updatedOrder)
                } catch (fe: Exception) {
                    android.util.Log.e("SmartSales", "Firestore sync order payment failed", fe)
                }
            }
        }
    }

    // Super Admin SKU product actions
    fun createOrUpdateProduct(skuId: String, name: String, price: Int, maxLimit: Int, stock: Int) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val product = Product(
                skuId = skuId.trim(),
                projectId = user.projectId,
                name = name.trim(),
                price = price,
                maxOrderLimit = maxLimit,
                stock = stock
            )
            repository.insertProduct(product)
        }
    }

    fun removeProduct(skuId: String) {
        viewModelScope.launch {
            repository.deleteProduct(skuId)
        }
    }

    // Dynamic store registration in the field
    fun createStore(
        storeId: String,
        storeName: String,
        ownerName: String,
        category: String,
        potential: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            try {
                val user = _currentUser.value ?: return@launch
                val newStore = Store(
                    storeId = storeId.trim().ifEmpty { "STR-${UUID.randomUUID().toString().substring(0, 8).uppercase()}" },
                    projectId = user.projectId,
                    storeName = storeName.trim(),
                    ownerName = ownerName.trim(),
                    category = category,
                    potential = potential,
                    address = address.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    registeredBy = user.userId,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertStore(newStore)
                if (FirebaseManager.isInitialized(context)) {
                    try {
                        FirebaseManager.syncStoreToFirestore(newStore)
                    } catch (fe: Exception) {
                        android.util.Log.e("SmartSales", "Firestore sync store failed", fe)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SmartSales", "Gagal mendaftarkan toko baru", e)
            }
        }
    }

    // Super Admin/Supervisor create staff accounts
    fun createStaffAccount(userId: String, name: String, email: String, role: String, password: String) {
        viewModelScope.launch {
            val creator = _currentUser.value ?: return@launch
            val salesmanSpvId = if (role == "salesman") creator.userId else null
            
            val staff = User(
                userId = userId.trim().uppercase(),
                projectId = creator.projectId,
                name = name.trim(),
                email = email.trim(),
                role = role,
                supervisorId = salesmanSpvId,
                deviceId = null,
                password = password
            )
            repository.saveUser(staff)
            if (FirebaseManager.isInitialized(context)) {
                try {
                    FirebaseManager.firebaseSignUp(email.trim(), password)
                    FirebaseManager.syncUserToFirestore(staff)
                } catch (fe: Exception) {
                    android.util.Log.e("SmartSales", "Firebase signup staff failed", fe)
                }
            }
            refreshUserLists(creator.projectId, creator.role)
        }
    }

    // Reset specific user's device lock
    fun resetDeviceBinding(userId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SmartSales", "Resetting device lock for: $userId")
                val user = repository.getUser(userId)
                if (user != null) {
                    user.deviceId = null
                    repository.saveUser(user)
                    android.util.Log.d("SmartSales", "Successfully cleared device binding for $userId")
                    if (inputUserId.value.trim().equals(userId, ignoreCase = true)) {
                        authError.value = null
                    }
                    val current = _currentUser.value
                    if (current != null) {
                        refreshUserLists(current.projectId, current.role)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SmartSales", "Error resetting device lock", e)
            }
        }
    }

    // Resets device locks for dynamic accounts registered by the user
    fun clearAllDeviceLocks() {
        viewModelScope.launch {
            try {
                android.util.Log.d("SmartSales", "Clearing device locks dynamically...")
                val typedUser = inputUserId.value.trim()
                val typedProject = inputProjectId.value.trim()

                if (typedUser.isNotEmpty()) {
                    val user = repository.getUser(typedUser)
                    if (user != null) {
                        user.deviceId = null
                        repository.saveUser(user)
                    }
                }

                val projId = currentUser.value?.projectId ?: typedProject
                if (projId.isNotEmpty()) {
                    val usersInProj = repository.getUsersFlow(projId).firstOrNull() ?: emptyList()
                    usersInProj.forEach { user ->
                        if (user.deviceId != null) {
                            user.deviceId = null
                            repository.saveUser(user)
                        }
                    }
                }
                authError.value = null
                android.util.Log.d("SmartSales", "Device locks reset successfully.")
            } catch (e: Exception) {
                android.util.Log.e("SmartSales", "Error in clearAllDeviceLocks", e)
            }
        }
    }

    // Haversine distance tracking formula
    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}

// Factory matching Android dependency standards
class MainViewModelFactory(
    private val context: Context,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context.applicationContext, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
