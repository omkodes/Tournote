package com.example.tournote.Onboarding.Activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Groups.Fragment.ProfileFragment
import com.example.tournote.R
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivityLogInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LogInActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    private lateinit var binding: ActivityLogInBinding
    private lateinit var phone_dialog : BottomSheetDialog

    val repo1 = FirebaseRTDBRepository()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.e("authViewModel", "Google Sign In Result Received")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            }
        }

    private val viewModel: authViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in)

        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtPass.typeface
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.btnpasswordvisibility.setImageResource(R.drawable.closeeye)
                binding.txtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.btnpasswordvisibility.setImageResource(R.drawable.openeye)
                binding.txtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.txtPass.typeface = currentTypeface

            binding.txtPass.setSelection(binding.txtPass.text?.length ?: 0)
        }

        binding.txtSignup.setOnClickListener {
            redirectToActivity(SignUpActivity::class.java)
        }

        observeModel()

        binding.btnsignin.setOnClickListener {
            val mail = binding.txtEmail.text.toString()
            val pass = binding.txtPass.text.toString()
            if (validateInputs(mail,pass)){
                viewModel.cus_login(mail,pass)
            }
        }

        binding.btnGoogle.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            googleSignInClient.signOut()
            Log.e("authViewModel", "Google Sign In Button Clicked")
            val intent = googleSignInClient.signInIntent
            launcher.launch(intent)
        }


    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.txtEmail.error = "Email is required"
            binding.progressBar.visibility = View.GONE
            isValid = false
        }

        if (password.isEmpty()) {
            binding.txtPass.error = "Password is required"
            binding.progressBar.visibility = View.GONE
            isValid = false
        } else if (password.length < 6) {
            binding.txtPass.error = "Password should be at least 6 characters"
            isValid = false
            binding.progressBar.visibility = View.GONE
        }

        return isValid
    }

    private fun observeModel(){

        viewModel.googleResponse.observe(this) { user ->
            if (user != null) {
                Log.e("error authViewModel", "Google Sign In successful: ${user.email}")
                val email = user.email ?: ""
                val name = user.displayName ?: ""
                val userId = user.uid
                CoroutineScope(Dispatchers.Main).launch {
                    Log.d("authViewModel", "saveFCM called with userId: $userId")
                    viewModel.saveFCM(userId)
                    Log.d("authViewModel", "saveFCM call finished.")
                    val snapshot = viewModel.repo.userDetailGetLogin(userId)
                    Log.d("authViewModel", "userDetailGetLogin call finished.")
                    if (snapshot == null) {
                        // Network error or permission issue
                        Toast.makeText(this@LogInActivity, "Network error or permission issue", Toast.LENGTH_SHORT).show()
                    } else if (!snapshot.exists()) {
                        // User does not exist
                        phone_Dialog(name, email, userId)
                    } else {

                    val userResult = repo1.getUserByMailId(email)

                    Log.d("authViewModel", "getUserByMailId call finished.")

                    Log.d("authViewModel", "User fetched: ${userResult.isSuccess}")
                    userResult.onSuccess { user ->
                        GlobalClass.Me = user
                        Log.d("authViewModel", "User data set: ${GlobalClass.Me}")
                    }.onFailure {
                        Log.e("authViewModel", "Failed to fetch user: ${it.message}")
                    }
                        viewModel.isLoading.value = false
                        Toast.makeText(this@LogInActivity, "Login successful", Toast.LENGTH_SHORT)
                            .show()
                        val intent = Intent(this@LogInActivity, GroupSelectorActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }


        viewModel.loginError.observe(this)
        { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this)
        { loading ->
            // Show/hide progress bar based on `loading`
            binding.progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
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
    fun phone_Dialog(name: String, email: String, userId: String?) {
        phone_dialog = BottomSheetDialog(this)
        phone_dialog.setContentView(R.layout.bsfragment_phone)
        phone_dialog.setCanceledOnTouchOutside(true)
        phone_dialog.setCancelable(true)
        phone_dialog.show()

        var code : String?= null

        val spinner = phone_dialog.findViewById<Spinner>(R.id.cmbcountrycode)
        val countryCodes = resources.getStringArray(R.array.country_codes)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, countryCodes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val codeOnly = getItem(position)?.split(" →")?.get(0) ?: ""
                (view as TextView).text = codeOnly
                return view
            }
        }
        spinner?.adapter = adapter
        phone_dialog.setOnCancelListener {
            viewModel.isLoading.value = false
        }

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                val selectedCode = selectedItem.split(" →")[0].trim()
                code = selectedCode
                Log.d("Spinner", "User selected: $selectedItem | Code: $selectedCode")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d("Spinner", "Nothing selected")
                code = null
            }
        }

        val phoneNumber = phone_dialog.findViewById<EditText>(R.id.txtPhone)

        phone_dialog.findViewById<RelativeLayout>(R.id.btnCnfrm)?.setOnClickListener {
            val phone = phoneNumber?.text.toString().trim()

            if (phone.isEmpty()) {
                phoneNumber?.error = "Phone number is required"
            } else if (phone.length != 10) {
                phoneNumber?.error = "Phone number must be exactly 10 digits"
            } else if (code == null || code == "Select country") {
                Toast.makeText(this, "Please select a country code.", Toast.LENGTH_SHORT).show()
            } else {
                    val fullPhone = "$code$phone"
                    GlobalClass.Me = UserModel(
                        uid = userId ?: "",
                        name = name,
                        email = email,
                        phoneNumber = fullPhone,
                        profilePic = "null")
                    Log.d("authViewModel", "User data set: ${GlobalClass.Me}")

                    viewModel.user_dataTO_firebase(userId!!, name, email, fullPhone, "null")
            }

        }



    }

}