package com.example.tournote.Groups.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.tournote.Groups.Adapter.GroupSelectorActivityPagerAdapter
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2
import com.example.tournote.Onboarding.Activity.LogInActivity
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.example.tournote.databinding.ActivityGroupSelectorBinding


class GroupSelectorActivity : AppCompatActivity() {
    private val viewModel: authViewModel by viewModels()
    private lateinit var binding: ActivityGroupSelectorBinding
    private lateinit var viewPager : ViewPager2

    private val viewModel2 : GroupSelectorActivityViewModel by viewModels()
    private val viewModel3 : GroupSelectorActivityViewModel2 by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGroupSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        viewPager=binding.viewPager
        viewPager.adapter= GroupSelectorActivityPagerAdapter(this)
        viewPager.currentItem=0

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeModel()

        binding.viewPager.setPageTransformer(null)
        binding.viewPager.isUserInputEnabled=false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Triggered when a new page becomes selected
                when(position){
                    0->{
                        binding.imgHome.setImageResource(R.drawable.homeselected)
                        binding.imgAcc.setImageResource(R.drawable.accnotselected)
                    }
                    1->{
                        binding.bottomButtons.visibility= View.GONE
                    }
                    2->{
                        binding.imgHome.setImageResource(R.drawable.homenotselected)
                        binding.imgAcc.setImageResource(R.drawable.accselected)
                    }
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                // Triggered while scrolling (optional)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // SCROLL_STATE_IDLE = 0, DRAGGING = 1, SETTLING = 2
            }
        })

        binding.btnAddGrp.setOnClickListener {
            viewPager.setCurrentItem(1, false)
        }
        binding.btnHome.setOnClickListener {
            viewPager.setCurrentItem(0, false)
        }
        binding.btnAcc.setOnClickListener {
            viewPager.setCurrentItem(2, false)
        }
        /*binding.signOutButton.setOnClickListener {
            viewModel.signOut()
        }*/


    }


    private fun observeModel(){
        viewModel2.navigateToHome.observe(this) { status ->
            if (status) {
                viewModel3.refreshGroupData()

                binding.bottomButtons.visibility = View.VISIBLE
                viewPager.currentItem = 0
                //viewModel2.fetchGroupDetails() // 🛠 Force refresh when returning to HomeFragment
            }
        }


        viewModel.loginError.observe(this)
        { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel3.isLoading.observe(this)
        { loading ->
            // Show/hide progress bar based on `loading`
            binding.progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
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
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                viewModel.clearNavigationLogin()
            }
        }

        viewModel.navigateToMain.observe(this){
            if (it) {
                val intent = Intent(this, GroupSelectorActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                viewModel.clearRoleLoadingMain()
            }
        }
    }
}