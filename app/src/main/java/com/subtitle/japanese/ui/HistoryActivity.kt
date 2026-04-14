package com.subtitle.japanese.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.subtitle.japanese.data.AppDatabase
import com.subtitle.japanese.data.SubtitleEntry
import com.subtitle.japanese.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val dao = AppDatabase.getInstance(this).subtitleDao()

        lifecycleScope.launch {
            dao.getAll().collect { entries ->
                val adapter = SubtitleAdapter(entries)
                binding.recyclerView.adapter = adapter

                if (entries.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    inner class SubtitleAdapter(
        private val entries: List<SubtitleEntry>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<SubtitleAdapter.ViewHolder>() {

        inner class ViewHolder(
            val view: android.view.View
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val tv = android.widget.TextView(parent.context).apply {
                setPadding(24, 16, 24, 16)
                textSize = 14f
            }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            (holder.view as android.widget.TextView).text =
                "${entry.sourceText}\n→ ${entry.translatedText}"
        }

        override fun getItemCount() = entries.size
    }
}
