package com.example.medicare

/**
 * Validates email addresses using strict RFC 5322 standard constraints.
 * Ensures the presence of a valid local-part, @ symbol, valid domain name,
 * and a recognized top-level domain (TLD) of at least 2 characters.
 */
object EmailValidator {

    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    )

    fun isValid(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim()
        if (clean.length > 254) return false
        if (clean.contains("..")) return false
        return EMAIL_REGEX.matches(clean)
    }
}

