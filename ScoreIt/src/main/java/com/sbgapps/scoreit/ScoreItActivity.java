/*
 * Copyright (C) 2013 SBG Apps
 * http://baiget.fr
 * stephane@baiget.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sbgapps.scoreit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sbgapps.scoreit.game.ClassicBeloteLap;
import com.sbgapps.scoreit.game.CoincheBeloteLap;
import com.sbgapps.scoreit.game.FivePlayerTarotLap;
import com.sbgapps.scoreit.game.FourPlayerTarotLap;
import com.sbgapps.scoreit.game.GameData;
import com.sbgapps.scoreit.game.Lap;
import com.sbgapps.scoreit.game.ThreePlayerTarotLap;
import com.sbgapps.scoreit.game.UniversalLap;
import com.sbgapps.scoreit.util.TypefaceSpan;
import com.sbgapps.scoreit.widget.PlayerInfos;

public class ScoreItActivity extends BaseActivity
        implements NavigationDrawerFragment.NavigationDrawerListener {

    public static final String KEY_SELECTED_GAME = "selected_game";
    public static final String KEY_UNIVERSAL_PLAYER_CNT = "player_count";
    public static final String EXTRA_LAP = "com.sbgapps.scoreit.lap";
    public static final String EXTRA_TABLET = "com.sbgapps.scoreit.tablet";
    public static final String EXTRA_EDIT = "com.sbgapps.scoreit.edit";
    public static final String EXTRA_NAME = "com.sbgapps.scoreit.name";
    private static final int REQ_PICK_CONTACT = 1;
    private static final int REQ_LAP_ACTIVITY = 2;
    private static final int REQ_EDIT_NAME_ACTIVITY = 3;
    private TypefaceSpan mTypefaceSpan;
    private GameData mGameData;
    private SharedPreferences mPreferences;
    private SpannableString mTitle;
    private boolean mIsTablet;
    private PlayerInfos mEditedName;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private ScoreListFragment mScoreListFragment;
    private GraphFragment mGraphFragment;
    private HeaderFragment mHeaderFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAccentDecor();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int game = mPreferences.getInt(KEY_SELECTED_GAME, GameData.BELOTE_CLASSIC);
        mGameData = GameData.getInstance();
        mGameData.init(this, game);

        setContentView(R.layout.activity_scoreit);
        mIsTablet = (null != findViewById(R.id.fragment_container_large));

        final FragmentManager fm = getFragmentManager();

        // Init fragments
        if (null == savedInstanceState) {
            mHeaderFragment = new HeaderFragment();
            if (mIsTablet) {
                mScoreListFragment = new ScoreListFragment();
                mGraphFragment = new GraphFragment();
                fm.beginTransaction()
                        .add(R.id.fragment_header, mHeaderFragment, HeaderFragment.TAG)
                        .add(R.id.fragment_container, mScoreListFragment, ScoreListFragment.TAG)
                        .add(R.id.fragment_container_large, mGraphFragment, GraphFragment.TAG)
                        .commit();
            } else {
                mScoreListFragment = new ScoreListFragment();
                fm.beginTransaction()
                        .add(R.id.fragment_header, mHeaderFragment, HeaderFragment.TAG)
                        .add(R.id.fragment_container, mScoreListFragment, ScoreListFragment.TAG)
                        .commit();
            }
        } else {
            mHeaderFragment = (HeaderFragment) fm.findFragmentByTag(HeaderFragment.TAG);
            mGraphFragment = (GraphFragment) fm.findFragmentByTag(GraphFragment.TAG);
            mScoreListFragment = (ScoreListFragment) fm.findFragmentByTag(ScoreListFragment.TAG);
        }

        // Init drawer
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                fm.findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout), game);

        mTypefaceSpan = new TypefaceSpan(this, "Lobster.otf");
        setTitle();
    }

    public TypefaceSpan getTypefaceSpan() {
        return mTypefaceSpan;
    }

    public boolean isTablet() {
        return mIsTablet;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mNavigationDrawerFragment.isDrawerOpen()) {
            menu.clear();
            return false;
        } else {
            MenuItem item;
            if (0 == mGameData.getLaps().size()) {
                if (!mIsTablet) {
                    item = menu.findItem(R.id.menu_view);
                    item.setVisible(false);
                }
                item = menu.findItem(R.id.menu_clear);
                item.setVisible(false);
            }
            item = menu.findItem(R.id.menu_count);
            item.setVisible(GameData.UNIVERSAL == mGameData.getGame());
            getActionBar().setTitle(mTitle);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                showClearDialog();
                return true;

            case R.id.menu_new:
                addLap();
                return true;

            case R.id.menu_view:
                switchScoreViews();
                return true;

            case R.id.menu_count:
                showPlayerCountDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerGameSelected(int game) {
        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        mPreferences.edit().putInt(KEY_SELECTED_GAME, game).commit();
        mGameData.setGame(game);
        setTitle();

        reloadFragments();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String name;
        if (RESULT_OK != resultCode) return;

        switch (requestCode) {
            case REQ_PICK_CONTACT:
                Cursor cursor = getContentResolver().query(data.getData(),
                        new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    name = cursor.getString(columnIndex);
                    nameEdited(name);
                }
                break;

            case REQ_LAP_ACTIVITY:
                updateFragments();
                invalidateOptionsMenu();
                break;

            case REQ_EDIT_NAME_ACTIVITY:
                name = data.getStringExtra(EXTRA_NAME);
                nameEdited(name);
                break;
        }
    }

    public void addLap() {
        Lap lap;
        switch (mGameData.getGame()) {
            default:
            case GameData.UNIVERSAL:
                lap = new UniversalLap();
                break;
            case GameData.BELOTE_CLASSIC:
                lap = new ClassicBeloteLap();
                break;
            case GameData.BELOTE_COINCHE:
                lap = new CoincheBeloteLap();
                break;
            case GameData.TAROT_3_PLAYERS:
                lap = new ThreePlayerTarotLap();
                break;
            case GameData.TAROT_4_PLAYERS:
                lap = new FourPlayerTarotLap();
                break;
            case GameData.TAROT_5_PLAYERS:
                lap = new FivePlayerTarotLap();
                break;
        }
        showLapActivity(lap, false);
    }

    public void editLap(Lap lap) {
        showLapActivity(lap, true);
    }

    public void removeLap(Lap lap) {
        mGameData.removeLap(lap);
        updateFragments();
    }

    public void editName(View view) {
        mEditedName = (PlayerInfos) view.getParent().getParent();
        showEditNameActionChoices();
    }

    public void start(View view) {
        addLap();
    }

    private void reloadFragments() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
        mHeaderFragment = new HeaderFragment();
        ft.replace(R.id.fragment_header, mHeaderFragment, HeaderFragment.TAG);
        ft.commit();

        ft = getFragmentManager().beginTransaction();
        if (mIsTablet) {
            ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
            mScoreListFragment = new ScoreListFragment();
            mGraphFragment = new GraphFragment();
            ft.replace(R.id.fragment_container, mScoreListFragment, ScoreListFragment.TAG);
            ft.replace(R.id.fragment_container_large, mGraphFragment, GraphFragment.TAG);
        } else {
            ft.setCustomAnimations(R.animator.slide_top_in, R.animator.slide_top_out);
            mScoreListFragment = new ScoreListFragment();
            ft.replace(R.id.fragment_container, mScoreListFragment, ScoreListFragment.TAG);
        }
        ft.commit();
    }

    private void updateFragments() {
        mHeaderFragment.updateScores();
        if (null != mScoreListFragment && mScoreListFragment.isVisible())
            mScoreListFragment.getListAdapter().notifyDataSetChanged();
        if (null != mGraphFragment && mGraphFragment.isVisible())
            mGraphFragment.traceGraph();
    }

    private void showLapActivity(Lap lap, boolean edit) {
        Intent intent;
        if (null != mScoreListFragment) mScoreListFragment.getListView().closeOpenedItems();

        switch (mGameData.getGame()) {
            default:
            case GameData.UNIVERSAL:
                intent = new Intent(this, UniversalLapActivity.class);
                break;
            case GameData.BELOTE_CLASSIC:
                intent = new Intent(this, BeloteLapActivity.class);
                break;
            case GameData.BELOTE_COINCHE:
                intent = new Intent(this, CoincheLapActivity.class);
                break;
            case GameData.TAROT_3_PLAYERS:
            case GameData.TAROT_4_PLAYERS:
            case GameData.TAROT_5_PLAYERS:
                intent = new Intent(this, TarotLapActivity.class);
                break;
        }
        intent.putExtra(EXTRA_LAP, lap);
        intent.putExtra(EXTRA_TABLET, mIsTablet);
        intent.putExtra(EXTRA_EDIT, edit);

        startActivityForResult(intent, REQ_LAP_ACTIVITY);
    }

    private void switchScoreViews() {
        if (null != mGraphFragment && mGraphFragment.isVisible()) {
            getFragmentManager().popBackStack();
            return;
        }

        if (null == mGraphFragment)
            mGraphFragment = new GraphFragment();

        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_bottom_in,
                        R.animator.slide_top_out,
                        R.animator.slide_top_in,
                        R.animator.slide_bottom_out)
                .addToBackStack(null)
                .replace(R.id.fragment_container, mGraphFragment, GraphFragment.TAG)
                .commit();
    }

    private void setTitle() {
        switch (mGameData.getGame()) {
            default:
            case GameData.UNIVERSAL:
                mTitle = new SpannableString(getResources().getString(R.string.universal));
                break;
            case GameData.BELOTE_CLASSIC:
                mTitle = new SpannableString(getResources().getString(R.string.belote));
                break;
            case GameData.BELOTE_COINCHE:
                mTitle = new SpannableString(getResources().getString(R.string.coinche));
                break;
            case GameData.TAROT_3_PLAYERS:
            case GameData.TAROT_4_PLAYERS:
            case GameData.TAROT_5_PLAYERS:
                mTitle = new SpannableString(getResources().getString(R.string.tarot));
                break;
        }
        mTitle.setSpan(mTypefaceSpan, 0, mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void nameEdited(String name) {
        mGameData.setPlayerName(mEditedName.getPlayer(), name);
        mEditedName.setName(name);
    }

    private void showClearDialog() {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.clear_game)
                .setMessage(R.string.sure)
                .setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mGameData.deleteAll();
                                mHeaderFragment.updateScores();
                                if (null != mScoreListFragment && mScoreListFragment.isVisible())
                                    mScoreListFragment.getListAdapter().removeAll();
                                if (null != mGraphFragment && mGraphFragment.isVisible()) {
                                    mGraphFragment.traceGraph();
                                    if (!mIsTablet) getFragmentManager().popBackStack();
                                }
                                invalidateOptionsMenu();
                            }
                        })
                .setNegativeButton(
                        R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nothing to do!
                            }
                        })
                .create();
        dialog.show();
        getAccentHelper().prepareDialog(this, dialog.getWindow());
    }

    private void showPlayerCountDialog() {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.player_number)
                .setItems(R.array.player_count,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPreferences
                                        .edit()
                                        .putInt(KEY_UNIVERSAL_PLAYER_CNT, which + 2)
                                        .commit();
                                reloadFragments();
                            }
                        })
                .create();
        dialog.show();
        getAccentHelper().prepareDialog(this, dialog.getWindow());
    }

    private void showEditNameActionChoices() {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_name)
                .setItems(R.array.edit_name_action,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent;
                                switch (which) {
                                    default:
                                    case 0:
                                        intent = new Intent(Intent.ACTION_PICK,
                                                ContactsContract.Contacts.CONTENT_URI);
                                        startActivityForResult(intent, REQ_PICK_CONTACT);
                                        break;
                                    case 1:
                                        intent = new Intent(ScoreItActivity.this, EditNameActivity.class);
                                        startActivityForResult(intent, REQ_EDIT_NAME_ACTIVITY);
                                        break;
                                }
                            }
                        })
                .create();
        dialog.show();
        getAccentHelper().prepareDialog(this, dialog.getWindow());
    }
}
