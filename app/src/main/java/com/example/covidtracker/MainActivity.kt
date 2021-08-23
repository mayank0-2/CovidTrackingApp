package com.example.covidtracker

import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
private const val ALL_STATES = "All (Nationwide)"

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
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
                setUpEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(tag,"Updated graph with national data")
                //Update graph with national data
                updateDisplayWithData(nationalDailyData)


            }

        })

        //fetching states data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(tag, "OnFailure $t")
            }
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(tag,"No states data recieved form api")
                    return
                }
                perStatesDailyData = statesData.reversed().groupBy { it.state }
                Log.i(tag,"update graph with states data")
                //TODO : update spiner with states names
                updateSpinnerWithStateData(perStatesDailyData.keys)
            }


        })
     }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        //Add state list as data source for the spinner
        binding.spinnerSelect.attachDataSource(stateAbbreviationList)
        binding.spinnerSelect.setOnSpinnerItemSelectedListener{parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStatesDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }
    }

    private fun setUpEventListeners() {
        // Add a listner
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        //Respond to the radio button click
        binding.radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedID ->
            adapter.daysAgo = when(checkedID) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        binding.radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId->
            when(checkedId) {
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateDisplayMetric(metric: Metric){
        //update the color of the chart
        val colorRes = when(metric) {
            Metric.POSITIVE -> R.color.colorPositive
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        binding.sparkView.lineColor = colorInt
        binding.tvMetricLabel.setTextColor(colorInt)
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        //reset the no of date shown in the tvdatelabel
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        //Create spark adapter with the data
        adapter = CovidSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter
        //Set default radio buttons

        binding.radioButtonPositive.isChecked = true
        binding.radioButtonMax.isChecked = true


        //Display metric for recent date
        updateDisplayMetric(Metric.POSITIVE)

    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when(adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}