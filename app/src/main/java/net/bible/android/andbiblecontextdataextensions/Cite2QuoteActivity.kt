package net.bible.android.andbiblecontextdataextensions

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class Cite2QuoteActivity : ComponentActivity() {
    private lateinit var getPassageLauncher: ActivityResultLauncher<Intent>

    private var isReadOnly: Boolean = false
    private var cite: String? = null
    // Define the preferences datastore
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsDataStore = SettingsDataStore(this)

        getPassageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data?.action == INTENT_PUT_PASSAGE) {
                val citation = data.getStringExtra("citation")
                val rawQuote = data.getStringExtra("quote")
                val format = data.getStringExtra("format")
                if (citation == null || rawQuote == null || format == null) {
                    val message = "No quote found"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    Log.w(TAG, message)
                    finish() // Finish if data is invalid
                } else {
                    // Launch a coroutine to process the data with preferences from DataStore
                    lifecycleScope.launch {
                        val quote: String
                        if (format == "application/xml+osis") {

                            quote = processXml(
                                rawQuote,
                                linebreaks = settingsDataStore.linebreaks.first(),
                                pilcrows = settingsDataStore.pilcrows.first(),
                                verseNumbers = settingsDataStore.verseNumbers.first(),
                                chevrons = settingsDataStore.chevrons.first(),
                                brackets = settingsDataStore.brackets.first()
                            )
                        } else {
                            quote = rawQuote
                        }
                        val out = quote + ' ' + citation

                        // if not readonly, replace cite with quote, otherwise put quote in clipboard
                        if (!isReadOnly) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, out)
                            setResult(RESULT_OK, resultIntent)
                        } else {
                            val clipboard =
                                getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Quote", out)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this@Cite2QuoteActivity, "$cite copied to clipboard", Toast.LENGTH_SHORT)
                                .show()
                        }
                        // Finish the activity after handling the result.
                        finish()
                    }
                }
            } else {
                Log.w(TAG, "No text selected, this should be impossible?")
                // Finish the activity in this case as well.
                finish()
            }
        }

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

            Log.i(TAG, "Cite2Quote: got cite '$selectedText', isReadOnly: $isReadOnly")

            if (!selectedText.isNullOrEmpty()) {
                cite = selectedText.toString()
                val myBiblePassageIntent = Intent(INTENT_GET_PASSAGE).apply {
                    component = ComponentName(AB_PACKAGE, AB_CLASS_GET_PASSAGE)
                    putExtra("search_string", selectedText)
                }
                try {
                    getPassageLauncher.launch(myBiblePassageIntent)
                    Log.i(TAG, "startActivity called for $INTENT_GET_PASSAGE, $AB_PACKAGE, $AB_CLASS_GET_PASSAGE")
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "AndBible app not found. Cannot launch search.", e)
                    Toast.makeText(
                        this,
                        "AndBible app not found. Please install it to use this feature.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    fun processXml(
        xmlInput: String,
        linebreaks: Boolean,
        pilcrows: Boolean,
        verseNumbers: Boolean,
        chevrons: Boolean,
        brackets: Boolean
    ): String {
        //Log.d(TAG, "processXml called with xmlInput: $xmlInput, linebreaks: $linebreaks, pilcrows: $pilcrows, verseNumbers: $verseNumbers, chevrons: $chevrons")
        val factory = XmlPullParserFactory.newInstance()
        val xpp = factory.newPullParser()
        xpp.setInput(StringReader("<root>$xmlInput</root>")) // Wrapped in root for valid XML
        var eventType = xpp.eventType
        var verseNumber: String? = null
        var pilcrow = false
        var dropText = false
        var spaceBeforeNextText = false
        var para = StringBuilder()
        val paras = mutableListOf(para)

        val addText = { text: String ->
            if(!dropText) {
                if(spaceBeforeNextText){
                    if(para.isNotEmpty()){
                        para.append(' ')
                    }
                    spaceBeforeNextText = false
                }
                if(verseNumber != null){
                    para.append(verseNumber)
                    verseNumber = null
                }
                if(pilcrow && pilcrows){
                    para.append("¶")
                    pilcrow = false
                }
                para.append(text)
            }
        }

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (xpp.name) {
                        "verse" -> {
                            // Extract the verse number from osisID (e.g., "John.1.6" -> "6")
                            val osisId = xpp.getAttributeValue(null, "osisID")
                            osisId?.split('.')?.lastOrNull()?.let {
                                verseNumber = it
                            }
                        }
                        "milestone" -> {
                            // Check for the paragraph marker
                            if (xpp.getAttributeValue(null, "marker") == "¶") {
                                para = StringBuilder()
                                paras.add(para)
                                pilcrow = true
                            }
                        }
                        "title" -> {
                            dropText = true
                        }
                        "chapter" -> {
                            dropText = true
                        } // this was never part of the original text
                        "transChange" -> {
                            if (brackets) {
                                addText("[")
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (xpp.name) {
                        "verse" -> {
                            if(para.length != 0){
                                spaceBeforeNextText = true
                            }
                        }
                        "title" -> {
                            dropText = false
                        }
                        "chapter" -> {
                            dropText = false
                        }
                        "transChange" -> {
                            if (brackets) {
                                addText("]")
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    addText(xpp.text)
                }
            }
            eventType = xpp.next()
        }

        return paras.joinToString("\n") { (if (chevrons) "\n> " else "\n  ") + it.toString() }
    }

    companion object {
        private const val TAG = "Cite2QuoteActivity"
        private const val INTENT_GET_PASSAGE = "net.bible.android.action.GET_PASSAGE"
        private const val INTENT_PUT_PASSAGE = "net.bible.android.action.PUT_PASSAGE"
        private const val AB_PACKAGE = "net.bible.android.activity"
        private const val AB_CLASS_GET_PASSAGE = "net.bible.android.activity.GetPassageActivity"
    }
}
