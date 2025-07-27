package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.tournote.Onboarding.Adapter.OnboardingPagerAdapter
import com.example.tournote.R
import com.example.tournote.databinding.ActivityOnboardingBinding


class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding : ActivityOnboardingBinding

    private lateinit var viewPager : ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewPager=binding.viewPager
        viewPager.adapter= OnboardingPagerAdapter(this)
        viewPager.currentItem=0

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Triggered when a new page becomes selected
                when(position){
                    0->{
                        binding.imgStatus.setImageResource(R.drawable.screen1active)
                        binding.btnChangePage.visibility=View.VISIBLE
                        binding.btnGoToSignup.visibility=View.GONE
                    }
                    1->{
                        binding.imgStatus.setImageResource(R.drawable.screen2active)
                        binding.btnChangePage.visibility=View.VISIBLE
                        binding.btnGoToSignup.visibility=View.GONE
                    }
                    2->{
                        binding.imgStatus.setImageResource(R.drawable.screen3active)
                        binding.btnChangePage.visibility=View.VISIBLE
                        binding.btnGoToSignup.visibility=View.GONE
                    }
                    3->{
                        binding.imgStatus.setImageResource(R.drawable.screen4active)
                        binding.btnChangePage.visibility=View.VISIBLE
                        binding.btnGoToSignup.visibility=View.GONE
                    }
                    4->{
                        binding.imgStatus.setImageResource(R.drawable.screen5active)
                        binding.btnChangePage.visibility=View.GONE
                        binding.btnGoToSignup.visibility=View.VISIBLE
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


        binding.btnChangePage.setOnClickListener {
            if(viewPager.currentItem<4){
                viewPager.currentItem++
            }
        }

        binding.btnGoToSignin.setOnClickListener{
            val intent = Intent(this, LogInActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnGoToSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}