package com.danl.incovpn.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.danl.incovpn.R
import com.danl.incovpn.databinding.SettingsActivityBinding
import com.danl.viewbindinghelper.ViewBindingActivity

class SettingsActivity : ViewBindingActivity<SettingsActivityBinding>() {

    override fun onBindingCreated(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.settings.id, SettingsFragment())
                .commit()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}