
package com.aditi.lostfoundindia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChatBlue = Color(0xFF2457C5)
private val ChatBackground = Color(0xFFF5F7FC)

@Composable
fun ChatInboxScreen(
    onOpenChat: (String, String) -> Unit
) {
    val repository = remember { ChatRepository() }

    var chats by remember {
        mutableStateOf<List<ChatRoom>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val listener: ListenerRegistration? =
            repository.observeMyChats(
                onData = {
                    chats = it
                    loading = false
                    error = ""
                },
                onError = {
                    error = it
                    loading = false
                }
            )

        onDispose {
            listener?.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
            .padding(16.dp)
    ) {
        Text(
            "My Chats",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Your private conversations",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(18.dp))

        when {
            loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ChatBlue)
                }
            }

            error.isNotBlank() -> {
                Text(error, color = Color.Red)
            }

            chats.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No chats yet", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Open a Lost or Found item and chat with its owner.",
                            color = Color.Gray
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chats, key = { it.id }) { chat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(15.dp),
                            onClick = {
                                onOpenChat(chat.id, chat.itemTitle)
                            }
                        ) {
                            Column(
                                Modifier.padding(16.dp)
                            ) {
                                Text(
                                    chat.itemTitle.ifBlank {
                                        "Item conversation"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )

                                Spacer(Modifier.height(5.dp))

                                Text(
                                    chat.lastMessage.ifBlank {
                                        "Start your conversation"
                                    },
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )

                                Spacer(Modifier.height(5.dp))

                                Text(
                                    "Tap to open conversation",
                                    color = ChatBlue,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateChatScreen(
    chatId: String,
    itemTitle: String,
    onBack: () -> Unit
) {
    val repository = remember { ChatRepository() }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var messages by remember {
        mutableStateOf<List<ChatMessage>>(emptyList())
    }

    var messageText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        val listener = repository.observeMessages(
            chatId = chatId,
            onData = {
                messages = it
                loading = false
                error = ""
            },
            onError = {
                error = it
                loading = false
            }
        )

        onDispose {
            listener.remove()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    itemTitle.ifBlank { "Private Chat" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Text(
                    "Private conversation",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = Color.Red,
                modifier = Modifier.padding(10.dp)
            )
        }

        if (loading) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChatBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val mine = message.senderId == currentUid

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (mine) {
                            Alignment.End
                        } else {
                            Alignment.Start
                        }
                    ) {
                        Surface(
                            color = if (mine) {
                                ChatBlue
                            } else {
                                Color.White
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                Modifier
                                    .widthIn(max = 280.dp)
                                    .padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    )
                            ) {
                                Text(
                                    message.text,
                                    color = if (mine) {
                                        Color.White
                                    } else {
                                        Color.Black
                                    },
                                    fontSize = 15.sp
                                )

                                Spacer(Modifier.height(5.dp))

                                Text(
                                    formatChatTime(message.timestamp),
                                    color = if (mine) {
                                        Color.White.copy(alpha = 0.75f)
                                    } else {
                                        Color.Gray
                                    },
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(
                                        Alignment.End
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = Color.White,
            shadowElevation = 5.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = {
                        if (it.length <= 2000) {
                            messageText = it
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Type a message...")
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        val text = messageText.trim()

                        if (text.isNotBlank() && !sending) {
                            sending = true
                            error = ""

                            repository.sendMessage(
                                chatId = chatId,
                                message = text,
                                onError = {
                                    error = it
                                    sending = false
                                }
                            )

                            messageText = ""
                            sending = false
                        }
                    },
                    enabled = messageText.isNotBlank() && !sending,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text("Send")
                }
            }
        }
    }
}

private fun formatChatTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    return SimpleDateFormat(
        "dd MMM, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}
