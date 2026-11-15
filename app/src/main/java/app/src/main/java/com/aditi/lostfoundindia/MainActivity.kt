
package com.aditi.lostfoundindia

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // Connect to Firestore
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
                            "Your College",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Select your college and location",
                            color = Blue,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(18.dp))

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

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "Recent Listings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(12.dp))

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

                        items(
                            items = filtered,
                            key = { it.id }
                        ) { item ->

                            ItemCard(
                                item = item,
                                showSolvedButton = true,
                                onSolved = {

                                    repository.markSolved(
                                        itemId = item.id,
                                        onSuccess = {
                                            message = "Item moved to Solved History"
                                        },
                                        onError = {
                                            message = it
                                        }
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

                        if (history.isEmpty()) {
                            Text(
                                "No solved items yet.",
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                items(history, key = { it.id }) {
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
                        onPost = { title, description, college,
                                   categoryValue, status ->

                            repository.postItem(
                                title = title,
                                description = description,
                                college = college,
                                category = categoryValue,
                                status = status,

                                onSuccess = {
                                    selectedTab =
                                        if (status == "LOST") "Lost" else "Found"

                                    message = "Item posted successfully!"
                                    selectedPage = "Home"
                                },

                                onError = {
                                    message = it
                                }
                            )
                        }
                    )
                }

                "Profile" -> {

                    Column(
                        Modifier.padding(20.dp)
                    ) {

                        Text(
                            "My Profile",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )

                        Spacer(Modifier.height(20.dp))

                        Text("My Lost Items")
                        Text("My Found Items")
                        Text("My Solved History")
                        Text("Privacy Settings")
                        Text("Login / Logout")

                        Spacer(Modifier.height(15.dp))

                        Text(
                            "Your personal contact details are private.",
                            color = Color.Gray
                        )
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
    onPost: (
        String, String, String, String, String
    ) -> Unit
) {

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
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

        OutlinedTextField(
            value = college,
            onValueChange = { college = it },
            label = { Text("College Name") },
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
                    description.isNotBlank() &&
                    college.isNotBlank()
                ) {
                    onPost(
                        title.trim(),
                        description.trim(),
                        college.trim(),
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
