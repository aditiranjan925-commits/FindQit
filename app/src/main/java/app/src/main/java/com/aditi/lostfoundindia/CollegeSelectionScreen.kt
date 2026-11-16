
package com.aditi.lostfoundindia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

private val CollegeBlue = Color(0xFF2457C5)

data class CollegeOption(
    val name: String,
    val district: String,
    val state: String
)

@Composable
fun CollegeSelectionScreen(
    onCollegeSelected: (CollegeOption) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var search by remember { mutableStateOf("") }
    var selectedCollege by remember {
        mutableStateOf<CollegeOption?>(null)
    }
    var colleges by remember {
        mutableStateOf<List<CollegeOption>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Load colleges from Firebase Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("colleges")
            .get()
            .addOnSuccessListener { snapshot ->

                colleges = snapshot.documents
                    .mapNotNull { document ->

                        val name = document
                            .getString("name")
                            ?.trim()
                            .orEmpty()

                        if (name.isBlank()) {
                            null
                        } else {
                            CollegeOption(
                                name = name,
                                district = document
                                    .getString("district")
                                    ?.trim()
                                    .orEmpty(),
                                state = document
                                    .getString("state")
                                    ?.trim()
                                    .orEmpty()
                            )
                        }
                    }
                    .distinctBy {
                        "${it.name.lowercase()}|${it.district.lowercase()}|${it.state.lowercase()}"
                    }
                    .sortedBy { it.name.lowercase() }

                loading = false
                errorMessage = ""
            }
            .addOnFailureListener { error ->
                loading = false
                errorMessage = error.localizedMessage
                    ?: "Unable to load colleges"
            }
    }

    // Search by college name, district or state
    val filteredColleges = remember(colleges, search) {
        val query = search.trim()

        if (query.isBlank()) {
            colleges
        } else {
            colleges.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.district.contains(query, ignoreCase = true) ||
                it.state.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .padding(20.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }

            Text(
                "FindQit",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CollegeBlue
            )
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "Select Your College",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Choose your college to see relevant Lost & Found items.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search college, district or state")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(Modifier.height(18.dp))

        Text(
            if (loading) "Loading colleges..."
            else "Colleges (${filteredColleges.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(12.dp))

        when {

            loading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CollegeBlue
                    )
                }
            }

            errorMessage.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        errorMessage,
                        color = Color.Red
                    )
                }
            }

            filteredColleges.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (colleges.isEmpty())
                            "No colleges available"
                        else
                            "No matching college found.",
                        color = Color.Gray
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        filteredColleges,
                        key = {
                            "${it.name}|${it.district}|${it.state}"
                        }
                    ) { college ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCollege = college
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    if (selectedCollege == college)
                                        Color(0xFFE8EFFF)
                                    else
                                        Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = CollegeBlue
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        college.name,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        listOf(
                                            college.district,
                                            college.state
                                        )
                                            .filter { it.isNotBlank() }
                                            .joinToString(", "),
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }

                                RadioButton(
                                    selected = selectedCollege == college,
                                    onClick = {
                                        selectedCollege = college
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                selectedCollege?.let {
                    onCollegeSelected(it)
                }
            },
            enabled = selectedCollege != null && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Continue")
        }
    }
}
