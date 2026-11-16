
package com.aditi.lostfoundindia

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val Blue = Color(0xFF2457C5)

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

    var loggedIn by remember {
        mutableStateOf(auth.currentUser != null)
    }

    var selectedCollege by remember {
        mutableStateOf<CollegeOption?>(null)
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            loggedIn = it.currentUser != null

            if (it.currentUser == null) {
                selectedCollege = null
            }
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    when {
        !loggedIn -> PhoneLoginScreen()

        selectedCollege == null -> {
            CollegeSelectionScreen(
                onCollegeSelected = {
                    selectedCollege = it
                }
            )
        }

        else -> {
            FindQitMainContent(
                college = selectedCollege!!
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
            .background(Color(0xFFF7F8FC))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "FindQit",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Blue
        )

        Text(
            "Find what you lost. Return what you found.",
            color = Color.Gray
        )

        Spacer(Modifier.height(35.dp))

        Text(
            "Welcome!",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Login with your mobile number",
            color = Color.Gray
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it.filter { ch -> ch.isDigit() }.take(10)
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
                    otp = it.filter { ch -> ch.isDigit() }.take(6)
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
                            onError = { error ->
                                loading = false
                                message = error
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
fun FindQitMainContent(college: CollegeOption) {
    val repository = remember { FirebaseRepository() }

    var selectedTab by remember { mutableStateOf("Lost") }
    var selectedPage by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }

    var activeItems by remember {
        mutableStateOf<List<FindQitItem>>(emptyList())
    }

    var history by remember {
        mutableStateOf<List<FindQitItem>>(emptyList())
    }

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

        onDispose {
            activeListener.remove()
            historyListener.remove()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    "Home", "Search", "Post", "History", "Profile"
                ).forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = { selectedPage = page },
                        icon = {
                            Icon(
                                when (page) {
                                    "Home" -> Icons.Default.Home
                                    "Search" -> Icons.Default.Search
                                    "Post" -> Icons.Default.AddCircle
                                    "History" -> Icons.Default.Home
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
                .background(Color(0xFFF7F8FC))
                .padding(padding)
        ) {

            if (message.isNotBlank()) {
                Text(
                    message,
                    color = Color.Red,
                    modifier = Modifier.padding(12.dp)
                )
            }

            when (selectedPage) {

                "Home", "Search" -> {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "FindQit",
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )

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

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Search mobile, books, ID card...")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            shape = RoundedCornerShape(15.dp),
                            singleLine = true
                        )

                        Spacer(Modifier.height(15.dp))

                        Text(
                            "Categories",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                "All", "Mobile", "Books",
                                "ID Card", "Others"
                            ).forEach { name ->
                                Surface(
                                    color = if (category == name)
                                        Blue else Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        category = name
                                    }
                                ) {
                                    Text(
                                        name,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 10.dp
                                        ),
                                        color = if (category == name)
                                            Color.White else Color.DarkGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(15.dp))

                        Text(
                            "Recent Listings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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

                    val filtered = activeItems.filter {
                        it.status == selectedTab.uppercase() &&
                        it.college.equals(college.name, ignoreCase = true) &&
                        (category == "All" || it.category == category) &&
                        (
                            it.title.contains(search, ignoreCase = true) ||
                            it.description.contains(search, ignoreCase = true)
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 10.dp
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
                                Text(
                                    "No ${selectedTab.lowercase()} items found.",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }
                }

                "History" -> {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Solved History",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )

                        Spacer(Modifier.height(15.dp))

                        val collegeHistory = history.filter {
                            it.college.equals(college.name, ignoreCase = true)
                        }

                        if (collegeHistory.isEmpty()) {
                            Text(
                                "No solved items yet.",
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                items(collegeHistory, key = { it.id }) {
                                    ItemCard(
                                        item = it,
                                        showSolvedButton = false,
                                        onSolved = {}
                                    )
                                }
                            }
                        }
                    }
                }

                "Post" -> {
                    PostScreen(
                        selectedCollege = college.name,
                        onPost = { title, description,
                                   categoryValue, status ->

                            repository.postItem(
                                title = title,
                                description = description,
                                college = college.name,
                                category = categoryValue,
                                status = status,
                                onSuccess = {
                                    selectedTab =
                                        if (status == "LOST") "Lost" else "Found"
                                    message = "Item posted successfully!"
                                    selectedPage = "Home"
                                },
                                onError = { message = it }
                            )
                        }
                    )
                }

                "Profile" -> {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "My Profile",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )

                        Spacer(Modifier.height(20.dp))

                        Text("College: ${college.name}")
                        Text("${college.district}, ${college.state}")

                        Spacer(Modifier.height(20.dp))

                        Text("My Lost Items")
                        Text("My Found Items")
                        Text("My Solved History")
                        Text("Privacy Settings")

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "Your personal contact details are private.",
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
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

            Spacer(Modifier.height(7.dp))

            Text(item.description, color = Color.DarkGray)

            Text(
                "${item.college} • ${item.category}",
                color = Color.Gray,
                fontSize = 12.sp
            )

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

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Post an Item",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Blue
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "College: $selectedCollege",
            fontWeight = FontWeight.Bold,
            color = Blue
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Text("Select Item Type")

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

        Spacer(Modifier.height(10.dp))

        Text("Category")

        Column {
            listOf("Mobile", "Books", "ID Card", "Others")
                .forEach { value ->
                    FilterChip(
                        selected = selectedCategory == value,
                        onClick = { selectedCategory = value },
                        label = { Text(value) }
                    )
                }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    title.isNotBlank() &&
                    description.isNotBlank()
                ) {
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
            Text("Publish Item")
        }
    }
}
