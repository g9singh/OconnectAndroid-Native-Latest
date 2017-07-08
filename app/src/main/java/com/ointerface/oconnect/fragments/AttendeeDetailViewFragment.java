package com.ointerface.oconnect.fragments;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ointerface.oconnect.App;
import com.ointerface.oconnect.R;
import com.ointerface.oconnect.activities.AttendeeDetailViewActivity;
import com.ointerface.oconnect.activities.OConnectBaseActivity;
import com.ointerface.oconnect.activities.SpeakerDetailViewActivity;
import com.ointerface.oconnect.adapters.AttendeeDetailExpandableListView;
import com.ointerface.oconnect.adapters.SpeakerDetailExpandableListViewAdapter;
import com.ointerface.oconnect.data.Attendee;
import com.ointerface.oconnect.data.Event;
import com.ointerface.oconnect.data.EventAbstract;
import com.ointerface.oconnect.data.EventFile;
import com.ointerface.oconnect.data.EventJournal;
import com.ointerface.oconnect.data.EventMisc;
import com.ointerface.oconnect.data.Person;
import com.ointerface.oconnect.data.Speaker;
import com.ointerface.oconnect.data.SpeakerAbstract;
import com.ointerface.oconnect.data.SpeakerEventCache;
import com.ointerface.oconnect.data.SpeakerFile;
import com.ointerface.oconnect.data.SpeakerJournal;
import com.ointerface.oconnect.data.SpeakerLink;
import com.ointerface.oconnect.data.SpeakerMisc;
import com.ointerface.oconnect.messaging.MessagingActivity;
import com.ointerface.oconnect.util.AppConfig;
import com.ointerface.oconnect.util.AppUtil;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 */
public class AttendeeDetailViewFragment extends Fragment {

    public int pageNumber = 0;

    static public AttendeeDetailViewActivity activity;

    static private ArrayList<RealmObject> mItems;

    static private int currentAttendeeNumber;

    private ExpandableListView elvAttendeeDetailInfo;
    private AttendeeDetailExpandableListView adapter;

    private List<String> _listDataHeader;
    private List<Integer> _listHeaderNumber;
    private List<Boolean> _listGroupHasListView;
    private HashMap<Integer, Integer> _listChildCount;
    private HashMap<Integer, ArrayList<String>> _listChildItems;
    private Attendee _listAttendee;

    private Bitmap bmp;

    public AttendeeDetailViewFragment() {
        // Required empty public constructor
    }

    public static AttendeeDetailViewFragment newInstance(int pageNumberArg, AttendeeDetailViewActivity activityArg,
                                                      ArrayList<RealmObject> mItemsArg,
                                                      int currentSpeakerNumberArg) {
        AttendeeDetailViewFragment fragment = new AttendeeDetailViewFragment();

        activity = activityArg;

        // pageNumber = pageNumberArg;

        Bundle bundle = new Bundle();
        bundle.putInt("PAGE_NUMBER", pageNumberArg);

        fragment.setArguments(bundle);

        mItems = mItemsArg;

        currentAttendeeNumber = currentSpeakerNumberArg;

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_attendee_detail_view, container, false);

        RelativeLayout mainContainer = (RelativeLayout) rootView.findViewById(R.id.main_container);
        LinearLayout llMain = (LinearLayout) rootView.findViewById(R.id.llMain);
        RelativeLayout rlTopSection = (RelativeLayout) rootView.findViewById(R.id.rlTopSection);
        RelativeLayout rlMiscItems = (RelativeLayout) rootView.findViewById(R.id.rlMiscItems);

        mainContainer.setClipChildren(false);
        llMain.setClipChildren(false);
        rlTopSection.setClipChildren(false);
        rlMiscItems.setClipChildren(false);

        final Realm realm = AppUtil.getRealmInstance(App.getInstance());

        Bundle bundle = getArguments();

        pageNumber = bundle.getInt("PAGE_NUMBER");

        final Attendee attendee = (Attendee) mItems.get(pageNumber);

        final Person person = realm.where(Person.class).equalTo("objectId", attendee.getUserLink()).findFirst();

        final ImageView ivConnect = (ImageView) rootView.findViewById(R.id.ivConnect);

        ivConnect.setBackground(AppUtil.changeDrawableColor(activity, R.drawable.icon_blue_star_empty, AppUtil.getPrimaryThemColorAsInt()));

        ImageView ivMessage = (ImageView) rootView.findViewById(R.id.ivMessage);

