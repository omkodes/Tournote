package com.example.tournote.Onboarding.Activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val viewModel: authViewModel by viewModels()
    private lateinit var phone_dialog: BottomSheetDialog
    private var phoneCode: String? = null
    private var selectedImageUri: Uri? = null
    private var isPasswordVisible = false
    private var isReEnterPasswordVisible = false

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            }
        }

    private val ImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                uri?.let {
                    selectedImageUri = it
                    binding.imgProfilePhoto.setImageURI(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        observeModel()

        binding.btnreenterpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtReEnterPass.typeface
            isReEnterPasswordVisible = !isReEnterPasswordVisible

            binding.btnreenterpasswordvisibility.setImageResource(
                if (isReEnterPasswordVisible) R.drawable.closeeye else R.drawable.openeye
            )
            binding.txtReEnterPass.inputType = if (isReEnterPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.txtReEnterPass.typeface = currentTypeface
            binding.txtReEnterPass.setSelection(binding.txtReEnterPass.text?.length ?: 0)
        }

        binding.btnpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtPass.typeface
            isPasswordVisible = !isPasswordVisible

            binding.btnpasswordvisibility.setImageResource(
                if (isPasswordVisible) R.drawable.closeeye else R.drawable.openeye
            )
            binding.txtPass.inputType = if (isPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.txtPass.typeface = currentTypeface
            binding.txtPass.setSelection(binding.txtPass.text?.length ?: 0)
        }

        binding.txtLogIn.setOnClickListener {
            redirectToActivity(LogInActivity::class.java)
        }

        val spinner2 = findViewById<Spinner>(R.id.cmbcountrycode)
        val countryCodes = resources.getStringArray(R.array.country_codes)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, countryCodes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val codeOnly = getItem(position)?.split(" →")?.get(0) ?: ""
                (view as TextView).text = codeOnly
                return view
            }
        }
        spinner2.adapter = adapter

        binding.imgProfilePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"
            }
            ImagePickerLauncher.launch(intent)
        }

        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                val selectedCode = selectedItem.split(" →")[0].trim()
                phoneCode = selectedCode
                Log.d("Spinner", "User selected: $selectedItem | Code: $selectedCode")
                // Now you can use selectedCode as needed (e.g. +91)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.d("Spinner", "Nothing selected")
                phoneCode = null

            }
        }

        binding.btnGoogle.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            googleSignInClient.signOut()
            val intent = googleSignInClient.signInIntent
            launcher.launch(intent)
        }

        binding.btnSignUp.setOnClickListener {
            val mail = binding.txtEmail.text.toString()
            val name = binding.txtName.text.toString()
            val phone = binding.txtPhone.text.toString()
            val pass = binding.txtPass.text.toString()
            val cnf_pass = binding.txtReEnterPass.text.toString()

            if (validateInputs(mail, pass, cnf_pass, name, phone)) {
                if (selectedImageUri != null) {
                    viewModel.uploadImageToCloudinary(selectedImageUri!!, this)
                    viewModel.imageUrl.observe(this) { url ->
                        if (!url.isNullOrBlank()) {
                            val phoneNumber = "$phoneCode$phone"
                            viewModel.cus_signup(mail, pass, name, phoneNumber,url)
                        }
                    }
                } else {
                    viewModel.cus_signup(mail, pass, name, phone,"null")
                }
            }

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateInputs(email: String, password: String, cnfPass: String, name: String, phone: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            binding.txtEmail.error = "Email is required"
            isValid = false
        }
        if (name.isEmpty()) {
            binding.txtName.error = "Name is required"
            isValid = false
        }
        if (phone.isEmpty()) {
            binding.txtPhone.error = "Phone is required"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.txtPass.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.txtPass.error = "Password should be at least 6 characters"
            isValid = false
        } else if (cnfPass.length < 6) {
            binding.txtReEnterPass.error = "Password should be at least 6 characters"
            isValid = false
        } else if (cnfPass != password) {
            Toast.makeText(this, "Both password should be matched.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (phoneCode==null) {
            Toast.makeText(this, "Please select a country code.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (phone.length < 10) {
            binding.txtPhone.error = "Phone number should be at least 10 digits"
            isValid = false
        }

        return isValid
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    private fun observeModel() {
        viewModel.googleResponse.observe(this) { user ->
            if (user != null) {
                Log.e("error SignUpActivity", "Google Sign In successful: ${user.email}")
                val email = user.email ?: ""
                val name = user.displayName ?: ""
                val userId = user.uid
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.saveFCM(userId)
                    val snapshot = viewModel.repo.userDetailGetLogin(userId)
                    if (snapshot == null) {
                        // Network error or permission issue
                        Toast.makeText(this@SignUpActivity, "Network error or permission issue", Toast.LENGTH_SHORT).show()
                    } else if (!snapshot.exists()) {
                        // User does not exist
                        phone_Dialog(name, email, userId)
                    } else {
                        // User exists, proceed
                        Toast.makeText(this@SignUpActivity, "User already exists", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        viewModel.loginError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.isLoading.observe(this) { loading ->
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

    fun phone_Dialog(name: String, email: String, userId: String) {
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
                viewModel.user_dataTO_firebase(userId, name, email, fullPhone, "null")
            }

        }



    }

}
