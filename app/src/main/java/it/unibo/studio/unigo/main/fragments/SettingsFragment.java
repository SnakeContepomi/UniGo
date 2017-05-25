package it.unibo.studio.unigo.main.fragments;

import android.os.Bundle;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.XpPreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.unibo.studio.unigo.R;

public class SettingsFragment extends XpPreferenceFragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onCreatePreferences2(Bundle savedInstanceState, String rootKey)
    {
        // Add 'notifications' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(getPreferenceManager().getContext());
        fakeHeader.setTitle("Notifiche");
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.preferences);
    }
}