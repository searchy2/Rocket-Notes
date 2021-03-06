package stream.rocketnotes;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import es.dmoral.toasty.Toasty;
import stream.crosspromotion.AdActivity;
import stream.customalert.CustomAlertDialogue;
import stream.rocketnotes.utils.AnalyticsUtils;
import stream.rocketnotes.utils.FileUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    private ProgressDialog pDialog;

    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mAppearanceGroup;
    private PreferenceCategory mBackupGroup;
    private PreferenceCategory mSocialGroup;
    private PreferenceCategory mAboutGroup;

    private SwitchPreference showQuickActions;
    private SwitchPreference enableFastScroller;
    private SwitchPreference enablePopupFullscreen;

    private Preference itemLocalBackup;
    private Preference itemLocalRestore;
    private Preference itemCacheImage;

    private Preference itemMoreApps;
    private Preference itemContactUs;

    private Preference itemVersion;
    private Preference itemTerms;
    private Preference itemPrivacy;
    private Preference itemThanks;

    DatabaseHelper dbHelper;

    Context mContext;
    private final String mActivity = getClass().getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mContext = getActivity().getApplicationContext();
        dbHelper = new DatabaseHelper(mContext);

        setRetainInstance(true);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        mPreferenceScreen = (PreferenceScreen) findPreference("preferenceScreen");
        mAppearanceGroup = (PreferenceCategory) findPreference("header_appearance");
        mBackupGroup = (PreferenceCategory) findPreference("header_backup");
        mSocialGroup = (PreferenceCategory) findPreference("header_social");
        mAboutGroup = (PreferenceCategory) findPreference("header_about");

        showQuickActions = (SwitchPreference) findPreference("show_quickactions");
        enableFastScroller = (SwitchPreference) findPreference("enable_fastscroller");
        enablePopupFullscreen = (SwitchPreference) findPreference("enable_popup_fullscreen");

        itemLocalBackup = findPreference("settings_local_backup");
        itemLocalRestore = findPreference("settings_local_restore");
        itemCacheImage = findPreference("settings_cache_image");

        itemMoreApps = findPreference("settings_moreapps");
        itemContactUs = findPreference("settings_contact_us");

        itemVersion = findPreference("settings_version");
        itemTerms = findPreference("settings_terms");
        itemPrivacy = findPreference("settings_privacy");
        itemThanks = findPreference("settings_thanks");

        showQuickActions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                RefreshMainActivity();
                return false;
            }
        });
        enableFastScroller.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                RefreshMainActivity();
                return false;
            }
        });
        enablePopupFullscreen.setVisible((Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP));

        //Backup
        itemLocalBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

