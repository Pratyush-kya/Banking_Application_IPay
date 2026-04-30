package com.example.bankingapp.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bankingapp.R
import com.example.bankingapp.databinding.ActivityOverviewBinding
import android.content.Intent
import com.example.bankingapp.ui.adapter.TransactionAdapter
import com.example.bankingapp.ui.viewmodel.BankingViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.util.Locale

class OverviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOverviewBinding
    private val viewModel: BankingViewModel by viewModels()
    private val transactionAdapter = TransactionAdapter { transaction ->
        val intent = Intent(this, TransactionDetailActivity::class.java).apply {
            putExtra("transaction_name", transaction.name)
            putExtra("transaction_amount", transaction.amount)
            putExtra("transaction_date", transaction.date)
            putExtra("transaction_type", transaction.type)
            putExtra("transaction_category", transaction.category)
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_image", transaction.imageUrl)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("OverviewActivity", "onCreate: Activity started")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        binding.backBtn.setOnClickListener { finish() }
        binding.notifBtn.setOnClickListener { Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show() }
        binding.seeMoreTrans.setOnClickListener { Toast.makeText(this, "All transactions coming soon", Toast.LENGTH_SHORT).show() }
        
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@OverviewActivity)
            adapter = transactionAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userData.collect { user ->
                        if (user == null) return@collect
                        binding.totalBalance.text = user.formattedBalance
                        binding.dateText.text = user.overviewDate
                        setupPieChart(user.statistics)
                    }
                }

                launch {
                    viewModel.transactions.collect { transactions ->
                        transactionAdapter.submitList(transactions)
                        updateWeeklySpending(transactions)
                    }
                }
            }
        }
    }

    private fun updateWeeklySpending(transactions: List<com.example.bankingapp.domain.model.Transaction>) {
        val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val spendingByDay = FloatArray(7) { 0f }
        
        val calendar = java.util.Calendar.getInstance()
        val currentWeek = calendar.get(java.util.Calendar.WEEK_OF_YEAR)

        transactions.forEach { tx ->
            if (tx.amountString.startsWith("-")) {
                calendar.timeInMillis = tx.timestamp
                if (calendar.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek) {
                    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                    // Calendar.MONDAY is 2, SUNDAY is 1
                    val index = when(dayOfWeek) {
                        java.util.Calendar.MONDAY -> 0
                        java.util.Calendar.TUESDAY -> 1
                        java.util.Calendar.WEDNESDAY -> 2
                        java.util.Calendar.THURSDAY -> 3
                        java.util.Calendar.FRIDAY -> 4
                        java.util.Calendar.SATURDAY -> 5
                        java.util.Calendar.SUNDAY -> 6
                        else -> 0
                    }
                    val amountVal = tx.amountString.replace("-", "").replace("$", "").replace("₹", "").replace(",", "").trim().toFloatOrNull() ?: 0f
                    spendingByDay[index] += amountVal
                }
            }
        }

        val entries = spendingByDay.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Weekly Spending")
        dataSet.color = getColor(R.color.purple)
        dataSet.setCircleColor(getColor(R.color.purple))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = getColor(R.color.purple)
        dataSet.fillAlpha = 50

        val lineData = LineData(dataSet)
        binding.lineChart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return days.getOrNull(value.toInt()) ?: ""
                    }
                }
                granularity = 1f
            }
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupPieChart(statistics: Map<String, Any>) {
        val categories = listOf("Food", "Bills", "Transfer", "Shopping")
        val entries = categories.map { category ->
            PieEntry(parseStatValue(statistics[category]), category)
        }.filter { it.value > 0 }

        Log.d("OverviewActivity", "setupPieChart: Entries count: ${entries.size}")

        if (entries.isEmpty()) {
            binding.pieChart.visibility = View.GONE
            Log.w("OverviewActivity", "setupPieChart: No category data, hiding chart")
            return
        }

        binding.pieChart.visibility = View.VISIBLE
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            getColor(R.color.blue),
            getColor(R.color.pink),
            getColor(R.color.green),
            getColor(R.color.purple)
        )
        dataSet.sliceSpace = 3f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            centerText = "Spending"
            setCenterTextSize(18f)
            setHoleColor(Color.TRANSPARENT)
            animateXY(1000, 1000)
            invalidate()
        }
    }

    private fun parseStatValue(valueObj: Any?): Float {
        return when(valueObj) {
            is Number -> valueObj.toFloat()
            is String -> valueObj.replace("$", "").replace("₹", "").replace(",", "").trim().toFloatOrNull() ?: 0f
            else -> 0f
        }
    }
}
