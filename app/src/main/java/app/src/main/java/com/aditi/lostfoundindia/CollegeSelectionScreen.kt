
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

    // Temporary sample data.
    // Later this will be connected to Firestore.
    val sampleColleges = remember {
        listOf(
            CollegeOption(
                "Institute of Engineering and Management",
                "Kolkata",
                "West Bengal"
            ),
            CollegeOption(
                "Jadavpur University",
                "Kolkata",
                "West Bengal"
            ),
            CollegeOption(
                "University of Calcutta",
                "Kolkata",
                "West Bengal"
            ),
            CollegeOption(
                "IIT Kharagpur",
                "Paschim Medinipur",
                "West Bengal"
            ),
            CollegeOption(
                "Delhi Technological University",
                "Delhi",
                "Delhi"
            ),
            CollegeOption(
                "IIT Bombay",
                "Mumbai",
                "Maharashtra"
            )
        )
    }

    val filteredColleges = sampleColleges.filter {
        it.name.contains(search, ignoreCase = true) ||
        it.district.contains(search, ignoreCase = true) ||
        it.state.contains(search, ignoreCase = true)
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
            "Suggested Colleges",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredColleges) { college ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCollege = college
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (
                            selectedCollege == college
                        ) Color(0xFFE8EFFF)
                        else Color.White
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
                                "${college.district}, ${college.state}",
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

            if (filteredColleges.isEmpty()) {
                item {
                    Text(
                        "No matching college found.",
                        color = Color.Gray,
                        modifier = Modifier.padding(15.dp)
                    )
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
            enabled = selectedCollege != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Continue")
        }
    }
}
