// MainActivity.kt
package com.example.expendituremanager

//region --- IMPORTS ---
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // This imports all 'filled' icons
import androidx.compose.material.icons.filled.CreditCard // Explicit import for CreditCard
import androidx.compose.material.icons.filled.Fastfood // Explicit import for Fastfood
import androidx.compose.material.icons.filled.Movie // Explicit import for Movie
import androidx.compose.material.icons.filled.MusicNote // Explicit import for MusicNote
import androidx.compose.material.icons.filled.ShoppingCart // Explicit import for ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz // Explicit import for SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp // Explicit import for TrendingUp
import androidx.compose.material.icons.filled.CallReceived // Replaced ArrowInward
import androidx.compose.material.icons.filled.Mic // Explicit import for Mic icon
import androidx.compose.material.icons.filled.Lens // Explicit import for Lens icon (for active mic state)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expendituremanager.ui.theme.ExpenditureManagerTheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties // For making dialog fullscreen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi // For combinedClickable
import androidx.compose.foundation.combinedClickable // For combinedClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider // Explicit import for EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity // Import LocalDensity
import com.google.firebase.auth.ktx.auth
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.Query
import androidx.compose.foundation.layout.ExperimentalLayoutApi // Required for FlowRow
import androidx.compose.foundation.layout.FlowRow // Required for FlowRow
import androidx.compose.animation.animateContentSize // For smoother animations
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign // For Text alignment
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.foundation.rememberScrollState // Import rememberScrollState
import androidx.compose.foundation.verticalScroll // Import verticalScroll
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson // For JSON serialization
import java.util.Locale
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.WebSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

//endregion

// Data Models
// Represents a financial transaction (expense or income)
data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String = "", // Added default for Firestore
    val amount: Double = 0.0,    // Added default for Firestore
    val currency: String = "USD", // Added default for Firestore
    val type: TransactionType = TransactionType.EXPENSE, // Added default for Firestore
    val cardOrWallet: String = "", // Added default for Firestore
    val category: Category = Category(), // Added default for Firestore
    val date: Long = System.currentTimeMillis(), // Added default for Firestore
    var tempHeardText: String? = null // Temporary field for voice input
)

// Enum for transaction type
enum class TransactionType {
    EXPENSE, INCOME
}

// Represents a financial card or wallet
data class Card(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bankName: String = "",
    val cardNumber: String = "",
    val cardHolderName: String = "",
    val expiryDate: String = "", // MM/YY format
    val monthlyBudget: Double? = null, // Optional monthly budget for this card
    val currentMonthLeft: Double? = null, // Optional current month's remaining budget
    val previousMonthBudget: Double? = null, // Optional previous month's budget
    val previousMonthLeft: Double? = null // Optional previous month's remaining budget
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this("", "", "", "", "")
}

// Represents a transaction category
data class Category(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val colorHex: String = "#00B140" // Store color as hex string for Firestore
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this("", "")

    // Helper to convert hex string to Compose Color
    fun toComposeColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: IllegalArgumentException) {
            Color.Gray // Fallback for invalid hex
        }
    }
}

// NEW: Data class to hold expense thresholds
data class Threshold(
    val daily: Double = 0.0,
    val monthly: Double = 0.0,
    val yearly: Double = 0.0
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(0.0, 0.0, 0.0)
}

// Represents a notification item
data class NotificationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String = "",
    val amount: Double = 0.0,
    val timeAgo: String = "",
    // val isIncome: Boolean = false, // Removed as it's derived from categories
    val categories: List<String> = emptyList() // e.g., "Income", "Loan", "Others"
)

// Define routes for navigation within the app
sealed class Screen(val route: String, val icon: ImageVector? = null, val label: String? = null) {
    // Authentication Screens
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")

    // Main App Screens (with bottom navigation icons/labels)
    object Home : Screen("home", Icons.Default.Home, "Home")
    object Cards : Screen("cards", Icons.Default.CreditCard, "Cards")
    object AddTransaction : Screen("add_transaction", Icons.Default.Add, "Add") // Changed to Add icon
    object Analytics : Screen("analytics", Icons.Default.Analytics, "Analytics")
    object Settings : Screen("settings", Icons.Default.Settings, "Settings")

    // Sub-screens accessible from other main screens
    object Categories : Screen("categories", Icons.Default.Category, "Categories")
    object History : Screen("history", Icons.Default.History, "History")
    object ChangePassword : Screen("change_password", Icons.Default.Lock, "Change Password")
    object AddCategory : Screen("add_category", Icons.Default.Add, "Add Category")
    object EditProfile : Screen("edit_profile", Icons.Default.Person, "Edit Profile") // New Edit Profile screen
    object ExpenseThreshold : Screen("expense_threshold", Icons.Default.MoneyOff, "Expense Threshold") // NEW: Expense Threshold screen
    // AddCard is now a dialog, so it's doesn't need a direct composable route here
}

// Main Activity for the Android application
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            // Apply the custom theme to the entire application
            ExpenditureManagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Entry point for the Compose UI
                    ExpenditureManagerApp()
                }
            }
        }
    }
}

// Composable function for the main application structure and navigation
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class) // Added ExperimentalFoundationApi for combinedClickable
@Composable
fun ExpenditureManagerApp() {
    val navController = rememberNavController() // Controller for navigating between screens
    // List of items for the bottom navigation bar
    val navItems = listOf(
        Screen.Home,
        Screen.Cards,
        Screen.AddTransaction, // This will be the central Add button
        Screen.Analytics,
        Screen.Settings
    )

    // Observe the current route to determine if the bottom bar should be shown
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if the bottom navigation bar should be visible
    // It should be hidden on authentication screens (SignIn, SignUp) and other full-screen dialogs/sub-screens
    val showBottomBar = currentRoute in navItems.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }, // Initialized snackbarHostState here
        bottomBar = {
            if (showBottomBar) { // Only show bottom bar on main app screens
                BottomAppBar(
                    containerColor = Color(0xFF3B89FD), // Changed to blue (3B89FD)
                    modifier = Modifier.height(64.dp) // Adjust height as needed for the bottom bar
                ) {
                    // Iterate through navigation items to create NavigationBarItems
                    navItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route // Check if the current screen is selected

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                // For the AddTransaction screen, navigate directly on click
                                if (screen == Screen.AddTransaction) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else if (currentRoute != screen.route) {
                                    // For other screens, navigate normally
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                // The central button is now a regular Add FAB
                                FloatingActionButton(
                                    onClick = {
                                        // This onClick is for the FAB itself, which will trigger navigation
                                        // when the NavigationBarItem's onClick is called.
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.size(56.dp), // Standard FAB size
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    screen.icon?.let {
                                        Icon(it, contentDescription = screen.label, tint = Color.White)
                                    }
                                }
                            },
                            label = {
                                // Removed labels from navigation bar items
                                // screen.label?.let { Text(it) }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                selectedTextColor = Color.Transparent, // No text label
                                unselectedTextColor = Color.Transparent, // No text label
                                indicatorColor = Color.White.copy(alpha = 0.2f) // Subtle indicator color
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // NavHost manages the navigation between different composable screens
        NavHost(
            navController,
            startDestination = Screen.SignIn.route, // Start the app with the Sign In screen
            Modifier.padding(innerPadding) // Apply padding from the Scaffold (for bottom bar)
        ) {
            // Define all possible navigation routes and their corresponding composables
            composable(Screen.SignIn.route) { SignInScreen(navController) }
            composable(Screen.SignUp.route) { SignUpScreen(navController) }
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Cards.route) { CardsScreen(navController) }
            composable(Screen.AddTransaction.route) { AddTransactionScreen(navController) }
            composable(Screen.Analytics.route) { AnalyticsScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.Categories.route) { CategoriesScreen(navController) }
            composable(Screen.History.route) { HistoryScreen(navController) }
            composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController) }
            composable(Screen.AddCategory.route) { AddCategoryScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) } // New Edit Profile route
            composable(Screen.ExpenseThreshold.route) { ExpenseThresholdScreen(navController) } // NEW: Expense Threshold route
            // AddCard is now a dialog, so it's doesn't need a direct composable route here
        }
    }
}

// --- Authentication Screens ---

