package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.R
import kotlinx.coroutines.launch

class SmsGroupSelectionActivity : AppCompatActivity() {

    private val mainRepo = MainActivityRepository()
    private lateinit var bar: ProgressBar
    private var selectedGroup: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sms_group_selection)


        val amount = intent.getStringExtra("amount")
        val description = intent.getStringExtra("description")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bar = findViewById(R.id.progressBar)
        showGroupSelectionDialog()
    }

    private fun showGroupSelectionDialog() {
        lifecycleScope.launch {
            bar.visibility = View.VISIBLE
            val response = mainRepo.getAllMyDetailedGroups()
            bar.visibility = View.GONE

            val groups = response.getOrNull()

            if (response.isSuccess && !groups.isNullOrEmpty()) {
                val groupNames = groups.map { it.name ?: "Unnamed Group" }.toTypedArray()
                val groupIds = groups.map { it.groupID ?: "Unknown ID" }.toTypedArray()

                AlertDialog.Builder(this@SmsGroupSelectionActivity)
                    .setTitle("Select Group")
                    .setItems(groupNames) { _, which ->
                        selectedGroup = groupNames[which]
                        Toast.makeText(this@SmsGroupSelectionActivity, "Group ID: ${groupIds[which]}", Toast.LENGTH_SHORT).show()
                        // Proceed with using selectedGroup or groupIds[which]
                        bar.visibility = View.VISIBLE
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this@SmsGroupSelectionActivity, "No groups available or error fetching", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
