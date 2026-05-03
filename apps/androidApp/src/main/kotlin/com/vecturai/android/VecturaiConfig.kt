package com.Vectura AI.android

/**
 * Global configuration for the Android app.
 * 
 * UPDATE THIS: Replace "192.168.1.XX" with your PC's actual local IP address
 * so your phone can talk to the server running on your computer.
 */
object Vectura AIConfig {
    const val PC_IP = "10.187.102.95" // <--- UPDATED
    const val API_BASE_URL = "http://$PC_IP:8080"
}