// Composable for the Sign In screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance() // Get Firebase Auth instance
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(horizontal = 32.dp), // Horizontal padding for better aesthetics
            horizontalAlignment = Alignment.CenterHorizontally, // Center content horizontally
            verticalArrangement = Arrangement.Center // Center content vertically
        ) {
            // App Logo/Icon - Using a generic savings icon as a placeholder
            Icon(
                Icons.Default.Savings,
                contentDescription = "App Logo",
                modifier = Modifier.size(96.dp), // Large size for the logo
                tint = MaterialTheme.colorScheme.primary // Tint with primary color
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Title for the sign-in page
            Text(
                text = "Sign in your account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Start) // Align title to the start
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Email input field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("ex : jon.smith@example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // Suggest email keyboard
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password input field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(), // Hide password characters
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Suggest password keyboard
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Sign In button
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.SignIn.route) { inclusive = true }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Sign In Failed: ${task.exception?.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error: ${e.message}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Standard button height
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium // Rounded corners
            ) {
                Text("SIGN IN", color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp)) // Adjusted spacing after button

            // Removed "or sign in with" text and social media icons
            // Text("or sign in with", style = MaterialTheme.typography.bodyMedium)
            // Spacer(modifier = Modifier.height(16.dp))
            // Row(
            //     modifier = Modifier.fillMaxWidth(),
            //     horizontalArrangement = Arrangement.SpaceEvenly
            // ) {
            //     SocialSignInButton(icon = Icons.Default.Google) { /* TODO: Handle Google Sign In */ }
            //     SocialSignInButton(icon = Icons.Default.Facebook) { /* TODO: Handle Facebook Sign In */ }
            //     SocialSignInButton(icon = Icons.Default.Twitter) { /* TODO: Handle Twitter Sign In */ }
            // }
            // Spacer(modifier = Modifier.height(32.dp))

            // Link to the Sign Up screen
            Row {
                Text("Don't have an account ? ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "SIGN UP",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { navController.navigate(Screen.SignUp.route) } // Navigate to Sign Up
                )
            }
        }
    }
}

// Composable for the Sign Up screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) } // State for terms and policy checkbox
    val auth = FirebaseAuth.getInstance() // Get Firebase Auth instance
    val db = FirebaseFirestore.getInstance() // Initialize db here
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Back button and App Logo/Icon at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) { // Navigate back
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Icon(
                    Icons.Default.Savings, // Placeholder logo
                    contentDescription = "App Logo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Title for the sign-up page
            Text(
                text = "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Name input field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("ex : jon smith") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email input field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("ex : jon.smith@example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password input field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm password input field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Terms & Policy checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it }
                )
                Text(buildAnnotatedString {
                    append("I understood the ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("terms & policy") // Highlight terms & policy
                    }
                    append(".")
                })
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Sign Up button
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (!termsAccepted) {
                            snackbarHostState.showSnackbar(
                                message = "Please accept the terms & policy.",
                                duration = SnackbarDuration.Short
                            )
                            return@launch
                        }
                        if (password != confirmPassword) {
                            snackbarHostState.showSnackbar(
                                message = "Passwords do not match.",
                                duration = SnackbarDuration.Short
                            )
                            return@launch
                        }
                        if (password.isEmpty()) {
                            snackbarHostState.showSnackbar(
                                message = "Password cannot be empty.",
                                duration = SnackbarDuration.Short
                            )
                            return@launch
                        }

                        try {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // User signed up successfully, now create their Firestore profile document
                                        val newUser = auth.currentUser
                                        if (newUser != null) {
                                            // Update Auth profile display name
                                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                .setDisplayName(name) // Set display name from signup form
                                                .build()
                                            newUser.updateProfile(profileUpdates)
                                                .addOnCompleteListener { profileTask ->
                                                    if (profileTask.isSuccessful) {
                                                        println("Auth profile display name updated.")
                                                    } else {
                                                        println("Error updating Auth profile display name: ${profileTask.exception?.message}")
                                                    }
                                                }

                                            val userProfile = hashMapOf(
                                                "email" to newUser.email,
                                                "fullName" to name, // Use name from signup form
                                                "whatDoWeCallYou" to name, // Use name from signup form
                                                "phoneNumber" to "" // Initialize as empty
                                            )
                                            db.collection("users").document(newUser.uid)
                                                .set(userProfile) // Use set() to create/overwrite document
                                                .addOnSuccessListener {
                                                    println("User profile document created in Firestore for ${newUser.uid}")
                                                    navController.navigate(Screen.Home.route) {
                                                        popUpTo(Screen.SignUp.route) { inclusive = true }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    println("Error creating user profile document: $e")
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Error creating profile: ${e.message}",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Sign Up successful, but user object is null.",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Sign Up Failed: ${task.exception?.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error: ${e.message}",
                                    duration = SnackbarDuration.Long // Use Long for more critical errors
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("SIGN UP", color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp)) // Adjusted spacing after button

            // Removed "or sign up with" text and social media icons
            // Text("or sign in with", style = MaterialTheme.typography.bodyMedium)
            // Spacer(modifier = Modifier.height(16.dp))
            // Row(
            //     modifier = Modifier.fillMaxWidth(),
            //     horizontalArrangement = Arrangement.SpaceEvenly
            // ) {
            //     SocialSignInButton(icon = Icons.Default.Google) { /* TODO: Handle Google Sign In */ }
            //     SocialSignInButton(icon = Icons.Default.Facebook) { /* TODO: Handle Facebook Sign In */ }
            //     SocialSignInButton(icon = Icons.Default.Twitter) { /* TODO: Handle Twitter Sign In */ }
            // }
            // Spacer(modifier = Modifier.height(32.dp))

            // Link to the Sign In screen
            Row {
                Text("Already have an account ? ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "SIGN IN",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { navController.navigate(Screen.SignIn.route) } // Navigate to Sign In
                )
            }
        }
    }
}

// Reusable Composable for social sign-in/sign-up buttons (now unused, but kept for reference if needed elsewhere)
@Composable
fun SocialSignInButton(icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(56.dp), // Fixed size for social buttons
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Use a surface variant color
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) // Icon tinted with onSurfaceVariant
    }
}


// --- Main App Screens ---

