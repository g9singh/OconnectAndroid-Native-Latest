package com.ointerface.oconnect.activities;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.ointerface.oconnect.R;
import com.ointerface.oconnect.fragments.EventDetailViewFragment;
import com.ointerface.oconnect.fragments.SpeakerDetailViewFragment;
import com.ointerface.oconnect.util.AppUtil;

import java.util.ArrayList;

import io.realm.RealmObject;

import static android.view.View.GONE;

public class SpeakerDetailViewActivity extends OConnectBaseActivity {

    public ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    static public ArrayList<RealmObject> mItems;

    private int currentSpeakerNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker_detail_view);
        super.onCreateDrawer();

        tvToolbarTitle.setText("Speaker");

        ivProfileLanyard.setVisibility(GONE);
        ivHelp.setVisibility(GONE);
        ivRightToolbarIcon.setVisibility(GONE);
        ivSearch.setVisibility(GONE);

        ivHeaderBack.setVisibility(View.VISIBLE);
        tvHeaderBack.setVisibility(View.VISIBLE);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Drawable dr = getResources().getDrawable(R.drawable.icon_back);
        Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();

        Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, AppUtil.convertDPToPXInt(SpeakerDetailViewActivity.this, 20), AppUtil.convertDPToPXInt(SpeakerDetailViewActivity.this, 20), true));

        getSupportActionBar().setHomeAsUpIndicator(d);

        ivHeaderBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeakerDetailViewActivity.this.finish();
            }
        });

        tvHeaderBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeakerDetailViewActivity.this.finish();
            }
        });

        tvHeaderBack.setText("Back");

        tvHeaderBack.bringToFront();

        currentSpeakerNumber = getIntent().getIntExtra("SPEAKER_NUMBER", 0);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new SpeakerDetailViewActivity.ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(currentSpeakerNumber);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return SpeakerDetailViewFragment.newInstance(position, SpeakerDetailViewActivity.this, mItems, currentSpeakerNumber);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }
    }
}
