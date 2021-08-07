package com.example.covidtracker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.covidtracker.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val url = "https://covidtracking.com/api/v1/"
private const val tag = "Main activity"

class MainActivity : AppCompatActivity() {
    private lateinit var perStatesDailyData: Map<String,List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1,TimeUnit.MINUTES)
            .readTimeout(1,TimeUnit.MINUTES)
            .writeTimeout(1,TimeUnit.MINUTES)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidService = retrofit.create(CovidService :: class.java)

        // fetch national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(tag,"OnFailure $t")
            }

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(tag,"Did not recieve data from the api")
                    return
                }
                nationalDailyData = nationalData.reversed()
                Log.i(tag,"Updated graph with national data")
                //TOdo : Update graph with national data
                updateDisplayWithData(nationalDailyData)


            }

        })

        //fetching states data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.i(tag, "OnFailure $t")
            }
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val statesData = response.body()
                if (statesData == null) {
                    Log.e(tag,"No states data recieved form api")
                    return
                }
                perStatesDailyData = statesData.reversed().groupBy { it.state }
                Log.i(tag,"update graph with states data")
                //TODO : update spiner with states names
            }


        })
     }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        //Todo : Create spark adapter with the data
        val adapter = CovidSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter
        // Todo : Set default radio buttons

        binding.radioButtonMax.isChecked = true
        binding.radioButtonPositive.isChecked = true


        // Todo : Display metric for recent date
        updateInfoForDate(dailyData.last())

    }

    private fun updateInfoForDate(covidData: CovidData) {
        binding.tvMetricLabel.text = NumberFormat.getInstance().format(covidData.positiveIncrease)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}