package com.example.tournote.Profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.example.tournote.databinding.ActivityGroupSelectorBinding
import com.example.tournote.databinding.ActivityUpdateProfileBinding
import kotlin.getValue

class UpdateProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateProfileBinding
    private var selectedImageUri: Uri? = null
    private val viewModel1: authViewModel by viewModels()
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
        setContentView(R.layout.activity_update_profile)

        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.imgProfilePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"
            }
            ImagePickerLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.txtName.text.toString()

            if (validateInputs(name)) {
                if (selectedImageUri != null) {
                    viewModel1.uploadImageToCloudinary(selectedImageUri!!, this)
                    viewModel1.imageUrl.observe(this) { url ->
                        if (!url.isNullOrBlank()) {
                            //val phoneNumber = "$phoneCode$phone"
                            GlobalClass.Me?.profilePic = url  // <-- Update this line
                            viewModel1.update_profile(name, url)
                            finish()
                        }
                    }
                }else {
                    viewModel1.update_profile(name, (GlobalClass.Me?.profilePic)!!)
                }
            }

        }

        viewModel1.navigateToMain.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                finish()
            }
        }

        binding.btnCloseActivity.setOnClickListener {
            finish()
        }

        viewModel1.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
        }

        val profilePicUrl = GlobalClass.Me?.profilePic
        if (!profilePicUrl.isNullOrBlank() && profilePicUrl != "null") {
            Glide.with(this)
                .load(profilePicUrl)
                .placeholder(R.drawable.imageselector)
                .error(R.drawable.imageselector)
                .skipMemoryCache(true)                      // skip memory cache
                .into(binding.imgProfilePhoto)

        } else {
            binding.imgProfilePhoto.setImageResource(R.drawable.imageselector) // your default image
        }

        binding.txtName.setText(GlobalClass.Me?.name)
    }

    private fun validateInputs(name: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            binding.txtName.error = "Name is required"
            isValid = false
        }

        return isValid
    }
}