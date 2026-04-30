package com.example.alearning.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAthleteResults(context: Context, athlete: Individual, results: List<TestResult>, tests: Map<String, FitnessTest>) {
        val fileName = "Athlete_${athlete.lastName}_${athlete.firstName}_Export.csv"
        val header = "Test Name,Date,Score,Unit,Percentile,Classification\n"
        
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        val content = results.sortedByDescending { it.createdAt }.joinToString("\n") { result ->
            val test = tests[result.testId]
            val date = df.format(Date(result.createdAt))
            val score = result.rawScore
            val unit = test?.unit ?: ""
            val percentile = result.percentile ?: ""
            val classification = result.classification ?: ""
            
            "\"${test?.name ?: "Unknown"}\",$date,$score,\"$unit\",$percentile,$classification"
        }
        
        val csvData = header + content
        shareFile(context, fileName, csvData)
    }

    fun exportEventResults(context: Context, eventName: String, results: List<Pair<Individual, TestResult>>, tests: Map<String, FitnessTest>) {
        val fileName = "Event_${eventName.replace(" ", "_")}_Export.csv"
        val header = "Athlete Name,Test Name,Date,Score,Unit,Percentile,Classification\n"
        
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        val content = results.sortedBy { it.first.lastName }.joinToString("\n") { (athlete, result) ->
            val test = tests[result.testId]
            val date = df.format(Date(result.createdAt))
            val score = result.rawScore
            val unit = test?.unit ?: ""
            val percentile = result.percentile ?: ""
            val classification = result.classification ?: ""
            
            "\"${athlete.fullName}\",\"${test?.name ?: "Unknown"}\",$date,$score,\"$unit\",$percentile,$classification"
        }
        
        val csvData = header + content
        shareFile(context, fileName, csvData)
    }

    private fun shareFile(context: Context, fileName: String, content: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }
}
