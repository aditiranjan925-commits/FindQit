
package com.aditi.lostfoundindia
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

private val AppBlue = Color(0xFF2457C5)
private val AppBackground = Color(0xFFF5F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                FindQitApp()
            }
        }
    }
}

@Composable
fun FindQitApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var currentUser by remember {
        mutableStateOf(auth.currentUser)
    }

    var collegeName by remember { mutableStateOf("") }
    var collegeDistrict by remember { mutableStateOf("") }
    var collegeState by remember { mutableStateOf("") }
    var checkingProfile by remember { mutableStateOf(false) }
    var changingCollege by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUser = it.currentUser
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid

        if (uid == null) {
            collegeName = ""
            collegeDistrict = ""
            collegeState = ""
            checkingProfile = false
        } else {
            checkingProfile = true

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    collegeName = document.getString("collegeName") ?: ""
                    collegeDistrict = document.getString("collegeDistrict") ?: ""
                    collegeState = document.getString("collegeState") ?: ""
                    checkingProfile = false
                }
                .addOnFailureListener {
                    checkingProfile = false
                }
        }
    }

    when {
        currentUser == null -> {
            PhoneLoginScreen()
        }

        checkingProfile -> {
            LoadingScreen("Loading your profile...")
        }

        collegeName.isBlank() || changingCollege -> {
            CollegeSelectionScreen(
                onCollegeSelected = { college ->
                    val uid = auth.currentUser?.uid

                    if (uid != null) {
                        val userData = hashMapOf(
                            "collegeName" to college.name,
                            "collegeDistrict" to college.district,
                            "collegeState" to college.state
                        )

                        db.collection("users")
                            .document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                collegeName = college.name
                                collegeDistrict = college.district
                                collegeState = college.state
                                changingCollege = false
                            }
                    }
                },
                onBack = if (collegeName.isNotBlank()) {
                    { changingCollege = false }
                } else null
            )
        }

        else -> {
            FindQitMainContent(
                collegeName = collegeName,
                onChangeCollege = {
                    changingCollege = true
                },
                onLogout = {
                    auth.signOut()
                }
            )
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppBlue)
            Spacer(Modifier.height(15.dp))
            Text(message, color = Color.Gray)
        }
    }
}

