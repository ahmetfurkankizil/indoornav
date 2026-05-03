package com.vecturai.tools.admin

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

object Env {
    private val dotenv: Dotenv = try {
        dotenv {
            // Try current directory first
            directory = "." 
            ignoreIfMalformed = true
            ignoreIfMissing = false // We want to know if it's missing here to try next
        }
    } catch (e: Exception) {
        dotenv {
            // Try tools/admin-api if running from root
            directory = "./tools/admin-api"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }
    }

    fun get(key: String): String? {
        return dotenv[key] ?: System.getenv(key)
    }

    fun get(key: String, default: String): String {
        return get(key) ?: default
    }
}
