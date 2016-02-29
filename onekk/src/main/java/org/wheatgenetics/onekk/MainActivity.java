package org.wheatgenetics.onekk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.wheatgenetics.imageprocess.ImgProcess1KK;
import org.wheatgenetics.imageprocess.ImgProcess1KK.Seed;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnInitListener {

    public final static String TAG = "OneKK";
    private SharedPreferences ep;

    private UsbDevice mDevice;

    private EditText mWeightEditText;
    private EditText inputText;

    private ArrayList<Seed> seeds;
    private TableLayout OneKKTable;
    private MySQLiteHelper db;

    int seedCount = 0;

    private String firstName = "";
    private String lastName = "";

    private ScrollView sv1;
    static int currentItemNum = 1;
    FrameLayout preview;

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private CameraPreview mPreview;
    private String picName = "";
    private String photoName;
    public static final int MEDIA_TYPE_IMAGE = 1;

    private LinearLayout parent;
    private ScrollView changeContainer;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.bringToFront();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView nvDrawer = (NavigationView) findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);
        setupDrawer();

        ep = getSharedPreferences("Settings", 0);

        inputText = (EditText) findViewById(R.id.etInput);
        mWeightEditText = (EditText) findViewById(R.id.etWeight);
        mWeightEditText.setText(getResources().getString(R.string.not_connected));

        sv1 = (ScrollView) findViewById(R.id.svData);
        OneKKTable = (TableLayout) findViewById(R.id.tlInventory);

        preview = (FrameLayout) findViewById(R.id.camera_preview);
        parent = new LinearLayout(this);
        changeContainer = new ScrollView(this);
        changeContainer.removeAllViews();
        changeContainer.addView(parent);

        db = new MySQLiteHelper(this);

        OneKKTable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });

        Intent intent = getIntent();
        mDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        inputText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(inputText,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);
                    if (event.getAction() != KeyEvent.ACTION_DOWN)
                        return true;
                    picName = inputText.getText().toString();
                    takePic();
                    goToTop();
                    inputText.requestFocus(); // Set focus back to Enter box
                }

                if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        return true;
                    }
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        picName = inputText.getText().toString();
                        takePic();
                        goToTop();
                    }
                    inputText.requestFocus(); // Set focus back to Enter box
                }
                return false;
            }
        });

        mWeightEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(inputText,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);
                    if (event.getAction() != KeyEvent.ACTION_DOWN)
                        return true;

                    goToTop();

                    if (mDevice != null) {
                        mWeightEditText.setText("");
                    }
                    inputText.requestFocus(); // Set focus back to Enter box
                }

                if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        return true;
                    }
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        goToTop();
                    }
                    inputText.requestFocus(); // Set focus back to Enter box
                }
                return false;
            }
        });

        startCamera();
        createDirs();
        parseDbToTable();
        goToTop();
        settingsDialog();

        //makeToast(String.valueOf(preview.getHeight()) + " " + String.valueOf(preview.getWidth()));

        if (!OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.INIT_FAILED);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (ep.getString("FirstName", "").length() == 0) {
            setPersonDialog();
        }

        if (!ep.getBoolean("ignoreScale", false)) {
            findScale();
        }

        Editor ed = ep.edit();
        if (ep.getInt("UpdateVersion", -1) < getVersion()) {
            ed.putInt("UpdateVersion", getVersion());
            ed.apply();
            changelog();
        }

        FrameLayout measuringStick = (FrameLayout) findViewById(R.id.measureStick);
        measuringStick.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            int count = 0;
            double ratio = 0.0;
            int height1 = 0;
            int width1 = 0;

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                if (preview.getWidth() != 0 && preview.getHeight() != 0) {
                    if (preview.getWidth() > width1 && preview.getHeight() > height1) {
                        width1 = preview.getWidth();
                        height1 = preview.getHeight();
                        count++;
                    }

                    if(count!=0) {
                        ratio = ((double) height1)/((double) width1);
                    }
                }

                if(bottom<oldBottom) {
                    int newWidth = (int) (bottom/ratio);
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(newWidth,bottom);
                    lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    lp.addRule(RelativeLayout.ALIGN_TOP);
                    preview.setLayoutParams(lp);
                }

                if(oldBottom<bottom) {
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width1,height1);
                    preview.setLayoutParams(lp);
                }

                //makeToast(String.valueOf(bottom) + " " + String.valueOf(oldBottom));
                //makeToast(String.valueOf(preview.getHeight()));
            }
        });
    }

    private void createDirs() {
        createDir(Constants.MAIN_PATH);
        createDir(Constants.EXPORT_PATH);
        createDir(Constants.PHOTO_PATH);
        createDir(Constants.ANALYZED_PHOTO_PATH);
    }

    public int getVersion() {
        int v = 0;
        try {
            v = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Field Book", "" + e.getMessage());
        }
        return v;
    }

    private void createDir(File path) {
        File blankFile = new File(path, ".onekk");

        if (!path.exists()) {
            path.mkdirs();

            try {
                blankFile.getParentFile().mkdirs();
                blankFile.createNewFile();
                makeFileDiscoverable(blankFile, this);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @SuppressWarnings("deprecation")
    private void startCamera() {
        mCamera = getCameraInstance();

        PackageManager pm = getPackageManager();
        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)){
            Camera.Parameters params = mCamera.getParameters();

            if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            mCamera.setParameters(params);
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);
    }

    private void goToTop() {
        sv1.post(new Runnable() {
            public void run() {
                sv1.fullScroll(ScrollView.FOCUS_UP);
                inputText.requestFocus();
            }
        });
    }

    private void parseDbToTable() {
        OneKKTable.removeAllViews();
        List<SampleRecord> list = db.getAllSamples();
        int itemCount = list.size();
        if (itemCount != 0) {
            for (int i = 0; i < itemCount; i++) {
                String[] temp = list.get(i).toString().split(",");

                Log.d(TAG, temp[0] + " " + temp[1] + " " + temp[2] + " "
                        + temp[3] + " " + temp[4] + " " + temp[5] + " "
                        + temp[6] + " " + temp[7]);

                createNewTableEntry(temp[0], temp[5], stringDecimal(temp[7]), stringDecimal(temp[8]), stringDecimal(temp[6]));
            }
        }
    }

    private String stringDecimal(String input) {
        if (!input.equals("null")) {
            return String.format("%.2f", Double.parseDouble(input));
        }

        return "null";
    }

    /**
     * Adds a new record to the internal list of records
     */
    private void addRecord() {
        String ut;
        String date = getDate();

        ut = inputText.getText().toString();
        if (ut.equals("")) {
            return; // check for empty user input
        }

        String weight;
        if (mDevice == null
                && mWeightEditText.getText().toString().equals("Not connected")) {
            weight = "null";
        } else {
            weight = mWeightEditText.getText().toString();
        }

        // Add all measured seeds to database
        for (int j = 0; j < seeds.size(); j++) {
            //TODO add other parameters (weight, color)
            db.addSeedRecord(new SeedRecord(inputText.getText().toString(), seeds.get(j).getLength(), seeds.get(j).getWidth(), seeds.get(j).getCirc(), seeds.get(j).getArea(), "", ""));
        }

        // Calculate averages
        double lengthAvg = db.averageSample(inputText.getText().toString(), "length");
        double lengthVar = Math.pow(db.sdSample(inputText.getText().toString(), "length"), 2);
        double lengthCV = (db.sdSample(inputText.getText().toString(), "length")) / (db.averageSample(inputText.getText().toString(), "length"));

        double widthAvg = db.averageSample(inputText.getText().toString(), "width");
        double widthVar = Math.pow(db.sdSample(inputText.getText().toString(), "width"), 2);
        double widthCV = (db.sdSample(inputText.getText().toString(), "width")) / db.averageSample(inputText.getText().toString(), "width");

        double areaAvg = db.averageSample(inputText.getText().toString(), "area");
        double areaVar = Math.pow(db.sdSample(inputText.getText().toString(), "area"), 2);
        double areaCV = (db.sdSample(inputText.getText().toString(), "area")) / (db.averageSample(inputText.getText().toString(), "area"));

        String seedCountString = String.valueOf(seedCount);

        // Add sample to database
        db.addSampleRecord(new SampleRecord(inputText.getText().toString(), photoName,
                ep.getString("FirstName", "").toLowerCase() + "_" + ep.getString("LastName", "").toLowerCase(),
                date, seedCountString, weight, lengthAvg, lengthVar, lengthCV, widthAvg,
                widthVar, widthCV, areaAvg, areaVar, areaCV));


        // Round values for UI
        String avgLengthStr = String.format("%.2f", lengthAvg);
        String avgWidthStr = String.format("%.2f", widthAvg);

        createNewTableEntry(inputText.getText().toString(), seedCountString, avgLengthStr, avgWidthStr, weight);
        currentItemNum++;
    }

    @SuppressWarnings("deprecation")
    public static Camera getCameraInstance() {
        @SuppressWarnings("deprecation")
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return c;
    }

    private String getDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat date = new SimpleDateFormat(
                "yyyy-MM-dd-hh-mm-ss", Locale.getDefault());
        return date.format(cal.getTime());
    }

    /**
     * Adds a new entry to the end of the TableView
     */
    private void createNewTableEntry(String sample, String seedCount, String avgL, String avgW, String wt) {
        inputText.setText("");

		/* Create a new row to be added. */
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new TableLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		/* Create the sample name field */
        TextView sampleName = new TextView(this);
        sampleName.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        sampleName.setTextColor(Color.BLACK);
        sampleName.setTextSize(20.0f);
        sampleName.setText(sample);
        sampleName.setTag(sample);
        sampleName.setLayoutParams(new TableRow.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 0.4f));

		/* Create the number of seeds field */
        TextView numSeeds = new TextView(this);
        numSeeds.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        numSeeds.setTextColor(Color.BLACK);
        numSeeds.setTextSize(20.0f);
        numSeeds.setText(seedCount);
        numSeeds.setLayoutParams(new TableRow.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the length field */
        TextView avgLength = new TextView(this);
        avgLength.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        avgLength.setTextColor(Color.BLACK);
        avgLength.setTextSize(20.0f);
        avgLength.setText(avgL);
        avgLength.setLayoutParams(new TableRow.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the width field */
        TextView avgWidth = new TextView(this);
        avgWidth.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        avgWidth.setTextColor(Color.BLACK);
        avgWidth.setTextSize(20.0f);
        avgWidth.setText(avgW);
        avgWidth.setLayoutParams(new TableRow.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the area field */
        TextView sampleWeight = new TextView(this);
        sampleWeight.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        sampleWeight.setTextColor(Color.BLACK);
        sampleWeight.setTextSize(20.0f);
        sampleWeight.setText(wt);
        sampleWeight.setLayoutParams(new TableRow.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 0.12f));

		/* Define the listener for the longclick event */
        sampleName.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                final String tag = (String) v.getTag();
                deleteDialog(tag);
                return false;
            }
        });

		/* Add UI elements to row and add row to table */
        tr.addView(sampleName);
        tr.addView(numSeeds);
        tr.addView(avgLength);
        tr.addView(avgWidth);
        tr.addView(sampleWeight);
        OneKKTable.addView(tr, 0, new LayoutParams( // Adds row to top of table
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT));
    }

    private void changelog() {
        parent.setOrientation(LinearLayout.VERTICAL);
        parseLog(R.raw.changelog_releases);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.updatemsg));
        builder.setView(changeContainer)
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void parseLog(int resId) {
        try {
            InputStream is = getResources().openRawResource(resId);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr, 8192);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(20, 5, 20, 0);

            String curVersionName = null;
            String line;

            while ((line = br.readLine()) != null) {
                TextView header = new TextView(this);
                TextView content = new TextView(this);
                TextView spacer = new TextView(this);
                View ruler = new View(this);

                header.setLayoutParams(lp);
                content.setLayoutParams(lp);
                spacer.setLayoutParams(lp);
                ruler.setLayoutParams(lp);

                spacer.setTextSize(5);

                ruler.setBackgroundColor(getResources().getColor(R.color.main_colorAccent));
                header.setTextAppearance(getApplicationContext(), R.style.ChangelogTitles);
                content.setTextAppearance(getApplicationContext(), R.style.ChangelogContent);

                if (line.length() == 0) {
                    curVersionName = null;
                    spacer.setText("\n");
                    parent.addView(spacer);
                } else if (curVersionName == null) {
                    final String[] lineSplit = line.split("/");
                    curVersionName = lineSplit[1];
                    header.setText(curVersionName);
                    parent.addView(header);
                    parent.addView(ruler);
                } else {
                    content.setText("•  " + line);
                    parent.addView(content);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDialog(String tag) {
        final String sampleName = tag;

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.delete_entry));
        builder.setMessage(getResources().getString(R.string.delete_msg_1) + " \"" + sampleName + "\". " + getResources().getString(R.string.delete_msg_2))
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                db.deleteSample(sampleName);
                                parseDbToTable();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                TextView person = (TextView) findViewById(R.id.nameLabel);
                person.setText(ep.getString("FirstName", "") + " " + ep.getString("LastName", ""));
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_settings:
                settingsDialog();
                break;

            case R.id.nav_export:
                exportDialog();
                break;

            case R.id.nav_person:
                setPersonDialog();
                break;

            case R.id.nav_scaleConnect:
                findScale();
                break;

            case R.id.nav_clearData:
                clearDialog();
                break;

            case R.id.nav_help:
                makeToast(getResources().getString(R.string.coming_soon));
                break;

            case R.id.nav_about:
                aboutDialog();
                break;
        }

        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCamera == null) {
            startCamera(); // Local method to handle camera initialization
        }
        Log.v(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera == null) {
            startCamera(); // Local method to handle camera initialization
        }
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return true;
    }

    private void settingsDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View settingsView = inflater.inflate(R.layout.settings, new LinearLayout(this), false);

        final Switch analysisPreview = (Switch) settingsView.findViewById(R.id.swAnalysisPreview);
        final Switch cropImage = (Switch) settingsView.findViewById(R.id.swCropImage);
        final EditText refDiam = (EditText) settingsView.findViewById(R.id.etReferenceDiameter);
        final EditText minSize = (EditText) settingsView.findViewById(R.id.etMinSize);
        final EditText maxSize = (EditText) settingsView.findViewById(R.id.etMaxSize);

        refDiam.setText(ep.getString("refDiam", "1"));
        analysisPreview.setChecked(ep.getBoolean("analysisPreview", false));
        minSize.setText(ep.getString("minSize", "0.0"));
        maxSize.setText(ep.getString("maxSize", "0.0"));
        cropImage.setChecked(ep.getBoolean("cropImage", true));

        alert.setCancelable(false);
        alert.setTitle(getResources().getString(R.string.settings));
        alert.setView(settingsView);
        alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editor ed = ep.edit();
                ed.putString("refDiam", refDiam.getText().toString());
                ed.putString("minSize", minSize.getText().toString());
                ed.putString("maxSize", maxSize.getText().toString());
                ed.putBoolean("analysisPreview", analysisPreview.isChecked());
                ed.putBoolean("cropImage", cropImage.isChecked());
                ed.apply();
            }
        });
        alert.show();
    }

    private void takePic() {
        //inputText.setEnabled(false); //TODO fix camera preview and enable this
        mCamera.takePicture(null, null, mPicture);
    }

    @SuppressWarnings("deprecation")
    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            Uri outputFileUri = Uri.fromFile(pictureFile);
            makeFileDiscoverable(pictureFile, MainActivity.this);

            imageAnalysis(outputFileUri);

            mCamera.startPreview();

            inputText.setEnabled(true);
            inputText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(inputText, InputMethodManager.SHOW_IMPLICIT);
        }
    };

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        File mediaFile;
        String fileName;

        if (picName.length() > 0) {
            fileName = picName + "_";
        } else {
            fileName = "temp_";
        }

        mediaFile = new File(Constants.PHOTO_PATH, fileName + "IMG_" + getDate() + ".jpg");
        return mediaFile;
    }

    private void imageAnalysis(Uri photo) {
        photoName = photo.getLastPathSegment();
        makeToast(photoName);

        double refDiam = Double.valueOf(ep.getString("refDiam", "1")); // Wheat default

        ImgProcess1KK imgP = new ImgProcess1KK(Constants.PHOTO_PATH.toString() + "/" + photoName, refDiam, ep.getBoolean("crop", true), Double.valueOf(ep.getString("minSize", "0.0")), Double.valueOf(ep.getString("maxSize", "0.0"))); //TODO the min/max sizes are bad
        imgP.writeProcessedImg(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg");
        makeFileDiscoverable(new File(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg"), this);

        seedCount = imgP.getSeedCount();

        seeds = imgP.getList();
        addRecord(); // Add the current record to the table

        if (ep.getBoolean("analysisPreview", false)) {
            postImageDialog(photoName);
        }
    }

    private void postImageDialog(String imageName) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        final View personView = inflater.inflate(R.layout.post_image, new LinearLayout(this), false);

        File imgFile = new File(Constants.ANALYZED_PHOTO_PATH, imageName + "_new.jpg");

        if (imgFile.exists()) {
            TouchImageView imgView = (TouchImageView) personView.findViewById(R.id.postImage);
            Bitmap bmImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rbmImg = Bitmap.createBitmap(bmImg, 0, 0, bmImg.getWidth(), bmImg.getHeight(), matrix, true); //TODO change preview size to avoid out of memory errors

            imgView.setImageBitmap(rbmImg);
        }

        alert.setCancelable(true);
        alert.setTitle(getResources().getString(R.string.analysis_preview));
        alert.setView(personView);
        alert.setNegativeButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }

    private void aboutDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        final View personView = inflater.inflate(R.layout.about, new LinearLayout(this), false);
        TextView version = (TextView) personView.findViewById(R.id.tvVersion);
        TextView otherApps = (TextView) personView.findViewById(R.id.tvOtherApps);


        final PackageManager packageManager = this.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
            version.setText(getResources().getString(R.string.versiontitle) + " " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }

        version.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changelog();
            }
        });

        otherApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOtherAppsDialog();
            }
        });


        alert.setCancelable(true);
        alert.setTitle(getResources().getString(R.string.about));
        alert.setView(personView);
        alert.setNegativeButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void showOtherAppsDialog() {
        final AlertDialog.Builder otherAppsAlert = new AlertDialog.Builder(this);

        ListView myList = new ListView(this);
        myList.setDivider(null);
        myList.setDividerHeight(0);
        String[] appsArray = new String[3];

        appsArray[0] = "Field Book";
        appsArray[1] = "Inventory";
        appsArray[2] = "Coordinate";
        //appsArray[3] = "Intercross";
        //appsArray[4] = "Rangle";

        Integer app_images[] = {R.drawable.other_ic_field_book, R.drawable.other_ic_inventory, R.drawable.other_ic_coordinate};
        final String[] links = {"https://play.google.com/store/apps/details?id=com.fieldbook.tracker",
                "https://play.google.com/store/apps/details?id=org.wheatgenetics.inventory",
                "http://wheatgenetics.org/apps"}; //TODO update these links

        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View arg1, int which, long arg3) {
                Uri uri = Uri.parse(links[which]);
                Intent intent;

                switch (which) {
                    case 0:
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        break;
                }
            }
        });

        CustomListAdapter adapterImg = new CustomListAdapter(this, app_images, appsArray);
        myList.setAdapter(adapterImg);

        otherAppsAlert.setCancelable(true);
        otherAppsAlert.setTitle(getResources().getString(R.string.otherapps));
        otherAppsAlert.setView(myList);
        otherAppsAlert.setNegativeButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        otherAppsAlert.show();
    }

    public class CustomListAdapter extends ArrayAdapter<String> {
        String[] color_names;
        Integer[] image_id;
        Context context;

        public CustomListAdapter(Activity context, Integer[] image_id, String[] text) {
            super(context, R.layout.appline, text);
            this.color_names = text;
            this.image_id = image_id;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View single_row = inflater.inflate(R.layout.appline, null, true);
            TextView textView = (TextView) single_row.findViewById(R.id.txt);
            ImageView imageView = (ImageView) single_row.findViewById(R.id.img);
            textView.setText(color_names[position]);
            imageView.setImageResource(image_id[position]);
            return single_row;
        }
    }

    public void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setPersonDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View personView = inflater.inflate(R.layout.person, new LinearLayout(this), false);

        final EditText fName = (EditText) personView
                .findViewById(R.id.firstName);
        final EditText lName = (EditText) personView
                .findViewById(R.id.lastName);

        fName.setText(ep.getString("FirstName", ""));
        lName.setText(ep.getString("LastName", ""));

        alert.setCancelable(false);
        alert.setTitle(getResources().getString(R.string.set_person));
        alert.setView(personView);
        alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                firstName = fName.getText().toString().trim();
                lastName = lName.getText().toString().trim();

                if (firstName.length() == 0 | lastName.length() == 0) {
                    makeToast(getResources().getString(R.string.no_blank));
                    setPersonDialog();
                    return;
                }

                makeToast(getResources().getString(R.string.person_set) + " " + firstName + " " + lastName);
                Editor ed = ep.edit();
                ed.putString("FirstName", firstName);
                ed.putString("LastName", lastName);
                ed.apply();
            }
        });
        alert.show();
    }

    private void clearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getResources().getString(R.string.delete_database))
                .setCancelable(false)
                .setTitle("Clear Data")
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                makeToast(getResources().getString(R.string.data_deleted));
                                dropTables();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void exportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getResources().getString(R.string.export_choice))
                .setCancelable(false)

                .setPositiveButton(getResources().getString(R.string.export_raw),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportRawData();
                                try {
                                    exportDatabase(exportCursor, "RawData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNeutralButton(getResources().getString(R.string.export_all),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportSummaryData();
                                try {
                                    exportDatabase(exportCursor, "RawData");
                                    exportDatabase(exportCursor, "SummaryData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.export_summaries),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportSummaryData();
                                try {
                                    exportDatabase(exportCursor, "SummaryData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setTitle(getResources().getString(R.string.export_data))
                .setNeutralButton(getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera(); // release the camera immediately on pause event
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public void makeFileDiscoverable(File file, Context context) {
        MediaScannerConnection.scanFile(context,
                new String[]{file.getPath()}, null, null);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(file)));
    }

    private void dropTables() {
        db.deleteAll();
        OneKKTable.removeAllViews();
        currentItemNum = 1;
    }

    public void exportDatabase(Cursor cursorForExport, String type) throws Exception {
        File file = null;

        try {
            file = new File(Constants.EXPORT_PATH, "export_" + type + "_" + getDate() + ".csv");
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }

        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            csvWrite.writeNext(cursorForExport.getColumnNames());

            while (cursorForExport.moveToNext()) {
                String arrStr[] = new String[cursorForExport.getColumnCount()];

                for (int k = 0; k < cursorForExport.getColumnCount(); k++) {
                    arrStr[k] = cursorForExport.getString(k);
                }

                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            cursorForExport.close();
        } catch (Exception sqlEx) {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
        }

        makeFileDiscoverable(file, MainActivity.this);
        shareFile(file.toString());
    }


    private void shareFile(String filePath) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
        startActivity(Intent.createChooser(intent, "Sending File..."));
    }

    public void findScale() {
        if (mDevice == null) {
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            for (UsbDevice usbDevice : deviceList.values()) {
                mDevice = usbDevice;
                Log.v(TAG,
                        String.format(
                                "name=%s deviceId=%d productId=%d vendorId=%d deviceClass=%d subClass=%d protocol=%d interfaceCount=%d",
                                mDevice.getDeviceName(), mDevice.getDeviceId(),
                                mDevice.getProductId(), mDevice.getVendorId(),
                                mDevice.getDeviceClass(),
                                mDevice.getDeviceSubclass(),
                                mDevice.getDeviceProtocol(),
                                mDevice.getInterfaceCount()));
                break;
            }
        }

        if (mDevice != null) {
            mWeightEditText.setText("0");
            new ScaleListener().execute();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getResources().getString(R.string.no_scale))
                    .setMessage(
                            getResources().getString(R.string.connect_scale))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.try_again),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    findScale();
                                }

                            })
                    .setNegativeButton(getResources().getString(R.string.ignore),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Editor ed = ep.edit();
                                    ed.putBoolean("ignoreScale", true);
                                    ed.apply();
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    private class ScaleListener extends AsyncTask<Void, Double, Void> {
        private double mLastWeight = 0;

        @Override
        protected Void doInBackground(Void... arg0) {

            byte[] data = new byte[128];
            int TIMEOUT = 2000;

            Log.v(TAG, "start transfer");

            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            if (mDevice == null) {
                Log.e(TAG, "no device");
                return null;
            }
            UsbInterface intf = mDevice.getInterface(0);

            Log.v(TAG,
                    String.format("endpoint count = %d",
                            intf.getEndpointCount()));
            UsbEndpoint endpoint = intf.getEndpoint(0);
            Log.v(TAG, String.format(
                    "endpoint direction = %d out = %d in = %d",
                    endpoint.getDirection(), UsbConstants.USB_DIR_OUT,
                    UsbConstants.USB_DIR_IN));
            UsbDeviceConnection connection = usbManager.openDevice(mDevice);
            Log.v(TAG, "got connection:" + connection.toString());
            connection.claimInterface(intf, true);
            while (true) {

                int length = connection.bulkTransfer(endpoint, data,
                        data.length, TIMEOUT);

                if (length != 6) {
                    Log.e(TAG, String.format("invalid length: %d", length));
                    return null;
                }

                byte report = data[0];
                byte status = data[1];
                //byte exp = data[3];
                short weightLSB = (short) (data[4] & 0xff);
                short weightMSB = (short) (data[5] & 0xff);

                // Log.v(TAG, String.format(
                // "report=%x status=%x exp=%x lsb=%x msb=%x", report,
                // status, exp, weightLSB, weightMSB));

                if (report != 3) {
                    Log.v(TAG, String.format("scale status error %d", status));
                    return null;
                }

                double mWeightGrams;
                if (mDevice.getProductId() == 519) {
                    mWeightGrams = (weightLSB + weightMSB * 256.0) / 10.0;
                } else {
                    mWeightGrams = (weightLSB + weightMSB * 256.0);
                }
                double mZeroGrams = 0;
                double zWeight = (mWeightGrams - mZeroGrams);

                switch (status) {
                    case 1:
                        Log.w(TAG, "Scale reports FAULT!\n");
                        break;
                    case 3:
                        Log.i(TAG, "Weighing...");
                        if (mLastWeight != zWeight) {
                            publishProgress(zWeight);
                        }
                        break;
                    case 2:
                    case 4:
                        if (mLastWeight != zWeight) {
                            Log.i(TAG, String.format("Final Weight: %f", zWeight));
                            publishProgress(zWeight);
                        }
                        break;
                    case 5:
                        Log.w(TAG, "Scale reports Under Zero");
                        if (mLastWeight != zWeight) {
                            publishProgress(0.0);
                        }
                        break;
                    case 6:
                        Log.w(TAG, "Scale reports Over Weight!");
                        break;
                    case 7:
                        Log.e(TAG, "Scale reports Calibration Needed!");
                        break;
                    case 8:
                        Log.e(TAG, "Scale reports Re-zeroing Needed!\n");
                        break;
                    default:
                        Log.e(TAG, "Unknown status code");
                        break;
                }

                mLastWeight = zWeight;
            }
        }

        @Override
        protected void onProgressUpdate(Double... weights) {
            Double weight = weights[0];
            Log.i(TAG, "update progress");
            String weightText = String.format("%.1f", weight);
            Log.i(TAG, weightText);
            mWeightEditText.setText(weightText);
            mWeightEditText.invalidate();
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.scale_disconnect),
                    Toast.LENGTH_LONG).show();
            mDevice = null;
            mWeightEditText.setText("Not connected");
        }
    }

    public void onInit(int status) {
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}