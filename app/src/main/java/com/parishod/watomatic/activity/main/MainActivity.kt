package com.parishod.watomatic.activity.main

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

class MainActivity : BaseActivity() {
    private lateinit var viewModel: SwipeToKillAppDetectViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle(R.string.app_name)

        viewModel = ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

    }
}