        ivMessage.setBackground(AppUtil.changeDrawableColor(activity, R.drawable.icon_envelop, AppUtil.getPrimaryThemColorAsInt()));

        TextView tvMessage = (TextView) rootView.findViewById(R.id.tvMessage);

        tvMessage.setTextColor(AppUtil.getPrimaryThemColorAsInt());

        TextView tvConnect = (TextView) rootView.findViewById(R.id.tvConnect);

        tvConnect.setTextColor(AppUtil.getPrimaryThemColorAsInt());

        try {
            Person user = realm.where(Person.class).equalTo("objectId", attendee.getUserLink()).findFirst();

            if (user != null && user.isContactable() == false) {
                ivMessage.setBackground(AppUtil.changeDrawableColor(activity, R.drawable.icon_envelop, AppConfig.hiddenGreyBackgroundColor));
                tvMessage.setTextColor(AppConfig.hiddenGreyBackgroundColor);
                tvMessage.setOnClickListener(null);
                ivMessage.setOnClickListener(null);
            }

            RealmList<Person> connectedUsers = OConnectBaseActivity.currentPerson.getFavoriteUsers();
            RealmList<Attendee> connectedAttendees = OConnectBaseActivity.currentPerson.getFavoriteAttendees();
            if (connectedUsers.contains(user) == true || connectedAttendees.contains(attendee) == true) {
                ivConnect.setBackground(AppUtil.changeDrawableColor(activity, R.drawable.icon_blue_star_filled, AppUtil.getPrimaryThemColorAsInt()));
            }
        } catch (Exception ex) {
            Log.d("SpeakerDetail", ex.getMessage());
        }

        tvConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ivConnect.setBackground(AppUtil.changeDrawableColor(activity, R.drawable.icon_blue_star_filled, AppUtil.getPrimaryThemColorAsInt()));

                    realm.beginTransaction();
                    RealmList<Person> realmFavoriteUsers = new RealmList<Person>();

                    ParseUser parseObject = ParseUser.getQuery().get(OConnectBaseActivity.currentPerson.getObjectId());

                    ParseRelation<ParseObject> usersRelation = parseObject.getRelation("favoriteUsersRelation");

                    ParseQuery<ParseObject> usersQuery = usersRelation.getQuery();

                    List<ParseObject> usersList = usersQuery.find();

                    Person user = realm.where(Person.class).equalTo("objectId", attendee.getUserLink()).findFirst();

                    ParseUser parseAttendee = ParseUser.getQuery().get(attendee.getUserLink());

                    RealmList<Attendee> connectedAttendees = OConnectBaseActivity.currentPerson.getFavoriteAttendees();

                    connectedAttendees.add(attendee);

                    OConnectBaseActivity.currentPerson.setFavoriteAttendees(connectedAttendees);

                    if (user != null) {
                        RealmList<Person> connectedUsers = OConnectBaseActivity.currentPerson.getFavoriteUsers();

                        connectedUsers.add(user);

                        OConnectBaseActivity.currentPerson.setFavoriteUsers(connectedUsers);
                    }

                    realm.commitTransaction();

