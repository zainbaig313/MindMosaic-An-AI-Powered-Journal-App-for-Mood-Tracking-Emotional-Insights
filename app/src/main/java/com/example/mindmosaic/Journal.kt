package com.example.mindmosaic

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

class Journal : Fragment() {

    private lateinit var journalEditText: EditText
    private lateinit var saveJournalButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var emotionResultText: TextView
    private lateinit var motivationalQuoteText: TextView
    private lateinit var activitySuggestionText: TextView
    // Add a new TextView for summary (you'll need to add this to your XML layout)
    private lateinit var summaryText: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val emotionRepository = EmotionRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_journal, container, false)

        initializeViews(view)
        setupClickListeners()

        return view
    }

    private fun initializeViews(view: View) {
        journalEditText = view.findViewById(R.id.journalEditText)
        saveJournalButton = view.findViewById(R.id.saveJournalButton)
        progressBar = view.findViewById(R.id.analysisProgressBar)
        emotionResultText = view.findViewById(R.id.emotionResultText)
        motivationalQuoteText = view.findViewById(R.id.motivationalQuoteText)
        activitySuggestionText = view.findViewById(R.id.activitySuggestionText)

        // Initialize summary TextView - add this to your XML layout if not present
         summaryText = view.findViewById(R.id.summaryText)
    }

    private fun setupClickListeners() {
        saveJournalButton.setOnClickListener {
            val content = journalEditText.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Please write something...", Toast.LENGTH_SHORT).show()
            } else {
                processJournalEntry(content)
            }
        }
    }

    private fun processJournalEntry(content: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d("Journal", "Starting comprehensive journal analysis for text: ${content.take(50)}...")

                // Analyze journal using comprehensive analysis
                val analysisResult = emotionRepository.analyzeJournal(content)

                if (analysisResult.isSuccess) {
                    val analysis = analysisResult.getOrNull()
                    if (analysis != null) {
                        Log.d("Journal", "Journal analysis successful: ${analysis.emotionalTone}")

                        // Save to Firestore with comprehensive analysis data
                        saveEntryToFirestore(content, analysis)

                        // Display results
                        displayAnalysisResults(analysis)
                    } else {
                        Log.e("Journal", "Analysis result was null")
                        // Save without analysis if parsing fails
                        saveEntryToFirestore(content, null)
                        Toast.makeText(requireContext(), "Journal saved! (Analysis parsing failed)", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val error = analysisResult.exceptionOrNull()
                    Log.e("Journal", "Journal analysis failed: ${error?.message}")

                    // Save without analysis if API fails
                    saveEntryToFirestore(content, null)
                    Toast.makeText(requireContext(), "Journal saved! (AI analysis unavailable)", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("Journal", "Exception during processing: ${e.message}", e)

                // Fallback: save without analysis
                saveEntryToFirestore(content, null)
                Toast.makeText(requireContext(), "Journal saved! (Analysis failed: ${e.message})", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }


    private fun saveEntryToFirestore(content: String, analysis: JournalAnalysis?) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in to save entries.", Toast.LENGTH_SHORT).show()
            return
        }

        val journalEntry = hashMapOf(
            "userId" to userId,
            "timestamp" to Date(),
            "content" to content,
            "emotion" to (analysis?.emotionalTone ?: "unknown"),
            "summary" to (analysis?.summary ?: ""),
            "motivationalQuote" to (analysis?.motivationalQuote ?: ""),
            "suggestedActivity" to (analysis?.relaxationActivity ?: "")
        )

        Log.d("Journal", "Saving entry to Firestore...")

        db.collection("journalEntries")
            .add(journalEntry)
            .addOnSuccessListener { documentReference ->
                Log.d("Journal", "Entry saved with ID: ${documentReference.id}")

                if (analysis != null) {
                    Toast.makeText(requireContext(), "Journal saved with AI insights! 🧠✨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Journal saved!", Toast.LENGTH_SHORT).show()
                }
                clearForm()
            }
            .addOnFailureListener { exception ->
                Log.e("Journal", "Failed to save entry: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Failed to save entry: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun displayAnalysisResults(analysis: JournalAnalysis) {
        // Display comprehensive analysis results
        emotionResultText.text = "🎭 Mood: ${analysis.emotionalTone}"
        motivationalQuoteText.text = "💫 ${analysis.motivationalQuote}"
        activitySuggestionText.text = "💡 Activity: ${analysis.relaxationActivity}"

        // If you have summaryText TextView in your layout, uncomment this:
         summaryText.text = "📝 Summary: ${analysis.summary}"

        // Make results visible with animation
        emotionResultText.visibility = View.VISIBLE
        motivationalQuoteText.visibility = View.VISIBLE
        activitySuggestionText.visibility = View.VISIBLE
         summaryText.visibility = View.VISIBLE

        // Optional: Add fade-in animation
        emotionResultText.alpha = 0f
        motivationalQuoteText.alpha = 0f
        activitySuggestionText.alpha = 0f
         summaryText.alpha = 0f

        emotionResultText.animate().alpha(1f).duration = 500
        motivationalQuoteText.animate().alpha(1f).setStartDelay(200).duration = 500
        activitySuggestionText.animate().alpha(1f).setStartDelay(400).duration = 500
         summaryText.animate().alpha(1f).setStartDelay(600).duration = 500
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        saveJournalButton.isEnabled = !show
        saveJournalButton.text = if (show) "🧠 Analyzing..." else "💾 Save Entry"

        // Optional: Add some visual feedback
        if (show) {
            saveJournalButton.alpha = 0.6f
        } else {
            saveJournalButton.alpha = 1.0f
        }
    }

    private fun clearForm() {
        journalEditText.setText("")

        // Hide results after a delay to let user read them
        view?.postDelayed({
            emotionResultText.visibility = View.GONE
            motivationalQuoteText.visibility = View.GONE
            activitySuggestionText.visibility = View.GONE
             summaryText.visibility = View.GONE
        }, 10000) // Hide after 10 seconds (more time to read comprehensive analysis)
    }
}