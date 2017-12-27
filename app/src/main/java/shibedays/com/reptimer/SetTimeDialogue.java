package shibedays.com.reptimer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Locale;


public class SetTimeDialogue extends DialogFragment implements NumberPicker.OnValueChangeListener{

    //region PRIVATE_VARS
    //Log tag
    private static String DEBUG_TAG = "SetTimeDialogue.tag";
    //The time array being set
    private int mTime[];
    //Preference editor for saving values
    private SharedPreferences.Editor mPrefEditor;
    //The type of time we are working with
    private String mTimeType;
    private String mPrefType;
    //The parent
    private MainActivity mParentActivity;
    //endregion

    //region PUBLIC_VARS
    //interface to be implemented for way the pos and neg buttons of the dialog are pressed
    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    NoticeDialogListener mListener;
    //endregion

    //region OVERRIDE_DEFAULT_FUNCTIONS
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Activity activity = null;
        if(context instanceof Activity)
            activity = (Activity)context;
        try{
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e){
            if(activity != null)
                throw new ClassCastException(activity.toString() + "must implement NoticeDialogListener");
            else
                throw new RuntimeException("Activity is null... why");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstances){
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        mParentActivity = (MainActivity) getActivity();
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_set_time_dialogue, null);

        Bundle args = getArguments();
        mTimeType = args.getString(MainActivity.TIME_TYPE_KEY);
        mPrefType = args.getString(MainActivity.PREF_TYPE_KEY);
        mTime = args.getIntArray(mTimeType);

        NumberPicker minutePicker = view.findViewById(R.id.MinutePicker);
        NumberPicker secondsPicker = view.findViewById(R.id.SecondsPicker);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(mTime[0]);
        minutePicker.setWrapSelectorWheel(true);
        minutePicker.setOnValueChangedListener(this);

        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(59);
        secondsPicker.setValue(mTime[1]);
        secondsPicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {
                return String.format(Locale.US,"%02d", i);            }
        });
        secondsPicker.setWrapSelectorWheel(true);
        secondsPicker.setOnValueChangedListener(this);

        builder.setView(view)
                .setTitle("Choose a time")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mPrefEditor = getActivity().getSharedPreferences(MainActivity.PREF_IDENTIFIER, Context.MODE_PRIVATE).edit();
                        mPrefEditor.putInt(mPrefType, MainActivity.convertToMillis(mTime));
                        mPrefEditor.apply();
                        TextView view = null;
                        if(MainActivity.TIME_TYPE.get(mTimeType) == 0){ //Rep
                            view = mParentActivity.findViewById(R.id.rep_time);
                        }else if(MainActivity.TIME_TYPE.get(mTimeType) == 1) { //Rest
                            view = mParentActivity.findViewById(R.id.rest_time);
                        }else if(MainActivity.TIME_TYPE.get(mTimeType) == 2) { // Break
                            view = mParentActivity.findViewById(R.id.break_time);
                        }else{
                            Log.e(DEBUG_TAG, "Unable to set view in dialog fragment positive button click.");
                        }
                        mParentActivity.updateTimeUI(view, mTime);
                        mListener.onDialogPositiveClick(SetTimeDialogue.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(SetTimeDialogue.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }


    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal){
        if(picker.getId() == R.id.MinutePicker){
            mTime[0] = newVal;
        }else if(picker.getId() == R.id.SecondsPicker) {
            mTime[1] = newVal;
        }
    }
    //endregion

}