@Composable
fun HomeScreen(navController: NavController) {
    var showNotificationPopup by remember { mutableStateOf(false) } // State to control notification dialog visibility
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid // Get current user ID

    // State to hold transactions fetched from Firestore
    val transactions = remember { mutableStateListOf<Transaction>() }
    // State to hold user's display name
    var userDisplayName by remember { mutableStateOf(auth.currentUser?.displayName ?: "User") } // Default to "User"

    // Fetch transactions in a LaunchedEffect
    LaunchedEffect(userId) {
        if (userId != null) {
            // Fetch transactions
            db.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING) // Order by date, newest first
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: Home transactions listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        println("Firestore Debug: Home raw snapshot received for user $userId: ${snapshot.documents.size} documents")
                        transactions.clear()
                        for (doc in snapshot.documents) {
                            val transaction = doc.toObject(Transaction::class.java)
                            if (transaction != null) {
                                transactions.add(transaction)
                            } else {
                                println("Firestore Error: Home failed to parse transaction document: ${doc.id}")
                            }
                        }
                    } else {
                        println("Firestore Debug: Home current data snapshot is null for user $userId")
                    }
                }

            // Fetch user profile data to update display name
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userDisplayName = document.getString("whatDoWeCallYou") ?: "User"
                    }
                }
                .addOnFailureListener { e ->
                    println("Firestore Error: Home failed to fetch user profile for user $userId: ${e.message}")
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Top section: User greeting and notification icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(userDisplayName, style = MaterialTheme.typography.headlineMedium) // Display user's name
                Text("Good Morning!", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { showNotificationPopup = true }) { // Show notification popup on click
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Total Balance section card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Calculate total balance from transactions
                val totalBalance = transactions.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
                Text("Your Total Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${"%.2f".format(totalBalance)} $", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                // Removed "Hide" text and icon
                // Spacer(modifier = Modifier.height(8.dp))
                // Row(verticalAlignment = Alignment.CenterVertically) {
                //     Text("Hide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                //     Icon(Icons.Default.VisibilityOff, contentDescription = "Hide Balance", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                // }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Transactions section title
        Text("Recent Transaction", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        // Display recent transactions using LazyColumn
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) { // Use weight to allow scrolling
            // Only show "No recent transactions..." if the list is actually empty
            if (transactions.isEmpty()) {
                item { // Wrap the Text in an item block for LazyColumn
                    Text("No recent transactions. Add some!", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center) // Used TextAlign.Center
                }
            }
            items(transactions) { transaction ->
                TransactionItem(
                    description = transaction.description,
                    amount = transaction.amount,
                    date = java.text.SimpleDateFormat("dd MMMYYYY 'at' hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(transaction.date)),
                    isExpense = transaction.type == TransactionType.EXPENSE
                )
            }
        }
    }

    // Show Notification Popup if state is true
    if (showNotificationPopup) {
        NotificationPopup(onDismissRequest = { showNotificationPopup = false })
    }
}

// Composable for displaying a single transaction item
@Composable
fun TransactionItem(description: String, amount: Double, date: String, isExpense: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon based on income/expense
            val icon = if (isExpense) Icons.Default.ArrowOutward else Icons.Default.CallReceived // Changed ArrowInward to CallReceived
            val iconTint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(description, style = MaterialTheme.typography.bodyLarge)
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Text(
            text = "${if (isExpense) "-" else ""}${amount} $", // Format amount with currency symbol
            style = MaterialTheme.typography.bodyLarge,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary // Color based on transaction type
        )
    }
}

// Composable for the Cards screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(navController: NavController) {
    var showAddCardPopup by remember { mutableStateOf(false) } // State to control Add Card dialog visibility
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    val cards = remember { mutableStateListOf<Card>() }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).collection("cards")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: Cards listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        println("Firestore Debug: Raw cards snapshot received for user $userId: ${snapshot.documents.size} documents")
                        cards.clear()
                        for (doc in snapshot.documents) {
                            val card = doc.toObject(Card::class.java)
                            if (card != null) {
                                cards.add(card)
                            } else {
                                println("Firestore Error: Failed to parse card document: ${doc.id}")
                            }
                        }
                    } else {
                        println("Firestore Debug: Current cards data snapshot is null for user $userId")
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cards") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddCardPopup = true }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("+ Add")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            )
        }
    ) { paddingValues -> // Use paddingValues from Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make the entire content scrollable
                .padding(horizontal = 16.dp) // Apply horizontal padding here
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Initial spacer after top app bar

            // Display cards list
            if (cards.isEmpty()) {
                Text("No cards added yet. Add one!", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
            } else {
                cards.forEach { card ->
                    val cardColors = listOf(
                        Color(0xFF6200EE), // Deep Purple
                        Color(0xFF03DAC5), // Teal
                        Color(0xFFBB86FC), // Light Purple
                        Color(0xFF00C853), // Green
                        Color(0xFFFFC107)  // Amber
                    )
                    val cardColor = cardColors[(card.id.hashCode() and 0x7FFFFFFF) % cardColors.size] // Simple hash for color

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp)
                            .animateContentSize( // Smooth size animation
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = LinearOutSlowInEasing
                                )
                            ),
                        colors = CardDefaults.cardColors(containerColor = cardColor), // Dynamic card color
                        shape = MaterialTheme.shapes.large // Large rounded corners
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(card.bankName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(card.cardNumber, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Card Holder Name", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    Text(card.cardHolderName, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                }
                                Column {
                                    Text("Expired Date", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    Text(card.expiryDate, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Budget Card (content removed)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .animateContentSize( // Smooth size animation
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                // Content removed , only title remains
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Budget", style = MaterialTheme.typography.titleMedium)
                }
            }
            // Previous Month Budget Card (content removed)
            Card(
                modifier = Modifier.fillMaxWidth()
                    .animateContentSize( // Smooth size animation
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                // Content removed , only title remains
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Previous Month", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Show Add Card Popup if state is true
    if (showAddCardPopup) {
        AddCardPopup(onDismissRequest = { showAddCardPopup = false })
    }
}

// Composable for the Add Transaction screen
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Added ExperimentalLayoutApi for FlowRow
@Composable
fun AddTransactionScreen(navController: NavController) {
    var amountText by remember { mutableStateOf("0") }
    var selectedTransactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCard by remember { mutableStateOf("") } // Initialize selectedCard here
    var selectedCategory by remember { mutableStateOf<Category?>(null) } // Hold selected Category object
    var description by remember { mutableStateOf("") } // For transaction description
    var showVoiceInputPopup by remember { mutableStateOf(false) } // State for voice input popup
    var tempHeardText by remember { mutableStateOf<String?>(null) } // To temporarily save heard text

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    val categories = remember { mutableStateListOf<Category>() }
    val cards = remember { mutableStateListOf<Card>() } // Fetch cards for selection
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Function to parse voice input and update transaction fields
    val parseVoiceInput: (String) -> Unit = { text ->
        var remainingText = text.lowercase()
        var parsedAmount: Double? = null
        var parsedType: TransactionType? = null
        var parsedCard: String? = null
        var parsedCategory: Category? = null

        // 1. Parse Transaction Type (keywords first)
        if (remainingText.contains("receive") || remainingText.contains("income") || remainingText.contains("salary") || remainingText.contains("deposit")) {
            parsedType = TransactionType.INCOME
            remainingText = remainingText
                .replace("income", "")
                .replace("salary", "")
                .replace("deposit", "")
                .trim()
        } else if (remainingText.contains("expense") || remainingText.contains("spent") || remainingText.contains("paid") || remainingText.contains("bought")) {
            parsedType = TransactionType.EXPENSE
            remainingText = remainingText
                .replace("expense", "")
                .replace("spent", "")
                .replace("paid", "")
                .replace("bought", "")
                .trim()
        }

        // 2. Parse Amount (look for numbers, handle "dollars" etc.)
        val amountRegex = "\\d+(\\.\\d+)?".toRegex()
        val amountMatch = amountRegex.find(remainingText)
        if (amountMatch != null) {
            parsedAmount = amountMatch.value.toDoubleOrNull()
            remainingText = remainingText.replace(amountMatch.value, "").trim()
        }
        // Remove common currency words if they don't precede a number directly
        remainingText = remainingText.replace("dollars", "").replace("usd", "").replace("euro", "").trim()


        // 3. Parse Card/Wallet
        // Iterate through existing cards to find a match
        var cardFoundInText = false
        cards.forEach { card ->
            val bankNameLower = card.bankName.lowercase()
            if (remainingText.contains(bankNameLower)) {
                parsedCard = card.bankName
                remainingText = remainingText.replace(bankNameLower, "").trim()
                cardFoundInText = true
                return@forEach // Found a card, exit loop
            }
        }
        // Handle "cash" as a potential wallet type if not matched with a bank card
        if (!cardFoundInText && remainingText.contains("cash")) {
            parsedCard = "Cash"
            remainingText = remainingText.replace("cash", "").trim()
        }


        // 4. Parse Category
        // Iterate through existing categories to find a match (prioritize exact matches or longer ones if desired)
        var categoryFoundInText = false
        categories.forEach { category ->
            val categoryNameLower = category.name.lowercase()
            if (remainingText.contains(categoryNameLower)) {
                parsedCategory = category
                remainingText = remainingText.replace(categoryNameLower, "").trim()
                categoryFoundInText = true
                return@forEach // Found a category, exit loop
            }
        }

        // Update state variables only if a value was actually parsed, otherwise keep existing UI state
        if (parsedAmount != null) amountText = parsedAmount.toString()
        if (parsedType != null) selectedTransactionType = parsedType
        // Only update selectedCard if a specific card/cash was explicitly recognized
        if (parsedCard != null) selectedCard = parsedCard!!
        // Only update selectedCategory if a specific category was explicitly recognized
        if (parsedCategory != null) selectedCategory = parsedCategory

        description = remainingText.trim().replace("\\s+".toRegex(), " ") // Clean up description
    }

    // Fetch categories and cards from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).collection("categories")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: Categories listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        println("Firestore Debug: Raw categories snapshot received for user $userId: ${snapshot.documents.size} documents")
                        categories.clear()
                        for (doc in snapshot.documents) {
                            val category = doc.toObject(Category::class.java)
                            if (category != null) {
                                categories.add(category)
                            } else {
                                println("Firestore Error: Failed to parse category document: ${doc.id}")
                            }
                        }
                        // Select the first category by default if available
                        if (selectedCategory == null && categories.isNotEmpty()) {
                            selectedCategory = categories.first()
                        }
                    } else {
                        println("Firestore Debug: Current categories data snapshot is null for user $userId")
                    }
                }
            db.collection("users").document(userId).collection("cards")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: Cards listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        println("Firestore Debug: Raw cards snapshot received for user $userId: ${snapshot.documents.size} documents")
                        cards.clear()
                        for (doc in snapshot.documents) {
                            val card = doc.toObject(Card::class.java)
                            if (card != null) {
                                cards.add(card)
                            } else {
                                println("Firestore Error: Failed to parse card document: ${doc.id}")
                            }
                        }
                        // Select the first card by default if available
                        if (cards.isNotEmpty()) {
                            // Ensure selectedCard is updated if the list changes
                            // For simplicity, just pick the first one if current is invalid
                            if (selectedCard.isEmpty() || !cards.any { it.bankName == selectedCard }) {
                                selectedCard = cards.first().bankName
                            }
                        } else {
                            selectedCard = "Cash" // Default to "Cash" if no cards exist
                        }
                    } else {
                        println("Firestore Debug: Current cards data snapshot is null for user $userId")
                    }
                }
        }
    }

    // Update description field and parse if tempHeardText changes
    LaunchedEffect(tempHeardText) {
        tempHeardText?.let {
            parseVoiceInput(it) // Call parsing logic
            tempHeardText = null // Clear after use
        }
    }


    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(horizontal = 16.dp) // Apply horizontal padding here
                .animateContentSize( // Smooth size animation for the whole column
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
        ) {
            // Top app bar
            TopAppBar(
                title = { Text("Add Transaction") }, // Added title
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } }) { // Navigate to Home
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    IconButton(onClick = { showVoiceInputPopup = true }) { // Microphone icon to open voice input popup
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color.White) // Ensure icon is white
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Expense/Income Toggle buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { selectedTransactionType = TransactionType.EXPENSE },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTransactionType == TransactionType.EXPENSE) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedTransactionType == TransactionType.EXPENSE) Color.White else Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraLarge // Highly rounded corners
                ) {
                    Text("Expense")
                }
                Button(
                    onClick = { selectedTransactionType = TransactionType.INCOME },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTransactionType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedTransactionType == TransactionType.INCOME) Color.White else Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("Income")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Amount Input field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Numeric keyboard
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Text("USD") } // Currency indicator
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Description input field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g., Coffee, Salary") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Cards/Wallet selection section (dynamic based on fetched cards)
            Text("Cards/Wallet", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards available. Add one from the Cards page.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    cards.forEach { card ->
                        FilterChip(
                            selected = selectedCard == card.bankName, // Select by bank name for simplicity
                            onClick = { selectedCard = card.bankName },
                            label = { Text(card.bankName) },
                            leadingIcon = {
                                if (selectedCard == card.bankName) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Categories selection section
            Text("Categories", style = MaterialTheme.typography.titleMedium)
            FlowRow( // Using FlowRow for wrapping categories
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp), // FIX: Replaced mainAxisSpacing
                verticalArrangement = Arrangement.spacedBy(8.dp) // FIX: Replaced crossAxisSpacing
            ) {
                categories.forEach { category ->
                    Button(
                        onClick = { selectedCategory = category },
                        modifier = Modifier.wrapContentWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory?.id == category.id) category.toComposeColor() else category.toComposeColor().copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(category.name)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f)) // Pushes the DONE button to the bottom

            // DONE button to finalize the transaction
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (userId == null || amount == null || selectedCategory == null || description.isBlank() || selectedCard.isBlank()) { // Added selectedCard check
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Please fill all fields correctly and select a card.",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@Button
                    }

                    val newTransaction = Transaction(
                        description = description,
                        amount = amount,
                        currency = "USD", // Hardcoded for now
                        type = selectedTransactionType,
                        cardOrWallet = selectedCard, // Now uses selected card
                        category = selectedCategory!!,
                        date = System.currentTimeMillis()
                    )

                    coroutineScope.launch {
                        try {
                            db.collection("users").document(userId).collection("transactions")
                                .add(newTransaction)
                                .addOnSuccessListener { documentReference ->
                                    // FIXED: Updated message for offline sync clarity
                                    val message = if (FirebaseFirestore.getInstance().firestoreSettings.isPersistenceEnabled) {
                                        "Transaction saved (syncing when online)!"
                                    } else {
                                        "Transaction added successfully!"
                                    }
                                    println("Firestore Debug: Transaction added successfully to path: users/$userId/transactions/${documentReference.id}")
                                    println("Firestore Debug: Added Transaction details: $newTransaction")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    println("Firestore Error: Failed to add transaction for user $userId: ${e.message}")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Error adding transaction: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error: ${e.message}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("DONE", color = Color.White)
            }
        }
    }
    // Show Voice Input Popup if state is true
    if (showVoiceInputPopup) {
        VoiceInputPopup(
            onDismissRequest = { showVoiceInputPopup = false },
            onTextRecognized = { text: String -> // Explicitly specify type for clarity
                tempHeardText = text
                showVoiceInputPopup = false
            }
        )
    }
}

// Composable for the Categories screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    val categories = remember { mutableStateListOf<Category>() }

    // Fetch categories from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).collection("categories")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: Categories listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        println("Firestore Debug: Raw categories snapshot received for user $userId: ${snapshot.documents.size} documents")
                        categories.clear()
                        for (doc in snapshot.documents) {
                            val category = doc.toObject(Category::class.java)
                            if (category != null) {
                                categories.add(category)
                            } else {
                                println("Firestore Error: Failed to parse category document: ${doc.id}")
                            }
                        }
                    } else {
                        println("Firestore Debug: Current categories data snapshot is null for user $userId")
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Make content scrollable
    ) {
        // Top app bar for Categories screen
        TopAppBar(
            title = { Text("Categories") },
            navigationIcon = {
                IconButton(onClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Settings.route) { inclusive = false } } }) { // Navigate to Settings
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Settings")
                }
            },
            actions = {
                TextButton(onClick = { navController.navigate(Screen.AddCategory.route) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { // Text and icon white
                    Text("+ Add") // Button to add a new category
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar for categories
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // List of categories (filtered by search text)
        Column(modifier = Modifier.fillMaxWidth()) { // Changed from LazyColumn to Column
            if (categories.isEmpty()) {
                Text("No categories found. Add one!", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
            } else {
                categories.filter { it.name.contains(searchText, ignoreCase = true) }.forEach { category ->
                    Button(
                        onClick = { /* TODO: Handle category edit/selection */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = category.toComposeColor()),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(category.name, color = Color.White)
                    }
                }
            }
        }
    }
}

// Composable for the Add/Change Category screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(navController: NavController) {
    var categoryName by remember { mutableStateOf("") }
    var categoryColorHex by remember { mutableStateOf("#49A078") } // Default color hex string
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(horizontal = 16.dp) // Apply horizontal padding here
                .animateContentSize( // Smooth size animation for the whole column
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
        ) {
            // Top app bar with a close icon
            TopAppBar(
                title = { Text("Category") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Categories.route) { popUpTo(Screen.Categories.route) { inclusive = false } } }) { // Navigate to Categories
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) // 'X' icon for closing
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Category name input
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category") },
                placeholder = { Text("ex: Food") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Category color (hex code) input
            OutlinedTextField(
                value = categoryColorHex,
                onValueChange = { categoryColorHex = it },
                label = { Text("Color") },
                placeholder = { Text("#49A078") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f)) // Pushes DONE button to bottom

            // DONE button to save the category
            Button(
                onClick = {
                    if (userId == null || categoryName.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Category name cannot be empty.",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@Button
                    }

                    val newCategory = Category(
                        name = categoryName,
                        colorHex = categoryColorHex
                    )

                    coroutineScope.launch {
                        try {
                            db.collection("users").document(userId).collection("categories")
                                .add(newCategory)
                                .addOnSuccessListener { documentReference ->
                                    println("Firestore Debug: Category added successfully to path: users/$userId/categories/${documentReference.id}")
                                    println("Firestore Debug: Added Category details: $newCategory")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Category added successfully!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    println("Firestore Error: Failed to add category for user $userId: ${e.message}")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Error adding category: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error: ${e.message}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("DONE", color = Color.White)
            }
        }
    }
}

// Composable for the Settings screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State to hold user's display name for Settings page
    var userDisplayName by remember { mutableStateOf(auth.currentUser?.displayName ?: "User") }

    // Fetch user profile data to update display name
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userDisplayName = document.getString("whatDoWeCallYou") ?: "User"
                    }
                }
                .addOnFailureListener { e ->
                    println("Firestore Error: Failed to fetch user profile for user $userId: ${e.message}")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Error fetching user display name: ${e.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(horizontal = 16.dp) // Apply horizontal padding here
        ) {
            // Top app bar for Settings
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } }) { // Navigate to Home
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // User Profile section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = "User", modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(userDisplayName, style = MaterialTheme.typography.headlineSmall) // Display user's name
            }
            Divider() // Separator
            Spacer(modifier = Modifier.height(16.dp))

            // Account Settings section
            Text("Account Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            // Updated Edit profile to navigate to EditProfileScreen
            SettingsItem(text = "Edit profile", navController = navController, destination = Screen.EditProfile.route)
            SettingsItem(text = "Change password", navController = navController, destination = Screen.ChangePassword.route)
            SettingsItem(text = "Expense Threshold", navController = navController, destination = Screen.ExpenseThreshold.route)
            SettingsItem(text = "History", navController = navController, destination = Screen.History.route)
            SettingsItem(text = "Categories", navController = navController, destination = Screen.Categories.route)
            SettingsItem(
                text = "Log out",
                navController = navController,
                destination = "", // No direct navigation, handle logout action
                onClickAction = {
                    coroutineScope.launch {
                        try {
                            auth.signOut()
                            navController.navigate(Screen.SignIn.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Logout failed: ${e.message}",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider() // Separator
            Spacer(modifier = Modifier.height(24.dp))

            // More section
            Text("More", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            SettingsItem(text = "About us", navController = navController, destination = "")
            SettingsItem(text = "Privacy policy", navController = navController, destination = "")
            SettingsItem(text = "Terms and conditions", navController = navController, destination = "")
        }
    }
}

// Reusable Composable for a single settings item row
@Composable
fun SettingsItem(text: String, navController: NavController, destination: String, onClickAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onClickAction != null) {
                    onClickAction.invoke()
                } else if (destination.isNotEmpty()) {
                    navController.navigate(destination) // Navigate if a destination is provided
                }
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (destination.isNotEmpty() || onClickAction != null) { // Show arrow if it's clickable
            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate") // Right arrow icon
        }
    }
}

// Composable for the Change Password screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(horizontal = 16.dp) // Apply horizontal padding here
        ) {
            // Top app bar for Change Password
            TopAppBar(
                title = { Text("Change your password") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Settings.route) { inclusive = false } } }) { // Navigate to Settings
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Old password input
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Old password") },
                placeholder = { Text("ex: jon smith") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // New password input
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm new password input
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

            // CHANGE PASSWORD button
            Button(
                onClick = {
                    coroutineScope.launch { // Launch coroutine for password change logic
                        if (newPassword != confirmPassword) {
                            snackbarHostState.showSnackbar(
                                message = "New passwords do not match.",
                                duration = SnackbarDuration.Short
                            )
                            return@launch // Exit coroutine
                        }
                        if (newPassword.isEmpty()) {
                            snackbarHostState.showSnackbar(
                                message = "New password cannot be empty.",
                                duration = SnackbarDuration.Short
                            )
                            return@launch // Exit coroutine
                        }

                        val user = auth.currentUser
                        if (user != null && user.email != null) {
                            try {
                                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                                user.reauthenticate(credential)
                                    .addOnCompleteListener { reauthTask ->
                                        if (reauthTask.isSuccessful) {
                                            user.updatePassword(newPassword)
                                                .addOnCompleteListener { updateTask ->
                                                    if (updateTask.isSuccessful) {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                message = "Password updated successfully!",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                        }
                                                        navController.popBackStack()
                                                    } else {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                message = "Password update failed: ${updateTask.exception?.message}",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                        }
                                                    }
                                                }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Authentication failed: ${reauthTask.exception?.message}",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Error: ${e.message}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "User not logged in or email not available.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }}
                    }, // Correctly closing onClick lambda
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                    ) {
                    Text("CHANGE PASSWORD", color = Color.White)
                }
                }
        }
    }

// Fixed Composable for the Analytics screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    // State to hold transactions fetched from Firestore.
    var transactions by remember { mutableStateOf(emptyList<Transaction>()) }

    // State to hold the currently selected time period for the chart
    var selectedPeriod by remember { mutableStateOf("Daily") }

    // FIXED: Improved filtering logic for better date handling
    val filteredTransactions = remember(transactions, selectedPeriod) {
        val calendar = Calendar.getInstance()
        val currentCalendar = Calendar.getInstance()

        transactions.filter { transaction ->
            calendar.timeInMillis = transaction.date

            when (selectedPeriod) {
                "Daily" -> {
                    // Show last 7 days instead of just today
                    val daysDiff = ((currentCalendar.timeInMillis - transaction.date) / (1000 * 60 * 60 * 24)).toInt()
                    daysDiff >= 0 && daysDiff < 7
                }
                "Weekly" -> {
                    // Show last 4 weeks
                    val weeksDiff = ((currentCalendar.timeInMillis - transaction.date) / (1000 * 60 * 60 * 24 * 7)).toInt()
                    weeksDiff >= 0 && weeksDiff < 4
                }
                "Monthly" -> {
                    // Show current year's data
                    val transactionYear = calendar.get(Calendar.YEAR)
                    val currentYear = currentCalendar.get(Calendar.YEAR)
                    transactionYear == currentYear
                }
                "Yearly" -> {
                    // Show last 5 years
                    val transactionYear = calendar.get(Calendar.YEAR)
                    val currentYear = currentCalendar.get(Calendar.YEAR)
                    transactionYear >= (currentYear - 4) && transactionYear <= currentYear
                }
                else -> true
            }
        }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    val incomeCategories = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }
            .groupBy { it.category.name }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }

    val expenseCategories = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category.name }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }

    // FIXED: Improved chart HTML generation with better date handling
    val generateChartHtml: (List<Transaction>, String) -> String = { txns, period ->
        val calendar = Calendar.getInstance()
        val currentCalendar = Calendar.getInstance()
        val dataMap = mutableMapOf<String, Pair<Double, Double>>()

        // Use all transactions for chart data
        val chartTransactions = transactions

        val xLabels = when (period) {
            "Daily" -> {
                // Get last 7 days
                val labels = mutableListOf<String>()
                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    labels.add(SimpleDateFormat("MMM dd", Locale.getDefault()).format(cal.time))
                }
                labels
            }
            "Weekly" -> {
                // Get last 4 weeks
                val labels = mutableListOf<String>()
                for (i in 3 downTo 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.WEEK_OF_YEAR, -i)
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    labels.add("Week ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(cal.time)}")
                }
                labels
            }
            "Monthly" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            "Yearly" -> {
                val currentYear = currentCalendar.get(Calendar.YEAR)
                (currentYear - 4..currentYear).map { it.toString() }
            }
            else -> emptyList()
        }

        // Initialize dataMap with all labels and zero values
        xLabels.forEach { label ->
            dataMap[label] = Pair(0.0, 0.0)
        }

        // FIXED: Better transaction processing for each period
        chartTransactions.forEach { transaction ->
            calendar.timeInMillis = transaction.date

            val label = when (period) {
                "Daily" -> {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transaction.date))
                }
                "Weekly" -> {
                    // Find which week this transaction belongs to
                    var weekLabel: String? = null
                    for (i in 3 downTo 0) {
                        val weekCal = Calendar.getInstance()
                        weekCal.add(Calendar.WEEK_OF_YEAR, -i)
                        weekCal.set(Calendar.DAY_OF_WEEK, weekCal.firstDayOfWeek)

                        val weekStart = weekCal.timeInMillis
                        val weekEnd = weekStart + (7 * 24 * 60 * 60 * 1000L)

                        if (transaction.date >= weekStart && transaction.date < weekEnd) {
                            weekLabel = "Week ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekCal.time)}"
                            break
                        }
                    }
                    weekLabel
                }
                "Monthly" -> {
                    val currentYear = currentCalendar.get(Calendar.YEAR)
                    val transactionYear = calendar.get(Calendar.YEAR)
                    if (transactionYear == currentYear) {
                        SimpleDateFormat("MMM", Locale.getDefault()).format(Date(transaction.date))
                    } else null
                }
                "Yearly" -> {
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(transaction.date))
                }
                else -> null
            }

            // Only add data if the label exists in our xLabels
            if (label != null && dataMap.containsKey(label)) {
                dataMap[label]?.let { currentSums ->
                    if (transaction.type == TransactionType.INCOME) {
                        dataMap[label] = Pair(currentSums.first + transaction.amount, currentSums.second)
                    } else {
                        dataMap[label] = Pair(currentSums.first, currentSums.second + transaction.amount)
                    }
                }
            }
        }

        // Convert map to a list of lists for Google Charts DataTable
        val chartData = mutableListOf<List<Any>>()
        chartData.add(listOf("Period", "Income", "Expense"))

        xLabels.forEach { label ->
            val sums = dataMap[label] ?: Pair(0.0, 0.0)
            chartData.add(listOf(label, sums.first, sums.second))
        }

        val gson = Gson()
        val jsonData = gson.toJson(chartData)

        // FIXED: Improved HTML with responsive design
        """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
                console.log('Chart HTML loaded');
                
                google.charts.load('current', {'packages':['corechart']});
                google.charts.setOnLoadCallback(drawChart);

                function drawChart() {
                    console.log('Drawing chart with data:', $jsonData);
                    
                    try {
                        var data = google.visualization.arrayToDataTable($jsonData);

                        var options = {
                            title: 'Income vs Expense - $period',
                            titleTextStyle: { 
                                color: '#333', 
                                fontSize: 16,
                                bold: true
                            },
                            legend: { 
                                position: 'top', 
                                alignment: 'center',
                                textStyle: { fontSize: 12 }
                            },
                            chartArea: { 
                                width: '90%', 
                                height: '80%',
                                left: 50,
                                top: 60,
                                right: 20,
                                bottom: 40
                            },
                            backgroundColor: 'transparent',
                            isStacked: false,
                            series: {
                                0: { color: '#4CAF50' }, // Income (Green)
                                1: { color: '#F44336' }  // Expense (Red)
                            },
                            vAxis: {
                                title: 'Amount ($)',
                                titleTextStyle: { fontSize: 12, bold: true },
                                minValue: 0,
                                format: 'short',
                                gridlines: { 
                                    count: 4,
                                    color: '#e0e0e0'
                                },
                                textStyle: { fontSize: 10 }
                            },
                            hAxis: {
                                title: 'Period',
                                titleTextStyle: { fontSize: 12, bold: true },
                                textStyle: { fontSize: 10 },
                                slantedText: ${if (period == "Daily" || period == "Weekly") "true" else "false"},
                                slantedTextAngle: 45,
                                maxTextLines: 1
                            },
                            bar: { groupWidth: '70%' },
                            animation: {
                                startup: true,
                                duration: 800,
                                easing: 'out'
                            }
                        };

                        var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
                        
                        // Add error handling
                        google.visualization.events.addListener(chart, 'error', function(e) {
                            console.error('Chart error:', e);
                            document.getElementById('chart_div').innerHTML = '<div style="padding: 20px; text-align: center; color: #666; font-size: 14px;">Error loading chart</div>';
                        });
                        
                        chart.draw(data, options);
                        console.log('Chart drawn successfully');
                        
                    } catch (error) {
                        console.error('Error creating chart:', error);
                        document.getElementById('chart_div').innerHTML = '<div style="padding: 20px; text-align: center; color: #666; font-size: 14px;">Error: ' + error.message + '</div>';
                    }
                }
                
                // Add window error handler
                window.onerror = function(msg, url, line, col, error) {
                    console.error('JavaScript error:', msg, 'at', url, ':', line);
                    return false;
                };
            </script>
            <style>
                * { 
                    margin: 0; 
                    padding: 0; 
                    box-sizing: border-box;
                }
                html, body { 
                    width: 100%;
                    height: 100%;
                    overflow: hidden; 
                    font-family: 'Roboto', sans-serif;
                    background: transparent;
                    display: flex; // update line
                    flex-direction: column; // update line
                }
                #chart_div { 
                    width: 100%; 
                    height: 100%; 
                    min-width: 100%;
                    min-height: 100%;
                    position: absolute;
                    top: 0;
                    left: 0;
                }
            </style>
        </head>
        <body>
            <div id="chart_div">
                <div style="padding: 20px; text-align: center; color: #666; font-size: 14px;">
                    Loading chart...
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    // FIXED: Better state management for WebView
    var webView by remember { mutableStateOf<WebView?>(null) }
    val chartHtml = remember(transactions, selectedPeriod) {
        generateChartHtml(filteredTransactions, selectedPeriod)
    }

    // Use DisposableEffect for Firestore listener setup and cleanup
    DisposableEffect(userId) {
        println("AnalyticsScreen Debug: DisposableEffect for userId triggered for user ID: $userId")
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        if (userId != null) {
            listenerRegistration = db.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: Analytics listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        println("Firestore Debug: Analytics raw snapshot received for user $userId: ${snapshot.documents.size} documents")
                        val fetchedTransactions = mutableListOf<Transaction>()
                        for (doc in snapshot.documents) {
                            val transaction = doc.toObject(Transaction::class.java)
                            if (transaction != null) {
                                fetchedTransactions.add(transaction)
                            } else {
                                println("Firestore Error: Analytics failed to parse transaction document: ${doc.id}")
                            }
                        }
                        transactions = fetchedTransactions
                        println("Firestore Debug: Analytics transactions list updated with ${transactions.size} items.")
                    } else {
                        println("Firestore Debug: Analytics current data snapshot is null for user $userId")
                    }
                }
        }

        onDispose {
            listenerRegistration?.remove()
            println("AnalyticsScreen Debug: Firestore listener removed for user $userId")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top app bar for Analytics
        TopAppBar(
            title = { Text("Analytics") },
            navigationIcon = {
                IconButton(onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: Handle download/export */ }) {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }
                IconButton(onClick = { /* TODO: Handle filter */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF3B89FD),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // DEBUGGING TEXT
//        Text("DEBUG: User ID: $userId", style = MaterialTheme.typography.bodySmall, color = Color.Red)
//        Text("DEBUG: Total transactions fetched: ${transactions.size}", style = MaterialTheme.typography.bodySmall, color = Color.Red)
//        Text("DEBUG: Selected Period: $selectedPeriod", style = MaterialTheme.typography.bodySmall, color = Color.Red)
//        Text("DEBUG: Filtered transactions for period: ${filteredTransactions.size}", style = MaterialTheme.typography.bodySmall, color = Color.Red)
//        Text("DEBUG: Total Income (filtered): $${"%.2f".format(totalIncome)}", style = MaterialTheme.typography.bodySmall, color = Color.Red)
//        Text("DEBUG: Total Expense (filtered): $${"%.2f".format(totalExpense)}", style = MaterialTheme.typography.bodySmall, color = Color.Red)
//        Spacer(modifier = Modifier.height(16.dp))

        // Time period selection buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { period ->
                Button(
                    onClick = { selectedPeriod = period },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPeriod == period) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedPeriod == period) Color.White else Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(period)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("Income & Expenses", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // FIXED: Larger chart with better responsive design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                println("WebView: Page finished loading")
                            }

                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                println("WebView Error: $description at $failingUrl")
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    println("WebView Console [${it.messageLevel()}]: ${it.message()} at ${it.sourceId()}:${it.lineNumber()}")
                                }
                                return true
                            }
                        }

                        webView = this
                        loadDataWithBaseURL(null, chartHtml, "text/html", "utf-8", null)
                    }
                },
                update = { view ->
                    view.loadDataWithBaseURL(null, chartHtml, "text/html", "utf-8", null)
                    println("WebView: Reloading with new chart data for period: $selectedPeriod")
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Income and Expense Summaries and Categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top
        ) {
            // Income Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CallReceived, contentDescription = "Income", tint = MaterialTheme.colorScheme.primary)
                Text("Income", style = MaterialTheme.typography.titleMedium)
                Text("$${"%.2f".format(totalIncome)}", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Income Categories", style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (incomeCategories.isEmpty()) {
                        Text("No income categories yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        incomeCategories.forEach { (categoryName, sum) ->
                            CategoryChip(name = categoryName, amount = sum, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            // Expense Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ArrowOutward, contentDescription = "Expense", tint = MaterialTheme.colorScheme.error)
                Text("Expense", style = MaterialTheme.typography.titleMedium)
                Text("$${"%.2f".format(totalExpense)}", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Expense Categories", style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (expenseCategories.isEmpty()) {
                        Text("No expense categories yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        expenseCategories.forEach { (categoryName, sum) ->
                            CategoryChip(name = categoryName, amount = sum, color = Color(0xFF9C27B0))
                        }
                    }
                }
            }
        }
    }
}



    @Composable
    fun CategoryChip(name: String, amount: Double, color: Color) {
        Card(
            colors = CardDefaults.cardColors(containerColor = color),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("$name $${amount.toInt()}", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }

    // Composable for the History screen
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HistoryScreen(navController: NavController) {
        var searchText by remember { mutableStateOf("") }
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid

        val transactions = remember { mutableStateListOf<Transaction>() }

        // Fetch transactions for history
        LaunchedEffect(userId) {
            if (userId != null) {
                db.collection("users").document(userId).collection("transactions")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            println("Firestore Error: History listen failed for user $userId: ${e.message}")
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            println("Firestore Debug: Raw history snapshot received for user $userId: ${snapshot.documents.size} documents")
                            transactions.clear()
                            for (doc in snapshot.documents) {
                                val transaction = doc.toObject(Transaction::class.java)
                                if (transaction != null) {
                                    transactions.add(transaction)
                                } else {
                                    println("Firestore Error: Failed to parse history transaction document: ${doc.id}")
                                }
                            }
                        } else {
                            println("Firestore Debug: Current history data snapshot is null for user $userId")
                        }
                    }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Make content scrollable
        ) {
            // Top app bar for History
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Settings.route) { inclusive = false } } }) { // Navigate to Settings
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO: Handle filter */ }) { // Changed from IconButton to TextButton for consistency
                        Text("Filter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Search bar for history
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // List of transactions in history (filtered by search text)
            Column(modifier = Modifier.fillMaxWidth()) { // Changed from LazyColumn to Column
                if (transactions.isEmpty()) {
                    Text("No transaction history found.", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                } else {
                    transactions.filter { it.description.contains(searchText, ignoreCase = true) || it.category.name.contains(searchText, ignoreCase = true) }.forEach { transaction ->
                        HistoryTransactionItem(
                            icon = when (transaction.category.name) { // Choose icon based on category name
                                "Food" -> Icons.Default.Fastfood
                                "Shopping" -> Icons.Default.ShoppingCart
                                "Investment" -> Icons.Default.TrendingUp
                                "Music" -> Icons.Default.MusicNote
                                "Entertainment" -> Icons.Default.Movie
                                "Transaction" -> Icons.Default.SwapHoriz
                                else -> Icons.Default.Info // Default icon
                            },
                            title = transaction.description,
                            category = transaction.category.name,
                            amount = if (transaction.type == TransactionType.EXPENSE) -transaction.amount else transaction.amount
                        )
                    }
                }
            }
        }
    }

    // Composable for a single history transaction item
    @Composable
    fun HistoryTransactionItem(icon: ImageVector, title: String, category: String, amount: Double) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Text(
                text = "${if (amount < 0) "-" else ""}${amount} $", // Format amount
                style = MaterialTheme.typography.bodyLarge,
                color = if (amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary // Color based on positive/negative amount
            )
        }
    }

    // Composable for the Add Card Popup (now a Dialog)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddCardPopup(onDismissRequest: () -> Unit) {
        var cardNumber by remember { mutableStateOf("") }
        var expiryDate by remember { mutableStateOf("") }
        var cvv by remember { mutableStateOf("") }
        var cardHolderName by remember { mutableStateOf("") }
        var saveMyCard by remember { mutableStateOf(false) }
        var bankName by remember { mutableStateOf("") } // New field for bank name

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize( // Smooth size animation
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    ),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = "Add Card Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add new card", style = MaterialTheme.typography.headlineSmall)
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Streamline your checkout process by adding a new card for future transactions. Your card information is secured with advanced encryption technology.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Card Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("Expiry Date") },
                            placeholder = { Text("MM/YY") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it },
                            label = { Text("CVV") },
                            placeholder = { Text("...") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = cardHolderName,
                        onValueChange = { cardHolderName = it },
                        label = { Text("Cardholder's Name") },
                        placeholder = { Text("Enter cardholder's full name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Save my card", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = saveMyCard, onCheckedChange = { saveMyCard = it })
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { /* TODO: Implement Scan Card functionality */ onDismissRequest() },
                            modifier = Modifier.weight(1f).height(56.dp).padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.medium,
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Text("Scan Card")
                        }
                        Button(
                            onClick = {
                                if (userId == null || bankName.isBlank() || cardNumber.isBlank() || cardHolderName.isBlank() || expiryDate.isBlank() || cvv.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Please fill all card details.",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    return@Button
                                }

                                val newCard = Card(
                                    bankName = bankName,
                                    cardNumber = cardNumber,
                                    cardHolderName = cardHolderName,
                                    expiryDate = expiryDate
                                )

                                coroutineScope.launch {
                                    try {
                                        db.collection("users").document(userId).collection("cards")
                                            .add(newCard)
                                            .addOnSuccessListener { documentReference ->
                                                println("Firestore Debug: Card added successfully to path: users/$userId/cards/${documentReference.id}")
                                                println("Firestore Debug: Added Card details: $newCard")
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Card added successfully!",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                                onDismissRequest()
                                            }
                                            .addOnFailureListener { e ->
                                                println("Firestore Error: Failed to add card for user $userId: ${e.message}")
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Error adding card: ${e.message}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                    } catch (e: Exception) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Error: ${e.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Add Card", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Composable for the Notification Popup (Dialog)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NotificationPopup(onDismissRequest: () -> Unit) {
        // Sample notification data
        val notifications = remember { mutableStateListOf(
            NotificationItem(description = "+ 200$ From A", amount = 200.0, timeAgo = "2m", categories = listOf("Income", "Loan", "Others")),
            NotificationItem(description = "- 500$ To B", amount = -500.0, timeAgo = "15m", categories = listOf("Food", "Shopping", "Others")),
            NotificationItem(description = "+ 200$ From C", amount = 200.0, timeAgo = "1d", categories = listOf("Income", "Loan", "Others")),
            NotificationItem(description = "- 500$ To D", amount = -500.0, timeAgo = "2d", categories = listOf("Food", "Shopping", "Others")),
            NotificationItem(description = "- 500$ To E", amount = -500.0, timeAgo = "1w", categories = listOf("Food", "Shopping", "Others"))
        )}

        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .animateContentSize( // Smooth size animation
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    ),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close Notifications")
                        }
                    }
                    Divider() // Separator

                    // List of notifications
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(notifications) { notification ->
                            NotificationItemRow(notification = notification)
                            Divider(modifier = Modifier.padding(horizontal = 16.dp)) // Divider between items
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun NotificationItemRow(notification: NotificationItem) {
        val isIncome = notification.categories.contains("Income") // Determine if it's income based on categories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon based on income/expense
                val icon = if (isIncome) Icons.Default.CallReceived else Icons.Default.ArrowOutward // Changed ArrowInward to CallReceived
                val iconTint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(notification.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        notification.categories.forEach { category ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (category) {
                                    "Income" -> Color(0xFF4CAF50) // Green
                                    "Loan" -> Color(0xFF9C27B0) // Purple
                                    "Food" -> Color(0xFF4CAF50) // Green
                                    "Shopping" -> Color(0xFF9C27B0) // Purple
                                    else -> Color.Black // Default for "Others"
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(notification.timeAgo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }

// Composable for the Voice Input Popup
@SuppressLint("QueryPermissionsNeeded") // Suppress warning about checking if activity exists to handle intent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputPopup(onDismissRequest: () -> Unit, onTextRecognized: (String) -> Unit) {
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) } // Initially not listening until recognizer starts
    var errorText by remember { mutableStateOf<String?>(null) } // To display STT errors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // SpeechRecognizer instance
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // SpeechRecognizer Intent
    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // You can make this dynamic
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) // Get only the best result
        }
    }

    // Launcher for requesting RECORD_AUDIO permission
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, start listening
            speechRecognizer.startListening(speechRecognizerIntent)
        } else {
            // Permission denied, show an error
            errorText = "Microphone permission denied. Please enable it in settings."
            println("VoiceInputPopup Error: Microphone permission denied.")
            coroutineScope.launch {
                delay(2000)
                onDismissRequest()
            }
        }
    }

    // RecognitionListener to handle speech events
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                errorText = null // Clear any previous errors
                println("VoiceInputPopup Debug: onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                isListening = true
                println("VoiceInputPopup Debug: onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // You can use this for a visualizer, e.g., a pulsing mic icon
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Not typically used for simple voice input
            }

            override fun onEndOfSpeech() {
                isListening = false
                println("VoiceInputPopup Debug: onEndOfSpeech")
            }

            override fun onError(error: Int) {
                isListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions (RECORD_AUDIO required)"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown speech recognition error"
                }
                errorText = message
                println("VoiceInputPopup Error: $message (Code: $error)")
                // Optionally dismiss after a delay if there's an error
                coroutineScope.launch {
                    delay(2000)
                    onDismissRequest()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    recognizedText = matches[0]
                    println("VoiceInputPopup Debug: Recognized: ${matches[0]}")
                    coroutineScope.launch {
                        delay(500) // Small delay to show recognized text
                        onTextRecognized(matches[0]) // Pass the result to AddTransactionScreen
                    }
                } else {
                    errorText = "No speech recognized."
                    println("VoiceInputPopup Debug: No matches found.")
                    coroutineScope.launch {
                        delay(2000)
                        onDismissRequest()
                    }
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Optionally show partial results as they come in
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    recognizedText = matches[0] + "..." // Show partial with ellipsis
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Custom events
            }
        }
    }

    // Attach listener and start listening when composable enters composition
    DisposableEffect(speechRecognizer) {
        // Check and request permission when the dialog is launched
        when {
            // Check if permission is already granted
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                speechRecognizer.setRecognitionListener(recognitionListener)
                println("VoiceInputPopup Debug: Starting speech recognition...")
                speechRecognizer.startListening(speechRecognizerIntent)
            }
            // If permission is not granted, request it
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        onDispose {
            println("VoiceInputPopup Debug: Stopping and destroying speech recognizer.")
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Make it full screen
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF3B89FD)), // Use primary color as background
            shape = MaterialTheme.shapes.extraSmall, // Use a very small or no corner radius for full screen
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary) // Explicitly set container color
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Main content: Listening / Recognized text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    if (errorText != null) {
                        Text(
                            "Error: ${errorText!!}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    } else if (isListening) {
                        Icon(
                            Icons.Default.Mic, // Changed to Mic icon, Lens was just for simulation
                            contentDescription = "Listening",
                            tint = Color.White,
                            modifier = Modifier.size(96.dp)
                            // For pulsing animation, you'd typically use InfiniteTransition or rememberInfiniteTransition
                            // .graphicsLayer {
                            //     val scale by infiniteTransition.animateFloat(
                            //         initialValue = 1f,
                            //         targetValue = 1.2f,
                            //         animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                            //     )
                            //     scaleX = scale
                            //     scaleY = scale
                            // }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Listening...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    } else if (recognizedText != null) {
                        Text(
                            "Recognized text:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            recognizedText!!,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Tap microphone to speak...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                    // Suggested actions
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("I lost my card", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Why was my card declined?", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("How do I redeem rewards?", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                // Bottom section (optional, could have more controls)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
    // Composable for the Edit Profile screen
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditProfileScreen(navController: NavController) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // States for profile fields, initialized with current user data if available
        var whatDoWeCallYou by remember { mutableStateOf("") } // Start as empty, will be fetched
        var fullName by remember { mutableStateOf("") } // Start as empty, will be fetched
        var phoneNumber by remember { mutableStateOf("") } // Start as empty, will be fetched
        val userEmail = auth.currentUser?.email ?: "" // Directly get email from Auth, non-editable

        // Fetch additional user data from Firestore and update local states
        LaunchedEffect(userId) {
            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            whatDoWeCallYou = document.getString("whatDoWeCallYou") ?: ""
                            fullName = document.getString("fullName") ?: ""
                            phoneNumber = document.getString("phoneNumber") ?: ""
                            // emailAddress is now directly from auth.currentUser?.email and not fetched from Firestore
                        }
                    }
                    .addOnFailureListener { e ->
                        println("Firestore Error: Failed to fetch profile for user $userId: ${e.message}")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Error fetching profile: ${e.message}",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
            }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply padding from Scaffold
                    .verticalScroll(rememberScrollState()) // Make content scrollable
                    .padding(horizontal = 16.dp) // Apply horizontal padding here
                    .animateContentSize( // Smooth size animation
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    )
            ) {
                // Top app bar for Edit Profile
                TopAppBar(
                    title = { Text("Edit Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Settings.route) { inclusive = false } } }) { // Navigate to Settings
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Profile Picture section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile Picture", modifier = Modifier.size(96.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Change Picture", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(32.dp))

                // Input fields
                OutlinedTextField(
                    value = whatDoWeCallYou,
                    onValueChange = { whatDoWeCallYou = it },
                    label = { Text("What do we call you") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Email field (non-editable)
                OutlinedTextField(
                    value = userEmail, // Display user's email from Auth
                    onValueChange = { /* Do nothing, it's read-only */ },
                    label = { Text("Email") },
                    readOnly = true, // Make it non-editable
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

                // Update button
                Button(
                    onClick = {
                        if (userId == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "User not logged in.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            return@Button
                        }

                        // Update Firestore document for other details like phone number
                        val userDocRef = db.collection("users").document(userId)
                        userDocRef.update(
                            mapOf(
                                "whatDoWeCallYou" to whatDoWeCallYou,
                                "fullName" to fullName,
                                "phoneNumber" to phoneNumber
                                // Email is not updated here as it's from Auth and read-only
                            )
                        )
                            .addOnSuccessListener {
                                // Update Auth profile display name if it changed
                                if (auth.currentUser?.displayName != fullName) {
                                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build()
                                    auth.currentUser?.updateProfile(profileUpdates)
                                        ?.addOnCompleteListener { authTask ->
                                            if (authTask.isSuccessful) {
                                                println("Auth profile display name updated.")
                                            } else {
                                                println("Error updating Auth profile display name: ${authTask.exception?.message}")
                                            }
                                        }
                                }
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Profile updated successfully!",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Error updating Firestore: ${e.message}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Update", color = Color.White)
                }
            }
        }
    }

// Composable for the Expense Threshold screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseThresholdScreen(navController: NavController) {
    var dailyThresholdText by remember { mutableStateOf("0.0") }
    var monthlyThresholdText by remember { mutableStateOf("0.0") }
    var yearlyThresholdText by remember { mutableStateOf("0.0") }

    var showCheckThresholdPopup by remember { mutableStateOf(false) } // State for popup visibility

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States to hold calculated current expenses
    var totalDailyExpense by remember { mutableStateOf(0.0) }
    var totalMonthlyExpense by remember { mutableStateOf(0.0) }
    var totalYearlyExpense by remember { mutableStateOf(0.0) }

    // State to hold all transactions (needed for expense calculation)
    val allTransactions = remember { mutableStateListOf<Transaction>() }


    // Fetch existing thresholds from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            // Fetch thresholds
            db.collection("users").document(userId).collection("settings").document("thresholds")
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val thresholds = document.toObject(Threshold::class.java)
                        if (thresholds != null) {
                            dailyThresholdText = thresholds.daily.toString()
                            monthlyThresholdText = thresholds.monthly.toString()
                            yearlyThresholdText = thresholds.yearly.toString()
                        }
                    } else {
                        println("Firestore Debug: No existing thresholds found for user $userId.")
                    }
                }
                .addOnFailureListener { e ->
                    println("Firestore Error: Failed to fetch thresholds for user $userId: ${e.message}")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Error fetching thresholds: ${e.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                }

            // Fetch all transactions to calculate current expenses
            db.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("Firestore Error: ExpenseThresholdScreen transactions listen failed for user $userId: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        allTransactions.clear()
                        for (doc in snapshot.documents) {
                            val transaction = doc.toObject(Transaction::class.java)
                            if (transaction != null) {
                                allTransactions.add(transaction)
                            }
                        }
                        // Recalculate expenses whenever transactions change
                        val currentCalendar = Calendar.getInstance()
                        val today = currentCalendar.get(Calendar.DAY_OF_YEAR)
                        val thisMonth = currentCalendar.get(Calendar.MONTH)
                        val thisYear = currentCalendar.get(Calendar.YEAR)

                        totalDailyExpense = allTransactions.filter {
                            val transactionCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
                            it.type == TransactionType.EXPENSE &&
                                    transactionCalendar.get(Calendar.DAY_OF_YEAR) == today &&
                                    transactionCalendar.get(Calendar.YEAR) == thisYear
                        }.sumOf { it.amount }

                        totalMonthlyExpense = allTransactions.filter {
                            val transactionCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
                            it.type == TransactionType.EXPENSE &&
                                    transactionCalendar.get(Calendar.MONTH) == thisMonth &&
                                    transactionCalendar.get(Calendar.YEAR) == thisYear
                        }.sumOf { it.amount }

                        totalYearlyExpense = allTransactions.filter {
                            val transactionCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
                            it.type == TransactionType.EXPENSE &&
                                    transactionCalendar.get(Calendar.YEAR) == thisYear
                        }.sumOf { it.amount }

                    }
                }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = { Text("Expense Thresholds") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B89FD), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Set limits for your daily, monthly, and yearly expenses. You will be notified if you exceed these amounts.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = dailyThresholdText,
                onValueChange = { dailyThresholdText = it },
                label = { Text("Daily Threshold ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = monthlyThresholdText,
                onValueChange = { monthlyThresholdText = it },
                label = { Text("Monthly Threshold ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = yearlyThresholdText,
                onValueChange = { yearlyThresholdText = it },
                label = { Text("Yearly Threshold ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))

            // Check Thresholds Button
            Button(
                onClick = { showCheckThresholdPopup = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), // Blue color
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Check Thresholds", color = Color.White)
            }

            // Save Thresholds Button
            Button(
                onClick = {
                    val daily = dailyThresholdText.toDoubleOrNull() ?: 0.0
                    val monthly = monthlyThresholdText.toDoubleOrNull() ?: 0.0
                    val yearly = yearlyThresholdText.toDoubleOrNull() ?: 0.0

                    if (userId == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "User not logged in.",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@Button
                    }

                    val thresholds = Threshold(daily, monthly, yearly)
                    db.collection("users").document(userId).collection("settings").document("thresholds")
                        .set(thresholds) // Use set to create or overwrite
                        .addOnSuccessListener {
                            println("Firestore Debug: Thresholds saved successfully for user $userId: $thresholds")
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Thresholds saved successfully!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            println("Firestore Error: Failed to save thresholds for user $userId: ${e.message}")
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error saving thresholds: ${e.message}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save Thresholds", color = Color.White)
            }
        }
    }

    // Show Check Threshold Popup if state is true
    if (showCheckThresholdPopup) {
        ThresholdCheckPopup(
            onDismissRequest = { showCheckThresholdPopup = false },
            dailyThreshold = dailyThresholdText.toDoubleOrNull() ?: 0.0,
            monthlyThreshold = monthlyThresholdText.toDoubleOrNull() ?: 0.0,
            yearlyThreshold = yearlyThresholdText.toDoubleOrNull() ?: 0.0,
            totalDailyExpense = totalDailyExpense,
            totalMonthlyExpense = totalMonthlyExpense,
            totalYearlyExpense = totalYearlyExpense
        )
    }
}

// NEW: Composable for the Threshold Check Popup
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdCheckPopup(
    onDismissRequest: () -> Unit,
    dailyThreshold: Double,
    monthlyThreshold: Double,
    yearlyThreshold: Double,
    totalDailyExpense: Double,
    totalMonthlyExpense: Double,
    totalYearlyExpense: Double
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Expense Threshold Check", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Daily Threshold Check
                ThresholdItem(
                    period = "Daily",
                    currentExpense = totalDailyExpense,
                    threshold = dailyThreshold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Monthly Threshold Check
                ThresholdItem(
                    period = "Monthly",
                    currentExpense = totalMonthlyExpense,
                    threshold = monthlyThreshold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Yearly Threshold Check
                ThresholdItem(
                    period = "Yearly",
                    currentExpense = totalYearlyExpense,
                    threshold = yearlyThreshold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        }
    }
}

// Helper Composable for displaying a single threshold item
@Composable
fun ThresholdItem(period: String, currentExpense: Double, threshold: Double) {
    val isOverThreshold = currentExpense > threshold
    val textColor = if (isOverThreshold) Color.Red else Color.Green

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$period Expense:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "$${"%.2f".format(currentExpense)} / $${"%.2f".format(threshold)}",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}


