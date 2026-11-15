
package com.aditi.lostfoundindia

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loggedIn by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }

    if (loggedIn) {
        HomeScreen(
            onLogout = {
                FirebaseAuth.getInstance().signOut()
                loggedIn = false
            }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "FindQit",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Lost Today. Found Tomorrow.")

            Spacer(Modifier.height(25.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(15.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        message = "Enter valid email and 6-character password"
                    } else {
                        FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener {
                                loggedIn = true
                                message = ""
                            }
                            .addOnFailureListener {
                                message = it.localizedMessage ?: "Login failed"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }

            OutlinedButton(
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        message = "Enter valid email and 6-character password"
                    } else {
                        FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener {
                                loggedIn = true
                                message = ""
                            }
                            .addOnFailureListener {
                                message = it.localizedMessage ?: "Signup failed"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Account")
            }

            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedType by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val postedItems = remember { mutableStateListOf<Map<String, Any>>() }

    LaunchedEffect(refresh) {
        FirebaseFirestore.getInstance()
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                postedItems.clear()
                documents.forEach {
                    it.data?.let { data -> postedItems.add(data) }
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FindQit",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        Text("Welcome to FindQit!")

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { selectedType = "Lost" }) {
                Text("Lost Item")
            }
            Button(onClick = { selectedType = "Found" }) {
                Text("Found Item")
            }
        }

        Spacer(Modifier.height(15.dp))

        if (selectedType.isNotEmpty()) {
            ItemForm(
                type = selectedType,
                onCancel = { selectedType = "" },
                onSaved = {
                    selectedType = ""
                    refresh++
                }
            )
        } else {
            Text(
                "Recent Items",
                style = MaterialTheme.typography.headlineSmall
            )

            LazyColumn {
                items(postedItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                item["title"]?.toString() ?: "Item",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Type: ${item["type"] ?: ""}")
                            Text("College: ${item["college"] ?: ""}")
                            Text("Location: ${item["location"] ?: ""}")
                            Text("Description: ${item["description"] ?: ""}")
                            Text("Contact: ${item["contact"] ?: ""}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemForm(
    type: String,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    fun saveToFirestore(imageUrl: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            saving = false
            message = "Please login again"
            return
        }

        val data = hashMapOf(
            "type" to type,
            "title" to title.trim(),
            "description" to description.trim(),
            "college" to college.trim(),
            "location" to location.trim(),
            "contact" to contact.trim(),
            "imageUrl" to imageUrl,
            "userId" to user.uid,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("items")
            .add(data)
            .addOnSuccessListener {
                saving = false
                onSaved()
            }
            .addOnFailureListener {
                saving = false
                message = it.localizedMessage ?: "Could not save item"
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "$type Item Details",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(10.dp))

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

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Contact Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (imageUri == null) "Choose Photo" else "Photo Selected")
        }

        Text(message, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(10.dp))

        Button(
            enabled = !saving,
            onClick = {
                if (
                    title.isBlank() ||
                    description.isBlank() ||
                    college.isBlank() ||
                    location.isBlank() ||
                    contact.isBlank()
                ) {
                    message = "Please fill all details"
                } else {
                    saving = true
                    message = "Saving..."

                    if (imageUri != null) {
                        val reference = FirebaseStorage.getInstance()
                            .reference
                            .child("items/${UUID.randomUUID()}.jpg")

                        reference.putFile(imageUri!!)
                            .addOnSuccessListener {
                                reference.downloadUrl
                                    .addOnSuccessListener { uri ->
                                        saveToFirestore(uri.toString())
                                    }
                                    .addOnFailureListener {
                                        saving = false
                                        message = "Photo link could not be created"
                                    }
                            }
                            .addOnFailureListener {
                                saving = false
                                message = it.localizedMessage ?: "Photo upload failed"
                            }
                    } else {
                        saveToFirestore("")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Submit Item")
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
