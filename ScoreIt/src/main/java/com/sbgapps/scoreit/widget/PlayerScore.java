package com.sbgapps.scoreit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.sbgapps.scoreit.R;

/**
 * Created by sbaiget on 21/01/14.
 */
public class PlayerScore extends FrameLayout {

    private int mPlayer;
    private final TextView mScore;
    private final ImageView mImage;

    public PlayerScore(Context context) {
        this(context, null);
    }

    public PlayerScore(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerScore(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_score, this, true);

        mScore = (TextView) findViewById(R.id.score);
        mImage = (ImageView) findViewById(R.id.image);
    }

    public void setPlayer(int player) {
        mPlayer = player;
    }

    public int getPlayer() {
        return mPlayer;
    }

    public TextView getScore() {
        return mScore;
    }

    public ImageView getImage() {
        return mImage;
    }
}
