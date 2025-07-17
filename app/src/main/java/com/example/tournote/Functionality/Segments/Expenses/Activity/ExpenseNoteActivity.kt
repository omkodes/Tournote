package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.R
import com.example.tournote.databinding.ActivityExpenseNoteBinding

class ExpenseNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // This tells the system to draw behind system bars

        binding = ActivityExpenseNoteBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the content view using binding immediately

        // Apply window insets to the root view to push content below system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to the *root* view. This will push the entire layout down
            // below the status bar and away from the navigation bar.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            // Or, if you want the toolbar to be at the absolute top and just adjust
            // content below it, you can apply padding to a view *below* the toolbar.
            // For your current setup, applying to the root (binding.main) is the correct approach
            // because your toolbar is part of that root and needs to be positioned relative
            // to the new "safe" top.

            // Importantly, consume the insets so they don't get dispatched further to children
            // unless you explicitly want specific children to handle parts of the insets.
            insets
        }


        // Set navigation bar color (status bar color is usually handled by theme or can be set here)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        // If you want to explicitly set the status bar color, you can uncomment this:
        // window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)


        binding.btnCancel.setOnClickListener{
            finish()
        }

        binding.btnDone.setOnClickListener {
            val note = binding.txtNote.text.toString()
            val resultIntent = Intent()
            resultIntent.putExtra("note_data", note)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}