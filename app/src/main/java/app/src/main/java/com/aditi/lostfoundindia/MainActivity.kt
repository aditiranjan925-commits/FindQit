
package com.aditi.lostfoundindia

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private val Blue = Color(0xFF2457C5)
private val AppBackground = Color(0xFFF7F8FC)

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

    var loggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var selectedCollege by remember { mutableStateOf<CollegeOption?>(null) }
    var loading by remember { mutableStateOf(auth.currentUser != null) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            loggedIn = user != null

            if (user == null) {
                selectedCollege = null
                loading = false
                error = ""
            } else {
                loading = true
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("collegeName").orEmpty()

                        selectedCollege = if (name.isNotBlank()) {
                            CollegeOption(
                                name = name,
                                district = doc.getString("collegeDistrict").orEmpty(),
                                state = doc.getString("collegeState").orEmpty()
                            )
                        } else {
                            null
                        }
                        loading = false
                    }
                    .addOnFailureListener {
                        error = it.localizedMessage ?: "Unable to load college"
                        loading = false
                    }
            }
        }

        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    when {
        !loggedIn -> PhoneLoginScreen()

        loading -> {
            Box(
                Modifier.fillMaxSize().background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue)
            }
        }

        selectedCollege == null -> {
            Column {
                if (error.isNotBlank()) {
                    Text(error, color = Color.Red)
                }

                // FIX: named callback argument
                CollegeSelectionScreen(onCollegeSelected = { college ->
                    val uid = auth.currentUser?.uid

                    if (uid != null) {
                        db.collection("users").document(uid)
                            .set(
                                mapOf(
                                    "collegeName" to college.name,
                                    "collegeDistrict" to college.district,
                                    "collegeState" to college.state,
                                    "collegeUpdatedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )
                            .addOnSuccessListener {
                                selectedCollege = college
                                error = ""
                            }
                            .addOnFailureListener {
                                error = it.localizedMessage ?: "College save failed"
                            }
                    }
                })
            }
        }

        else -> {
            FindQitMainContent(
                college = selectedCollege!!,
                onChangeCollege = {
                    val uid = auth.currentUser?.uid

                    if (uid != null) {
                        db.collection("users").document(uid)
                            .update(
                                mapOf(
                                    "collegeName" to FieldValue.delete(),
                                    "collegeDistrict" to FieldValue.delete(),
                                    "collegeState" to FieldValue.delete()
                                )
                            )
                            .addOnSuccessListener {
                                selectedCollege = null
                                error = ""
                            }
                            .addOnFailureListener {
                                error = it.localizedMessage ?: "Unable to change college"
                            }
                    }
                }
            )
        }
    }
}

