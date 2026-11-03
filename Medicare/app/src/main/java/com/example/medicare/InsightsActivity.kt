package com.example.medicare

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.medicare.api.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InsightsActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnRefresh: ImageView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var txtErrorMsg: TextView
    private lateinit var btnRetry: Button
    private lateinit var scrollContent: View

    // Insufficient data card
    private lateinit var cardInsufficientData: MaterialCardView
    private lateinit var txtInsufficientMsg: TextView
    private lateinit var progressColdStart: ProgressBar
    private lateinit var txtColdStartCount: TextView

    // Card 1: Adherence Prediction
    private lateinit var txtRiskBadge: TextView
    private lateinit var txtHistoricalAdherence: TextView
    private lateinit var txtPredictedMissProb: TextView
    private lateinit var layoutContributingFactors: LinearLayout

    // Card 2: Routine Analysis
    private lateinit var txtArchetypeBadge: TextView
    private lateinit var txtArchetypeDescription: TextView
    private lateinit var txtMorningPct: TextView
    private lateinit var progressMorning: ProgressBar
    private lateinit var txtAfternoonPct: TextView
    private lateinit var progressAfternoon: ProgressBar
    private lateinit var txtEveningPct: TextView
    private lateinit var progressEvening: ProgressBar
    private lateinit var txtWeekendPct: TextView
    private lateinit var progressWeekend: ProgressBar
    private lateinit var layoutRoutineRecommendations: LinearLayout

    // Card 3: Reminder Optimization
    private lateinit var txtMostResponsive: TextView
    private lateinit var txtLeastResponsive: TextView
    private lateinit var txtNoSuggestions: TextView
    private lateinit var layoutSuggestionsContainer: LinearLayout
    private lateinit var txtSafetyNotice: TextView
    private lateinit var txtDisclaimer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)

        initViews()
        loadInsights()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnRefresh = findViewById(R.id.btn_refresh)
        layoutLoading = findViewById(R.id.layout_loading)
        layoutError = findViewById(R.id.layout_error)
        txtErrorMsg = findViewById(R.id.txt_error_msg)
        btnRetry = findViewById(R.id.btn_retry)
        scrollContent = findViewById(R.id.scroll_content)

        cardInsufficientData = findViewById(R.id.card_insufficient_data)
        txtInsufficientMsg = findViewById(R.id.txt_insufficient_msg)
        progressColdStart = findViewById(R.id.progress_cold_start)
        txtColdStartCount = findViewById(R.id.txt_cold_start_count)

        txtRiskBadge = findViewById(R.id.txt_risk_badge)
        txtHistoricalAdherence = findViewById(R.id.txt_historical_adherence)
        txtPredictedMissProb = findViewById(R.id.txt_predicted_miss_prob)
        layoutContributingFactors = findViewById(R.id.layout_contributing_factors)

        txtArchetypeBadge = findViewById(R.id.txt_archetype_badge)
        txtArchetypeDescription = findViewById(R.id.txt_archetype_description)
        txtMorningPct = findViewById(R.id.txt_morning_pct)
        progressMorning = findViewById(R.id.progress_morning)
        txtAfternoonPct = findViewById(R.id.txt_afternoon_pct)
        progressAfternoon = findViewById(R.id.progress_afternoon)
        txtEveningPct = findViewById(R.id.txt_evening_pct)
        progressEvening = findViewById(R.id.progress_evening)
        txtWeekendPct = findViewById(R.id.txt_weekend_pct)
        progressWeekend = findViewById(R.id.progress_weekend)
        layoutRoutineRecommendations = findViewById(R.id.layout_routine_recommendations)

        txtMostResponsive = findViewById(R.id.txt_most_responsive)
        txtLeastResponsive = findViewById(R.id.txt_least_responsive)
        txtNoSuggestions = findViewById(R.id.txt_no_suggestions)
        layoutSuggestionsContainer = findViewById(R.id.layout_suggestions_container)
        txtSafetyNotice = findViewById(R.id.txt_safety_notice)
        txtDisclaimer = findViewById(R.id.txt_disclaimer)

        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { loadInsights() }
        btnRetry.setOnClickListener { loadInsights() }
    }

    private fun loadInsights() {
        layoutLoading.visibility = View.VISIBLE
        layoutError.visibility = View.GONE
        scrollContent.visibility = View.GONE

        RetrofitClient.getApiService(this).getPredictiveInsights().enqueue(object : Callback<PredictiveInsightsResponse> {
            override fun onResponse(call: Call<PredictiveInsightsResponse>, response: Response<PredictiveInsightsResponse>) {
                layoutLoading.visibility = View.GONE
                if (response.isSuccessful && response.body()?.insights != null) {
                    val insights = response.body()!!.insights!!
                    bindInsights(insights)
                    scrollContent.visibility = View.VISIBLE
                } else {
                    showError("Failed to retrieve insights (${response.code()})")
                }
            }

            override fun onFailure(call: Call<PredictiveInsightsResponse>, t: Throwable) {
                layoutLoading.visibility = View.GONE
                showError("Network connection error: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

    private fun showError(message: String) {
        layoutError.visibility = View.VISIBLE
        scrollContent.visibility = View.GONE
        txtErrorMsg.text = message
    }

    private fun bindInsights(insights: PredictiveInsightsData) {
        // Cold-Start Check
        if (insights.status == "insufficient_data") {
            cardInsufficientData.visibility = View.VISIBLE
            val adh = insights.adherence
            val cur = adh?.currentLoggedDoses ?: 0
            val min = adh?.minimumRequiredDoses ?: 5
            progressColdStart.max = min
            progressColdStart.progress = cur
            txtColdStartCount.text = "$cur of $min doses logged"
            if (!adh?.message.isNullOrEmpty()) {
                txtInsufficientMsg.text = adh?.message
            }
        } else {
            cardInsufficientData.visibility = View.GONE
        }

        // 1. Adherence Prediction
        insights.adherence?.let { adh ->
            txtHistoricalAdherence.text = "${adh.historicalAdherencePct.toInt()}%"
            val missPct = (adh.predictedMissProbability * 100f).toInt()
            txtPredictedMissProb.text = "$missPct%"

            // Clean high-contrast risk badge styling
            val riskUpper = adh.riskLevel.uppercase()
            txtRiskBadge.text = "$riskUpper RISK"
            when (riskUpper) {
                "LOW" -> {
                    txtRiskBadge.setTextColor(Color.parseColor("#2E7D32"))
                    txtRiskBadge.setBackgroundResource(R.drawable.bg_badge_low_risk)
                }
                "MODERATE" -> {
                    txtRiskBadge.setTextColor(Color.parseColor("#E65100"))
                    txtRiskBadge.setBackgroundResource(R.drawable.bg_badge_moderate_risk)
                }
                else -> {
                    txtRiskBadge.setTextColor(Color.parseColor("#C62828"))
                    txtRiskBadge.setBackgroundResource(R.drawable.bg_badge_high_risk)
                }
            }

            // Contributing factors bullets with clean styled views
            layoutContributingFactors.removeAllViews()
            if (adh.contributingFactors.isEmpty()) {
                layoutContributingFactors.addView(createBulletView("Consistent dose timing observed across daily schedule"))
            } else {
                for (factor in adh.contributingFactors) {
                    layoutContributingFactors.addView(createBulletView(factor))
                }
            }
        }

        // 2. Routine Analysis
        insights.routine?.let { routine ->
            txtArchetypeBadge.text = routine.archetype
            txtArchetypeBadge.setTextColor(ContextCompat.getColor(this, R.color.primary))
            txtArchetypeBadge.setBackgroundResource(R.drawable.bg_badge_primary_soft)
            txtArchetypeDescription.text = routine.description ?: "Daily routine pattern based on dose adherence and timing delay."

            routine.timeOfDayBreakdown?.let { b ->
                val mVal = b.morning.toInt()
                txtMorningPct.text = "$mVal%"
                progressMorning.progress = mVal

                val aVal = b.afternoon.toInt()
                txtAfternoonPct.text = "$aVal%"
                progressAfternoon.progress = aVal

                val eVal = b.evening.toInt()
                txtEveningPct.text = "$eVal%"
                progressEvening.progress = eVal

                val wVal = b.weekend.toInt()
                txtWeekendPct.text = "$wVal%"
                progressWeekend.progress = wVal
            }

            layoutRoutineRecommendations.removeAllViews()
            if (routine.recommendations.isEmpty()) {
                layoutRoutineRecommendations.addView(createBulletView("Continue taking your medications according to your current schedule."))
            } else {
                for (rec in routine.recommendations) {
                    layoutRoutineRecommendations.addView(createBulletView(rec))
                }
            }
        }

        // 3. Smart Reminder Optimization
        insights.reminderOptimization?.let { rem ->
            txtMostResponsive.text = rem.mostResponsivePeriod
            txtLeastResponsive.text = rem.leastResponsivePeriod

            if (!rem.safetyNotice.isNullOrEmpty()) {
                txtSafetyNotice.text = rem.safetyNotice
            }

            layoutSuggestionsContainer.removeAllViews()
            if (rem.suggestions.isEmpty()) {
                txtNoSuggestions.visibility = View.VISIBLE
            } else {
                txtNoSuggestions.visibility = View.GONE
                val inflater = LayoutInflater.from(this)
                for (sug in rem.suggestions) {
                    val itemView = inflater.inflate(R.layout.item_reminder_suggestion, layoutSuggestionsContainer, false)
                    val txtMedName = itemView.findViewById<TextView>(R.id.txt_med_name)
                    val txtShiftBadge = itemView.findViewById<TextView>(R.id.txt_shift_badge)
                    val txtCurrentTime = itemView.findViewById<TextView>(R.id.txt_current_time)
                    val txtSuggestedTime = itemView.findViewById<TextView>(R.id.txt_suggested_time)
                    val txtRationale = itemView.findViewById<TextView>(R.id.txt_rationale)
                    val btnAccept = itemView.findViewById<MaterialButton>(R.id.btn_accept_suggestion)

                    txtMedName.text = sug.medicineName
                    txtShiftBadge.text = "+${sug.shiftMinutes} min"
                    txtShiftBadge.setTextColor(ContextCompat.getColor(this, R.color.primary))
                    txtShiftBadge.setBackgroundResource(R.drawable.bg_badge_primary_soft)

                    txtCurrentTime.text = sug.currentReminderTime
                    txtCurrentTime.setBackgroundResource(R.drawable.bg_time_chip_current)

                    txtSuggestedTime.text = sug.suggestedReminderTime
                    txtSuggestedTime.setBackgroundResource(R.drawable.bg_time_chip_suggested)

                    txtRationale.text = sug.rationale ?: "Recommended adjustment based on your typical dose time delay."

                    btnAccept.setOnClickListener {
                        confirmAndApplySuggestion(sug)
                    }

                    layoutSuggestionsContainer.addView(itemView)
                }
            }
        }

        if (!insights.disclaimer.isNullOrEmpty()) {
            txtDisclaimer.text = insights.disclaimer
        }
    }

    private fun confirmAndApplySuggestion(suggestion: ReminderSuggestionDto) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Reminder Adjustment")
            .setMessage("Would you like to shift your ${suggestion.medicineName} reminder from ${suggestion.currentReminderTime} to ${suggestion.suggestedReminderTime}?\n\n" +
                    "SAFETY NOTE: This will only adjust your app reminder notification alarm. Your prescribed dosage and frequency remain completely unchanged.")
            .setPositiveButton("Accept & Apply") { _, _ ->
                sendAcceptanceRequest(suggestion)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor("#004D60"))
                typeface = Typeface.DEFAULT_BOLD
                textSize = 14f
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(Color.parseColor("#5A6E78"))
                typeface = Typeface.DEFAULT_BOLD
                textSize = 14f
            }
        }

        dialog.show()
    }

    private fun sendAcceptanceRequest(suggestion: ReminderSuggestionDto) {
        val req = AcceptReminderSuggestionRequest(
            medicineId = suggestion.medicineId,
            currentReminderTime = suggestion.currentReminderTime,
            suggestedReminderTime = suggestion.suggestedReminderTime,
            confirmedByUser = true
        )

        layoutLoading.visibility = View.VISIBLE

        RetrofitClient.getApiService(this).acceptReminderSuggestion(req).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                layoutLoading.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@InsightsActivity, "Reminder updated to ${suggestion.suggestedReminderTime}", Toast.LENGTH_LONG).show()
                    loadInsights() // Refresh insights after adjustment
                } else {
                    val msg = response.body()?.message ?: "Failed to update reminder."
                    Toast.makeText(this@InsightsActivity, msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                layoutLoading.visibility = View.GONE
                Toast.makeText(this@InsightsActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createBulletView(text: String): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_insight_bullet, null, false)
        val tv = view.findViewById<TextView>(R.id.txt_bullet_content)
        tv.text = text
        return view
    }
}
