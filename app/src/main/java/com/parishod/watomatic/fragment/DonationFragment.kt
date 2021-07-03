package com.parishod.watomatic.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlin.math.roundToInt

class DonationFragment: Fragment() {
    private val url = "https://home.deekshith.in/tmp/donations.txt"
    private lateinit var fragmentView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentView = inflater.inflate(R.layout.fragment_donations, container, false)

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
                    showError(resources.getString(R.string.donations_data_fetch_error))
                }
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                showError(t.localizedMessage)
            }
        })
    }

    private fun showError(message: String?) {
        fragmentView.progress.visibility = View.GONE

        fragmentView.errorText.visibility = View.VISIBLE
        fragmentView.errorText.text = message?:resources.getString(R.string.donations_data_fetch_error)
    }

    @SuppressLint("SetTextI18n")
    private fun parseResponse(response: String) {
        fragmentView.progress.visibility = View.GONE

        val receivedStartIndex : Int = response.indexOf("total-received-pct = ", 0, false)
        val totalGoalStartIndex : Int = response.indexOf("total-goal = ", receivedStartIndex, false)
        val unitStartIndex : Int = response.indexOf("unit = ", totalGoalStartIndex, false)
        val donationReceived : Float = response.subSequence(receivedStartIndex + "total-received-pct = ".length, totalGoalStartIndex).toString().toFloat()
        val totalGoal : Float = response.subSequence(totalGoalStartIndex + "total-goal = ".length, unitStartIndex).toString().toFloat()

        val percentReceived = (donationReceived * 100) / totalGoal

        fragmentView.donation_pct.text = percentReceived.roundToInt().toString() + "%"

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
        showDonationProgressData(items)
    }

    private fun showDonationProgressData(items: List<DonationProgressItem>){
        fragmentView.donationsProgressLayout.setAdapter(DonationsAdapter(items))
    }

    private fun getData() = listOf(
        DonationProgressItem(false, "0%", resources.getString(R.string.donations_goal_title_0), resources.getString(R.string.donations_goal_0)),
        DonationProgressItem(false, "20%", resources.getString(R.string.donations_goal_title_20), resources.getString(R.string.donations_goal_20)),
        DonationProgressItem(false, "30%", resources.getString(R.string.donations_goal_title_30),resources.getString(R.string.donations_goal_30)),
        DonationProgressItem(false, "70%", resources.getString(R.string.donations_goal_title_70),resources.getString(R.string.donations_goal_70)),
        DonationProgressItem(false, "100%", resources.getString(R.string.donations_goal_title_100), resources.getString(R.string.donations_goal_100))
    )
}