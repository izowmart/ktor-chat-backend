package com.example.room

import com.example.data.MessageDataSource
import com.example.data.model.Message
import io.ktor.http.cio.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RoomController(
    private val messageDataSource: MessageDataSource
) {
    // Prevent multi-threading issues and ensures that data is stored concurrently
    private val members = ConcurrentHashMap<String, Member>()

    fun onJoin(
        username: String,
        sessionId: String,
        socket: WebSocketSession
    ) {
        if (members.containsKey(username)) {
            throw MemberAlreadyExistsException()
        }
        // if member is not in the members room, then add him via that username
        members[username] = Member(
            username = username,
            sessionId = sessionId,
            socket = socket
        )
    }

    suspend fun sendMessage(senderUsername: String, message:String){
        members.values.forEach { member ->
            val messageEntity = Message(
                text = message,
                username = senderUsername,
                timestamp = System.currentTimeMillis()
            )
            // insert it into db
            messageDataSource.insertMessage(messageEntity)

            // send the message to user via websockets.We normally send the message inform of frames (string bytes.)
            val parsedMessage = Json.encodeToString(messageEntity)
            member.socket.send(Frame.Text(parsedMessage))
        }
    }

    suspend fun getAllMessages(): List<Message>{
        return messageDataSource.getAllMessages()
    }

    suspend fun tryDisconnect(username: String){
        members[username]?.socket?.close() //We close the socket associated with that username
        if (members.containsKey(username)){
            members.remove(username)
        }
    }
}