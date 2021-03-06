package com.sbgapps.scoreit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sbgapps.scoreit.LapActivity;
import com.sbgapps.scoreit.R;

/**
 * Created by sbaiget on 26/01/14.
 */
public class SeekbarInputPoints extends FrameLayout {

    private final LapActivity mContext;
    private final TextView mTextViewPoints;
    private final SeekBar mSeekBarPoints;
    private int mProgress;

    public SeekbarInputPoints(Context context) {
        this(context, null);
    }

    public SeekbarInputPoints(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekbarInputPoints(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = (LapActivity) context;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.seekbar_input_points, this, true);

        mTextViewPoints = (TextView) findViewById(R.id.textview_points);
        mSeekBarPoints = (SeekBar) findViewById(R.id.seekbar_points);

        init();
    }

    private void init() {
        ImageButton button = (ImageButton) findViewById(R.id.btn_less);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgress--;
                displayPoints();
            }
        });

        button = (ImageButton) findViewById(R.id.btn_more);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgress++;
                displayPoints();
            }
        });

        mSeekBarPoints.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mProgress = progress;
                    displayPoints();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void setMax(int max) {
        mSeekBarPoints.setMax(max);
    }

    public int getPoints() {
        return mContext.progressToPoints(mProgress);
    }

    public void setPoints(int points) {
        mProgress = mContext.pointsToProgress(points);
        displayPoints();
    }

    public void displayPoints() {
        int points = mContext.progressToPoints(mProgress);
        mTextViewPoints.setText(Integer.toString(points));
        mSeekBarPoints.setProgress(mProgress);
    }
}
