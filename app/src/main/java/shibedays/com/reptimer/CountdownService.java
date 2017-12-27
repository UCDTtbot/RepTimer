package shibedays.com.reptimer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class CountdownService extends Service {

    //region PRIVATE_TAGS_AND_KEYS
    private final static String DEBUG_TAG = "com.sd.CntDwnServ";
    //TODO: What are these actions used for?
    private final static String REPETITION_ACTION = "com.sd.CntDwnRep";
    private final static String REST_ACTION = "com.sd.CntDwnRest";
    private final static String BREAK_ACTION = "com.sd.CntDwnBreak";
    //endregion

    //region PRIVATE_VARS
    //private constants
    private final static int NOTIF_ID = 112893;
    private final static long ONE_SECOND = 1000;

    //private intents
    private Intent mTimerBroadcastIntent = new Intent(TimerActivity.TIMER_BROADCAST_FILTER);
    private Intent mTTSBroadcastIntent = new Intent(TextToSpeechService.TTS_BROADCAST_FILTER);

    //private vars
    private int mRepTime;
    private int mRestTime;
    private int mBreakTime;
    private int mNumRounds;
    private int mNumReps;

    private int mCurRep;
    private int mCurRound;

    private long mMillisLeft = 0;
    private String mCurrentAction;

    private Handler mHandler = new Handler();

    //Notification Manager and Builder
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifManager;
    //endregion

    //region OVERRIDE_DEFAULT_FUNCTIONS
    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        //region VARIABLE_INIT
        //get intent and do shit
        //get the data from the intent and create the TimerScreen
        mRepTime = intent.getIntExtra(MainActivity.REP_KEY, 0);
        mRestTime = intent.getIntExtra(MainActivity.REST_KEY, 0);
        mBreakTime = intent.getIntExtra(MainActivity.BREAK_KEY, 0);
        mNumReps = intent.getIntExtra(MainActivity.REP_NUM_KEY, 1);
        mNumRounds = intent.getIntExtra(MainActivity.ROUND_NUM_KEY, 1);
        //Init cur rep and round to the first rep/round
        mCurRep = 1;
        mCurRound = 1;
        //endregion

        //region NOTIFICATION_SETUP
        //TODO: Finish setting up the notification
        mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notifIntent = new Intent(this, TimerActivity.class);
        //https://developer.android.com/guide/topics/ui/notifiers/notifications.html
        //Stack Builder ensures we can return through our app
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //Adds where to go from, the back stack for the intent (but not the intent)
        stackBuilder.addParentStack(TimerActivity.class);
        //Add the intent that starts the activity
        stackBuilder.addNextIntent(notifIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this, "MainTimerChannel_1");
        mBuilder.setContentTitle("My Foreground Notif")
                .setContentText("This is the notification running text")
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        startForeground(NOTIF_ID, mBuilder.build());
        //endregion

        //TODO: Do the TTS speech here
        mCurrentAction = REPETITION_ACTION;
        beginTimer(mRepTime);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        mHandler.removeCallbacks(timerUpdate);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    //endregion

    //region UTILITY
    private void beginTimer(int time){
        mMillisLeft = time;
        mHandler.removeCallbacks(timerUpdate);
        //TODO: this countdown is too fast
        mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Starting in. 5. 4. 3. 2. 1.");
        sendBroadcast(mTTSBroadcastIntent);

        mHandler.postDelayed(timerUpdate, (ONE_SECOND * 5));
    }
    //endregion

    //TODO: Countdown is skipping the rest time
    private Runnable timerUpdate = new Runnable(){
        public void run(){
            mMillisLeft -= ONE_SECOND;
            mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, mMillisLeft);
            sendBroadcast(mTimerBroadcastIntent);
            if(mMillisLeft > 0) {
                if(mMillisLeft == 7000 && mCurrentAction.equals(REST_ACTION)){
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Rest ending in");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 5000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "5.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 4000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "4.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 3000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "3.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 2000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "2.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 1000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "1.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                mHandler.postDelayed(this, ONE_SECOND); //1000ms = 1s

            } else if(mMillisLeft <= 0){
                Log.i(DEBUG_TAG, "TimerScreen Finished");

                if(mCurrentAction.equals(REPETITION_ACTION) && mCurRep == mNumReps && mCurRound != mNumRounds) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Round finished. Take a break.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mCurrentAction = BREAK_ACTION;
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, mBreakTime);

                    sendBroadcast(mTimerBroadcastIntent);
                    beginTimer(mBreakTime);
                } else if(mCurrentAction.equals(REPETITION_ACTION) && mCurRep == mNumReps && mCurRound == mNumRounds){
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Finished.");
                    sendBroadcast(mTTSBroadcastIntent);
                    stopForeground(true);
                    //TODO: Send us back to MainScreen
                    stopSelf();
                } else if(mCurrentAction.equals(REPETITION_ACTION) && mCurRep != mNumReps){
                    //Repetition finished. Rest.
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Take a rest.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, mRestTime);
                    sendBroadcast(mTimerBroadcastIntent);

                    mCurrentAction = REST_ACTION;
                    beginTimer(mRestTime);
                } else if(mCurrentAction.equals(REST_ACTION)){
                    //Rest finished. Start next rep.
                    mCurRep++;
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_REP_UI, mCurRep);
                    sendBroadcast(mTimerBroadcastIntent);
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Begin.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, mRepTime);
                    sendBroadcast(mTimerBroadcastIntent);

                    //TODO: Timer is skipping over rest time
                    mCurrentAction = REPETITION_ACTION;
                    beginTimer(mRepTime);
                } else if(mCurrentAction.equals(BREAK_ACTION)){
                    //Break finished, start next round
                    mCurRound++;
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_ROUND_UI, mCurRound);
                    sendBroadcast(mTimerBroadcastIntent);
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH_BROADCAST, "Beginning next round.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mCurRep = 1;
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_REP_UI, mCurRep);
                    sendBroadcast(mTimerBroadcastIntent);

                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, mRepTime);
                    sendBroadcast(mTimerBroadcastIntent);

                    mCurrentAction = REPETITION_ACTION;
                    beginTimer(mRepTime);
                } else {

                }
            } else {
                Log.e(DEBUG_TAG, "Something went wrong");
            }
        }
    };



}
