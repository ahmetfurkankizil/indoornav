package com.vecturai.tools.admin

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

object Env {
    private val dotenv: Dotenv = dotenv {
        directory = "." // The server runs from tools/admin-api, so .env is right here
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    fun get(key: String): String? {
        return dotenv[key] ?: System.getenv(key)
    }

    fun get(key: String, default: String): String {
        return get(key) ?: default
    }
}
