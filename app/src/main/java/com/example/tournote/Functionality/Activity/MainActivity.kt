package com.example.tournote.Functionality.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.tournote.Functionality.Adapter.FunctionalityPagerAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.Object.SocketManager
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.example.tournote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private val viewModel: MainActivityViewModel by viewModels()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.loadGroup()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.loadGroupValidity(GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.isGroupValid ?: true)

        //val groupId = intent.getStringExtra("GROUP_ID")
        viewModel.isGroupValid.observe(this){valid->
            if(valid?:true){
                binding.btnTrackGroupMates.visibility=View.VISIBLE
            }else{
                binding.btnTrackGroupMates.visibility=View.GONE
            }
        }

        // Check and request location permission
        checkLocationPermission()

        window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        viewPager = binding.viewPager
        viewPager.adapter = FunctionalityPagerAdapter(this)
        viewPager.currentItem = 0

        handleKeyboardVisibility()

        binding.viewPager.setPageTransformer(null)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //super.onPageSelected(position)
                // Triggered when a new page becomes selected
                when (position) {
                    0 -> {
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplanneractive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)

                        window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.white)
                        binding.bottomButtons.visibility = View.VISIBLE

                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                    }
                    1 -> {
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)

                        window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.green_theme_Light_taskbar)
                        binding.bottomButtons.visibility = View.VISIBLE

                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                    }
                    2 -> {
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expenseactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)

                        window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.green_theme_Light_taskbar)
                        binding.bottomButtons.visibility = View.VISIBLE

                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                    }
                    3 -> {
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)

                        window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.green_theme_Light_taskbar)
                        binding.bottomButtons.visibility = View.GONE

                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                    }
                    4->{
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsactive)

                        window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.white)
                        binding.bottomButtons.visibility = View.VISIBLE

                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
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

        binding.btnSmartRoute.setOnClickListener {
            viewPager.setCurrentItem(0, false)
        }
        binding.btnMemories.setOnClickListener {
            viewPager.setCurrentItem(1, false)
        }
        binding.btnExpenses.setOnClickListener {
            viewPager.setCurrentItem(2, false)
        }
        binding.btnChats.setOnClickListener {
            viewPager.setCurrentItem(3, false)
        }
        binding.btnTrackGroupMates.setOnClickListener {
            viewPager.setCurrentItem(4, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                    // Permission granted, fragments can now access location
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                    // Permission denied, inform user
                }
            }
        }
    }

    // Method to check if location permission is granted (can be called by fragments)
    fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    private fun handleKeyboardVisibility() {
        val rootView = findViewById<View>(R.id.main)
        val bottomNav = findViewById<View>(R.id.bottomButtons)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is open
                bottomNav.visibility = View.GONE
            } else {
                // Keyboard is closed
                if (viewPager.currentItem != 3) {
                    bottomNav.visibility = View.VISIBLE
                } else {
                    bottomNav.visibility = View.GONE
                }
            }
        }
    }

    override fun onBackPressed() {
        if(viewPager.currentItem==3){
            viewPager.setCurrentItem(0, false)
        }else{
            redirectToActivity(GroupSelectorActivity::class.java)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("ChatDebug", "socket disconnected")
        SocketManager.disconnect()
    }



}