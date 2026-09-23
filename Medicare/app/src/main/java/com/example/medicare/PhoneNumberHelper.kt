package com.example.medicare

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import java.util.Locale

object PhoneNumberHelper {
    
    data class Country(
        val code: String,       // ISO Country Code, e.g. "IN"
        val name: String,       // Display Name, e.g. "🇮🇳 India"
        val callingCode: String, // Calling Prefix, e.g. "+91"
        val nationalDigits: Int // National number digits limit (e.g. 10 for India)
    )

    val supportedCountries = listOf(
        Country("IN", "🇮🇳 India", "+91", 10),
        Country("US", "🇺🇸 United States", "+1", 10),
        Country("GB", "🇬🇧 United Kingdom", "+44", 10),
        Country("CA", "🇨🇦 Canada", "+1", 10),
        Country("AU", "🇦🇺 Australia", "+61", 9),
        Country("DE", "🇩🇪 Germany", "+49", 11),
        Country("SG", "🇸🇬 Singapore", "+65", 8)
    )

    private val phoneUtil: PhoneNumberUtil by lazy {
        PhoneNumberUtil.getInstance()
    }

    /**
     * Retrieves the country mapping by its region code (e.g., "IN").
     * Defaults to India if not found or empty.
     */
    fun getCountryByCode(code: String?): Country {
        if (code.isNullOrEmpty()) return supportedCountries[0]
        return supportedCountries.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: supportedCountries[0]
    }

    /**
     * Retrieves the country mapping by its calling code (e.g., "+91").
     * Defaults to India if not found.
     */
    fun getCountryByCallingCode(callingCode: String?): Country {
        if (callingCode.isNullOrEmpty()) return supportedCountries[0]
        val cleanCode = if (callingCode.startsWith("+")) callingCode else "+$callingCode"
        return supportedCountries.firstOrNull { it.callingCode == cleanCode }
            ?: supportedCountries[0]
    }

    /**
     * Validates a national phone number according to the selected country rules.
     */
    fun isValidNumber(nationalNumber: String, countryCode: String): Boolean {
        if (nationalNumber.isEmpty()) return false
        return try {
            val proto: PhoneNumber = phoneUtil.parse(nationalNumber, countryCode)
            phoneUtil.isValidNumberForRegion(proto, countryCode)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Formats the national phone number.
     */
    fun formatNationalNumber(nationalNumber: String, countryCode: String): String {
        if (nationalNumber.isEmpty()) return nationalNumber
        return try {
            val proto: PhoneNumber = phoneUtil.parse(nationalNumber, countryCode)
            phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
        } catch (e: Exception) {
            nationalNumber
        }
    }

    /**
     * Normalizes a national phone number to E.164 format.
     */
    fun getNormalizedNumber(nationalNumber: String, countryCode: String): String? {
        if (nationalNumber.isEmpty()) return null
        return try {
            val proto: PhoneNumber = phoneUtil.parse(nationalNumber, countryCode)
            if (phoneUtil.isValidNumberForRegion(proto, countryCode)) {
                phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses an E.164 international phone number back into its Country and National Number.
     * Useful for backward-compatible rendering of legacy profile values.
     */
    fun parseInternationalNumber(fullNumber: String): Pair<Country, String>? {
        if (fullNumber.isEmpty() || fullNumber == "Not Specified") return null
        return try {
            val clean = if (fullNumber.startsWith("+")) fullNumber else "+$fullNumber"
            val proto: PhoneNumber = phoneUtil.parse(clean, null)
            val regionCode = phoneUtil.getRegionCodeForNumber(proto) ?: return null
            val country = supportedCountries.firstOrNull { it.code.equals(regionCode, ignoreCase = true) }
                ?: Country(regionCode, Locale("", regionCode).displayCountry, "+${proto.countryCode}", 10)
            val national = proto.nationalNumber.toString()
            Pair(country, national)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Show AlertDialog with supported country options.
     */
    fun showCountryPickerDialog(context: android.content.Context, onSelected: (Country) -> Unit) {
        val names = supportedCountries.map { "${it.name} (${it.callingCode})" }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Select Country")
            .setItems(names.toTypedArray()) { _, which ->
                onSelected(supportedCountries[which])
            }
            .show()
    }

    /**
     * Applies a strict country-specific length filter and digits-only filter to an EditText.
     * Physically prevents entering more than the allowed national digits for the country (e.g. 10 for India).
     * If the current content exceeds the new limit, it truncates the excess digits.
     */
    fun applyCountryInputFilter(editText: android.widget.EditText, country: Country) {
        val digitFilter = android.text.InputFilter { source, start, end, _, _, _ ->
            for (i in start until end) {
                if (!Character.isDigit(source[i])) {
                    return@InputFilter ""
                }
            }
            null
        }
        val lengthFilter = android.text.InputFilter.LengthFilter(country.nationalDigits)
        editText.filters = arrayOf(digitFilter, lengthFilter)

        // Truncate if existing text exceeds country's digit limit
        val currentDigits = editText.text.toString().filter { it.isDigit() }
        if (currentDigits.length > country.nationalDigits) {
            val truncated = currentDigits.substring(0, country.nationalDigits)
            editText.setText(truncated)
            editText.setSelection(truncated.length)
        }
        editText.hint = "enter ${country.nationalDigits}-digit number"
    }

    /**
     * Checks if the clean national digits match the exact required digit count for the country.
     */
    fun hasExactNationalDigits(number: String, country: Country): Boolean {
        val digits = number.filter { it.isDigit() }
        return digits.length == country.nationalDigits
    }
}

