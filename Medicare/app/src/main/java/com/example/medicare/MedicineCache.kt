package com.example.medicare

import android.content.Context
import android.util.Log
import com.example.medicare.api.ApiMedicine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MedicineCache {
    private val cache = mutableMapOf<String, ApiMedicine>()

    private fun getPrefsName(context: Context): String {
        val sessionManager = SessionManager(context)
        val userId = sessionManager.getUserId() ?: "default"
        return "medicare_medicine_cache_$userId"
    }

    fun loadFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE)
        val json = prefs.getString("cached_medicines", null)
        if (json != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<ApiMedicine>>() {}.type
                val list: List<ApiMedicine> = gson.fromJson(json, type)
                cache.clear()
                for (med in list) {
                    cache[med.id] = med
                }
                Log.d("MedicineCache", "Loaded ${cache.size} medicines from local preferences.")
            } catch (e: Exception) {
                Log.e("MedicineCache", "Error deserializing medicine cache. Clearing corrupted cache.", e)
                prefs.edit().remove("cached_medicines").apply()
                cache.clear()
            }
        }
    }

    private fun saveToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE)
        try {
            val gson = Gson()
            val json = gson.toJson(cache.values.toList())
            prefs.edit().putString("cached_medicines", json).apply()
            Log.d("MedicineCache", "Saved cache containing ${cache.size} medicines.")
        } catch (e: Exception) {
            Log.e("MedicineCache", "Error saving medicines to preferences", e)
        }
    }

    fun updateCache(context: Context, medicines: List<ApiMedicine>) {
        cache.clear()
        for (med in medicines) {
            cache[med.id] = med
        }
        saveToPrefs(context)
    }

    fun getMedicine(id: String): ApiMedicine? {
        return cache[id]
    }
    
    fun getMedicines(): List<ApiMedicine> {
        return cache.values.toList()
    }

    fun removeMedicine(context: Context, id: String) {
        cache.remove(id)
        saveToPrefs(context)
    }

    fun clearMemoryCache() {
        cache.clear()
    }
}
