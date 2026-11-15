
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Blue = Color(0xFF2457C5)
private val LightBlue = Color(0xFFEAF1FF)

data class ItemData(
    val title: String,
    val description: String,
    val college: String,
    val category: String,
    val status: String
)

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
    var selectedTab by remember { mutableStateOf("Lost") }
    var selectedPage by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }

    var itemList by remember {
        mutableStateOf(
            listOf(
                ItemData(
                    "Samsung Mobile",
                    "Black Samsung phone",
                    "IEM Kolkata",
                    "Mobile",
                    "LOST"
                ),
                ItemData(
                    "College ID Card",
                    "ID card found near college gate",
                    "IEM Kolkata",
                    "ID Card",
                    "FOUND"
                ),
                ItemData(
                    "Engineering Book",
                    "BTech first year book",
                    "IEM Kolkata",
                    "Books",
                    "LOST"
                )
            )
        )
    }

    var history by remember { mutableStateOf(listOf<ItemData>()) }

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
                                    "History" -> Icons.Default.Folder
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
                            "IEM Kolkata • West Bengal",
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
                            horizontalArrangement =
                                Arrangement.SpaceBetween
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
                                            horizontal = 9.dp,
                                            vertical = 10.dp
                                        ),
                                        color = if (category == name)
                                            Color.White else Color.DarkGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Recent Listings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Text(
                                "View All",
                                color = Blue
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
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

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        val filtered = itemList.filter {
                            it.status == selectedTab.uppercase() &&
                            (category == "All" ||
                                it.category == category) &&
                            (it.title.contains(
                                search,
                                ignoreCase = true
                            ) || it.description.contains(
                                search,
                                ignoreCase = true
                            ))
                        }

                        items(filtered) { item ->
                            ItemCard(
                                item = item,
                                onSolved = {
                                    history = history + item
                                    itemList = itemList - item
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
                            history.forEach {
                                ItemCard(it, onSolved = {})
                            }
                        }
                    }
                }

                "Post" -> {
                    PostScreen(
                        onPost = { newItem ->
                            itemList = itemList + newItem
                            selectedTab = if (
                                newItem.status == "LOST"
                            ) "Lost" else "Found"
                            selectedPage = "Home"
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
                            "Your phone number and personal details are private.",
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
    item: ItemData,
    onSolved: () -> Unit
) {
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
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    item.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = if (item.status == "LOST")
                        Color(0xFFFFE5E5)
                    else Color(0xFFE0F5E8),
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

            OutlinedButton(onClick = onSolved) {
                Text("Mark as Solved")
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
    onPost: (ItemData) -> Unit
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
            title,
            { title = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            description,
            { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            college,
            { college = it },
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

        Text("Category")

        Row {
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
                        ItemData(
                            title,
                            description,
                            college,
                            selectedCategory,
                            selectedStatus
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publish Item")
        }
    }
}
