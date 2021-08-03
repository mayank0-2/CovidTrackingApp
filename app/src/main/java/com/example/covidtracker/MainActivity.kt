package com.example.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.internal.GsonBuildConfig
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var perStatesDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>
    val url = "https://api.covidtracking.com"
    val Tag = "Main activity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd 'T' HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val CovidService = retrofit.create(CovidService :: class.java)

        // fetch national data
        CovidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(Tag,"Did not recieve data from the api")
                    return
                }
                nationalDailyData = nationalData.reversed()
                Log.i(Tag,"Updated graph with national data")


            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(Tag,"OnFailure $t")
            }
        })

        //fetching states data
        CovidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val statesData = response.body()
                if (statesData == null) {
                    Log.e(Tag,"No states data recieved form api")
                    return
                }
                perStatesDailyData = statesData.reversed().groupBy { it.state }
                Log.i(Tag,"update graph with states data")
                //TODO : update spiner with states names
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.i(Tag, "OnFailure $t")
            }
        })
    }
}