                    if (parseAttendee != null) {
                        usersRelation.add(parseAttendee);

                        parseObject.put("favoriteUsersRelation", usersRelation);

                        parseObject.save();
                    }

                } catch (Exception ex) {
                    Log.d("AttendeeDetail", "Exception: " + ex.getMessage());
                }
            }
        });

        ivConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ivConnect.setBackground(AppUtil.changeDrawableColor(activity, R.drawable.icon_blue_star_filled, AppUtil.getPrimaryThemColorAsInt()));

                    realm.beginTransaction();
                    RealmList<Person> realmFavoriteUsers = new RealmList<Person>();

                    ParseUser parseObject = ParseUser.getQuery().get(OConnectBaseActivity.currentPerson.getObjectId());

                    ParseRelation<ParseObject> usersRelation = parseObject.getRelation("favoriteUsersRelation");

                    ParseQuery<ParseObject> usersQuery = usersRelation.getQuery();

                    List<ParseObject> usersList = usersQuery.find();

                    Person user = realm.where(Person.class).equalTo("objectId", attendee.getUserLink()).findFirst();

                    ParseUser parseSpeaker = ParseUser.getQuery().get(attendee.getUserLink());

                    RealmList<Attendee> connectedAttendees = OConnectBaseActivity.currentPerson.getFavoriteAttendees();

                    connectedAttendees.add(attendee);

                    OConnectBaseActivity.currentPerson.setFavoriteAttendees(connectedAttendees);

                    if (user != null) {
                        RealmList<Person> connectedUsers = OConnectBaseActivity.currentPerson.getFavoriteUsers();

                        connectedUsers.add(user);

                        OConnectBaseActivity.currentPerson.setFavoriteUsers(connectedUsers);
                    }

                    realm.commitTransaction();

                    if (parseSpeaker != null) {
                        usersRelation.add(parseSpeaker);

                        parseObject.put("favoriteUsersRelation", usersRelation);

                        parseObject.save();
                    }

                } catch (Exception ex) {
                    Log.d("AttendeeDetail", "Exception: " + ex.getMessage());
                }
            }
        });

        tvMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Person user = realm.where(Person.class).equalTo("objectId", attendee.getUserLink()).findFirst();

                if (user != null) {
                    Intent intent = new Intent(activity, MessagingActivity.class);

                    MessagingActivity.recipientIDStr = user.getObjectId();

                    activity.startActivity(intent);
                } else {
                    AppUtil.displayPersonNotAvailable(activity);
                }
            }
        });

        ivMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Person user = realm.where(Person.class).equalTo("objectId", attendee.getUserLink()).findFirst();

                if (user != null) {
                    Intent intent = new Intent(activity, MessagingActivity.class);

                    MessagingActivity.recipientIDStr = user.getObjectId();

                    activity.startActivity(intent);
                } else {
                    AppUtil.displayPersonNotAvailable(activity);
                }
            }
        });

        ImageView ivProfile = (ImageView) rootView.findViewById(R.id.ivProfile);

        /*
        if (person != null && person.getPictureURL() != null && !person.getPictureURL().equalsIgnoreCase("")) {
            final String pictureURL = person.getPictureURL();

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        InputStream in = new URL(pictureURL).openStream();
                        bmp = BitmapFactory.decodeStream(in);
                    } catch (Exception e) {
                        Log.d("APD", e.getMessage());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    if (bmp != null) {
                        ivProfile.setImageBitmap(bmp);
                    }
                }

            }.execute();
        } else if (attendee.getImage() != null) {
            Bitmap bm2 = BitmapFactory.decodeByteArray(attendee.getImage(), 0, attendee.getImage().length);

            ivProfile.setImageBitmap(bm2);
        }
        */

        if (attendee.getImage() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(attendee.getImage(), 0, attendee.getImage().length);
            Drawable d = new BitmapDrawable(activity.getResources(), bmp);
            ivProfile.setBackground(d);

            // Bitmap bm2 = BitmapFactory.decodeByteArray(attendee.getImage(), 0, attendee.getImage().length);

            // ivProfile.setImageBitmap(bm2);
        }

        TextView tvAttendeeName = (TextView) rootView.findViewById(R.id.tvAttendeeName);
        tvAttendeeName.setText(attendee.getName());

        elvAttendeeDetailInfo = (ExpandableListView) rootView.findViewById(R.id.elvAttendeeInfo);

        getAttendeeDetailListData();

        elvAttendeeDetailInfo.setAdapter(adapter);

        for (int i = 0; i < adapter.getGroupCount(); ++i) {
            elvAttendeeDetailInfo.expandGroup(i);
        }

        return rootView;
    }

    public void getAttendeeDetailListData() {
        _listDataHeader = new ArrayList<String>();
        _listHeaderNumber = new ArrayList<Integer>();
        _listGroupHasListView = new ArrayList<Boolean>();
        _listChildCount = new HashMap<Integer, Integer>();
        _listChildItems = new HashMap<Integer, ArrayList<String>>();

        Bundle bundle = getArguments();

        pageNumber = bundle.getInt("PAGE_NUMBER");

        Attendee attendee = (Attendee) mItems.get(pageNumber);

        int groupNum = 0;

        if (attendee.getJob() != null && !attendee.getJob().equalsIgnoreCase("") ||
                attendee.getOrganization() != null && !attendee.getOrganization().equalsIgnoreCase("") ||
                attendee.getLocation() != null && !attendee.getLocation().equalsIgnoreCase("")) {
            _listDataHeader.add("About");
            _listHeaderNumber.add(groupNum);
            _listGroupHasListView.add(false);
            _listChildCount.put(groupNum,1);
            ++groupNum;
        }

        adapter = new AttendeeDetailExpandableListView(activity, _listDataHeader, _listHeaderNumber, _listChildCount,
                _listGroupHasListView, _listChildItems, attendee);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
