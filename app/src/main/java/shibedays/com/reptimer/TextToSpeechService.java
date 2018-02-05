package shibedays.com.reptimer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechService extends Service implements TextToSpeech.OnInitListener {

    //region PRIVATE_TAGS_AND_KEYS
    private final static String SPEECH_ID = "com.shibadays.mTTS.speech";
    private static final String DEBUG_TAG = TextToSpeechService.class.getSimpleName();
    //endregion

    //region PUBLIC_TAGS_AND_KEYS
    //Broadcast Action
    public final static String TTS_SPEECH = "com.shibadays.mTTS.sentence";
    //Broadcast filter
    public final static String TTS_BROADCAST_FILTER = "com.shibadays.mTTS.broadcast_filter";
    public final static String TTS_BROADCAST_STOP = "com.shibadays.mTTS.broadcast_stop";
    //endregion

    //region PRIVATE_VARS
    //TTS object
    private TextToSpeech mTTS;
    private boolean mIsTTSReady = false;
    //endregion

    //region BROADCAST_RECIEVER
    /**
     * Broadcast receiver filtered to receive speech strings for the TTS service to say
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String speechString = intent.getStringExtra(TTS_SPEECH);
            Log.i(DEBUG_TAG, "TTS Received action: " + action);
            if(intent.getExtras() != null) {
                if(action.equals(TTS_BROADCAST_FILTER)) {
                    if(mIsTTSReady){
                        mTTS.speak(speechString, TextToSpeech.QUEUE_FLUSH, null, SPEECH_ID);
                        Log.i(DEBUG_TAG, "Speaking: " + speechString);
                    }
                } else if(action.equals(TTS_BROADCAST_STOP)) {
                    mTTS.stop();
                }
                //else if (action == TimerScreen.TTS_READY_FILTER) {
                //idk what this was...
                //}
            }
        }
    };
    //endregion

    //region OVERRIDE_DEFAULT_REGION

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        //init mTTS
        mTTS = new TextToSpeech(this, this);
        //TODO: adjust this speed
        mTTS.setSpeechRate(0.7f);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TTS_BROADCAST_FILTER);
        intentFilter.addAction(TTS_BROADCAST_STOP);
        registerReceiver(mReceiver, intentFilter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        //Keep the service sticky
        return START_STICKY;
    }

    @Override
    public void onInit(int status){
        //This is called once the text to speech has finished init'ing
        if(status == TextToSpeech.SUCCESS){
            int result = mTTS.setLanguage(Locale.US);

            if(result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.i(DEBUG_TAG, "TTS is speaking with id: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(DEBUG_TAG, "Something bad happened in TTS Utterance");
                    }
                });
            } else {
                Log.e(DEBUG_TAG, "Missing data or Language not supported.");
            }

            mIsTTSReady = true;
            Intent broadcastIntent = new Intent(MainActivity.TTS_BROADCAST_FILTER);
            broadcastIntent.putExtra(MainActivity.ACTION_TTS_READY_KEY, mIsTTSReady);
            Log.i(DEBUG_TAG, "TTS Good");
            sendBroadcast(broadcastIntent);

        } else {
            Log.e(DEBUG_TAG, "TTS Failed");
        }
    }

    @Override
    public void onDestroy(){
        //destroy the mTTS
        unregisterReceiver(mReceiver);
        Log.i(DEBUG_TAG, "TTS Destroyed");
        mTTS.shutdown();
    }
    //endregion
}
