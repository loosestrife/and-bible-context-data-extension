package net.bible.android.andbiblecontextdataextensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.bible.android.andbiblecontextdataextensions.databinding.ActivityAbcdeConfiguratorBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityAbcdeConfiguratorBinding
    private lateinit var settingsDataStore: SettingsDataStore

    private val cite2QuoteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val quote = data?.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
                if (quote != null) {
                    Log.i(TAG, "Received quote: $quote")
                    binding.quoteTextview.text = quote
                } else {
                    val message = "Quote not found in result"
                    Log.w(TAG, message)
                    binding.quoteTextview.text = message
                }
            } else {
                val message = "Cite2QuoteActivity was canceled or failed."
                Log.w(TAG, message)
                binding.quoteTextview.text = message
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        binding = ActivityAbcdeConfiguratorBinding.inflate(layoutInflater)
        settingsDataStore = SettingsDataStore(this)
        setContentView(binding.root)
        Log.d(TAG, "Layout inflated and content view set.")

        setupPreferenceSwitches()
        binding.quoteTextview.movementMethod = ScrollingMovementMethod()
        displayAndBibleVersion()

        binding.buttonCiteToQuote.setOnClickListener {
            val query = binding.editTextQuery.text.toString()
            Log.i(TAG, "Cite2Quote button clicked with query: $query")
            if (query.isNotEmpty()) {
                launchCite2QuoteActivity(query)
            }
        }

        binding.buttonBibleSearch.setOnClickListener {
            val query = binding.editTextQuery.text.toString()
            Log.i(TAG, "Search button clicked with query: $query")
            if (query.isNotEmpty()) {
                launchBibleSearchActivity(query)
            }
        }
    }

    private fun setupPreferenceSwitches() {
        val preferenceBindings = mapOf(
            binding.switchLinebreaks to (settingsDataStore.linebreaks to settingsDataStore::saveLinebreaks),
            binding.switchPilcrows to (settingsDataStore.pilcrows to settingsDataStore::savePilcrows),
            binding.switchVerseNumbers to (settingsDataStore.verseNumbers to settingsDataStore::saveVerseNumbers),
            binding.switchChevrons to (settingsDataStore.chevrons to settingsDataStore::saveChevrons),
            binding.switchBrackets to (settingsDataStore.brackets to settingsDataStore::saveBrackets)
        )

        preferenceBindings.forEach { (switch, preference) ->
            val (readFlow, saveFunction) = preference
            lifecycleScope.launch {
                readFlow.collect { isChecked ->
                    switch.isChecked = isChecked
                }
            }
            switch.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    saveFunction(isChecked)
                }
            }
        }
    }

    private fun displayAndBibleVersion() {
        try {
            val andBiblePackageName = "net.bible.android.activity"
            val packageInfo = packageManager.getPackageInfo(andBiblePackageName, 0)
            val andBibleVersion = packageInfo.versionName
            binding.andbibleVersionTextview.text = "AndBible Version: $andBibleVersion"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.andbibleVersionTextview.text = "AndBible Version: Not Found"
            Log.w(TAG, "AndBible app not found when checking version", e)
        }
    }

    private fun launchCite2QuoteActivity(query: String) {
        val intent = Intent(this, Cite2QuoteActivity::class.java).apply {
            action = Intent.ACTION_PROCESS_TEXT
            putExtra(Intent.EXTRA_PROCESS_TEXT, query)
        }
        cite2QuoteLauncher.launch(intent)
    }

    private fun launchBibleSearchActivity(query: String) {
        val intent = Intent(this, BibleSearchActivity::class.java).apply {
            action = Intent.ACTION_PROCESS_TEXT
            putExtra(Intent.EXTRA_PROCESS_TEXT, query)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "abcde_configurator"
    }
}
