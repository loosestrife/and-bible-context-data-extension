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

    // <div><title type='x-gen'>Matthew 21</title><div osisID='Matt.21.0' type='x-gen'><chapter chapterTitle='CHAPTER 21.' osisID='Matt.21' sID='gen1119'></chapter> <title type='chapter'>CHAPTER 21.</title> </div><verse osisID='Matt.21.1' verseOrdinal='24820'><w lemma='strong:G2532 lemma.TR:και' morph='robinson:CONJ' src='1'>And</w> <w lemma='strong:G3753 lemma.TR:οτε' morph='robinson:ADV' src='2'>when</w> <w lemma='strong:G1448 lemma.TR:ηγγισαν' morph='robinson:V-AAI-3P' src='3'>they drew nigh</w> <w lemma='strong:G1519 lemma.TR:εις' morph='robinson:PREP' src='4'>unto</w> <w lemma='strong:G2414 lemma.TR:ιεροσολυμα' morph='robinson:N-APN' src='5'>Jerusalem</w>, <w lemma='strong:G2532 lemma.TR:και' morph='robinson:CONJ' src='6'>and</w> <w lemma='strong:G2064 lemma.TR:ηλθον' morph='robinson:V-2AAI-3P' src='7'>were come</w> <w lemma='strong:G1519 lemma.TR:εις' morph='robinson:PREP' src='8'>to</w> <w lemma='strong:G967 lemma.TR:βηθφαγη' morph='robinson:N-PRI' src='9'>Bethphage</w>, <w lemma='strong:G4314 lemma.TR:προς' morph='robinson:PREP' src='10'>unto</w> <w lemma='strong:G3588 strong:G3735 lemma.TR:το lemma.TR:ορος' morph='robinson:T-ASN robinson:N-ASN' src='11 12'>the mount</w> <w lemma='strong:G3588 strong:G1636 lemma.TR:των lemma.TR:ελαιων' morph='robinson:T-GPF robinson:N-GPF' src='13 14'>of Olives</w>, <w lemma='strong:G5119 lemma.TR:τοτε' morph='robinson:ADV' src='15'>then</w> <w lemma='strong:G649 lemma.TR:απεστειλεν' morph='robinson:V-AAI-3S' src='18'>sent</w> <w lemma='strong:G3588 strong:G2424 lemma.TR:ο lemma.TR:ιησους' morph='robinson:T-NSM robinson:N-NSM' src='16 17'>Jesus</w> <w lemma='strong:G1417 lemma.TR:δυο' morph='robinson:A-NUI' src='19'>two</w> <w lemma='strong:G3101 lemma.TR:μαθητας' morph='robinson:N-APM' src='20'>disciples</w>,</verse><verse osisID='Matt.21.2' verseOrdinal='24821'><w lemma='strong:G3588 lemma.TR:την' morph='robinson:T-ASF' src='7'></w><w lemma='strong:G3004 lemma.TR:λεγων' morph='robinson:V-PAP-NSM' src='1'>Saying</w> <w lemma='strong:G846 lemma.TR:αυτοις' morph='robinson:P-DPM' src='2'>unto them</w>, <q marker='' who='Jesus'><w lemma='strong:G4198 lemma.TR:πορευθητε' morph='robinson:V-AOM-2P' src='3'>Go</w> <w lemma='strong:G1519 lemma.TR:εις' morph='robinson:PREP' src='4'>into</w> <w lemma='strong:G3588 strong:G2968 lemma.TR:την lemma.TR:κωμην' morph='robinson:T-ASF robinson:N-ASF' src='5 6'>the village</w> <w lemma='strong:G561 lemma.TR:απεναντι' morph='robinson:ADV' src='8'>over against</w> <w lemma='strong:G4771 lemma.TR:υμων' morph='robinson:P-2GP' src='9'>you</w>, <w lemma='strong:G2532 lemma.TR:και' morph='robinson:CONJ' src='10'>and</w> <w lemma='strong:G2112 lemma.TR:ευθεως' morph='robinson:ADV' src='11'>straightway</w> <w lemma='strong:G2147 lemma.TR:ευρησετε' morph='robinson:V-FAI-2P' src='12'>ye shall find</w> <w lemma='strong:G3688 lemma.TR:ονον' morph='robinson:N-ASF' src='13'>an ass</w> <w lemma='strong:G1210 lemma.TR:δεδεμενην' morph='robinson:V-RPP-ASF' src='14'>tied</w>, <w lemma='strong:G2532 lemma.TR:και' morph='robinson:CONJ' src='15'>and</w> <w lemma='strong:G4454 lemma.TR:πωλον' morph='robinson:N-ASM' src='16'>a colt</w> <w lemma='strong:G3326 lemma.TR:μετ' morph='robinson:PREP' src='17'>with</w> <w lemma='strong:G846 lemma.TR:αυτης' morph='robinson:P-GSF' src='18'>her</w>: <w lemma='strong:G3089 lemma.TR:λυσαντες' morph='robinson:V-AAP-NPM' src='19'>loose</w> <transChange type='added'>them</transChange>, <w lemma='strong:G71 lemma.TR:αγαγετε' morph='robinson:V-2AAM-2P' src='20'>and bring</w> <transChange type='added'>them</transChange> <w lemma='strong:G1473 lemma.TR:μοι' morph='robinson:P-1DS' src='21'>unto me</w>.</q></verse><verse osisID='Matt.21.3' verseOrdinal='24822'><q marker='' who='Jesus'><w lemma='strong:G2532 lemma.TR:και' morph='robinson:CONJ' src='1'>And</w> <w lemma='
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
        val linebreak = if (chevrons) "\n> " else "\n"

        val result = StringBuilder()
        var eventType = xpp.eventType
        val verseBuf = StringBuilder()
        var verseHasTextNow = false
        var dropText = false

        if(chevrons){
            result.append(">")
        }
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (xpp.name) {
                        "verse" -> {
                            // Extract the verse number from osisID (e.g., "John.1.6" -> "6")
                            val osisId = xpp.getAttributeValue(null, "osisID")
                            osisId?.split('.')?.lastOrNull()?.let {
                                if (verseNumbers) {
                                    verseBuf.append(it)
                                }
                            }
                        }
                        "milestone" -> {
                            // Check for the paragraph marker
                            if (xpp.getAttributeValue(null, "marker") == "¶") {
                                if (linebreaks) {
                                    if (!verseHasTextNow) {
                                        verseBuf.insert(0, linebreak)
                                    } else {
                                        verseBuf.append(linebreak)
                                    }
                                }
                                if (pilcrows) {
                                    verseBuf.append("¶")
                                }
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
                                verseBuf.append('[')
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (xpp.name) {
                        "verse" -> {
                            if(result.length != 0 && !verseBuf[0].isWhitespace()){
                                result.append(' ')
                            }
                            result.append(verseBuf.toString())
                            verseBuf.clear()
                            verseHasTextNow = false
                        }
                        "title" -> {
                            dropText = false
                        }
                        "chapter" -> {
                            dropText = false
                        }
                        "transChange" -> {
                            if (brackets) {
                                verseBuf.append(']')
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    // This captures text inside <w> tags and stray punctuation
                    if(!dropText) {
                        verseBuf.append(xpp.text)
                        verseHasTextNow = true
                    }
                }
            }
            eventType = xpp.next()
        }
        result.append(verseBuf.toString())
        return result.toString();
    }

    companion object {
        private const val TAG = "Cite2QuoteActivity"
        private const val INTENT_GET_PASSAGE = "net.bible.android.action.GET_PASSAGE"
        private const val INTENT_PUT_PASSAGE = "net.bible.android.action.PUT_PASSAGE"
        private const val AB_PACKAGE = "net.bible.android.activity"
        private const val AB_CLASS_GET_PASSAGE = "net.bible.android.activity.GetPassageActivity"
    }
}
