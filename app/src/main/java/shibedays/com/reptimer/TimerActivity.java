package shibedays.com.reptimer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    //region PRIVATE_TAGS_AND_KEYS
    private final static String DEBUG_TAG = "TimerScreen";
    private final static String REP_OPERATION = "REP_OPERATION";
    private final static String REST_OPERATION = "REST_OPERATION";
    private final static String BREAK_OPERATION = "BREAK_OPERATION";
    //endregion

    //region PUBLIC_TAGS_AND_KEYS
    public final static String UPDATE_TIMER_UI = "com.shibadays.TimerScreen.UPDATE_TIMER_UI";
    public final static String UPDATE_REP_UI = "com.shibadays.TimerScreen.UPDATE_REP_UI";
    public final static String UPDATE_ROUND_UI = "com.shibadays.TimerScreen.UPDATE_ROUND_UI";

    //Broadcast filter for the timer receiver
    public final static String TIMER_BROADCAST_FILTER = "com.shibadays.TimerScreen.TimerFilter";
    //endregion

    //region PRIVATE_VARS
    //Time and Round nums
    private int mRepTimeMillis;
    private int mRestTimeMillis;
    private int mBreakTimeMillis;
    private int mNumRounds;
    private int mNumReps;

    //Is TTS ready or not
    private Boolean mTTSIsReady = false;

    //endregion

    //region PUBLIC_VARS

    //public variables for timeleft and current round
    public int mTimeLeft;
    public int mCurRep;
    public int mCurRound;

    //Map for mapping the diff operations to ints
    public static final HashMap<String, Integer> operation = new HashMap<String, Integer>();
    static {
        operation.put(REP_OPERATION, 0);
        operation.put(REST_OPERATION, 1);
        operation.put(BREAK_OPERATION, 2);
    }
    //endregion

    //region BROADCAST_HANDLER
    /**
     * Broadcast receiver for the timer screen that receives calls from the CountdownService for updating the UI
     */
    private final BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.i(DEBUG_TAG, action);
            if(intent.getExtras() != null) {
                if(action.equals(TIMER_BROADCAST_FILTER)) {
                    mTimeLeft = (int) intent.getLongExtra(UPDATE_TIMER_UI, 0);
                    int newReps = intent.getIntExtra(UPDATE_REP_UI, 0 );
                    int newRounds = intent.getIntExtra(UPDATE_ROUND_UI, 0);
                    //Log.i(DEBUG_TAG, Integer.toString(mTimeLeft));
                    if (mTimeLeft >= 0) {
                        TextView view = findViewById(R.id.time_view);
                        int[] timeArr = MainActivity.convertFromMillis(mTimeLeft);
                        int minutes = timeArr[0], seconds = timeArr[1];
                        if((seconds % 10) == 0){
                            view.setText(String.format(Locale.US, "%d:%d%d", minutes, seconds, 0));
                        }else if (seconds < 10){
                            view.setText(String.format(Locale.US, "%d:%d%d", minutes, 0, seconds));
                        } else {
                            view.setText(String.format(Locale.US, "%d:%d", minutes, seconds));
                        }
                    }
                    if(newReps != mCurRep && newReps != 0){
                        TextView view = findViewById(R.id.rep_view);
                        view.setText(String.format(Locale.US, "%d of %d", newReps, mNumReps));
                        mCurRep = newReps;
                    }
                    if(newRounds != mCurRound && newRounds != 0){
                        TextView view = findViewById(R.id.round_view);
                        view.setText(String.format(Locale.US, "%d of %d", newRounds, mNumRounds));
                        mCurRound = newRounds;
                    }
                }
            }
        }
    };
    //endregion

    //TODO: Interrupt TTS when going back

    //region OVERRIDE_DEFAULT_FUNCTIONS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        //region GET_INTENT_VALUES
        //Get all the values from the intent
        Intent intent = getIntent();
        mRepTimeMillis = intent.getIntExtra(MainActivity.REP_KEY, 0);
        mRestTimeMillis = intent.getIntExtra(MainActivity.REST_KEY, 0);
        mBreakTimeMillis = intent.getIntExtra(MainActivity.BREAK_KEY, 0);

        mNumRounds = intent.getIntExtra(MainActivity.ROUND_NUM_KEY, 0);
        mNumReps = intent.getIntExtra(MainActivity.REP_NUM_KEY, 0);
        mCurRep = 1;
        mCurRound = 1;

        mTTSIsReady = intent.getBooleanExtra(MainActivity.TTS_READY_KEY, false);
        //endregion
        //region TOOLBAR_SETUP
        //Toolbar setup
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //endregion

        //region UI_INITIATION
        //Init all the text fields
        TextView curRepView = findViewById(R.id.rep_view);
        curRepView.setText(String.format(Locale.US, "%d of %d", mCurRep, mNumReps));

        TextView curRoundView = findViewById(R.id.round_view);
        curRoundView.setText(String.format(Locale.US, "%d of %d", mCurRound, mNumRounds));

        TextView timeView = findViewById(R.id.time_view);
        int[] timeArr = MainActivity.convertFromMillis(mRepTimeMillis);
        int minutes = timeArr[0], seconds = timeArr[1];
        Boolean testing = true;
        if((seconds % 10) == 0 && !testing){
            timeView.setText(String.format(Locale.US, "%d:%d%d", minutes, seconds, 0));
        }else if (seconds < 10 && !testing){
            timeView.setText(String.format(Locale.US, "%d:%d%d", minutes, 0, seconds));
        } else {
            timeView.setText(String.format(Locale.US, "%d:%d", minutes, seconds));
        }
        //endregion

        //region BROADCAST_INIT
        //Register the broadcast receiver with the filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(TIMER_BROADCAST_FILTER);
        registerReceiver(mReciever, filter);
        if(mTTSIsReady)
            startTimer();
        else
            Log.e(DEBUG_TAG, "TTS IS NOT READY");
        //endregion
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_timer, menu);
        return true;
    }

    //Called when an options-action is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    @Override
    public void onPause() {
        //unregisterReceiver(mReciever);
        //Log.i(DEBUG_TAG, "Unregistered broadcast receiver");
        super.onPause();
    }

    //TODO: Doublecheck these to make sure we're closing things correctly
    @Override
    public void onResume() {
        //IntentFilter filter = new IntentFilter();
        //filter.addAction(TIMER_BROADCAST_FILTER);
        //registerReceiver(mReciever, filter);
        //Log.i(DEBUG_TAG, "Registered broadcast receiver");
        super.onResume();
    }

    //TODO: onStop is called when the app is minimzed.
    @Override
    public void onStop() {
        try {
            //unregisterReceiver(mReciever);
            //stopService(new Intent(this, CountdownBroadcastService.class));
            //Log.i(DEBUG_TAG, "CountdownBroadcastService Stopped");
        } catch (Exception e) {
            // Receiver was probably already stopped in onPause()
        }
        super.onStop();
    }

    @Override
    public void onRestart(){

        super.onRestart();
    }
    @Override
    public void onDestroy() {
        stopService(new Intent(this, CountdownService.class));
        Log.i(DEBUG_TAG, "CountdownBroadcastService Stopped");
        unregisterReceiver(mReciever);
        super.onDestroy();
    }
    //endregion

    //region TIMER_FUNCTIONS
    //Start the TimerScreen
    public void startTimer(){
        Intent timerIntent = new Intent(this, CountdownService.class);

        timerIntent.putExtra(MainActivity.REP_KEY, mRepTimeMillis);
        timerIntent.putExtra(MainActivity.REST_KEY, mRestTimeMillis);
        timerIntent.putExtra(MainActivity.BREAK_KEY, mBreakTimeMillis);
        timerIntent.putExtra(MainActivity.REP_NUM_KEY, mNumReps);
        timerIntent.putExtra(MainActivity.ROUND_NUM_KEY, mNumRounds);

        startService(timerIntent);
    }

    //Pause the TimerScreen
    public void pauseTimer(View view){

    }

    //Stop the TimerScreen
    public void cancelTimer(View view){

    }
    //endregion

}
