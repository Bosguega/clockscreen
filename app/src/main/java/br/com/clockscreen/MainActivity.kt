package br.com.clockscreen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import br.com.clockscreen.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScreenSaver.setOnClickListener {
            startActivity(Intent(this, ScreenSaverActivity::class.java))
        }

        binding.btnDisplayStyle.setOnClickListener {
            val preferences = getSharedPreferences("clock_settings", MODE_PRIVATE)
            val currentStyle = preferences.getString("display_style", "classic")
            val selectedIndex = if (currentStyle == "modern") 1 else 0
            val styles = arrayOf(
                getString(R.string.display_style_classic),
                getString(R.string.display_style_modern)
            )

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.display_style_title)
                .setSingleChoiceItems(styles, selectedIndex) { dialog, which ->
                    preferences.edit()
                        .putString("display_style", if (which == 1) "modern" else "classic")
                        .apply()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.btnTimeFormat.setOnClickListener {
            val preferences = getSharedPreferences("clock_settings", MODE_PRIVATE)
            val use24HourFormat = preferences.getBoolean("use_24_hour_format", true)
            val selectedIndex = if (use24HourFormat) 0 else 1
            val formats = arrayOf(
                getString(R.string.time_format_24_hour),
                getString(R.string.time_format_12_hour)
            )

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.time_format_title)
                .setSingleChoiceItems(formats, selectedIndex) { dialog, which ->
                    preferences.edit()
                        .putBoolean("use_24_hour_format", which == 0)
                        .apply()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
