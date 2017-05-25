package it.unibo.studio.unigo.main.fragments;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.XpPreferenceFragment;
import android.text.TextUtils;
import net.xpece.android.support.preference.ListPreference;
import net.xpece.android.support.preference.RingtonePreference;
import net.xpece.android.support.preference.SwitchPreference;
import it.unibo.studio.unigo.R;

public class SettingsFragment extends XpPreferenceFragment
{
    // Chiavi per accedere alle shared preferences
    public static String KEY_PREF_NOTIF = "pref_notification";
    public static String KEY_PREF_NOTIF_RINGTONE = "pref_notificationRingtone";
    public static String KEY_PREF_NOTIF_VIBRATION = "pref_notificationVibration";
    public static String KEY_PREF_NOTIF_PRIORITY = "pref_notificationPriority";
    public static String KEY_PREF_NOTIF_COLOR = "pref_notificationColor";

    @Override
    public void onCreatePreferences2(Bundle savedInstanceState, String rootKey)
    {
        // Caricamento delle preferences dal file xml
        addPreferencesFromResource(R.xml.preferences);

        // Avvio di un listener sui cambiamenti di ciascuna voce delle preferences
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_NOTIF));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_NOTIF_RINGTONE));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_NOTIF_VIBRATION));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_NOTIF_PRIORITY));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_NOTIF_COLOR));
    }

    // Event listener sui cambiamenti delle impostazioni
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String stringValue = value.toString();

            // Per le ListPreference viene impostato come summary la voce corrispondente al valore scelto
            if (preference instanceof ListPreference)
            {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                preference.setSummary(index >= 0 ? String.valueOf(listPreference.getEntries()[index]) : null);
            }
            // Per le Ringtone Preferences viene impostato come summary il nome del tono selezionato
            else if (preference instanceof RingtonePreference)
            {
                // Nessun tono selezionato
                if (TextUtils.isEmpty(stringValue))
                    preference.setSummary("Silenzioso");
                else
                {
                    Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

                    // Se il tono non è stato trovato, viene resettato il summary
                    if (ringtone == null)
                        preference.setSummary(null);
                    // Altrimenti viene impostato come summary il nome del tono selezionato
                    else
                        preference.setSummary(ringtone.getTitle(preference.getContext()));
                }
            }
            // Per tutte le altre Preferences (esclusa la Switch in cui non serve il sottotesto)
            // viene impostato come summary una semplice stringa
            else if (!(preference instanceof SwitchPreference))
                preference.setSummary(stringValue);

            return true;
        }
    };

    // Metodo per collegare un PreferenceChangeListener alla preference passata per parametro
    private static void bindPreferenceSummaryToValue(Preference preference)
    {
        // Avvio del listener
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger del cambiamento con il valore corrente
        // Lo switch preference passa un valore booleano
        if (preference instanceof SwitchPreference)
        {
            boolean value = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getBoolean(preference.getKey(), true);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
        }
        // Tutti gli altri tipi di Preferences passano una stringa
        else
        {
            String value = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), "");
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
        }
    }
}