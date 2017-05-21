package com.ointerface.oconnect.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ointerface.oconnect.App;
import com.ointerface.oconnect.R;
import com.ointerface.oconnect.adapters.MyAgendaListViewAdapter;
import com.ointerface.oconnect.adapters.ParticipantsSwipeListAdapter;
import com.ointerface.oconnect.data.Attendee;
import com.ointerface.oconnect.data.Session;
import com.ointerface.oconnect.data.Speaker;
import com.ointerface.oconnect.util.AppConfig;
import com.ointerface.oconnect.util.AppUtil;

import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.view.View.GONE;

public class ParticipantsActivity extends OConnectBaseActivity {
    private ListView lvParticipantsList;
    private ParticipantsSwipeListAdapter adapter;

    private SearchView participantsSearch;

    private BottomNavigationView navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants);
        super.onCreateDrawer();

        tvToolbarTitle = (TextView) findViewById(R.id.tvToolbarTitle);

        if (selectedConference.getToolbarLabelParticipants() != null &&
                !selectedConference.getToolbarLabelParticipants().equalsIgnoreCase("")) {
            tvToolbarTitle.setText(selectedConference.getToolbarLabelParticipants());
        } else {
            tvToolbarTitle.setText("Participants");
        }

        ivSearch.setVisibility(GONE);

        ivProfileLanyard.setVisibility(View.VISIBLE);

        ivProfileLanyard.setBackgroundResource(R.drawable.icon_header_calendar);

        ivProfileLanyard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ParticipantsActivity.this, ScheduleActivity.class);
                startActivity(i);
            }
        });

        initSearchView();

        ivHelp.setVisibility(View.VISIBLE);

        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setBackgroundColor(AppUtil.getPrimaryThemColorAsInt());

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
                new int[] { android.R.attr.state_checked}, // unchecked
                new int[] { android.R.attr.state_pressed},  // pressed
                new int[] { android.R.attr.state_selected}  // selected
        };

        int[] colors = new int[] {
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.WHITE,
                Color.WHITE
        };

        ColorStateList colorStateList = new ColorStateList(states, colors);

        navigation.setItemTextColor(colorStateList);

        View view1 = navigation.findViewById(R.id.navigation_speakers);

        view1.setPadding(0,0,0,30);

        View view2 = navigation.findViewById(R.id.navigation_attendees);

        view2.setPadding(0,0,0,30);

        Menu menu = navigation.getMenu();

        if (selectedConference.getParticipantsLabelSpeakers() != null &&
                !selectedConference.getParticipantsLabelSpeakers().equalsIgnoreCase("")) {
            menu.getItem(0).setTitle(selectedConference.getParticipantsLabelSpeakers());
        }

        if (selectedConference.getParticipantsLabelParticipants() != null &&
                !selectedConference.getParticipantsLabelParticipants().equalsIgnoreCase("")) {
            menu.getItem(1).setTitle(selectedConference.getParticipantsLabelParticipants());
        }

        lvParticipantsList = (ListView) findViewById(R.id.lvParticipants);

        getListViewData();

        lvParticipantsList.setAdapter(adapter);

        lvParticipantsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.showingSpeakers) {
                    SpeakerDetailViewActivity.mItems = new ArrayList<RealmObject>();

                    SpeakerDetailViewActivity.mItems.addAll(adapter.mSpeakers);

                    Intent i = new Intent(ParticipantsActivity.this, SpeakerDetailViewActivity.class);
                    i.putExtra("SPEAKER_NUMBER", position);
                    startActivity(i);
                } else {
                    SpeakerDetailViewActivity.mItems = new ArrayList<RealmObject>();

                    SpeakerDetailViewActivity.mItems.addAll(adapter.mAttendees);

                    Intent i = new Intent(ParticipantsActivity.this, AttendeeDetailViewActivity.class);
                    i.putExtra("ATTENDEE_NUMBER", position);
                    startActivity(i);
                }

            }
        });

        navigation.setSelectedItemId(R.id.navigation_speakers);

        displaySpeakers();
    }

    public void initSearchView() {
        participantsSearch = (SearchView) findViewById(R.id.participantSearch);

        participantsSearch.setBackgroundColor(AppUtil.getPrimaryThemColorAsInt());

        participantsSearch.setActivated(true);
        participantsSearch.setQueryHint("Search");

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        participantsSearch.setIconified(false);
        participantsSearch.clearFocus();

        participantsSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                participantsSearch.setIconified(false);
            }
        });

        participantsSearch.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                participantsSearch.setIconified(false);
                return false;
            }
        });

        ImageView closeButton = (ImageView)participantsSearch.findViewById(R.id.search_close_btn);

        // Set on click listener
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.search_src_text);

                et.setText("");

                participantsSearch.clearFocus();

                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            }
        });

        LinearLayout linearLayout1 = (LinearLayout) participantsSearch.getChildAt(0);
        LinearLayout linearLayout2 = (LinearLayout) linearLayout1.getChildAt(2);
        LinearLayout linearLayout3 = (LinearLayout) linearLayout2.getChildAt(1);
        AutoCompleteTextView autoComplete = (AutoCompleteTextView) linearLayout3.getChildAt(0);
        autoComplete.setTextSize(14);
        autoComplete.setBackground(getResources().getDrawable(R.drawable.search_view));

        autoComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                participantsSearch.setIconified(false);
            }
        });

        linearLayout3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                participantsSearch.setIconified(false);
            }
        });

        linearLayout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                participantsSearch.setIconified(false);
            }
        });

        linearLayout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                participantsSearch.setIconified(false);
            }
        });

    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            Menu menu = navigation.getMenu();

            View speakersView = navigation.findViewById(R.id.navigation_speakers);

            if (speakersView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) speakersView.getLayoutParams();
                p.setMargins(0, 0, 0, 0);
                speakersView.requestLayout();
            }

            View attendeesView = navigation.findViewById(R.id.navigation_attendees);

            if (attendeesView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) attendeesView.getLayoutParams();
                p.setMargins(0, 0, 0, 0);
                attendeesView.requestLayout();
            }

            switch (item.getItemId()) {
                case R.id.navigation_speakers:
                    speakersView.setBackgroundColor(AppConfig.lightGreyColor);

                    attendeesView.setBackgroundColor(AppUtil.getPrimaryThemColorAsInt());

                    displaySpeakers();
                    return true;
                case R.id.navigation_attendees:
                    speakersView.setBackgroundColor(AppUtil.getPrimaryThemColorAsInt());

                    attendeesView.setBackgroundColor(AppConfig.lightGreyColor);

                    displayAttendees();
                    return true;
            }
            return false;
        }

    };

    public void displaySpeakers() {
        adapter.showingSpeakers = true;
        adapter.notifyDataSetChanged();
    }

    public void displayAttendees() {
        adapter.showingSpeakers = false;
        adapter.notifyDataSetChanged();
    }

    public void getListViewData() {
        Realm realm = AppUtil.getRealmInstance(App.getInstance());

        adapter = new ParticipantsSwipeListAdapter(this);

        RealmResults<Speaker> speakerResults;

        speakerResults = realm.where(Speaker.class).equalTo("conference", AppUtil.getSelectedConferenceID(ParticipantsActivity.this)).findAllSorted("name", Sort.ASCENDING);

        for (int i = 0; i < speakerResults.size(); ++i) {
            adapter.addSpeaker(speakerResults.get(i));
        }

        RealmResults<Attendee> attendeeResults;

        attendeeResults = realm.where(Attendee.class).equalTo("conference", AppUtil.getSelectedConferenceID(ParticipantsActivity.this)).findAllSorted("name", Sort.ASCENDING);

        for (int i = 0; i < attendeeResults.size(); ++i) {
            adapter.addAttendee(attendeeResults.get(i));
        }
    }
}
