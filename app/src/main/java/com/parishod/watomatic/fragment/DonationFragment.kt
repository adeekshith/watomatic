package com.parishod.watomatic.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.parishod.watomatic.R
import com.parishod.watomatic.model.adapters.DonationsAdapter
import com.parishod.watomatic.model.data.DonationProgressItem
import com.parishod.watomatic.network.GetDonationsProgressService
import com.parishod.watomatic.network.RetrofitInstance
import kotlinx.android.synthetic.main.fragment_donations.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DonationFragment: Fragment() {
    private val url = "https://home.deekshith.in/tmp/donations.txt"
    private lateinit var fragmentView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentView = inflater.inflate(R.layout.fragment_donations, container, false)

        //Set value to default while fetching data
        fragmentView.donation_pct.text = "0%"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fragmentView.donation_pct.tooltipText = getString(R.string.current_donation_progress)
        }

        fetchDonationsProgressData()

        fragmentView.librapay.setOnClickListener {
            launchUrl("https://liberapay.com/dk")
        }
        fragmentView.paypal.setOnClickListener{
            launchUrl("https://paypal.me/deek")
        }
        return fragmentView
    }

    private fun launchUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun fetchDonationsProgressData() {
        fragmentView.progress.visibility = View.VISIBLE
        val donationsProgressService = RetrofitInstance.getRetrofitInstance()
            .create(GetDonationsProgressService::class.java)
        val call = donationsProgressService.getDonationProgress(url)
        call.enqueue(object : Callback<String?>{
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if(response.isSuccessful) {
                    response.body()?.let {
                        parseResponse(it)
                    }
                }else{
                    showDonationProgressData(0F)
                    Toast.makeText(activity, resources.getString(R.string.donations_data_fetch_error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                showDonationProgressData(0F)
                Toast.makeText(activity, resources.getString(R.string.donations_data_fetch_error), Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun parseResponse(response: String) {

        val percentReceived: Float = response.lines()
            .map { thisStr -> thisStr.split("=").map { s -> s.trim() } } // Split to KV pairs
            .find { kvp -> kvp.first() == "total-received-pct" }
            ?.last()?.toFloat() ?: 0F

        fragmentView.donation_pct.text = String.format("%.1f%%", percentReceived)

        showDonationProgressData(percentReceived)
    }

    private fun showDonationProgressData(percentReceived: Float){
        fragmentView.progress.visibility = View.GONE
        val items = getData()
        when {
            percentReceived < 15 -> {
                items.elementAt(0).isActive = true
            }
            percentReceived < 28 -> {
                items.elementAt(1).isActive = true
            }
            percentReceived < 68 -> {
                items.elementAt(2).isActive = true
            }
            percentReceived < 99 -> {
                items.elementAt(3).isActive = true
            }
            else -> {
                items.elementAt(4).isActive = true
            }
        }
        fragmentView.donationsProgressLayout.setAdapter(DonationsAdapter(items))
        fragmentView.donationsProgressLayout.visibility = View.VISIBLE
    }

    private fun getData() = listOf(
        DonationProgressItem(false, "0%", resources.getString(R.string.donations_goal_title_0), resources.getString(R.string.donations_goal_0)),
        DonationProgressItem(false, "20%", resources.getString(R.string.donations_goal_title_20), resources.getString(R.string.donations_goal_20)),
        DonationProgressItem(false, "30%", resources.getString(R.string.donations_goal_title_30),resources.getString(R.string.donations_goal_30)),
        DonationProgressItem(false, "70%", resources.getString(R.string.donations_goal_title_70),resources.getString(R.string.donations_goal_70)),
        DonationProgressItem(false, "100%", resources.getString(R.string.donations_goal_title_100), resources.getString(R.string.donations_goal_100))
    )
}