@Composable
fun PhoneLoginScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember { PhoneAuthManager() }

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "FindQit",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = AppBlue
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "India's Lost & Found Community",
            color = Color.Gray
        )

        Spacer(Modifier.height(35.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    if (otpSent) "Verify OTP" else "Login with Phone",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(18.dp))

                if (!otpSent) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it.filter { char -> char.isDigit() }
                                .take(10)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mobile Number") },
                        placeholder = { Text("10 digit mobile number") },
                        prefix = { Text("+91 ") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (phone.length != 10) {
                                errorMessage = "Enter a valid 10 digit number"
                            } else if (activity == null) {
                                errorMessage = "Activity not available"
                            } else {
                                loading = true
                                errorMessage = ""

                                authManager.sendOtp(
                                    activity = activity,
                                    phoneNumber = "+91$phone",
                                    onCodeSent = { id ->
                                        verificationId = id
                                        otpSent = true
                                        loading = false
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                        loading = false
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !loading
                    ) {
                        Text(if (loading) "Sending..." else "Send OTP")
                    }
                } else {
                    Text(
                        "OTP sent to +91 $phone",
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = otp,
                        onValueChange = {
                            otp = it.filter { char -> char.isDigit() }
                                .take(6)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter OTP") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (otp.length != 6) {
                                errorMessage = "Enter the 6 digit OTP"
                            } else {
                                loading = true
                                errorMessage = ""

                                authManager.verifyOtp(
                                    verificationId = verificationId,
                                    otp = otp,
                                    onSuccess = {
                                        loading = false
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                        loading = false
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !loading
                    ) {
                        Text(if (loading) "Verifying..." else "Verify & Login")
                    }

                    TextButton(
                        onClick = {
                            otpSent = false
                            otp = ""
                            verificationId = ""
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Mobile Number")
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Find your lost belongings. Help others find theirs.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FindQitMainContent(
    collegeName: String,
    onChangeCollege: () -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }

    var activeItems by remember {
        mutableStateOf<List<FindQitItem>>(emptyList())
    }

    var myItems by remember {
        mutableStateOf<List<FindQitItem>>(emptyList())
    }

    var solvedItems by remember {
        mutableStateOf<List<FindQitItem>>(emptyList())
    }

    var selectedPage by remember { mutableStateOf("Home") }
    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }

    DisposableEffect(auth.currentUser?.uid) {
        var activeListener: ListenerRegistration? = null
        var myListener: ListenerRegistration? = null
        var solvedListener: ListenerRegistration? = null

        if (auth.currentUser != null) {
            activeListener = repository.observeActiveItems(
                onData = {
                    activeItems = it
                    loading = false
                },
                onError = {
                    errorMessage = it
                    loading = false
                }
            )

            myListener = repository.observeMyItems(
                onData = { myItems = it },
                onError = { errorMessage = it }
            )

            solvedListener = repository.observeSolvedHistory(
                onData = { solvedItems = it },
                onError = { errorMessage = it }
            )
        }

        onDispose {
            activeListener?.remove()
            myListener?.remove()
            solvedListener?.remove()
        }
    }

    val visibleItems = remember(
        activeItems,
        selectedPage,
        searchText,
        selectedFilter,
        collegeName
    ) {
        var result = activeItems

        if (selectedPage == "Home") {
            result = result.filter {
                it.college.equals(collegeName, ignoreCase = true)
            }
        }

        if (selectedFilter != "ALL") {
            result = result.filter {
                it.status == selectedFilter
            }
        }

        if (searchText.isNotBlank()) {
            result = result.filter {
                it.title.contains(searchText, true) ||
                it.description.contains(searchText, true) ||
                it.college.contains(searchText, true) ||
                it.category.contains(searchText, true)
            }
        }

        result.sortedByDescending { it.createdAt }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar {
                val pages = listOf(
                    "Home",
                    "Search",
                    "Post",
                    "History",
                    "Profile"
                )

                pages.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = {
                            selectedPage = page
                            notice = ""
                        },
                        icon = {
                            Text(
                                when (page) {
                                    "Home" -> "⌂"
                                    "Search" -> "⌕"
                                    "Post" -> "+"
                                    "History" -> "◷"
                                    else -> "○"
                                },
                                fontSize = 21.sp
                            )
                        },
                        label = {
                            Text(page, fontSize = 10.sp)
                        }
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FindQit",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppBlue
                    )
                    Text(
                        collegeName,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    "INDIA",
                    color = AppBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            if (notice.isNotBlank()) {
                Text(
                    notice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE5F4E8))
                        .padding(10.dp),
                    color = Color(0xFF216B36)
                )
            }

            when (selectedPage) {
                "Home", "Search" -> {
                    ItemListScreen(
                        page = selectedPage,
                        items = visibleItems,
                        loading = loading,
                        errorMessage = errorMessage,
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        onMarkSolved = { item ->
                            repository.markSolved(
                                itemId = item.id,
                                onSuccess = {
                                    notice = "Item marked as solved!"
                                },
                                onError = {
                                    notice = it
                                }
                            )
                        }
                    )
                }

                "Post" -> {
                    PostScreen(
                        collegeName = collegeName,
                        onPosted = {
                            notice = "Your item has been posted!"
                            selectedPage = "Home"
                        },
                        onError = {
                            notice = it
                        }
                    )
                }

                "History" -> {
                    MyItemsScreen(
                        heading = "Solved History",
                        items = solvedItems,
                        emptyMessage = "No solved items yet.",
                        showSolved = false,
                        onMarkSolved = {}
                    )
                }

                "Profile" -> {
                    ProfileScreen(
                        phone = auth.currentUser?.phoneNumber ?: "Not available",
                        collegeName = collegeName,
                        onChangeCollege = onChangeCollege,
                        onMyItems = {
                            selectedPage = "MyItems"
                        },
                        onLogout = onLogout
                    )
                }

                "MyItems" -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TextButton(
                            onClick = { selectedPage = "Profile" }
                        ) {
                            Text("← Back to Profile")
                        }

                        MyItemsScreen(
                            heading = "My Active Items",
                            items = myItems,
                            emptyMessage = "You have not posted any active items.",
                            showSolved = true,
                            onMarkSolved = { item ->
                                repository.markSolved(
                                    itemId = item.id,
                                    onSuccess = {
                                        notice = "Item marked as solved!"
                                    },
                                    onError = {
                                        notice = it
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemListScreen(
    page: String,
    items: List<FindQitItem>,
    loading: Boolean,
    errorMessage: String,
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onMarkSolved: (FindQitItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(15.dp))

        Text(
            if (page == "Home") "Your College Community"
            else "Search Across India",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            if (page == "Home")
                "Lost and Found items from your college"
            else
                "Find items posted by colleges across India",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search item, category or college")
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "LOST", "FOUND").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            when (filter) {
                                "ALL" -> "All"
                                "LOST" -> "Lost"
                                else -> "Found"
                            }
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppBlue)
            }
        } else if (errorMessage.isNotBlank()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        } else if (items.isEmpty()) {
            EmptyState("No matching items found.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onMarkSolved = {
                            onMarkSolved(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: FindQitItem,
    onMarkSolved: () -> Unit
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = item.ownerId == currentUid

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (item.status == "LOST") {
                        Color(0xFFFFE8E8)
                    } else {
                        Color(0xFFE5F5E9)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        item.status,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        color = if (item.status == "LOST") {
                            Color(0xFFB3261E)
                        } else {
                            Color(0xFF217A3A)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    item.category,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                item.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                item.description,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "College: ${item.college}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (item.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Photo attached",
                    color = AppBlue,
                    fontSize = 12.sp
                )
            }

            if (isOwner) {
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onMarkSolved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Solved")
                }
            }
        }
    }
}

{
    val repository = remember { FirebaseRepository() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var status by remember { mutableStateOf("LOST") }
    var loading by remember { mutableStateOf(false) }

    val categories = listOf(
        "Electronics",
        "Documents",
        "ID Card",
        "Wallet",
        "Keys",
        "Books",
        "Bags",
        "Clothing",
        "Others"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Post an Item",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Share details about the lost or found item.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "Select Type",
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = status == "LOST",
                onClick = { status = "LOST" },
                label = { Text("I Lost Something") }
            )

            FilterChip(
                selected = status == "FOUND",
                onClick = { status = "FOUND" },
                label = { Text("I Found Something") }
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Item Name") },
            placeholder = { Text("Example: Samsung phone") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description and Location") },
            placeholder = {
                Text("Describe the item and where it was lost/found")
            },
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Category",
            fontWeight = FontWeight.Bold
        )

        var categoryExpanded by remember {
            mutableStateOf(false)
        }

        Box {
            OutlinedButton(
                onClick = { categoryExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(category)
            }

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = {
                    categoryExpanded = false
                }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },


@Composable
fun PostScreen(
    collegeName: String,
    onPosted: () -> Unit,
    onError: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val storage = remember { FirebaseStorage.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var status by remember { mutableStateOf("LOST") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }

    val categories = listOf(
        "Electronics", "Documents", "ID Card",
        "Wallet", "Keys", "Books", "Bags",
        "Clothing", "Others"
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImage = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Post an Item",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Share details about your lost or found item.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(15.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = status == "LOST",
                onClick = { status = "LOST" },
                label = { Text("I Lost Something") }
            )

            FilterChip(
                selected = status == "FOUND",
                onClick = { status = "FOUND" },
                label = { Text("I Found Something") }
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Item Name") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description and Location") },
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))

        Text("Category", fontWeight = FontWeight.Bold)

        var expanded by remember { mutableStateOf(false) }

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(category)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            category = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("College: $collegeName", color = Color.Gray)

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (selectedImage == null)
                    "Select Item Photo"
                else
                    "Change Selected Photo"
            )
        }

        selectedImage?.let { uri ->
            Spacer(Modifier.height(10.dp))

            AsyncImage(
                model = uri,
                contentDescription = "Selected item photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                if (title.isBlank() || description.isBlank()) {
                    onError("Please enter item name and description.")
                    return@Button
                }

                val uid = auth.currentUser?.uid

                if (uid == null) {
                    onError("Please login again.")
                    return@Button
                }

                loading = true

                fun saveItem(imageUrl: String) {
                    repository.postItem(
                        title = title.trim(),
                        description = description.trim(),
                        college = collegeName,
                        category = category,
                        status = status,
                        imageUrl = imageUrl,
                        onSuccess = {
                            loading = false
                            onPosted()
                        },
                        onError = { error ->
                            loading = false
                            onError(error)
                        }
                    )
                }

                val imageUri = selectedImage

                if (imageUri == null) {
                    saveItem("")
                } else {
                    val imageRef = storage.reference
                        .child("item_images/$uid/${UUID.randomUUID()}.jpg")

                    imageRef.putFile(imageUri)
                        .addOnSuccessListener {
                            imageRef.downloadUrl
                                .addOnSuccessListener { downloadUri ->
                                    saveItem(downloadUri.toString())
                                }
                                .addOnFailureListener { error ->
                                    loading = false
                                    onError(
                                        error.message ?: "Could not get photo URL"
                                    )
                                }
                        }
                        .addOnFailureListener { error ->
                            loading = false
                            onError(
                                error.message ?: "Photo upload failed"
                            )
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !loading
        ) {
            Text(
                if (loading) "Uploading..." else "Publish Item"
            )
        }
    }
}

@Composable
fun MyItemsScreen(
    heading: String,
    items: List<FindQitItem>,
    emptyMessage: String,
    showSolved: Boolean,
    onMarkSolved: (FindQitItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            heading,
            modifier = Modifier.padding(vertical = 16.dp),
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        if (items.isEmpty()) {
            EmptyState(emptyMessage)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onMarkSolved = {
                            if (showSolved) {
                                onMarkSolved(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    phone: String,
    collegeName: String,
    onChangeCollege: () -> Unit,
    onMyItems: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "My Profile",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    "FindQit User",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Text("Mobile Number", color = Color.Gray)
                Text(
                    phone,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(15.dp))

                Text("My College", color = Color.Gray)
                Text(
                    collegeName,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = onMyItems,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Active Items")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onChangeCollege,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change College")
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB3261E)
            )
        ) {
            Text("Logout")
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "FindQit helps students connect to recover lost belongings.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(35.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}
