package com.example.tournote.Functionality.Segments.ChatRoom.Object

import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter

object SocketManager {

    private const val SOCKET_URL = "https://tournote-chat-backend.onrender.com"

    private lateinit var socket : Socket

    fun initSocket(){
        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            forceNew = true
            reconnection = true
        }
        socket = IO.socket(SOCKET_URL,opts)
    }

    fun connect() {
        if (!::socket.isInitialized) {
            initSocket()
        }
        if (!socket.connected()) {
            socket.connect()
        } else {
            Log.d("Socket", "Already connected 🚫")
        }
    }

    fun disconnect() {
        if (::socket.isInitialized) {
            socket.disconnect()
        }
    }

    fun on(event : String , listener: Emitter.Listener){
        if (::socket.isInitialized) socket.on(event, listener)
        else socket.connect()
    }

    fun emit(event: String, data:Any ,ack: Ack?=null) {
        if(::socket.isInitialized) {
            if (ack != null) socket.emit(event,data,ack)
            else socket.emit(event,data)
        }
    }



}