@Composable
fun PhoneLoginScreen() {
    val context = LocalContext.current
    val manager = remember { PhoneAuthManager() }

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.findqit_logo),
            contentDescription = "FindQit Logo",
            modifier = Modifier.size(90.dp)
        )

        Text(
            "FindQit",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Blue
        )
        Text(
            "Find what you lost. Return what you found.",
            color = Color.Gray
        )

        Spacer(Modifier.height(30.dp))

        Text("Welcome!", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Login with your mobile number", color = Color.Gray)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it.filter(Char::isDigit).take(10)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mobile Number") },
            leadingIcon = { Text("+91") },
            singleLine = true
        )

        Spacer(Modifier.height(15.dp))

        if (verificationId.isNotBlank()) {
            OutlinedTextField(
                value = otp,
                onValueChange = {
                    otp = it.filter(Char::isDigit).take(6)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter OTP") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (otp.length == 6) {
                        loading = true
                        manager.verifyOtp(
                            verificationId = verificationId,
                            otp = otp,
                            onSuccess = {
                                loading = false
                                message = "Login successful!"
                            },
                            onError = {
                                loading = false
                                message = it
                            }
                        )
                    } else {
                        message = "Enter a valid 6-digit OTP"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text(if (loading) "Verifying..." else "Verify OTP")
            }
        } else {
            Button(
                onClick = {
                    if (phone.length == 10) {
                        loading = true
                        message = ""
                        manager.sendOtp(
                            activity = context as Activity,
                            phoneNumber = "+91$phone",
                            onCodeSent = { id ->
                                verificationId = id
                                loading = false
                                message = "OTP sent successfully"
                            },
                            onError = {
                                loading = false
                                message = it
                            }
                        )
                    } else {
                        message = "Enter a valid 10-digit number"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text(if (loading) "Sending OTP..." else "Send OTP")
            }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(15.dp))
            Text(
                message,
                color = if (
                    message.contains("successful", true) ||
                    message.contains("sent", true)
                ) Color(0xFF168447) else Color.Red
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Your phone number will not be displayed publicly.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FindQitMainContent(
    college: CollegeOption,
    onChangeCollege: () -> Unit
) {
    val repository = remember { FirebaseRepository() }

    var selectedTab by remember { mutableStateOf("Lost") }
    var selectedPage by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }

    var activeItems by remember { mutableStateOf<List<FindQitItem>>(emptyList()) }
    var history by remember { mutableStateOf<List<FindQitItem>>(emptyList()) }
    var myItems by remember { mutableStateOf<List<FindQitItem>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(repository) {
        val activeListener = repository.observeActiveItems(
            onData = { activeItems = it },
            onError = { message = it }
        )
        val historyListener = repository.observeSolvedHistory(
            onData = { history = it },
            onError = { message = it }
        )
        val myItemsListener = repository.observeMyItems(
            onData = { myItems = it },
            onError = { message = it }
        )

        onDispose {
            activeListener.remove()
            historyListener.remove()
            myItemsListener.remove()
        }
    }

    val categories = listOf(
        "All", "Mobile", "Books", "ID Card",
        "Notebooks", "Keys", "Electronic items", "Others"
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val pages = listOf("Home", "Search", "Post", "History", "Profile")
                pages.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = {
                            selectedPage = page
                            message = ""
                        },
                        icon = {
                            Icon(
                                imageVector = when (page) {
                                    "Home" -> Icons.Default.Home
                                    "Search" -> Icons.Default.Search
                                    "Post" -> Icons.Default.AddCircle
                                    "History" -> Icons.Default.History
                                    else -> Icons.Default.Person
                                },
                                contentDescription = page
                            )
                        },
                        label = { Text(page) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
        ) {
            if (message.isNotBlank()) {
                Text(
                    message,
                    color = if (
                        message.contains("success", true) ||
                        message.contains("moved", true)
                    ) Color(0xFF168447) else Color.Red,
                    modifier = Modifier.padding(12.dp)
                )
            }

            when (selectedPage) {
                "Home", "Search" -> {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.findqit_logo),
                                contentDescription = "FindQit Logo",
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "FindQit",
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue
                            )
                        }

                        Text(
                            "Find what you lost. Return what you found.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            color = Color(0xFFE8EFFF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Your College",
                                    fontWeight = FontWeight.Bold,
                                    color = Blue
                                )
                                Text(college.name)
                                Text(
                                    "${college.district}, ${college.state}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search item or college...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            shape = RoundedCornerShape(15.dp),
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("Categories", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            categories.forEach { name ->
                                FilterChip(
                                    selected = category == name,
                                    onClick = { category = name },
                                    label = { Text(name) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (selectedPage == "Search") "Search All Colleges"
                            else "Recent Listings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = selectedTab == "Lost",
                                onClick = { selectedTab = "Lost" },
                                label = { Text("Lost Items") }
                            )
                            FilterChip(
                                selected = selectedTab == "Found",
                                onClick = { selectedTab = "Found" },
                                label = { Text("Found Items") }
                            )
                        }
                    }

                    val filtered = activeItems.filter { item ->
                        val collegeMatches =
                            selectedPage == "Search" ||
                            item.college.equals(college.name, true)

                        val categoryMatches =
                            category == "All" ||
                            item.category.equals(category, true)

                        val searchMatches =
                            search.isBlank() ||
                            item.title.contains(search, true) ||
                            item.description.contains(search, true) ||
                            item.college.contains(search, true) ||
                            item.category.contains(search, true)

                        item.status.equals(selectedTab, true) &&
                            collegeMatches && categoryMatches && searchMatches
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                showSolvedButton = true,
                                onSolved = {
                                    repository.markSolved(
                                        itemId = item.id,
                                        onSuccess = {
                                            message = "Item moved to Solved History"
                                        },
                                        onError = { message = it }
                                    )
                                }
                            )
                        }

                        if (filtered.isEmpty()) {
                            item {
                                EmptyState("No ${selectedTab.lowercase()} items found.")
                            }
                        }
                    }
                }

                "Post" -> {
                    PostScreen(
                        selectedCollege = college.name,
                        onPost = { title, description, categoryValue, status ->
                            repository.postItem(
                                title = title,
                                description = description,
                                college = college.name,
                                category = categoryValue,
                                status = status,
                                onSuccess = {
                                    selectedTab = status
                                    message = "Item posted successfully!"
                                    selectedPage = "Home"
                                },
                                onError = { message = it }
                            )
                        }
                    )
                }

                "History" -> {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        Text(
                            "My Solved History",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )
                        Spacer(Modifier.height(15.dp))

                        if (history.isEmpty()) {
                            EmptyState("No solved items yet.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(history, key = { it.id }) { item ->
                                    ItemCard(item, false, {})
                                }
                            }
                        }
                    }
                }

                "Profile" -> {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp)
                    ) {
                        Text(
                            "My Profile",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )
                        Spacer(Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Blue,
                                    modifier = Modifier.size(55.dp)
                                )
                                Text(
                                    "My Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text("Mobile number: Hidden", color = Color.Gray)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "College: ${college.name}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${college.district}, ${college.state}",
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(15.dp))

                        OutlinedButton(
                            onClick = onChangeCollege,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // FIX: use an available icon
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Change College")
                        }

                        Spacer(Modifier.height(18.dp))
                        Text("My Listings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                selectedTab = "Lost"
                                selectedPage = "MyItems"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("My Lost & Found Items (${myItems.size})")
                        }

                        OutlinedButton(
                            onClick = { selectedPage = "History" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("My Solved History (${history.size})")
                        }

                        Spacer(Modifier.height(20.dp))
                        Text("Privacy", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Your personal contact details are not displayed publicly.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD93636)
                            )
                        ) {
                            // FIX: use ExitToApp instead of Logout
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Logout")
                        }
                    }
                }

                "MyItems" -> {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedPage = "Profile" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                "My Items",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedTab == "Lost",
                                onClick = { selectedTab = "Lost" },
                                label = { Text("My Lost") }
                            )
                            FilterChip(
                                selected = selectedTab == "Found",
                                onClick = { selectedTab = "Found" },
                                label = { Text("My Found") }
                            )
                        }

                        val myFiltered = myItems.filter {
                            it.status.equals(selectedTab, true)
                        }

                        if (myFiltered.isEmpty()) {
                            EmptyState("You have no ${selectedTab.lowercase()} items.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(myFiltered, key = { it.id }) { item ->
                                    ItemCard(
                                        item = item,
                                        showSolvedButton = true,
                                        onSolved = {
                                            repository.markSolved(
                                                itemId = item.id,
                                                onSuccess = {
                                                    message = "Item marked as solved"
                                                },
                                                onError = { message = it }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(25.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun ItemCard(
    item: FindQitItem,
    showSolvedButton: Boolean,
    onSolved: () -> Unit
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = currentUid != null && item.ownerId == currentUid

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = when (item.status) {
                        "LOST" -> Color(0xFFFFE5E5)
                        "FOUND" -> Color(0xFFE0F5E8)
                        else -> Color(0xFFE8E8E8)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        item.status,
                        modifier = Modifier.padding(7.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(item.description, color = Color.DarkGray)
            Spacer(Modifier.height(5.dp))

            Text(
                "${item.college} • ${item.category}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            if (item.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Image attached", color = Blue, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))

            if (showSolvedButton && isOwner) {
                OutlinedButton(onClick = onSolved) {
                    Text("Mark as Solved")
                }
            }

            Text(
                "Personal contact details are hidden",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PostScreen(
    selectedCollege: String,
    onPost: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("LOST") }
    var selectedCategory by remember { mutableStateOf("Others") }
    var warning by remember { mutableStateOf("") }

    val categories = listOf(
        "Mobile", "Books", "ID Card",
        "Notebooks", "Keys", "Electronic items", "Others"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(20.dp)
    ) {
        Text(
            "Post an Item",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Blue
        )

        Spacer(Modifier.height(15.dp))
        Text("College: $selectedCollege", fontWeight = FontWeight.Bold, color = Blue)
        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description and identifying details") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(15.dp))
        Text("Select Item Type", fontWeight = FontWeight.Bold)

        Row {
            FilterChip(
                selected = selectedStatus == "LOST",
                onClick = { selectedStatus = "LOST" },
                label = { Text("Lost") }
            )
            Spacer(Modifier.width(10.dp))
            FilterChip(
                selected = selectedStatus == "FOUND",
                onClick = { selectedStatus = "FOUND" },
                label = { Text("Found") }
            )
        }

        Spacer(Modifier.height(15.dp))
        Text("Category", fontWeight = FontWeight.Bold)

        Column {
            categories.chunked(2).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.forEach { value ->
                        FilterChip(
                            selected = selectedCategory == value,
                            onClick = { selectedCategory = value },
                            label = { Text(value) }
                        )
                    }
                }
            }
        }

        if (warning.isNotBlank()) {
            Text(warning, color = Color.Red)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isBlank() || description.isBlank()) {
                    warning = "Please enter item name and description"
                } else {
                    warning = ""
                    onPost(
                        title.trim(),
                        description.trim(),
                        selectedCategory,
                        selectedStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // FIX: use Send instead of Publish
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Publish Item")
        }
    }
}
