package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.components.LeafletMapWebView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header display
            HeaderWidget(viewModel)

            // Inner Workspace Router
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentScreen) {
                    "login" -> LoginScreen(viewModel)
                    "dashboard" -> {
                        when (currentUser?.role) {
                            "salesman" -> SalesmanDashboard(viewModel)
                            "supervisor" -> SupervisorDashboard(viewModel)
                            "super_admin" -> SuperAdminDashboard(viewModel)
                            else -> Text("Role tidak diketahui", color = Color.White)
                        }
                    }
                }
            }

            // QA Anti-Fraud Simulator Drawer Panel (Always floatable or docked at bottom for testing)
            SimulatorPanel(viewModel)
        }
    }
}

@Composable
fun HeaderWidget(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentCompany by viewModel.currentCompany.collectAsState()

    Surface(
        color = CardNavy,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (currentUser != null) {
                    Text(
                        text = currentCompany?.companyName ?: "PT Toko Sejahtera",
                        color = NeonTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentUser!!.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when (currentUser!!.role) {
                                "super_admin" -> WarmGold.copy(alpha = 0.2f)
                                "supervisor" -> SoftCyan.copy(alpha = 0.2f)
                                else -> NeonTeal.copy(alpha = 0.2f)
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = "  ${currentUser!!.role.uppercase()}  ",
                                color = when (currentUser!!.role) {
                                    "super_admin" -> WarmGold
                                    "supervisor" -> SoftCyan
                                    else -> NeonTeal
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project ID: ${currentUser!!.projectId}",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Logo",
                            tint = NeonTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMART SALES TRACKING",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            if (currentUser != null) {
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Keluar",
                        tint = AlertRed
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val projectId by viewModel.inputProjectId.collectAsState()
    val userId by viewModel.inputUserId.collectAsState()
    val password by viewModel.inputPassword.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val simulateId by viewModel.devSimulateDeviceId.collectAsState()

    val regProjectId by viewModel.regProjectId.collectAsState()
    val regCompanyName by viewModel.regCompanyName.collectAsState()
    val regAdminId by viewModel.regAdminId.collectAsState()
    val regAdminName by viewModel.regAdminName.collectAsState()
    val regAdminEmail by viewModel.regAdminEmail.collectAsState()
    val regAdminPassword by viewModel.regAdminPassword.collectAsState()
    val regError by viewModel.regError.collectAsState()
    val regSuccess by viewModel.regSuccess.collectAsState()
    val isRegisterMode by viewModel.isRegisterMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isRegisterMode) {
            // ==========================================
            // SIGN IN MODE
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Secure Login",
                        tint = NeonTeal,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sign In Kredensial",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart Sales & Anti-Fraud Suite",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Compact Status Badge with Expandable Config
                    var showFirebaseConfig by remember { mutableStateOf(false) }
                    val firebaseState = FirebaseManager.isInitialized(LocalContext.current)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFirebaseConfig = !showFirebaseConfig }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (firebaseState) Color(0xFF1B5E20) else Color(0xFF3E2723),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (firebaseState) "ONLINE: Sinkronisasi Firebase Aktif ⚡" else "OFFLINE: Penyimpanan Room SQLite Aktif 🔐",
                            color = if (firebaseState) Color(0xFF81C784) else Color(0xFFFFB74D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config Settings",
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (showFirebaseConfig) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SpaceBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Atur Koneksi Firebase Anda:",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val fbApiKey by viewModel.firebaseApiKey.collectAsState()
                                val fbAppId by viewModel.firebaseAppId.collectAsState()
                                val fbProjId by viewModel.firebaseProjectId.collectAsState()
                                val fbSuccess by viewModel.firebaseSetupSuccess.collectAsState()
                                val fbError by viewModel.firebaseSetupError.collectAsState()

                                OutlinedTextField(
                                    value = fbProjId,
                                    onValueChange = { viewModel.firebaseProjectId.value = it },
                                    label = { Text("Project ID", color = TextGray, fontSize = 10.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonTeal,
                                        unfocusedBorderColor = TextGray
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = fbApiKey,
                                    onValueChange = { viewModel.firebaseApiKey.value = it },
                                    label = { Text("API Key", color = TextGray, fontSize = 10.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonTeal,
                                        unfocusedBorderColor = TextGray
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = fbAppId,
                                    onValueChange = { viewModel.firebaseAppId.value = it },
                                    label = { Text("App ID", color = TextGray, fontSize = 10.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonTeal,
                                        unfocusedBorderColor = TextGray
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )

                                if (fbSuccess != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = fbSuccess!!, color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (fbError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = fbError!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.setupFirebaseDynamically() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftCyan),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("HUBUNGKAN FIREBASE ⚡", color = SpaceBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input fields
                    OutlinedTextField(
                        value = projectId,
                        onValueChange = { viewModel.inputProjectId.value = it },
                        label = { Text("Project ID", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userId,
                        onValueChange = { viewModel.inputUserId.value = it },
                        label = { Text("User ID (Contoh: ADM-99 / SLS-01)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.inputPassword.value = it },
                        label = { Text("Password", color = TextGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.checkLogin() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "VERIFIKASI & MASUK",
                            color = SpaceBlack,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { viewModel.isRegisterMode.value = true }
                    ) {
                        Text(
                            text = "Belum memiliki Project? DAFTAR INSTANSI BARU 💼",
                            color = NeonTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // REGISTER INSTANCE & ADMIN MODE (SaaS Slate)
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Register Instansi",
                        tint = SoftCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Registrasi Project Baru",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aktifkan instansi multi-tenant dengan Super Admin",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = regProjectId,
                        onValueChange = { viewModel.regProjectId.value = it },
                        label = { Text("PROJECT ID (contoh: MYCORP-2026)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regCompanyName,
                        onValueChange = { viewModel.regCompanyName.value = it },
                        label = { Text("Nama Perusahaan (contoh: PT Maju Lestari)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regAdminId,
                        onValueChange = { viewModel.regAdminId.value = it },
                        label = { Text("Buat ID Super Admin (contoh: ADM-01)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regAdminName,
                        onValueChange = { viewModel.regAdminName.value = it },
                        label = { Text("Nama Lengkap Super Admin", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regAdminEmail,
                        onValueChange = { viewModel.regAdminEmail.value = it },
                        label = { Text("Email Super Admin", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regAdminPassword,
                        onValueChange = { viewModel.regAdminPassword.value = it },
                        label = { Text("Password Super Admin", color = TextGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.registerCompany() },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "DAFTARKAN INSTANSI",
                            color = SpaceBlack,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { viewModel.isRegisterMode.value = false }
                    ) {
                        Text(
                            text = "Sudah memiliki Project ID? SIGN IN SEKARANG 🔒",
                            color = SoftCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // ALERTS & NOTIFICATIONS
        // ==========================================
        if (regError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = AlertRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AlertRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = AlertRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = regError!!, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (regSuccess != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = NeonTeal.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, NeonTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = NeonTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = regSuccess!!, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (authError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = AlertRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AlertRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = AlertRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = authError!!,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Info Hardware ID yang terdetect
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 STATUS HARDWARE DEVICE LOCK:",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID Perangkat Terdeteksi: $simulateId",
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sistem mengikat ID perangkat saat login pertama kali secara permanen untuk mencegah manipulasi absensi salesman luar area.",
                    color = TextGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// SIMULATOR PANEL FOR LIVE EVALUATION
@Composable
fun SimulatorPanel(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val fakeGps by viewModel.devSimulateFakeGps.collectAsState()
    val simDeviceId by viewModel.devSimulateDeviceId.collectAsState()
    val lat by viewModel.devCustomLatitude.collectAsState()
    val lon by viewModel.devCustomLongitude.collectAsState()
    val activeVisit by viewModel.currentActiveVisit.collectAsState()

    Surface(
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Builder",
                        tint = NeonTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚙️ PANEL SIMULASI ABSEN & ANTI-KECURANGAN (QA)",
                        color = NeonTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Toggle",
                    tint = TextWhite
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                // GPS Presets & Adjustments
                Text("1. Simulasi Geoposisi Lapangan (GPS):", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = {
                            viewModel.devCustomLatitude.value = -6.2088
                            viewModel.devCustomLongitude.value = 106.8456
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Set Jakarta (-6.2088, 106.8456)", color = TextWhite, fontSize = 9.sp)
                    }
                    Button(
                        onClick = {
                            // JUMP 450KM AWAY dynamically to trigger impossible speed!
                            viewModel.devCustomLatitude.value = -7.7523
                            viewModel.devCustomLongitude.value = 110.3789
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Set Sleman (+450km Jauh)", color = WarmGold, fontSize = 9.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lat: ${String.format("%.4f", lat)}", color = TextGray, fontSize = 10.sp)
                        Slider(
                            value = lat.toFloat(),
                            onValueChange = { viewModel.devCustomLatitude.value = it.toDouble() },
                            valueRange = -9.0f..-5.0f,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lon: ${String.format("%.4f", lon)}", color = TextGray, fontSize = 10.sp)
                        Slider(
                            value = lon.toFloat(),
                            onValueChange = { viewModel.devCustomLongitude.value = it.toDouble() },
                            valueRange = 105.0f..115.0f,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Fake GPS simulation representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("2. Simulasikan Fake GPS (isMocked = true)", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Saat diaktifkan, tombol check-in akan langsung diblokir.", color = TextGray, fontSize = 9.sp)
                    }
                    Switch(
                        checked = fakeGps,
                        onCheckedChange = { viewModel.devSimulateFakeGps.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonTeal, checkedTrackColor = NeonTeal.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fast Forward Duration countdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("3. Lewati Batas Waktu Kunjungan (Min 5 Menit)", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Memajukan waktu check-in 5+ menit ke bekalang agar instan checkout.", color = TextGray, fontSize = 9.sp)
                    }
                    Button(
                        onClick = { viewModel.cheatFastForwardCheckInTime() },
                        enabled = activeVisit != null,
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, disabledContainerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Text("Fast Forward 5 Menit", color = if (activeVisit != null) SpaceBlack else TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ID login mismatch simulation
                Column {
                    Text("4. Simulasikan Hardware ID yang Login (Device ID)", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = simDeviceId,
                            onValueChange = { viewModel.devSimulateDeviceId.value = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = TextGray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { viewModel.devSimulateDeviceId.value = "HARDWARE_CHEAT_99" },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                        ) {
                            Text("Bikin Salah ID", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text("5. Pintasan Reset Keamanan Perangkat (Anti-Locked):", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { viewModel.clearAllDeviceLocks() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔓 SET ULANG SEMUA PENGIKATAN DEVID SALESMAN", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// SIMULATED CAMERA DIALOG FOR TESTING IN EMULATOR WITHOUT PHONE HARDWARE
@Composable
fun SimulatedCameraDialog(
    title: String,
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Generate clean beautiful local placeholder templates of store/payment receipt to encode as mock base64!
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful interactive camera capture selector card
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, NeonTeal), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Cam", tint = NeonTeal, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("KAMERA RAW SIMULATOR", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Gallery Locked (Keamanan)", color = AlertRed, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Pilih Template Foto Toko Lapangan:", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = {
                            val dummyBase64 = generateMockBase64Photo("Store Front - Toko Kelontong Berkah aktif")
                            onPhotoCaptured(dummyBase64)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
                    ) {
                        Text("Toko Bukak 🏪", color = NeonTeal, fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            val dummyBase64 = generateMockBase64Photo("Store Front - Toko Tutup sore")
                            onPhotoCaptured(dummyBase64)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
                    ) {
                        Text("Toko Tutup 🔒", color = AlertRed, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val dummyBase64 = generateMockBase64Photo("Receipt payment Cash atau Bank Transfer physical printout")
                        onPhotoCaptured(dummyBase64)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kuitansi / Bukti Bayar Tunai 🧾", color = WarmGold, fontSize = 10.sp)
                }
            }
        }
    }
}

// Generate real local image representation encoded as Base64 to show checking-in photos
fun generateMockBase64Photo(tag: String): String {
    val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paintBg = Paint().apply { color = 0xFF191D30.toInt() }
    canvas.drawRect(0f, 0f, 160f, 160f, paintBg)

    val paintText = Paint().apply {
        color = 0xFF00FFCC.toInt()
        textSize = 10f
        isAntiAlias = true
    }
    canvas.drawText("SMART CRM PHOTO", 12f, 40f, paintText)
    canvas.drawText("Verified GPS", 12f, 65f, paintText)

    val paintTag = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 8f
        isAntiAlias = true
    }
    val words = tag.split(" ")
    var y = 100f
    words.forEach { word ->
        canvas.drawText(word, 12f, y, paintTag)
        y += 12f
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
    val bytes = stream.toByteArray()
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

// DASHBOARD FOR SALESMAN
@Composable
fun SalesmanDashboard(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf("kunjungan") } // kunjungan, order, collection, history
    val activeVisit by viewModel.currentActiveVisit.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (activeTab) {
                "kunjungan" -> 0
                "order" -> 1
                "collection" -> 2
                else -> 3
            },
            containerColor = CardNavy,
            contentColor = NeonTeal
        ) {
            Tab(selected = activeTab == "kunjungan", onClick = { activeTab = "kunjungan" }) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = "Run", tint = if (activeTab == "kunjungan") NeonTeal else TextGray)
                    Text("CRM Kunjungan", color = if (activeTab == "kunjungan") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "order", onClick = { activeTab = "order" }, enabled = activeVisit != null) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart", tint = if (activeVisit == null) TextGray.copy(alpha = 0.3f) else if (activeTab == "order") NeonTeal else TextGray)
                    Text("Input Order", color = if (activeVisit == null) TextGray.copy(alpha = 0.3f) else if (activeTab == "order") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "collection", onClick = { activeTab = "collection" }, enabled = activeVisit != null) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Collection", tint = if (activeVisit == null) TextGray.copy(alpha = 0.3f) else if (activeTab == "collection") NeonTeal else TextGray)
                    Text("Collection", color = if (activeVisit == null) TextGray.copy(alpha = 0.3f) else if (activeTab == "collection") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "history", onClick = { activeTab = "history" }) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "History", tint = if (activeTab == "history") NeonTeal else TextGray)
                    Text("Histori Absen", color = if (activeTab == "history") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                "kunjungan" -> CRMKunjunganScreen(viewModel)
                "order" -> OrderEntryScreen(viewModel)
                "collection" -> InvoiceCollectionScreen(viewModel)
                "history" -> HistoricVisitsScreen(viewModel)
            }
        }
    }
}

@Composable
fun CRMKunjunganScreen(viewModel: MainViewModel) {
    val stores by viewModel.allStores.collectAsState()
    val activeVisit by viewModel.currentActiveVisit.collectAsState()
    val lat by viewModel.devCustomLatitude.collectAsState()
    val lon by viewModel.devCustomLongitude.collectAsState()

    var showCamera by remember { mutableStateOf(false) }
    var showCameraCheckOut by remember { mutableStateOf(false) }
    var storeToCheckIn by remember { mutableStateOf<Store?>(null) }

    val countdown by viewModel.countdownSeconds.collectAsState()

    var showAddStoreDialog by remember { mutableStateOf(false) }
    var newStoreId by remember { mutableStateOf("") }
    var newStoreName by remember { mutableStateOf("") }
    var newOwnerName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Warung") }
    var newPotential by remember { mutableStateOf("Sedang") }
    var newAddress by remember { mutableStateOf("") }
    var newLatString by remember { mutableStateOf("") }
    var newLonString by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (activeVisit == null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 DAFTAR TOKO CRM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            newStoreId = "STR-${java.util.UUID.randomUUID().toString().substring(0, 8).uppercase()}"
                            newStoreName = ""
                            newOwnerName = ""
                            newCategory = "Warung"
                            newPotential = "Sedang"
                            newAddress = ""
                            newLatString = lat.toString()
                            newLonString = lon.toString()
                            showAddStoreDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Store", tint = SpaceBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TAMBAH TOKO CRM", color = SpaceBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (stores.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏪 BELUM ADA TOKO TERDAFTAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Project ID Anda saat ini tidak memiliki toko terdaftar. Silakan tambahkan Toko CRM baru untuk memulai check-in lapangan.", color = TextGray, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
                        }
                    }
                }
            }

            items(stores) { store ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(store.storeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Owner: ${store.ownerName} | ${store.category}", color = TextGray, fontSize = 12.sp)
                            }
                            Surface(
                                color = when (store.potential) {
                                    "Besar" -> NeonTeal.copy(alpha = 0.2f)
                                    "Sedang" -> WarmGold.copy(alpha = 0.2f)
                                    else -> TextGray.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "  ${store.potential}  ",
                                    color = when (store.potential) {
                                        "Besar" -> NeonTeal
                                        "Sedang" -> WarmGold
                                        else -> TextGray
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Pin", tint = NeonTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(store.address, color = TextGray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                storeToCheckIn = store
                                showCamera = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = "In", tint = SpaceBlack)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHECK-IN LAPANGAN", color = SpaceBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // There is an active checkin
            val currentCheckedStore = stores.firstOrNull { it.storeId == activeVisit!!.storeId }
            if (currentCheckedStore != null) {
                item {
                    Column {
                        Surface(
                            color = NeonTeal.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(NeonTeal, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("SEDANG CHECK-IN TERKAWAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Toko: ${currentCheckedStore.storeName}", color = TextGray, fontSize = 12.sp)
                                    Text("Mulai: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(activeVisit!!.checkInTime))}", color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // FREE OPENSTREETMAP LEAFLET INTEGRATED WEBVIEW MAP
                        Text("🗺️ KOORDINAT VALIDASI PETA (OSM & LEAFLET):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LeafletMapWebView(
                            storeLat = currentCheckedStore.latitude,
                            storeLon = currentCheckedStore.longitude,
                            storeName = currentCheckedStore.storeName,
                            currentLat = lat,
                            currentLon = lon,
                            onLocationChanged = { nLat, nLon ->
                                viewModel.devCustomLatitude.value = nLat
                                viewModel.devCustomLongitude.value = nLon
                            },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Checkout Section with duration verification
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("VALIDASI CHECK-OUT UTAMA", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Sesuai aturan anti-manipulasi, checkout hanya diperkenankan minimal setelah 5 menit (300 detik) berada di toko fisik.", color = TextGray, fontSize = 11.sp, lineHeight = 15.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                val minDurationMet = countdown <= 0

                                if (!minDurationMet) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = AlertRed)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Terkunci: Sisa countdown ${countdown}s sebelum checkout aktif",
                                            color = TextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeonTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check", tint = NeonTeal)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Batas durasi terpenuhi! Tombol check-out diaktifkan.",
                                            color = TextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                var selectedStatus by remember { mutableStateOf("Order") }
                                Text("Update Status Kunjungan:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))

                                val options = listOf("Order", "Toko Tutup", "Stok Banyak", "Owner Tidak Ada")
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(bottom = 12.dp)
                                ) {
                                    options.forEach { option ->
                                        Surface(
                                            color = if (selectedStatus == option) NeonTeal else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .clickable { selectedStatus = option }
                                        ) {
                                            Text(
                                                text = option,
                                                color = if (selectedStatus == option) SpaceBlack else TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { showCameraCheckOut = true },
                                    enabled = minDurationMet,
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, disabledContainerColor = TextGray.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Out", tint = if (minDurationMet) Color.White else TextGray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CHECK-OUT & KUNCI REKAP", color = if (minDurationMet) Color.White else TextGray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCamera) {
        SimulatedCameraDialog(
            title = "AMBIL FOTO TOKO (CHECK-IN)",
            onPhotoCaptured = { pic ->
                if (storeToCheckIn != null) {
                    viewModel.checkInStore(storeToCheckIn!!, pic)
                }
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
    }

    if (showCameraCheckOut) {
        SimulatedCameraDialog(
            title = "AMBIL FOTO PENUTUP (CHECK-OUT)",
            onPhotoCaptured = { pic ->
                viewModel.checkOutStore(pic, "Order")
                showCameraCheckOut = false
            },
            onDismiss = { showCameraCheckOut = false }
        )
    }

    if (showAddStoreDialog) {
        Dialog(onDismissRequest = { showAddStoreDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("🏪 DAFTAR TOKO CRM BARU", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newStoreId,
                        onValueChange = { newStoreId = it },
                        label = { Text("ID Toko (Auto-Generated)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newStoreName,
                        onValueChange = { newStoreName = it },
                        label = { Text("Nama Toko", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newOwnerName,
                        onValueChange = { newOwnerName = it },
                        label = { Text("Nama Pemilik (Owner)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Kategori Toko:", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Warung", "Minimarket", "Supermarket", "Horeca").forEach { cat ->
                            Button(
                                onClick = { newCategory = cat },
                                colors = ButtonDefaults.buttonColors(containerColor = if (newCategory == cat) NeonTeal else Color.White.copy(alpha = 0.05f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(cat, color = if (newCategory == cat) SpaceBlack else TextWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Potensi Toko:", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Besar", "Sedang", "Kecil").forEach { pot ->
                            Button(
                                onClick = { newPotential = pot },
                                colors = ButtonDefaults.buttonColors(containerColor = if (newPotential == pot) NeonTeal else Color.White.copy(alpha = 0.05f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(pot, color = if (newPotential == pot) SpaceBlack else TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Alamat Fisik Lengkap", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newLatString,
                            onValueChange = { newLatString = it },
                            label = { Text("Latitude", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newLonString,
                            onValueChange = { newLonString = it },
                            label = { Text("Longitude", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            newLatString = lat.toString()
                            newLonString = lon.toString()
                        }
                    ) {
                        Text("📍 Autofill Koordinat GPS Lapangan Saat Ini", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🗺️ KOORDINAT VALIDASI PETA (TAP/GESER PIN):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val previewLat = newLatString.toDoubleOrNull() ?: lat
                    val previewLon = newLonString.toDoubleOrNull() ?: lon
                    LeafletMapWebView(
                        storeLat = previewLat,
                        storeLon = previewLon,
                        storeName = if (newStoreName.isEmpty()) "Calon Toko" else newStoreName,
                        currentLat = previewLat,
                        currentLon = previewLon,
                        onLocationChanged = { nLat, nLon ->
                            newLatString = String.format(Locale.US, "%.6f", nLat)
                            newLonString = String.format(Locale.US, "%.6f", nLon)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddStoreDialog = false }) {
                            Text("BATAL", color = TextGray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (newStoreName.isNotEmpty() && newAddress.isNotEmpty()) {
                                    val latVal = newLatString.toDoubleOrNull() ?: lat
                                    val lonVal = newLonString.toDoubleOrNull() ?: lon
                                    viewModel.createStore(
                                        storeId = newStoreId,
                                        storeName = newStoreName,
                                        ownerName = newOwnerName,
                                        category = newCategory,
                                        potential = newPotential,
                                        address = newAddress,
                                        latitude = latVal,
                                        longitude = lonVal
                                    )
                                    showAddStoreDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                        ) {
                            Text("SIMPAN", color = SpaceBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderEntryScreen(viewModel: MainViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val cart by viewModel.shoppingCart.collectAsState()
    val activeVisit by viewModel.currentActiveVisit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🛒 INPUT TRANSAKSI SKU PRODUK",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Kelola keranjang belanja pesanan toko real-time.",
            color = TextGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                val qtyInCart = cart[product] ?: 0
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("ID SKU: ${product.skuId}", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp)) {
                                    Text(" Stok: ${product.stock}  ", color = SoftCyan, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp)) {
                                    Text(" Limit: ${product.maxOrderLimit}  ", color = WarmGold, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rp ${product.price}", color = NeonTeal, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.removeFromCart(product) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Sub", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Text("  $qtyInCart  ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { viewModel.addToCart(product) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(NeonTeal, CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SpaceBlack, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total omset calculator card
        if (cart.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmGold.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, WarmGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal Produk (${cart.entries.sumOf { it.value }} item):", color = TextWhite, fontSize = 12.sp)
                        Text("Rp ${viewModel.getCartTotal()}", color = WarmGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.submitActiveOrder() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.SendToMobile, contentDescription = "Submit", tint = SpaceBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("KIRIM TRANSAKSI BATCH", color = SpaceBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCollectionScreen(viewModel: MainViewModel) {
    val activeStore by viewModel.activeStore.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    
    val unpaidOrders = orders.filter { 
        it.storeId == activeStore?.storeId && it.paymentStatus != "Lunas" 
    }

    var orderToPay by remember { mutableStateOf<Order?>(null) }
    var payAmountText by remember { mutableStateOf("") }
    var showCameraPay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🧾 PENAGIHAN PIUTANG (COLLECTION)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "List orders belum lunas untuk toko: ${activeStore?.storeName ?: "N/A"}",
            color = TextGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (unpaidOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Empty", tint = NeonTeal, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SEMUA TAGIHAN TOKO INI BEBAS / LUNAS", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(unpaidOrders) { order ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ID Order: ${order.orderId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    color = if (order.paymentStatus == "Belum Bayar") AlertRed.copy(alpha = 0.2f) else WarmGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = " ${order.paymentStatus} ",
                                        color = if (order.paymentStatus == "Belum Bayar") AlertRed else WarmGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Tagihan:", color = TextGray, fontSize = 11.sp)
                                Text("Rp ${order.totalOmset}", color = TextWhite, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sudah Dibayar:", color = TextGray, fontSize = 11.sp)
                                Text("Rp ${order.totalPaid}", color = SoftCyan, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sisa Piutang (Settle):", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Rp ${order.remainingPiutang}", color = AlertRed, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    orderToPay = order
                                    payAmountText = order.remainingPiutang.toString()
                                    showCameraPay = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Payment, contentDescription = "Pay", tint = SpaceBlack)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("BAYAR CICILAN / PELUNASAN", color = SpaceBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraPay && orderToPay != null) {
        Dialog(onDismissRequest = { showCameraPay = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("💰 BAYAR SECURE BILLS", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ID Order: ${orderToPay!!.orderId}", color = TextWhite, fontSize = 11.sp)
                    Text("Sisa Piutang: Rp ${orderToPay!!.remainingPiutang}", color = WarmGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Jumlah Pembayaran (Rp)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aturan Anti-Fraud: Wajib lampirkan foto fisik kuitansi pelunasan dengan kamera langsung.", color = TextGray, fontSize = 10.sp, lineHeight = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { showCameraPay = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Text("Batal", color = TextWhite)
                        }
                        Button(
                            onClick = {
                                val amount = payAmountText.toIntOrNull() ?: 0
                                if (amount > 0 && amount <= orderToPay!!.remainingPiutang) {
                                    val dummyPic = generateMockBase64Photo("Receipt Payment - Rp $amount")
                                    viewModel.collectInvoicePayment(orderToPay!!, amount, dummyPic)
                                    showCameraPay = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                        ) {
                            Text("Ambil & Laporkan ✅", color = SpaceBlack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricVisitsScreen(viewModel: MainViewModel) {
    val visits by viewModel.allVisits.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "⏳ HISTORI KUNJUNGAN SALES",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (visits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada rekap kunjungan hari ini", color = TextGray)
            }
        } else {
            LazyColumn {
                items(visits) { visit ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (visit.fraudAlert) AlertRed.copy(alpha = 0.15f) else CardNavy
                        ),
                        border = if (visit.fraudAlert) BorderStroke(1.dp, AlertRed) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ID: ${visit.visitId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (visit.fraudAlert) {
                                    Surface(color = AlertRed, shape = RoundedCornerShape(4.dp)) {
                                        Text(" FRAUD DETECTED ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Surface(color = NeonTeal.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text(" AMAN ", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Store ID: ${visit.storeId}", color = TextWhite, fontSize = 12.sp)
                            Text("Check-In: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(visit.checkInTime))}", color = TextGray, fontSize = 11.sp)
                            
                            val out = visit.checkOutTime
                            if (out != null) {
                                Text("Check-Out: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(out))}", color = TextGray, fontSize = 11.sp)
                                Text("Durasi: ${visit.durationMinutes} menit", color = SoftCyan, fontSize = 11.sp)
                                Text("Visit Status: ${visit.visitStatus}", color = WarmGold, fontSize = 11.sp)
                            } else {
                                Text("Check-Out: Belum check-out", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (visit.fraudAlert) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Warn", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Alasan: ${visit.fraudReason}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// DASHBOARD FOR SUPERVISOR
@Composable
fun SupervisorDashboard(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf("team") } // team, register
    val creator by viewModel.currentUser.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (activeTab == "team") 0 else 1,
            containerColor = CardNavy,
            contentColor = NeonTeal
        ) {
            Tab(selected = activeTab == "team", onClick = { activeTab = "team" }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.People, contentDescription = "People", tint = if (activeTab == "team") NeonTeal else TextGray)
                    Text("  CRM Laporan Tim", color = if (activeTab == "team") NeonTeal else TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "register", onClick = { activeTab = "register" }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add", tint = if (activeTab == "register") NeonTeal else TextGray)
                    Text("  Daftar Salesman", color = if (activeTab == "register") NeonTeal else TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "team" -> SupervisorReportScreen(viewModel)
                "register" -> SupervisorRegisterSalesmanScreen(viewModel)
            }
        }
    }
}

@Composable
fun SupervisorReportScreen(viewModel: MainViewModel) {
    val salesmen by viewModel.salesmenList.collectAsState()
    val visits by viewModel.allVisits.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "📊 VISUALISASI KUNJUNGAN: RENCANA VS REALISASI",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Jetpack Compose Canvas comparative diagram rendering (Planned vs Realized visits)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val totalPlanned = (salesmen.size * 3).coerceAtLeast(3) // assume target 3 stores per salesman
                    val loggedVisitsList = visits.filter { v -> salesmen.any { it.userId == v.salesmanId } }
                    val totalRealised = loggedVisitsList.size
                    val totalFraudCount = loggedVisitsList.count { it.fraudAlert }

                    Text("Pencapaian Hari Ini:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        val maxVal = totalPlanned.coerceAtLeast(5).toFloat()
                        val w = size.width
                        val h = size.height

                        val barWidth = 60.dp.toPx()
                        val planHeight = ((totalPlanned / maxVal) * (h - 40f)).coerceAtLeast(10f)
                        val realHeight = ((totalRealised / maxVal) * (h - 40f)).coerceAtLeast(10f)

                        // 1. Planned Bar (Slate Grey)
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = Offset(w * 0.25f - barWidth / 2, h - planHeight - 20f),
                            size = Size(barWidth, planHeight)
                        )
                        // Label text inside DrawScope isn't trivial without an Android Paint context, let's keep it clean
                        
                        // 2. Realised Bar (Neon Teal)
                        drawRect(
                            color = NeonTeal,
                            topLeft = Offset(w * 0.75f - barWidth / 2, h - realHeight - 20f),
                            size = Size(barWidth, realHeight)
                        )

                        // Base axis
                        drawLine(
                            color = TextGray,
                            start = Offset(0f, h - 20f),
                            end = Offset(w, h - 20f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Target planned", color = TextGray, fontSize = 10.sp)
                            Text("$totalPlanned Toko", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Realisasi riil", color = NeonTeal, fontSize = 10.sp)
                            Text("$totalRealised Toko", color = NeonTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Fraud terdeteksi", color = AlertRed, fontSize = 10.sp)
                            Text("$totalFraudCount Kasus", color = AlertRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "🛒 LEDGER MONITOR KECURANGAN DAN AUDIT LAPANGAN",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        val filteredVisits = visits.filter { v -> salesmen.any { it.userId == v.salesmanId } }
        if (filteredVisits.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada laporan dari salesman bawahan Anda.", color = TextGray, fontSize = 12.sp)
                }
            }
        } else {
            items(filteredVisits) { visit ->
                val salesmanName = salesmen.firstOrNull { it.userId == visit.salesmanId }?.name ?: visit.salesmanId
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (visit.fraudAlert) AlertRed.copy(alpha = 0.15f) else CardNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (visit.fraudAlert) BorderStroke(1.dp, AlertRed) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(salesmanName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (visit.fraudAlert) {
                                Surface(color = AlertRed, shape = RoundedCornerShape(4.dp)) {
                                    Text(" INCIDENT ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(2.dp))
                                }
                            } else {
                                Surface(color = NeonTeal.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text(" APPROVED ", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toko: ${visit.storeId}", color = TextGray, fontSize = 11.sp)
                        Text("Waktu Check-In: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(visit.checkInTime))}", color = TextGray, fontSize = 11.sp)
                        
                        if (visit.checkOutTime != null) {
                            Text("Waktu Check-Out: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(visit.checkOutTime!!))}", color = TextGray, fontSize = 11.sp)
                            Text("Durasi: ${visit.durationMinutes} Menit", color = SoftCyan, fontSize = 11.sp)
                            Text("Status: ${visit.visitStatus}", color = WarmGold, fontSize = 11.sp)
                        } else {
                            Text("Checkout: Belum Checkout resmi", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (visit.fraudAlert) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.GpsOff, contentDescription = "Off", tint = AlertRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pemicu: ${visit.fraudReason}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupervisorRegisterSalesmanScreen(viewModel: MainViewModel) {
    var userId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val currentStaffList by viewModel.salesmenList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("👤 BUAT AKUN SALESMAN BARU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Daftarkan personil salesman di luar lapangan dan ikat dengan ID.", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID (contoh: SLS-201)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (contoh: salesid@projectid.com)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password Default", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (userId.isNotEmpty() && name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                            viewModel.createStaffAccount(userId, name, email, "salesman", password)
                            userId = ""
                            name = ""
                            email = ""
                            password = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DAFTARKAN SALESMAN", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Daftar Personil Terdaftar Anda:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        currentStaffList.forEach { staff ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(staff.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("UserID: ${staff.userId} | ${staff.email}", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Surface(
                        color = if (staff.deviceId != null) NeonTeal.copy(alpha = 0.15f) else TextGray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable(enabled = staff.deviceId != null) {
                            viewModel.resetDeviceBinding(staff.userId)
                        }
                    ) {
                        Text(
                            text = if (staff.deviceId != null) " LOCKED 📱 (TAP TO UNLOCK) " else " UNBOUND 🔓 ",
                            color = if (staff.deviceId != null) NeonTeal else TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }
            }
        }
    }
}

// DASHBOARD FOR SUPER ADMIN
@Composable
fun SuperAdminDashboard(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf("sku") } // sku, turnovers, spv, audits
    val admin by viewModel.currentUser.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (activeTab) {
                "sku" -> 0
                "turnovers" -> 1
                "spv" -> 2
                else -> 3
            },
            containerColor = CardNavy,
            contentColor = NeonTeal
        ) {
            Tab(selected = activeTab == "sku", onClick = { activeTab = "sku" }) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Inventory, contentDescription = "SKU", tint = if (activeTab == "sku") NeonTeal else TextGray)
                    Text("SKU Catalog", color = if (activeTab == "sku") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "turnovers", onClick = { activeTab = "turnovers" }) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = "Omset", tint = if (activeTab == "turnovers") NeonTeal else TextGray)
                    Text("Konsolidasi", color = if (activeTab == "turnovers") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "spv", onClick = { activeTab = "spv" }) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.SupervisorAccount, contentDescription = "SPV", tint = if (activeTab == "spv") NeonTeal else TextGray)
                    Text("Supervisors", color = if (activeTab == "spv") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == "audits", onClick = { activeTab = "audits" }) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Policy, contentDescription = "Audits", tint = if (activeTab == "audits") NeonTeal else TextGray)
                    Text("Audit Logs", color = if (activeTab == "audits") NeonTeal else TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "sku" -> SuperAdminSkuScreen(viewModel)
                "turnovers" -> SuperAdminTurnoverScreen(viewModel)
                "spv" -> SuperAdminSupervisorScreen(viewModel)
                "audits" -> SuperAdminAuditLogsScreen(viewModel)
            }
        }
    }
}

@Composable
fun SuperAdminSkuScreen(viewModel: MainViewModel) {
    val products by viewModel.allProducts.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var editSkuId by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editLimit by remember { mutableStateOf("") }
    var editStock by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📦 KELOLA SKU PRODUK & HARGA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(
                onClick = {
                    editSkuId = ""
                    editName = ""
                    editPrice = ""
                    editLimit = ""
                    editStock = ""
                    showEditor = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SpaceBlack)
                Text("PRODUK", color = SpaceBlack, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(products) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("ID SKU: ${product.skuId}", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("Harga: Rp ${product.price}", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Stok: ${product.stock} | Max Limit: ${product.maxOrderLimit}", color = TextGray, fontSize = 11.sp)
                        }

                        Row {
                            IconButton(onClick = {
                                editSkuId = product.skuId
                                editName = product.name
                                editPrice = product.price.toString()
                                editLimit = product.maxOrderLimit.toString()
                                editStock = product.stock.toString()
                                showEditor = true
                            }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = SoftCyan)
                            }
                            IconButton(onClick = { viewModel.removeProduct(product.skuId) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Del", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        Dialog(onDismissRequest = { showEditor = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("📝 FORM SKU CATALOUGES", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editSkuId,
                        onValueChange = { editSkuId = it },
                        label = { Text("ID SKU (contoh: PROD-101)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Barang", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Harga Jual (Rp)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editLimit,
                        onValueChange = { editLimit = it },
                        label = { Text("Maksimal Order Limit Qty", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editStock,
                        onValueChange = { editStock = it },
                        label = { Text("Jumlah Stok Gudang", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = { showEditor = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))) {
                            Text("Batal", color = TextWhite)
                        }
                        Button(
                            onClick = {
                                val price = editPrice.toIntOrNull() ?: 0
                                val limit = editLimit.toIntOrNull() ?: 0
                                val stock = editStock.toIntOrNull() ?: 0
                                if (editSkuId.isNotEmpty() && editName.isNotEmpty()) {
                                    viewModel.createOrUpdateProduct(editSkuId, editName, price, limit, stock)
                                    showEditor = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                        ) {
                            Text("Simpan SKU ✅", color = SpaceBlack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuperAdminTurnoverScreen(viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val stores by viewModel.allStores.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("🏢 GRAFIK OMSET KONSOLIDASI PERUSAHAAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Rincian akumulasi omset penjualan berdasarkan toko pendaftar.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

        val totalOmsetGlobal = orders.sumOf { it.totalOmset }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TOTAL OMSET TENTATIVE PT TOKO SEJAHTERA", color = TextGray, fontSize = 11.sp)
                Text("Rp $totalOmsetGlobal", color = NeonTeal, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }

        // Custom canvas draw bar diagram for consolidated sales across stores
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Diagram Bar Konsolidasi Toko:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val h = size.height
                    val w = size.width

                    // Let's allocate 3 bar channels for our 3 stores
                    val barWidth = 40.dp.toPx()
                    val gap = 30.dp.toPx()
                    val startX = (w - (3 * barWidth + 2 * gap)) / 2f

                    // Calculate totals per store
                    val storesWithOMset = stores.map { s ->
                        val storeTurnover = orders.filter { it.storeId == s.storeId }.sumOf { it.totalOmset }
                        Pair(s.storeName.take(12), s.potential to storeTurnover)
                    }

                    val maxOmset = storesWithOMset.maxOfOrNull { it.second.second }?.toFloat()?.coerceAtLeast(10000f) ?: 100000f

                    storesWithOMset.forEachIndexed { i, storeData ->
                        val x = startX + i * (barWidth + gap)
                        val barHeight = ((storeData.second.second / maxOmset) * (h - 40f)).coerceAtLeast(4f)

                        // Draw Bar
                        drawRect(
                            color = if (i == 0) NeonTeal else if (i == 1) SoftCyan else WarmGold,
                            topLeft = Offset(x, h - barHeight - 20f),
                            size = Size(barWidth, barHeight)
                        )
                    }

                    // Bottom horizontal axis
                    drawLine(
                        color = TextGray,
                        start = Offset(0f, h - 20f),
                        end = Offset(w, h - 20f),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend values list
                LazyColumn(modifier = Modifier.height(110.dp)) {
                    items(stores) { store ->
                        val storeTotal = orders.filter { it.storeId == store.storeId }.sumOf { it.totalOmset }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(NeonTeal, CircleShape))
                                Text("  ${store.storeName}", color = TextWhite, fontSize = 11.sp)
                            }
                            Text("Rp $storeTotal", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuperAdminSupervisorScreen(viewModel: MainViewModel) {
    var userId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val supervisors by viewModel.supervisorsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("🏢 KELOLA AKUN SUPERVISOR AREA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Buat supervisor penanggung-jawab wilayah penugasan.", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID Supervisor (contoh: SPV-015)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password Default", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = NeonTeal, unfocusedBorderColor = TextGray),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (userId.isNotEmpty() && name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                            viewModel.createStaffAccount(userId, name, email, "supervisor", password)
                            userId = ""
                            name = ""
                            email = ""
                            password = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CREATE SUPERVISOR", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Daftar Supervisor Aktif:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        supervisors.forEach { spv ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(spv.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("UserID: ${spv.userId} | ${spv.email}", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun SuperAdminAuditLogsScreen(viewModel: MainViewModel) {
    val visits by viewModel.allVisits.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("🕵️ AUDIT TRACE LOG GLOBAL & PENGAWASAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Pantau aktivitas salesman dan peringatan fraud waktu nyata seluruh kawasan.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

        if (visits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada rekap audit absensi hari ini.", color = TextGray)
            }
        } else {
            LazyColumn {
                items(visits) { visit ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (visit.fraudAlert) AlertRed.copy(alpha = 0.12f) else CardNavy
                        ),
                        border = if (visit.fraudAlert) BorderStroke(1.dp, AlertRed) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Salesman: ${visit.salesmanId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (visit.fraudAlert) {
                                    Surface(color = AlertRed, shape = RoundedCornerShape(4.dp)) {
                                        Text("  WARNING FRAUD  ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(2.dp))
                                    }
                                } else {
                                    Surface(color = NeonTeal.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                        Text("  VERIFIED OK  ", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Toko ID: ${visit.storeId}", color = TextGray, fontSize = 12.sp)
                            Text("Lokasi In: ${visit.checkInLatitude}, ${visit.checkInLongitude}", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            
                            val tCheckIn = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(visit.checkInTime))
                            Text("Waktu Absen: $tCheckIn", color = TextGray, fontSize = 11.sp)

                            if (visit.fraudAlert) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Warn", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uji Pemicu: ${visit.fraudReason}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
