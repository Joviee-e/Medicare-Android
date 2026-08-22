package com.example.medicare

object PhoneNumberUtils {
    fun normalizeIndianPhoneNumber(rawNumber: String): String {
        // 1. Remove all non-digit characters (except '+' at the start)
        val cleaned = rawNumber.replace(Regex("[^0-9+]"), "")
        
        // 2. If it starts with '+', keep it
        if (cleaned.startsWith("+")) {
            val plus = "+"
            val digits = cleaned.substring(1).replace("+", "")
            return plus + digits
        }
        
        // 3. If it starts with '91' and has length 12
        if (cleaned.startsWith("91") && cleaned.length == 12) {
            return "+$cleaned"
        }
        
        // 4. If it has 10 digits
        if (cleaned.length == 10) {
            return "+91$cleaned"
        }
        
        // Fallback: ensure + if starts with 91, or return cleaned
        return if (cleaned.startsWith("91")) "+$cleaned" else cleaned
    }
}
