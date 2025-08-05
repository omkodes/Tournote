package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R

class ForgotPassword : AppCompatActivity() {
    private val viewModel : authViewModel by viewModels()
    lateinit var progressBar: android.widget.ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val editView  = findViewById<android.widget.EditText>(R.id.txtEmail)
        val button = findViewById<RelativeLayout>(R.id.btnSendResetLink)
        val backToLogin = findViewById<android.widget.TextView>(R.id.txtBackToLogin)
         progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        button.setOnClickListener {
            val email = editView.text.toString()
            if (email.isNotEmpty()) {
                viewModel.forgot(email)
            }else{
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            }
        }

        observeModel()

        backToLogin.setOnClickListener {
            val intent = Intent(this, LogInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }



    }

    fun observeModel(){
        viewModel.loginError.observe(this)
        { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this)
        { loading ->
            // Show/hide progress bar based on `loading`
            progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
        }

        viewModel.toastmsg.observe(this) {
            it?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        viewModel.navigateToLogin.observe(this) {
            if (it) {
                val intent = Intent(this, LogInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                viewModel.clearNavigationLogin()
            }
        }

        viewModel.navigateToMain.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                val intent = Intent(this, GroupSelectorActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // 👈 kills the hosting activity so it's not in the back stack
                viewModel.clearRoleLoadingMain()
            }
        }
    }

}