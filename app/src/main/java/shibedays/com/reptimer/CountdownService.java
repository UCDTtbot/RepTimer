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
    private final static String DEBUG_TAG = CountdownService.class.getSimpleName();
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
    private Intent mNotifIntent;
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
        mRepTime = intent.getIntExtra(MainActivity.ACTION_REP_KEY, 0);
        mRestTime = intent.getIntExtra(MainActivity.ACTION_REST_KEY, 0);
        mBreakTime = intent.getIntExtra(MainActivity.ACTION_BREAK_KEY, 0);
        mNumReps = intent.getIntExtra(MainActivity.ACTION_REP_NUM_KEY, 1);
        mNumRounds = intent.getIntExtra(MainActivity.ACTION_ROUND_NUM_KEY, 1);
        //Init cur rep and round to the first rep/round
        mCurRep = 1;
        mCurRound = 1;
        mNotifIntent = new Intent(this, TimerActivity.class);
        //endregion

        //region NOTIFICATION_SETUP
        mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //TODO: stick this intent with shit like we do in main activity

        mNotifIntent.putExtra(MainActivity.ACTION_REP_KEY, mRepTime);
        mNotifIntent.putExtra(MainActivity.ACTION_REST_KEY, mRestTime);
        mNotifIntent.putExtra(MainActivity.ACTION_BREAK_KEY, mBreakTime);

        mNotifIntent.putExtra(MainActivity.ACTION_REP_NUM_KEY, mNumReps);
        mNotifIntent.putExtra(MainActivity.ACTION_ROUND_NUM_KEY, mNumRounds);

        mNotifIntent.putExtra(MainActivity.ACTION_TTS_READY_KEY, 1);
        mNotifIntent.putExtra(TimerActivity.ACTION_SERVICE_RUNNING_KEY, 1);
        mNotifIntent.putExtra(TimerActivity.ACTION_CURRENT_REP_KEY, mCurRep);
        mNotifIntent.putExtra(TimerActivity.ACTION_CURRENT_ROUND_KEY, mCurRound);

        /*  Dunno if I actually need this stackbuilder stuff
        //https://developer.android.com/guide/topics/ui/notifiers/notifications.html
        //Stack Builder ensures we can return through our app
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //Adds where to go from, the back stack for the intent (but not the intent)
        stackBuilder.addParentStack(TimerActivity.class);
        //Add the intent that starts the activity
        stackBuilder.addNextIntent(notifIntent);
        */
        //TODO: one possibility for fixing this is to send off a bundle of the relevent data in this pendingIntent
        //TODO: or to use the notification as a broadcast instead? but then how do we get the service back
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIF_ID, mNotifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
            intent.putExtra(ACTION_REP_KEY, mRepTimeInMillis);
            intent.putExtra(ACTION_REST_KEY, mRestTimeInMillis);
            intent.putExtra(ACTION_BREAK_KEY, mBreakTimeInMillis);

            intent.putExtra(ACTION_REP_NUM_KEY, mNumReps);
            intent.putExtra(ACTION_ROUND_NUM_KEY, mNumRounds);

            intent.putExtra(ACTION_TTS_READY_KEY, mIsTTSReady);
         */

        mBuilder = new NotificationCompat.Builder(this, "MainTimerChannel_1");
        mBuilder.setContentTitle("Timer")
                .setContentText("Rep: " + mCurRep + " Round: " + mCurRound )
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_timer_white)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        startForeground(NOTIF_ID, mBuilder.build());
        //endregion

        mCurrentAction = REPETITION_ACTION;
        mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Starting in. 5. 4. 3. 2. 1.");
        sendBroadcast(mTTSBroadcastIntent);
        beginTimer(mRepTime);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        mHandler.removeCallbacks(timerUpdate);
        Intent intent = new Intent(TextToSpeechService.TTS_BROADCAST_STOP);
        sendBroadcast(intent);
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
        mHandler.postDelayed(timerUpdate, (ONE_SECOND * 5));
    }

    private void updateNotification(){
        mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifIntent.putExtra(MainActivity.ACTION_REP_KEY, mRepTime);
        mNotifIntent.putExtra(MainActivity.ACTION_REST_KEY, mRestTime);
        mNotifIntent.putExtra(MainActivity.ACTION_BREAK_KEY, mBreakTime);

        mNotifIntent.putExtra(MainActivity.ACTION_REP_NUM_KEY, mNumReps);
        mNotifIntent.putExtra(MainActivity.ACTION_ROUND_NUM_KEY, mNumRounds);

        mNotifIntent.putExtra(MainActivity.ACTION_TTS_READY_KEY, 1);
        mNotifIntent.putExtra(TimerActivity.ACTION_SERVICE_RUNNING_KEY, 1);
        mNotifIntent.putExtra(TimerActivity.ACTION_CURRENT_REP_KEY, mCurRep);
        mNotifIntent.putExtra(TimerActivity.ACTION_CURRENT_ROUND_KEY, mCurRound);

        //Dunno if I actually need this stackbuilder stuff
        //https://developer.android.com/guide/topics/ui/notifiers/notifications.html

        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIF_ID, mNotifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this, "MainTimerChannel_1");
        mBuilder.setContentTitle("Timer")
                .setContentText("Rep: " + mCurRep + " Round: " + mCurRound )
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_timer_white)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotifManager.notify(NOTIF_ID, mBuilder.build());
    }

    private void continueTimer(int time, int delay){
        mMillisLeft = time;
        mHandler.removeCallbacks(timerUpdate);
        mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, time);
        sendBroadcast(mTimerBroadcastIntent);
        mHandler.postDelayed(timerUpdate, ONE_SECOND * delay);
    }


    //endregion

    //TODO: Rest time is not correctly updating until about 7 seconds left in the countdown
    private Runnable timerUpdate = new Runnable(){
        public void run(){
            mMillisLeft -= ONE_SECOND;

            if(mMillisLeft > 0) {
                if(mMillisLeft == 7000 && mCurrentAction.equals(REST_ACTION)){
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Rest ending in");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 5000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "5.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 4000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "4.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 3000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "3.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 2000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "2.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                if(mMillisLeft == 1000) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "1.");
                    sendBroadcast(mTTSBroadcastIntent);
                }
                mHandler.postDelayed(this, ONE_SECOND); //1000ms = 1s

            } else if(mMillisLeft <= 0){
                Log.i(DEBUG_TAG, "TimerScreen Finished");

                if(mCurrentAction.equals(REPETITION_ACTION) && mCurRep == mNumReps && mCurRound != mNumRounds) {
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Round finished. Take a break.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mCurrentAction = BREAK_ACTION;
                    continueTimer(mBreakTime, 2);

                } else if(mCurrentAction.equals(REPETITION_ACTION) && mCurRep == mNumReps && mCurRound == mNumRounds){
                    //TODO: "Finished" gets cut off because of TimerActivity exiting
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Finished.");
                    sendBroadcast(mTTSBroadcastIntent);
                    stopForeground(true);
                    //TODO: Send us back to MainScreen
                    Intent finished = new Intent(TimerActivity.TIMER_BROADCAST_FINISHED);
                    finished.putExtra(TimerActivity.ACTION_TIMER_FINISHED, true);
                    sendBroadcast(finished);
                    stopSelf();
                } else if(mCurrentAction.equals(REPETITION_ACTION) && mCurRep != mNumReps){
                    //Repetition finished. Rest.

                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Take a rest.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mCurrentAction = REST_ACTION;
                    continueTimer(mRestTime, 2);
                } else if(mCurrentAction.equals(REST_ACTION)){
                    //Rest finished. Start next rep.
                    mCurRep++;
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_REP_UI, mCurRep);
                    sendBroadcast(mTimerBroadcastIntent);
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Begin.");
                    updateNotification();
                    sendBroadcast(mTTSBroadcastIntent);

                    mCurrentAction = REPETITION_ACTION;
                    continueTimer(mRepTime, 1);
                } else if(mCurrentAction.equals(BREAK_ACTION)){
                    //Break finished, start next round
                    mCurRound++;
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_ROUND_UI, mCurRound);
                    sendBroadcast(mTimerBroadcastIntent);
                    mTTSBroadcastIntent.putExtra(TextToSpeechService.TTS_SPEECH, "Beginning next round.");
                    sendBroadcast(mTTSBroadcastIntent);

                    mCurRep = 1;
                    updateNotification();
                    mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_REP_UI, mCurRep);
                    sendBroadcast(mTimerBroadcastIntent);

                    mCurrentAction = REPETITION_ACTION;
                    continueTimer(mRepTime, 2);
                } else {

                }
            } else {
                Log.e(DEBUG_TAG, "Something went wrong");
            }

            mTimerBroadcastIntent.putExtra(TimerActivity.UPDATE_TIMER_UI, mMillisLeft);
            sendBroadcast(mTimerBroadcastIntent);
        }
    };



}
