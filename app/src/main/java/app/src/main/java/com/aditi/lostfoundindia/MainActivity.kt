
package com.aditi.lostfoundindia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth

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

    Surface(modifier = Modifier.fillMaxSize()) {
        if (loggedIn) {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    loggedIn = false
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "FindQit",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Lost Today. Found Tomorrow.")

                Spacer(modifier = Modifier.height(30.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.length < 6) {
                            message = "Enter valid email and 6-character password"
                        } else {
                            FirebaseAuth.getInstance()
                                .signInWithEmailAndPassword(email, password)
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
                                .createUserWithEmailAndPassword(email, password)
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
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Lost Items") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selectedTab = "Lost Items" }) {
                Text("Lost")
            }
            Button(onClick = { selectedTab = "Found Items" }) {
                Text("Found")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            selectedTab,
            style = MaterialTheme.typography.headlineSmall
        )

        Text("Find and recover your belongings.")

        Spacer(modifier = Modifier.height(20.dp))

        Text("More features coming soon!")
    }
}
