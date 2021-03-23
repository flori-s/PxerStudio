package com.benny.pxerstudio.activity

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import com.benny.pxerstudio.R

import de.psdev.licensesdialog.LicensesDialog
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20
import de.psdev.licensesdialog.licenses.MITLicense
import de.psdev.licensesdialog.model.Notice
import de.psdev.licensesdialog.model.Notices

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val aboutCreator = findViewById<TextView>(R.id.about_creator)
        aboutCreator.text = Html.fromHtml(getString(R.string.created_by_bennykok))
        aboutCreator.movementMethod = LinkMovementMethod.getInstance()

        val aboutMoreApps = findViewById<TextView>(R.id.about_moreApps)
        aboutMoreApps.text = Html.fromHtml(getString(R.string.get_more_apps))
        aboutMoreApps.movementMethod = LinkMovementMethod.getInstance()

        val aboutGplus = findViewById<TextView>(R.id.about_gplus)
        aboutGplus.text = Html.fromHtml(getString(R.string.join_the_community))
        aboutGplus.movementMethod = LinkMovementMethod.getInstance()

        val libraryInfo = findViewById<TextView>(R.id.about_libinfo)
        val sb = StringBuilder()

        with(sb) {
            append(getString(R.string.brough_to_you_by))
            append("<br>")

            append("androidx.appcompat:appcompat")
            append("<br>")
            append("androidx.cardview:cardview")
            append("<br>")
            append("androidx.constraintlayout:constraintlayout")
            append("<br>")
            append("com.google.android.material:material")
            append("<br>")
            append("com.afollestad.material-dialogs:core")
            append("<br>")
            append("com.afollestad.material-dialogs:files")
            append("<br>")
            append("com.afollestad.material-dialogs:input")
            append("<br>")
            append("com.mikepenz:fastadapter")
            append("<br>")
            append("com.mikepenz:fastadapter-extensions")
            append("<br>")
            append("de.psdev.licensesdialog:licensesdialog")
            append("<br>")
            append("com.github.clans:fab")
            append("<br>")
            append("com.google.code.gson:gson")
            append("<br>")
        }

        libraryInfo.movementMethod = LinkMovementMethod.getInstance()
        libraryInfo.text = Html.fromHtml("$sb")

        val notices = Notices()
        notices.addNotice(
            Notice(
                "Material Dialogs",
                "https://github.com/afollestad/material-dialogs",
                "Copyright (c) 2014-2016 Aidan Michael Follestad",
                MITLicense()
            )
        )
        notices.addNotice(
            Notice(
                "FastAdapter",
                "https://github.com/mikepenz/FastAdapter",
                "Copyright 2021 Mike Penz",
                ApacheSoftwareLicense20()
            )
        )
        notices.addNotice(
            Notice(
                "FloatingActionButton",
                "https://github.com/Clans/FloatingActionButton",
                "Copyright 2015 Dmytro Tarianyk",
                ApacheSoftwareLicense20()
            )
        )
        notices.addNotice(
            Notice(
                "Gson",
                "https://github.com/google/gson",
                "Copyright 2008 Google Inc.",
                ApacheSoftwareLicense20()
            )
        )

        val builder = LicensesDialog.Builder(this@AboutActivity)
        builder.setIncludeOwnLicense(true)
        builder.setNotices(notices)
        builder.setTitle(getString(R.string.opensource_library))
        val dialog = builder.build()

        libraryInfo.setOnClickListener { dialog.show() }

        findViewById<ImageView>(R.id.about_app_icon).setOnClickListener { v ->
            if (v.animation == null || v.animation != null && v.animation.hasEnded())
                v.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .rotationBy(-20f)
                    .withEndAction {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .rotation(0f)
                    }
        }
    }
}
