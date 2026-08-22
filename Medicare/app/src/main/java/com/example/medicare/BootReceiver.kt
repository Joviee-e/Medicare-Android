package com.example.medicare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medicare.api.RetrofitClient
import com.example.medicare.api.GetMedicinesResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot completed. Rescheduling all active alarms...")
            RetrofitClient.getApiService(context).getMedicines()
                .enqueue(object : Callback<GetMedicinesResponse> {
                    override fun onResponse(
                        call: Call<GetMedicinesResponse>,
                        response: Response<GetMedicinesResponse>
                    ) {
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            for (med in body.medicines) {
                                AlarmScheduler.scheduleAlarms(context, med)
                            }
                            Log.d("BootReceiver", "Rescheduled alarms for all active medicines successfully.")
                        }
                    }

                    override fun onFailure(call: Call<GetMedicinesResponse>, t: Throwable) {
                        Log.e("BootReceiver", "Failed to load medicines on boot: ${t.message}")
                    }
                })
        }
    }
}
