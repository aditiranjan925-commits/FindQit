
package com.aditi.lostfoundindia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun ChatInboxScreen(
    onOpenChat: (String, String) -> Unit
) {
    val repository = ChatRepository
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var chats by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val registration = repository.observeMyChats(
            onData = { data ->
                chats = data
            },
            onError = { message ->
                error = message
            }
        )

        onDispose {
            registration.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Chats",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color.Red
            )
        }

        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No chats yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = chats,
                    key = { it.id }
                ) { room ->

                    val unread =
                        room.unreadCounts[currentUid] ?: 0L

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenChat(
                                    room.id,
                                    room.itemTitle
                                )
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = room.itemTitle,
                                    fontWeight = if (unread > 0)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal,
                                    fontSize = 17.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = room.lastMessage.ifEmpty {
                                        "Start chatting"
                                    },
                                    maxLines = 1,
                                    color = if (unread > 0)
                                        Color.Black
                                    else
                                        Color.Gray,
                                    fontWeight = if (unread > 0)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal
                                )
                            }

                            if (unread > 0) {
                                Surface(
                                    color = Color(0xFF1976D2),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (unread > 99)
                                            "99+"
                                        else
                                            unread.toString(),
                                        color = Color.White,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 5.dp
                                        ),
                                        fontWeight = FontWeight.Bold
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
fun PrivateChatScreen(
    chatId: String,
    itemTitle: String,
    onBack: () -> Unit
) {
    val repository = ChatRepository

    var messages by remember {
        mutableStateOf<List<ChatMessage>>(emptyList>())
    }

    var messageText by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf("")
    }

    val currentUid =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        repository.markChatRead(chatId)

        val registration = repository.observeMessages(
            chatId = chatId,
            onData = { data ->
                messages = data
                repository.markChatRead(chatId)
            },
            onError = { message ->
                error = message
            }
        )

        onDispose {
            registration.remove()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1976D2))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                fontSize = 30.sp,
                color = Color.White,
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = itemTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->

                val isMine =
                    message.senderId == currentUid

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine)
                        Arrangement.End
                    else
                        Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine)
                            Color(0xFFDCF8C6)
                        else
                            Color(0xFFECECEC),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(
                            max = 280.dp
                        )
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Type a message")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val cleanMessage =
                        messageText.trim()

                    if (cleanMessage.isNotEmpty()) {
                        repository.sendMessage(
                            chatId = chatId,
                            message = cleanMessage,
                            onError = { message ->
                                error = message
                            }
                        )

                        messageText = ""
                    }
                },
                enabled = messageText.trim().isNotEmpty()
            ) {
                Text("Send")
            }
        }
    }
}
