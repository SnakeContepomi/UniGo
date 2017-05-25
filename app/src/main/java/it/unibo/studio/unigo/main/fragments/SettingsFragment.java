package it.unibo.studio.unigo.main.fragments;

import android.os.Bundle;
import android.support.v7.preference.XpPreferenceFragment;
import it.unibo.studio.unigo.R;

public class SettingsFragment extends XpPreferenceFragment
{
    @Override
    public void onCreatePreferences2(Bundle savedInstanceState, String rootKey)
    {
        addPreferencesFromResource(R.xml.preferences);
    }
}