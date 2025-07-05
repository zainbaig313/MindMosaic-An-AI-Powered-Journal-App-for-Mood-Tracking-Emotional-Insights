package com.example.mindmosaic

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class Insights : Fragment() {

    private lateinit var moodChart: BarChart
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var mostCommonMoodText: TextView
    private lateinit var totalEntriesText: TextView
    private lateinit var refreshButton: Button
    private lateinit var emptyStateText: TextView
    private lateinit var summaryLayout: LinearLayout
    private lateinit var debugText: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Simple mood mapping
    private val moodScores = mapOf(
        "Happy" to 5,
        "Excited" to 4,
        "Neutral" to 3,
        "Anxious" to 2,
        "Sad" to 1
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_insights, container, false)
        initializeViews(view)
        setupChart()
        setupClickListeners()
        return view
    }

    private fun initializeViews(view: View) {
        moodChart = view.findViewById(R.id.moodChart)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        mostCommonMoodText = view.findViewById(R.id.mostCommonMoodText)
        totalEntriesText = view.findViewById(R.id.totalEntriesText)
        refreshButton = view.findViewById(R.id.refreshButton)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        summaryLayout = view.findViewById(R.id.summaryLayout)
        debugText = view.findViewById(R.id.debugText)
    }

    private fun setupClickListeners() {
        refreshButton.setOnClickListener {
            loadMoodData()
        }
    }

    private fun setupChart() {
        moodChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setPinchZoom(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.BLACK
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 5f
                textColor = Color.BLACK
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun loadMoodData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        debugText.visibility = View.VISIBLE
        debugText.text = "Debug: Starting to load data for user: $userId"

        lifecycleScope.launch {
            try {
                // Simple query - get ALL entries for this user first
                Log.d("Insights", "Querying for userId: $userId")

                val querySnapshot = db.collection("journalEntries")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                Log.d("Insights", "Query completed. Documents found: ${querySnapshot.documents.size}")
                debugText.text = "Debug: Found ${querySnapshot.documents.size} documents"

                val entries = mutableListOf<Pair<Date, String>>()

                querySnapshot.documents.forEach { document ->
                    try {
                        Log.d("Insights", "Processing document: ${document.id}")

                        val timestamp = document.getTimestamp("timestamp")?.toDate()
                        val emotion = document.getString("emotion")

                        Log.d("Insights", "Document data - timestamp: $timestamp, emotion: $emotion")

                        if (timestamp != null && !emotion.isNullOrEmpty()) {
                            entries.add(Pair(timestamp, emotion))
                            Log.d("Insights", "Added entry: $emotion on $timestamp")
                        }
                    } catch (e: Exception) {
                        Log.e("Insights", "Error processing document: ${e.message}")
                    }
                }

                Log.d("Insights", "Total valid entries: ${entries.size}")
                debugText.text = "Debug: Processed ${entries.size} valid entries"

                if (entries.isEmpty()) {
                    showEmptyState()
                } else {
                    displayResults(entries)
                }

            } catch (e: Exception) {
                Log.e("Insights", "Error loading data: ${e.message}", e)
                debugText.text = "Debug: Error - ${e.message}"
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayResults(entries: List<Pair<Date, String>>) {
        // Show summary
        summaryLayout.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE

        totalEntriesText.text = "Total Entries: ${entries.size}"

        // Find most common mood
        val moodCounts = entries.groupingBy { it.second }.eachCount()
        val mostCommon = moodCounts.maxByOrNull { it.value }?.key ?: "None"
        mostCommonMoodText.text = "Most Common: $mostCommon"

        // Create simple chart - group by day
        val dailyEntries = groupEntriesByDay(entries)
        if (dailyEntries.isNotEmpty()) {
            createChart(dailyEntries)
        }

        debugText.text = "Debug: Success! Showing ${entries.size} entries, ${dailyEntries.size} days"
    }

    private fun groupEntriesByDay(entries: List<Pair<Date, String>>): List<Pair<String, Float>> {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val dailyMoods = mutableMapOf<String, MutableList<String>>()

        entries.forEach { (date, emotion) ->
            val dayKey = dateFormat.format(date)
            dailyMoods.getOrPut(dayKey) { mutableListOf() }.add(emotion)
        }

        return dailyMoods.map { (day, moods) ->
            val avgScore = moods.mapNotNull { moodScores[it] }.average().toFloat()
            Pair(day, avgScore)
        }.sortedBy { entries.find { entry -> dateFormat.format(entry.first) == it.first }?.first }
    }

    private fun createChart(dailyData: List<Pair<String, Float>>) {
        val barEntries = dailyData.mapIndexed { index, (_, score) ->
            BarEntry(index.toFloat(), score)
        }

        val dataSet = BarDataSet(barEntries, "Mood Score").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        val barData = BarData(dataSet)

        moodChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(dailyData.map { it.first }.toTypedArray())
            animateY(800)
            invalidate()
            visibility = View.VISIBLE
        }
    }

    private fun showEmptyState() {
        moodChart.visibility = View.GONE
        summaryLayout.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
        emptyStateText.text = "📝 No journal entries found.\nTry saving a journal entry first!"
    }

    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        refreshButton.isEnabled = !show
        refreshButton.text = if (show) "Loading..." else "🔄 Load Data"
    }
}