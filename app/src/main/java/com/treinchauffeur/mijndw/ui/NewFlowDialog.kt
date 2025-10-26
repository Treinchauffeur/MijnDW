package com.treinchauffeur.mijndw.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.util.Duration
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.treinchauffeur.mijndw.BuildConfig
import com.treinchauffeur.mijndw.MainActivity
import com.treinchauffeur.mijndw.R
import com.treinchauffeur.mijndw.io.ShiftsFileReader
import com.treinchauffeur.mijndw.misc.Settings
import com.treinchauffeur.mijndw.misc.Utils
import com.treinchauffeur.mijndw.obj.Shift
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Date
import java.util.Objects
import java.util.concurrent.TimeUnit
import androidx.core.graphics.drawable.toDrawable

open class NewFlowDialog(context: Context, protected val activity: MainActivity?) : Dialog(context),
    View.OnClickListener {
    protected var allViews: ViewGroup? = null
    private val prefs: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.sharedPrefs),
        Context.MODE_PRIVATE
    )
    private var previousWeek = false
    private var currentWeek = false
    private var nextWeek = true
    private var errorMessageContent = ""
    private lateinit var analytics: FirebaseAnalytics

    var urlTextField: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setContentView(R.layout.dialog_new_flow)
        analytics = FirebaseAnalytics.getInstance(context)

        allViews = findViewById<ViewGroup>(R.id.dialogCardView)
        urlTextField = findViewById<EditText>(R.id.urlEditText)

        for (v in allViews!!.getTouchables()) {
            if (v is Button && v.isClickable()) {
                v.setOnClickListener(this)
                if (v is MaterialButton) continue
                else if (v is MaterialCheckBox) continue
            }
        }

        val previousWeekCheckBox = findViewById<CheckBox>(R.id.checkBoxPrevious)
        val currentWeekCheckBox = findViewById<CheckBox>(R.id.checkBoxCurrent)
        val nextWeekCheckBox = findViewById<CheckBox>(R.id.checkBoxNext)

        previousWeekCheckBox.isChecked = prefs.getBoolean("previousWeek", false)
        currentWeekCheckBox.isChecked = prefs.getBoolean("currentWeek", false)
        nextWeekCheckBox.isChecked = prefs.getBoolean("nextWeek", true)
        previousWeek = previousWeekCheckBox.isChecked
        currentWeek = currentWeekCheckBox.isChecked
        nextWeek = nextWeekCheckBox.isChecked

        urlTextField!!.setText(prefs.getString("icsUrl", ""))

        previousWeekCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            prefs.edit().putBoolean("previousWeek", b).apply()
            previousWeek = b
        })
        currentWeekCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            prefs.edit().putBoolean("currentWeek", b).apply()
            currentWeek = b
        })
        nextWeekCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            prefs.edit().putBoolean("nextWeek", b).apply()
            nextWeek = b
        })
    }

    @SuppressLint("DiscouragedApi") //I REALLY don't want to have to type all those resource IDs..
    override fun onClick(view: View?) {
        if (view is MaterialButton) {
            if (urlTextField!!.text.toString().isEmpty()) {
                urlTextField!!.error = "Niet ingevuld!"
                return
            } else if (!urlTextField!!.text.toString().contains("donderdagseweek.ns.nl")) {
                urlTextField!!.error = "Geen DW URL!"
                return
            } else {
                prefs.edit().putString("icsUrl", urlTextField!!.text.toString().replace("webcal://", "https://")).apply()
                startFlow(urlTextField!!.text.toString().replace("webcal://", "https://"), context)
            }
            dismiss()
        }
    }

    companion object {
        const val TAG: String = "NewFlowDialog"
    }

    /**
     * Starts a new flow with the given webcal URL.
     */
    fun startFlow(url: String, context: Context) {
        val client = OkHttpClient()
        url.replace(
            "webcal://",
            "https://"
        ) //Can only handle http(s) requests. The response will be the same.

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("HTTP_REQUEST", "Failed to fetch URL", e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("HTTP_REQUEST", "Unexpected code $response")
                        errorMessageContent = response.toString()
                        showErrorDialog(context)
                        return
                    }

                    val responseBody = it.body?.string() ?: ""
                    errorMessageContent = responseBody
                    icsToDw(responseBody, context)
                }
            }
        })
    }

    fun icsToDw(ics: String, context: Context) {
        try {
            val dwPrevious = ArrayList<Shift>()
            val dwCurrent = ArrayList<Shift>()
            val dwNext = ArrayList<Shift>()
            val dwMerged = arrayListOf<Shift>()
            val calendar = Biweekly.parse(ics).first()
            val exportCalendar: ICalendar = ICalendar()
            val prefs: SharedPreferences = context.getSharedPreferences(
                context.getString(R.string.sharedPrefs),
                Context.MODE_PRIVATE
            )
            val fullDaysOnly = prefs.getBoolean("fullDaysOnly", false);
            val displayProfession = prefs.getBoolean("displayProfession", true);
            val toIgnore = prefs.getString("toIgnore", "")?.split(",") ?: emptyList()
            val replace = prefs.getString("replacement", "")?.split(",") ?: emptyList()
            val prefix: String? = prefs.getString("prefix", "");
            val daysOff = prefs.getBoolean("daysOff", false)
            val onlyVTA = prefs.getBoolean("onlyVTA", false)

            val params = Bundle()
            params.putString("newflow_attempt", "1")
            analytics.logEvent("newflow_attempt", params)

            for (event in calendar.events) {
                val shift = Shift()
                var summary: String = event.summary.value
                summary = summary.replace(" - ", " ")
                val shiftString = summary.split(" ")
                //Log.d(TAG, "icsToDw: $summary") //Print all for debug purposes

                if (!shiftString.isNotEmpty()) continue
                if (ShiftsFileReader.isDayOff(summary)) {
                    if (!daysOff) continue
                    if (!onlyVTA && ShiftsFileReader.isVTAComponent(summary)) continue
                    shift.shiftNumber = summary
                    continue
                }

                if(summary.isEmpty()) {
                    continue
                }

                if(ShiftsFileReader.isSpecial(summary)) {
                    shift.shiftNumber = summary
                    shift.location = ""
                    shift.profession = ""
                } else {
                    shift.profession = Utils.capitaliseFirstLetter(shiftString[0])
                    shift.location = Utils.capitaliseFirstLetter(shiftString[1])
                    shift.shiftNumber = shiftString[2]
                }

                shift.startMillis = event.dateStart.value.time
                shift.endMillis = event.dateEnd.value.time
                var diff: Long = shift.endMillis - shift.startMillis
                var diffMinutes: Long = TimeUnit.MILLISECONDS.toMinutes(diff);
                var diffHours: Long = TimeUnit.MILLISECONDS.toHours(diff);
                var minutes: Long = diffMinutes - (diffHours * 60);
                shift.lengthHours = diffHours.toInt()
                shift.lengthMinutes = minutes.toInt()

                shift.description = event.description.value

                val week: Int = Utils.weekNumberFromTimestamp(event.dateStart.value.time)
                val currentWeek = Utils.getCurrentWeekNumber()
                when (week) {
                    currentWeek -> dwCurrent.add(shift)
                    currentWeek + 1 -> dwNext.add(shift)
                    else -> dwPrevious.add(shift)
                }
            }
            Log.d(TAG, "prev: ${dwPrevious.size}")
            Log.d(TAG, "current: ${dwCurrent.size}")
            Log.d(TAG, "next: ${dwNext.size}")

            if (previousWeek) dwMerged.addAll(dwPrevious)
            if (currentWeek) dwMerged.addAll(dwCurrent)
            if (nextWeek) dwMerged.addAll(dwNext)

            for (shift in dwMerged) {
                val event: VEvent = VEvent()

                event.setSummary("")

                Log.d(TAG, "${shift.shiftNumber}")
                if(!ShiftsFileReader.isSpecial(shift.shiftNumber)) {
                    event.summary.value += "$prefix "

                    if (displayProfession) event.summary.value += shift.profession + " "
                }

                var shouldSkip = false;
                for (s in toIgnore) {
                    if (shift.shiftNumber == s.split(";")[0]) {
                        shouldSkip = true
                        break;
                    }
                }
                if (shouldSkip) continue

                var replaced = false;
                for (s in replace) {
                    if (shift.shiftNumber == s.split(";")[0]) {
                        shift.shiftNumber = s.split(";")[1];
                        replaced = true
                        break;
                    }
                }
                if (!replaced && !ShiftsFileReader.isSpecial(shift.shiftNumber)) {
                    event.summary.value += shift.location + " "
                    event.summary.value += shift.shiftNumber + " "
                } else event.summary.value = shift.neatShiftNumber + ""

                event.setDescription("")
                event.description.value = shift.description

                var start: Date = Date(shift.startMillis)
                if (fullDaysOnly) event.setDateStart(Utils.atStartOfDay(start))
                else if (daysOff && !onlyVTA && ShiftsFileReader.isDayOff(shift.shiftNumber))
                    event.setDateStart(Utils.atStartOfDay(start))
                else if (daysOff && onlyVTA && ShiftsFileReader.isVTAComponent(shift.shiftNumber))
                    event.setDateStart(Utils.atStartOfDay(start))
                else event.setDateStart(start)

                val duration = if (fullDaysOnly) {
                    Duration.Builder().days(1).build()
                } else {
                    Duration.Builder()
                        .hours(shift.lengthHours)
                        .minutes(shift.lengthMinutes)
                        .build()
                }
                event.setDuration(duration)

                if (ShiftsFileReader.isDayOff(shift.shiftNumber)) {
                    var duration2: Duration = Duration.Builder().days(1).build()
                    event.setDuration(duration2);
                }

                exportCalendar.addEvent(event)

                var ics = Biweekly.write(exportCalendar).go()

                var file: File = File(
                    Objects.requireNonNull(activity?.getExternalFilesDir(null))!!
                        .getPath() + "/converted.ics"
                ); // Null -> temp location
                var out: FileOutputStream = FileOutputStream(file)
                var writer: OutputStreamWriter = OutputStreamWriter(out)

                writer.write(ics)
                writer.close()
                out.close()

                params.putString("newflow_success", "1")
                analytics.logEvent("newflow_success", params)

                var intent: Intent = Intent(Intent.ACTION_VIEW);
                var uri1: Uri =
                    FileProvider.getUriForFile(context, context.packageName + ".provider", file);
                intent.setDataAndType(uri1, "text/calendar");
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }
        } catch (e: Exception) {
            Log.e(TAG, "icsToDw: ", e)
            showErrorDialog(context)
        }
    }


    /**
     * Shows the user a dialog with an error related to either reading or processing the file.
     *
     * @param context The context.
     * @param reason Where something went wrong.
     */
    private fun showErrorDialog(context: Context) { // Removed static, can be top-level or in a companion object
        var view: ConstraintLayout? = activity?.findViewById<ConstraintLayout>(R.id.parentView)
        view?.post({
            val params = Bundle()
            params.putString("failed_newflow", "1")
            analytics.logEvent("failed_newflow", params)

            var bodyText = """
            Er is een fout opgetreden tijdens het inlezen van jouw DW.
            Zou je deze willen emailen naar de ontwikkelaar voor analyse zodat deze de app kan verbeteren? :)
            
            Je kan eventueel deze mail zelf nog bewerken om je personeelsnummer en andere gevoelige gegevens aan te passen of te verwijderen.
            """.trimIndent() // Using trimIndent for cleaner multiline strings

            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialErrorDialog)
                .setTitle("Fout opgetreden")
                .setIcon(R.drawable.baseline_error_outline_24)
                .setMessage(bodyText)
                .setNegativeButton("DW E-MAILEN") { dialogInterface, _ -> // _ for unused 'i' parameter
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // Correct way to set data for mailto
                        putExtra(Intent.EXTRA_EMAIL, Settings.DEV_EMAIL)
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "Fout tijdens inlezen van DW"
                        )
                        putExtra(
                            Intent.EXTRA_TEXT,
                            """
                        Mijn DW versie ${BuildConfig.VERSION_NAME}
                        $errorMessageContent
                        
                        -------- Mocht je nog iets kwijt willen, graag onder deze lijn --------
                        
                        """.trimIndent()
                        )
                    }
                    context.startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."))
                }
                .setPositiveButton("NEE") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()
        })
    }
}