//                showpDialog();

                AnalyticsUtils.AnalyticEvent(mActivity, "Click", "Backup Database");
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.DIR_SELECT;
                properties.root = new File("/mnt/sdcard/");
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = null;
                FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
                dialog.setTitle("Select Backup Location");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        if (files.length >= 1) {
                            BackupDatabase(files[0]);
                        } else {
                            Toasty.error(mContext, "No Location Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.show();

                return false;
            }
        });
        itemLocalRestore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                AnalyticsUtils.AnalyticEvent(mActivity, "Click", "Restore Database");
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File("/mnt/sdcard/");
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = new String[]{"zip"};
                FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
                dialog.setTitle("Select Backup File");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        if (files.length >= 1) {
                            RestoreDatabase(files[0]);
                            RefreshMainActivity();
                            int widgetIDs[] = AppWidgetManager.getInstance(getActivity().getApplication()).getAppWidgetIds(new ComponentName(mContext, NotesWidget.class));
                            for (int id : widgetIDs) {
                                AppWidgetManager.getInstance(getActivity().getApplication()).notifyAppWidgetViewDataChanged(id, R.id.notes_listview);
                            }

                            int imageWidgetIDs[] = AppWidgetManager.getInstance(getActivity().getApplication()).getAppWidgetIds(new ComponentName(mContext, ImageWidget.class));
                            for (int id : imageWidgetIDs) {
                                AppWidgetManager.getInstance(getActivity().getApplication()).notifyAppWidgetViewDataChanged(id, R.id.image_gridview);
                            }
                        } else {
                            Toasty.error(mContext, "No File Selected", Toast.LENGTH_SHORT).show();
                        }
                        for (String filePath : files) {
                            Log.d("File Path", filePath);
                        }
                    }
                });
                dialog.show();

                return false;
            }
        });
        itemCacheImage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                AnalyticsUtils.AnalyticEvent(mActivity, "Click", "Refresh Cache");
                showpDialog(getString(R.string.loading_cache));
                Handler handler = new Handler();
                Runnable r1 = new Runnable() {
                    public void run() {
                        dbHelper.DeleteImagePreview();
                        dbHelper.CreateImagePreview(mContext);
                        RefreshMainActivity();
                        hidepDialog();
                        Toasty.success(mContext, "Cache Rebuilt", Toast.LENGTH_SHORT).show();
                    }

                };
                handler.postDelayed(r1, 100);

                return false;
            }
        });

        //Social
        itemContactUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                ArrayList<String> boxHint = new ArrayList<>();
                boxHint.add("Message");

                CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(getActivity())
                        .setStyle(CustomAlertDialogue.Style.INPUT)
                        .setTitle(getString(R.string.settings_contact))
                        .setMessage("Send us your feedback!")
                        .setPositiveText("Submit")
                        .setPositiveColor(R.color.positive)
                        .setPositiveTypeface(Typeface.DEFAULT_BOLD)
                        .setOnInputClicked(new CustomAlertDialogue.OnInputClicked() {
                            @Override
                            public void OnClick(View view, Dialog dialog, ArrayList<String> inputList) {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("message/rfc822");
                                intent.setType("vnd.android.cursor.item/email");
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mContext.getString(R.string.email_mailto)});
                                intent.putExtra(Intent.EXTRA_SUBJECT, mContext.getString(R.string.email_subject));
                                intent.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.email_message) + inputList.get(0));
                                try {
                                    Toasty.normal(mContext, "Send via email", Toast.LENGTH_SHORT).show();
                                    mContext.startActivity(Intent.createChooser(intent, "Send email using..."));
                                } catch (android.content.ActivityNotFoundException ex) {
                                    Toasty.normal(mContext, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeText("Close")
                        .setNegativeColor(R.color.negative)
                        .setOnNegativeClicked(new CustomAlertDialogue.OnNegativeClicked() {
                            @Override
                            public void OnClick(View view, Dialog dialog) {
                                dialog.dismiss();
                            }
                        })
                        .setBoxInputHint(boxHint)
                        .setDecorView(getActivity().getWindow().getDecorView())
                        .build();
                alert.show();

                return true;
            }
        });
        itemMoreApps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                Intent intent = new Intent(mContext, stream.crosspromotion.AdActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(AdActivity.AD_DEVELOPER_ID, getString(R.string.developer_id));
                intent.putExtra(AdActivity.AD_TITLE, getString(R.string.more_apps_title));
                mContext.startActivity(intent);

                return true;
            }
        });

        //About
        Preference pref = findPreference("settings_version");
        pref.setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
        itemVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {
                CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(getActivity())
                        .setStyle(CustomAlertDialogue.Style.DIALOGUE)
                        .setTitle(mContext.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
                        .setMessage(String.format(getString(R.string.copyrightText), Calendar.getInstance().get(Calendar.YEAR)))
                        .setNegativeText("OK")
                        .setNegativeColor(R.color.positive)
                        .setNegativeTypeface(Typeface.DEFAULT_BOLD)
                        .setOnNegativeClicked(new CustomAlertDialogue.OnNegativeClicked() {
                            @Override
                            public void OnClick(View view, Dialog dialog) {
                                dialog.dismiss();
                            }
                        })
                        .setDecorView(getActivity().getWindow().getDecorView())
                        .build();
                alert.show();
                return false;
            }
        });
        itemTerms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra(Constants.TITLE, getText(R.string.settings_tos));
                intent.putExtra(Constants.URL, getString(R.string.url_tos));
                startActivity(intent);

                return true;
            }
        });
        itemPrivacy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra(Constants.TITLE, getText(R.string.settings_privacy));
                intent.putExtra(Constants.URL, getString(R.string.url_privacy));
                startActivity(intent);

                return true;
            }
        });
        itemThanks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference arg0) {

                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra(Constants.TITLE, getText(R.string.settings_thanks));
                intent.putExtra(Constants.URL, getString(R.string.url_thanks));
                startActivity(intent);

                return true;
            }
        });

        initpDialog();
    }

    protected void initpDialog() {

        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage(getString(R.string.loading));
        pDialog.setCancelable(false);
    }

    protected void showpDialog(String message) {

        pDialog.setMessage(message);
        if (!pDialog.isShowing()) pDialog.show();
    }

    protected void hidepDialog() {

        if (pDialog.isShowing()) pDialog.dismiss();
    }

    public void BackupDatabase(final String savePath) {

        showpDialog(getString(R.string.loading_backup_database));
        Handler handler = new Handler();
        Runnable r1 = new Runnable() {
            public void run() {
                try {
                    //Make sure Pictures folder exists. User could have no picture notes.
                    FileUtils.InitializePicturesFolder(mContext);

                    //Copy notes database to Pictures folder.
                    final String inFileName = mContext.getDatabasePath("NotesDB").getPath();
                    File dbFile = new File(inFileName);
                    FileInputStream fis = new FileInputStream(dbFile);
                    String outFileName = mContext.getFilesDir() + "/" + ".Pictures/" + "NotesDB.db";
                    // Open the empty db as the output stream
                    OutputStream output = new FileOutputStream(outFileName);
                    // Transfer bytes from the inputfile to the outputfile
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    // Close the streams
                    output.flush();
                    output.close();
                    fis.close();

                    //Zip Pictures folder and save to user specified location.
                    File storageDir = new File(mContext.getFilesDir(), ".Pictures");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String currentDate = sdf.format(new Date());
                    String saveFilePath = savePath + "/" + "RocketNotes_" + currentDate + ".zip";
                    ZipUtil.pack(storageDir, new File(saveFilePath));

                    Toasty.success(mContext, "Backup Successful", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toasty.error(mContext, "Backup Failed", Toast.LENGTH_SHORT).show();
                }
                hidepDialog();
            }
        };
        handler.postDelayed(r1, 100);
    }

    public void RestoreDatabase(final String restorePath) {

        showpDialog(getString(R.string.loading_restore_database));
        Handler handler = new Handler();
        Runnable r1 = new Runnable() {
            public void run() {
                //Make sure Pictures folder exists. User could have no picture notes.
                FileUtils.InitializePicturesFolder(mContext);
                //Restoring backup requires NotesDB. If no NotesDB found, backup file is not valid.
                boolean validBackup = ZipUtil.containsEntry(new File(restorePath), "NotesDB.db");
                if (validBackup) {
                    ZipUtil.unpackEntry(new File(restorePath), "NotesDB.db", new File(mContext.getDatabasePath("NotesDB").getPath()));
                    ZipUtil.unpack(new File(restorePath), new File(mContext.getFilesDir(), ".Pictures"));
                    File file = new File(mContext.getFilesDir(), ".Pictures/NotesDB.db");
                    file.delete();
                    Toasty.success(mContext, "Backup Restored", Toast.LENGTH_SHORT).show();
                } else {
                    Toasty.error(mContext, "Invalid Backup File", Toast.LENGTH_SHORT).show();
                }
                hidepDialog();
            }

        };
        handler.postDelayed(r1, 100);
    }

    public void RefreshMainActivity() {
        //Refresh MainActivity
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(Constants.REFRESH, true);
        editor.apply();
    }
}