package com.example.routes

import com.example.room.MemberAlreadyExistsException
import com.example.room.RoomController
import com.example.session.ChatSession
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.chatSocket(roomController: RoomController) {
    webSocket("/chat-socket") {
        val session = call.sessions.get<ChatSession>()
        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
            return@webSocket
        }
        try {// we need to join the user to the hat room
            roomController.onJoin(
                username = session.username,
                sessionId = session.sessionId,
                socket = this
            )
            //here we consume the incoming messages from the client and send them to targeted room

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) { // just checking if the frame is of type text
                    roomController.sendMessage(
                        senderUsername = session.username,
                        message = frame.readText()
                    )
                }
            }
        } catch (e: MemberAlreadyExistsException) {
            call.respond(HttpStatusCode.Conflict)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Here we must try to disconnect the session where there there was a failure or a success
            roomController.tryDisconnect(session.username)
        }
    }
}

fun Route.getAllMessages(roomController: RoomController) {
    get("/messages") {
        call.respond(
            HttpStatusCode.OK,
            roomController.getAllMessages()
        )
    }
}