package amirz.shade.customization;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.widget.WidgetsBottomSheet;

import amirz.shade.ShadeAppFilter;
import amirz.shade.icons.pack.IconReloader;

public class PreferencesBottomSheet extends WidgetsBottomSheet {
    private final FragmentManager mFragmentManager;
    private View.OnClickListener mOnAppInfoClick;

    public PreferencesBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferencesBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFragmentManager = Launcher.getLauncher(context).getFragmentManager();
    }

    public void setOnAppInfoClick(View.OnClickListener onclick) {
        mOnAppInfoClick = onclick;
    }

    @Override
    public void populateAndShow(ItemInfo itemInfo) {
        super.populateAndShow(itemInfo);
        TextView title = findViewById(R.id.title);
        title.setText(itemInfo.title);

        View.OnClickListener l = v -> mOnAppInfoClick.onClick(v);
        title.setOnClickListener(l);
        findViewById(R.id.subtitle).setOnClickListener(l);

        PrefsFragment fragment =
                (PrefsFragment) mFragmentManager.findFragmentById(R.id.sheet_prefs);
        fragment.loadForApp(itemInfo, v -> handleClose(true));
    }

    @Override
    public void onDetachedFromWindow() {
        Fragment pf = mFragmentManager.findFragmentById(R.id.sheet_prefs);
        if (pf != null) {
            mFragmentManager.beginTransaction()
                    .remove(pf)
                    .commitAllowingStateLoss();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWidgetsBound() {
    }

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private final static String PREF_ICON = "pref_app_icon";
        private final static String PREF_CATEGORY = "pref_app_category";
        private final static String PREF_RESET = "pref_app_reset";

        private Context mContext;

        private ComponentName mComponent;
        private ComponentKey mKey;

        private IconPackPreference mPrefIcon;
        private CategoryPreference mPrefCategory;
        private Preference mPrefReset;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = getActivity();

            addPreferencesFromResource(R.xml.app_edit_prefs);
        }

        public void loadForApp(ItemInfo itemInfo, final View.OnClickListener onResetClick) {
            mComponent = itemInfo.getTargetComponent();
            mKey = new ComponentKey(mComponent, itemInfo.user);

            mPrefIcon = (IconPackPreference) findPreference(PREF_ICON);
            mPrefIcon.setValue(CustomizationDatabase.getIconPack(mContext, mKey));
            mPrefIcon.setOnPreferenceChangeListener(this);

            mPrefCategory = (CategoryPreference) findPreference(PREF_CATEGORY);
            mPrefCategory.setValue(CustomizationDatabase.getCategory(mContext, mKey));
            mPrefCategory.setOnPreferenceChangeListener(this);

            mPrefReset = findPreference(PREF_RESET);
            mPrefReset.setOnPreferenceClickListener(preference -> {
                CustomizationDatabase.clearIconPack(mContext, mKey);
                CustomizationDatabase.clearCategory(mContext, mKey);
                reloadIcon();
                onResetClick.onClick(PrefsFragment.this.getView());
                return true;
            });
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case PREF_ICON:
                    CustomizationDatabase.setIconPack(mContext, mKey, (String) newValue);
                    reloadIcon();
                    return true;
                case PREF_CATEGORY:
                    CustomizationDatabase.setCategory(mContext, mKey, (String) newValue);
                    return true;
            }
            return false;
        }

        private void reloadIcon() {
            IconReloader.get(mContext).reload(mKey);
        }
    }
}