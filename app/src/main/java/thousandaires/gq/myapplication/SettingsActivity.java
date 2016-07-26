package thousandaires.gq.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by jthomas on 7/22/16.
 */
public class SettingsActivity extends Activity {
    Bundle savedInstanceState;

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
        }
    }
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
    }
}
