package shibedays.com.reptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SetTimeDialogue.NoticeDialogListener{

    private static final String DEBUG_TAG = MainActivity.class.getSimpleName();

    //region PUBLIC_TAGS_AND_KEYS

    //keys for intent passing
    public static final String ACTION_REP_KEY = "com.shibedays.MainAct.REP";
    public static final String ACTION_REST_KEY = "com.shibedays.MainAct.REST";
    public static final String ACTION_BREAK_KEY = "com.shibedays.MainAct.BREAK";
    public static final String ACTION_REP_NUM_KEY = "com.shibedays.MainAct.REP_NUM";
    public static final String ACTION_ROUND_NUM_KEY = "com.shibedays.MainAct.BREAK_NUM";
    public static final String ACTION_TTS_READY_KEY = "com.shibedays.MainAct.TTS_READY";

    //pref ID keys
    public static final String PREF_IDENTIFIER = "com.shibedays.MainAct.PREFS";
    public static final String PREF_IS_VALID = "com.shibadays.MainAct.IS_VALID";
    public static final String PREF_REP_KEY = "com.shibadays.MainAct.PREF_REP_KEY";
    public static final String PREF_REST_KEY = "com.shibadays.MainAct.PREF_REST_KEY";
    public static final String PREF_BREAK_KEY = "com.shibadays.MainAct.PREF_BREAK_KEY";
    public static final String PREF_NUM_REPS = "com.shibadays.MainAct.PREF_NUM_REPS";
    public static final String PREF_NUM_ROUNDS = "com.shibadays.MainAct.PREF_NUM_ROUNDS";

    //broadcast intent filter
    public static final String TTS_BROADCAST_FILTER = "com.shibedays.MainAct.TTS_FILTER";

    //Intent and Pref ID for SetTimeDialog concering which time value we're working with
    public static final String TIME_TYPE_KEY = "com.shibedays.MainAct.TIME_TYPE_KEY";
    public static final String PREF_TYPE_KEY = "com.shibedays.MainAct.PREF_TYPE_KEY";


    //endregion

    //region PRIVATE_VARS

    //Basic variables time, rep, and rounds
    private int mNumReps = 1;
    private int mNumRounds = 1;
    private int[] mRepTime = {0,0};
    private int[] mRestTime = {0,0};
    private int[] mBreakTime = {0,0};

    //times in millis
    private int mRepTimeInMillis = 0;
    private int mRestTimeInMillis = 0;
    private int mBreakTimeInMillis = 0;

    //boolean for wether TTS is ready or not
    private boolean mIsTTSReady = false;

    //preference variables
    private SharedPreferences mSharedPref;
    private FragmentManager mFragmentManager;
    //endregion

    //region PUBLIC_VARS

    //hashmap for the type of time, binds text type to numbers 0-2
    public static final HashMap<String, Integer> TIME_TYPE = new HashMap<String, Integer>();
    static{
        TIME_TYPE.put(ACTION_REP_KEY, 0);
        TIME_TYPE.put(ACTION_REST_KEY, 1);
        TIME_TYPE.put(ACTION_BREAK_KEY, 2);
    }

    //endregion

    //region BROADCAST_HANDLER
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null){
                if(action.equals(TTS_BROADCAST_FILTER)){
                    mIsTTSReady = intent.getBooleanExtra(ACTION_TTS_READY_KEY, false);
                } else {
                    Log.e(DEBUG_TAG, "Broadcast in MainScreen in invalid");
                }
            }
        }
    };
    //endregion

    //region OVERRIDE_DEFAULT_FUNCTIONS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get fragment manager
        mFragmentManager = getSupportFragmentManager();

        //Register the brdcast receiver with the correct filter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TTS_BROADCAST_FILTER);
        registerReceiver(mReceiver, intentFilter);

        //Start the TTS service
        Intent ttsIntent = new Intent(this, TextToSpeechService.class);
        startService(ttsIntent);

        //region TOOLBAR_SETUP
        /*If I end up using a single toolbar setup, use:
        ConstraintLayout root = findViewById(R.id.MainLayout);
        Toolbar myBar = (Toolbar)LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
        root.addView(myBar, 0);
        myBar.setTitle(R.string.main_name); */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //endregion

        //region PREFERENCES_SETUP
        mSharedPref = getSharedPreferences(PREF_IDENTIFIER, MODE_PRIVATE);
        //if preferences don't exist, create a new file with default values
        if(mSharedPref == null){
            SharedPreferences.Editor tempEditor = mSharedPref.edit();
            tempEditor.putInt(PREF_IS_VALID, 1);
            if(mRepTimeInMillis != 0)
                Log.e(DEBUG_TAG, "Shared Prefs didn't exist and times were non-zero");
            tempEditor.putInt(PREF_REP_KEY, 0);
            tempEditor.putInt(PREF_REST_KEY, 0);
            tempEditor.putInt(PREF_BREAK_KEY, 0);
            tempEditor.putInt(PREF_NUM_REPS, 1);
            tempEditor.putInt(PREF_NUM_ROUNDS, 1);

            tempEditor.apply();
            Log.i(DEBUG_TAG, "Shared Prefences not found, setting up");
            //Input default values now?
        } else {
            Log.i(DEBUG_TAG, "Shared Preferenecs were found");
            mRepTimeInMillis = mSharedPref.getInt(PREF_REP_KEY, 0);
            mRestTimeInMillis = mSharedPref.getInt(PREF_REST_KEY, 0);
            mBreakTimeInMillis = mSharedPref.getInt(PREF_BREAK_KEY, 0);

            mNumReps = mSharedPref.getInt(PREF_NUM_REPS, 1);
            mNumRounds = mSharedPref.getInt(PREF_NUM_ROUNDS, 1);

            mRepTime = convertFromMillis(mRepTimeInMillis);
            mRestTime = convertFromMillis(mRestTimeInMillis);
            mBreakTime = convertFromMillis(mBreakTimeInMillis);
        }
        //endregion

        //region UI_SETUP
        TextView rep_view = findViewById(R.id.rep_time);
        TextView rest_view = findViewById(R.id.rest_time);
        TextView break_view = findViewById(R.id.break_time);
        updateTimeUI(rep_view, mRepTime);
        updateTimeUI(rest_view, mRestTime);
        updateTimeUI(break_view, mBreakTime);
        //endregion

        //region SPINNER_SETUP
        Spinner repSpinner = (Spinner)findViewById(R.id.rep_spinner);
        Spinner roundSpinner = (Spinner)findViewById(R.id.round_spinner);
        repSpinner.setOnItemSelectedListener(this);
        roundSpinner.setOnItemSelectedListener(this);

        //Spinner adapter setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.dropdown_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repSpinner.setAdapter(adapter);
        roundSpinner.setAdapter(adapter);

        //Set spinners to default values
        int repPos = adapter.getPosition("\u0020" + Integer.toString(mNumReps) + "\u0020");
        int roundPos = adapter.getPosition("\u0020" + Integer.toString(mNumRounds) + "\u0020");
        repSpinner.setSelection(repPos);
        roundSpinner.setSelection(roundPos);

        //endregion
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //region LIFETIME_FUNCTIONS
    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onDestroy(){
        stopService(new Intent(this, TextToSpeechService.class));
        Log.i(DEBUG_TAG, "TextToSpeechService Stopped");
        try{
            unregisterReceiver(mReceiver);
        } catch (Exception e){

        }
        super.onDestroy();
    }
    //endregion

    //endregion

    //region UI_INTERACTIONS

    /**
     * Opens the SetTimeDialog fragment to select the time for the passed TimeType
     * @param view View, the calling UI View (Rep, Rest, or Break)
     */
    public void getTime(View view){
        SetTimeDialogue newTime = new SetTimeDialogue();
        Bundle args = new Bundle();
        if(view.getId() == R.id.set_rep_button){
            args.putString(TIME_TYPE_KEY, ACTION_REP_KEY);
            args.putString(PREF_TYPE_KEY, PREF_REP_KEY);
            args.putIntArray(ACTION_REP_KEY, mRepTime);
        } else if(view.getId() == R.id.set_rest_button){
            args.putString(TIME_TYPE_KEY, ACTION_REST_KEY);
            args.putString(PREF_TYPE_KEY, PREF_REST_KEY);
            args.putIntArray(ACTION_REST_KEY, mRestTime);
        } else if(view.getId() == R.id.set_break_button){
            args.putString(TIME_TYPE_KEY, ACTION_BREAK_KEY);
            args.putString(PREF_TYPE_KEY, PREF_BREAK_KEY);
            args.putIntArray(ACTION_BREAK_KEY, mBreakTime);
        }
        newTime.setArguments(args);
        newTime.show(mFragmentManager, DEBUG_TAG);
    }

    /**
     * Starts the TimerActivity
     * @param view View, the UI view handling the button press
     */
    public void startTimer(View view){
        if(mIsTTSReady){
            Intent intent = new Intent(this, TimerActivity.class);

            intent.putExtra(ACTION_REP_KEY, mRepTimeInMillis);
            intent.putExtra(ACTION_REST_KEY, mRestTimeInMillis);
            intent.putExtra(ACTION_BREAK_KEY, mBreakTimeInMillis);

            intent.putExtra(ACTION_REP_NUM_KEY, mNumReps);
            intent.putExtra(ACTION_ROUND_NUM_KEY, mNumRounds);

            intent.putExtra(ACTION_TTS_READY_KEY, mIsTTSReady);

            startActivity(intent);
        } else {
            Log.e(DEBUG_TAG, "Attempted to start timer before TTS was ready");
            Toast.makeText(this, "Text to speech is not ready.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when the SetTimeDialog fragment Pos key is pressed
     * @param dialog DialogFragment
     */
    @Override
    public void onDialogPositiveClick(DialogFragment dialog){
        Log.i(DEBUG_TAG, "Pos button clicked");
        mRepTimeInMillis = mSharedPref.getInt(PREF_REP_KEY, 0);
        mRestTimeInMillis = mSharedPref.getInt(PREF_REST_KEY, 0);
        mBreakTimeInMillis = mSharedPref.getInt(PREF_BREAK_KEY, 0);
    }

    /**
     * Called when the SetTimeDialog fragment Neg key is pressed
     * @param dialog DialogFragment
     */
    @Override
    public void onDialogNegativeClick(DialogFragment dialog){
        Log.i(DEBUG_TAG, "Neg button clicked");
    }

    /**
     * Called when a number from the dropdowns are selected
     * @param parent AdapterView
     * @param view View, relevant UI view
     * @param pos int, pos of the item selected
     * @param id long, ID of the UI view
     */
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
        SharedPreferences.Editor prefEditor = mSharedPref.edit();
        if(parent.getId() == R.id.rep_spinner){
            mNumReps = Integer.parseInt(parent.getItemAtPosition(pos).toString().trim());
            prefEditor.putInt(PREF_NUM_REPS, mNumReps);
        } else if (parent.getId() == R.id.round_spinner){
            mNumRounds = Integer.parseInt(parent.getItemAtPosition(pos).toString().trim());
            prefEditor.putInt(PREF_NUM_ROUNDS, mNumRounds);
        }

        prefEditor.apply();
    }

    /**
     * When nothing is selected from a dropdown
     * @param parent AdapterView
     */
    public void onNothingSelected(AdapterView<?> parent){
        //do nothing
    }
    //endregion

    //region UTILITY_FUNCTIONS

    /**
     * Converts the time from a 2 piece array to milliseconds
     * @param time int[]
     * @return Returns the time in millis
     */
    public static int convertToMillis(int[] time){
        return ((time[0] * 60) + time[1]) * 1000;
    }

    /**
     * Convers the time from milliseconds to a 2 piece array
     * @param time int
     * @return returns the time as M/S
     */
    public static int[] convertFromMillis(int time){
        int[] newTime = {0, 0};
        newTime[0] = (int)(Math.floor(time/1000)/60);
        newTime[1] = ((time/1000) % 60);
        return newTime;
    }

    public void updateTimeUI(TextView view, int[] time){
        //TODO: Update the relevant UI element
        int minutes = time[0];
        int seconds = time[1];
        if(view != null){
            if(seconds == 0){
                view.setText(String.format(Locale.US, "%d:%d%d", minutes, seconds, 0));
            } else if( seconds < 10){
                view.setText(String.format(Locale.US, "%d:%d%d", minutes, 0, seconds));
            }
            else {
                view.setText(String.format(Locale.US, "%d:%d", minutes, seconds));
            }
        } else {
            Log.e(DEBUG_TAG, "View is null in updateTimeUI");
        }



    }
    //endregion
}
