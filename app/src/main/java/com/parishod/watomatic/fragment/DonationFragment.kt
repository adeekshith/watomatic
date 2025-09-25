package com.parishod.watomatic.fragment

import android.content.ActivityNotFoundException
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
import com.parishod.watomatic.model.utils.Constants
import com.parishod.watomatic.network.GetDonationsProgressService
import com.parishod.watomatic.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.parishod.watomatic.databinding.FragmentDonationsBinding

class DonationFragment : Fragment() {
    private val url = "https://health.watomatic.app/data/donations.txt"
    private var _binding: FragmentDonationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDonationsBinding.inflate(inflater, container, false)

        //Set value to default while fetching data
        binding.donationPct.text = "0%"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.donationPct.tooltipText = getString(R.string.current_donation_progress)
        }

        fetchDonationsProgressData()

        binding.librapay.setOnClickListener {
            launchUrl(Constants.libraPayUrl)
        }
        binding.paypal.setOnClickListener {
            launchUrl(Constants.paypalUrl)
        }

        binding.bitcoin.setOnClickListener {
            launchBitcoin()
        }
        return binding.root
    }

    private fun launchBitcoin(){
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("bitcoin:${Constants.BITCOIN_ADDRESS}")

        try {
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            launchUrl("${Constants.btcUrl}${Constants.BITCOIN_ADDRESS}")
        }
    }

    private fun launchUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun fetchDonationsProgressData() {
        binding.progress.visibility = View.VISIBLE
        val donationsProgressService = RetrofitInstance.getRetrofitInstance()
                .create(GetDonationsProgressService::class.java)
        val call = donationsProgressService.getDonationProgress(url)
        call.enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        parseResponse(it)
                    }
                } else {
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

        String.format("%.1f%%", percentReceived).also { binding.donationPct.text = it }

        showDonationProgressData(percentReceived)
    }

    private fun showDonationProgressData(percentReceived: Float) {
        binding.progress.visibility = View.GONE
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
        binding.donationsProgressLayout.setAdapter(DonationsAdapter(items))
        binding.donationsProgressLayout.visibility = View.VISIBLE
    }

    private fun getData() = listOf(
            DonationProgressItem(false, "0%", resources.getString(R.string.donations_goal_title_0), resources.getString(R.string.donations_goal_0)),
            DonationProgressItem(false, "20%", resources.getString(R.string.donations_goal_title_20), resources.getString(R.string.donations_goal_20)),
            DonationProgressItem(false, "30%", resources.getString(R.string.donations_goal_title_30), resources.getString(R.string.donations_goal_30)),
            DonationProgressItem(false, "70%", resources.getString(R.string.donations_goal_title_70), resources.getString(R.string.donations_goal_70)),
            DonationProgressItem(false, "100%", resources.getString(R.string.donations_goal_title_100), resources.getString(R.string.donations_goal_100))
    )
}