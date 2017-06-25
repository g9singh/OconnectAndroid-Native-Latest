package com.ointerface.oconnect.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;
import com.ointerface.oconnect.App;
import com.ointerface.oconnect.ConferenceListViewActivity;
import com.ointerface.oconnect.CustomSplashActivity;
import com.ointerface.oconnect.MainSplashActivity;
import com.ointerface.oconnect.R;
import com.ointerface.oconnect.data.Attendee;
import com.ointerface.oconnect.data.DataSyncManager;
import com.ointerface.oconnect.data.Person;
import com.ointerface.oconnect.data.Speaker;
import com.ointerface.oconnect.util.AppConfig;
import com.ointerface.oconnect.util.AppUtil;
import com.ointerface.oconnect.util.HTTPPostHandler;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import org.apache.http.NameValuePair;
import com.parse.SaveCallback;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;

public class SignInActivity2 extends AppCompatActivity {
    enum SignInType { Normal,Twitter,Facebook, LinkedIn}

    public SignInType currentSignInType = SignInType.Normal;

    private EditText username;
    private EditText password;

    private Button btnCreateAccount;
    private Button btnForgotPassword;
    private Button btnSignIn;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FacebookSdk.sdkInitialize(this);

        ParseTwitterUtils.initialize(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));

        setContentView(R.layout.activity_sign_in_2);

        Button btnClose = (Button) findViewById(R.id.btnClose);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignInActivity2.this.finish();
                Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
            }
        });

        username = (EditText) findViewById(R.id.etEmail);
        password = (EditText) findViewById(R.id.etPassword);

        btnCreateAccount = (Button) findViewById(R.id.btnCreateAccount);
        btnForgotPassword = (Button) findViewById(R.id.btnForgotPassword);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);

        btnCreateAccount.setTextColor(AppUtil.getPrimaryThemColorAsInt());
        btnForgotPassword.setTextColor(AppUtil.getPrimaryThemColorAsInt());
        btnSignIn.setBackgroundColor(AppUtil.getPrimaryThemColorAsInt());
    }

    public void createAccountClicked(View sender) {
        Intent i = new Intent(SignInActivity2.this, CreateAccountActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
    }

    public void forgotPasswordClicked(View sender) {
        ParseUser.requestPasswordResetInBackground(username.getText().toString(), new RequestPasswordResetCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    AlertDialog alertDialog = new AlertDialog.Builder(SignInActivity2.this).create();
                    alertDialog.setTitle("");
                    alertDialog.setMessage("An email with instructions on how to reset your password has been sent to your email.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(SignInActivity2.this).create();
                    alertDialog.setTitle("Error");
                    alertDialog.setMessage(e.getLocalizedMessage());
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        });
    }

    public void twitterLoginClicked(View sender) {
        ParseTwitterUtils.logIn(this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user == null) {
                    Toast.makeText(SignInActivity2.this,"Error during Twitter Login!",Toast.LENGTH_LONG).show();
                } else {
                    if (user.isNew()) {
                        user.put("userType", "app");
                        user.saveInBackground();
                    }

                    OConnectBaseActivity.currentPerson = Person.saveFromParseUser(user, false);
                    callTwitterImportAPI(user);

                    Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                    startActivity(i);
                }
            }
        });
    }

    public void callTwitterImportAPI(ParseUser user) {
        new TwitterImportTask().execute(user);

        OConnectBaseActivity.currentPerson = Person.saveFromParseUser(user, false);

        Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
        startActivity(i);
    }

    public void facebookLoginClicked(View sender) {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, Arrays.asList("email", "user_photos", "public_profile", "user_friends")
                , new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException err) {
                        if (user == null) {
                            Toast.makeText(SignInActivity2.this,"Error during Facebook Login!",Toast.LENGTH_LONG).show();
                        } else {
                            if (user.isNew()) {
                                user.put("userType", "app");
                                user.saveInBackground();
                            }

                            OConnectBaseActivity.currentPerson = Person.saveFromParseUser(user, false);
                            callFacebookImportAPI(user);

                            Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                            startActivity(i);
                        }
                    }

                });
    }

    public void callFacebookImportAPI(ParseUser user) {
        new FacebookImportTask().execute(user);

        OConnectBaseActivity.currentPerson = Person.saveFromParseUser(user, false);

        Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
        startActivity(i);
    }

    public void linkedInLoginClicked(View sender) {
        final Activity thisActivity = this;

        LISessionManager.getInstance(getApplicationContext()).init(thisActivity, buildScope(), new AuthListener() {
            @Override
            public void onAuthSuccess() {
                // Authentication was successful.  You can now do
                // other calls with the SDK.
                String url = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,email-address)";

                APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
                apiHelper.getRequest(SignInActivity2.this, url, new ApiListener() {
                    @Override
                    public void onApiSuccess(ApiResponse apiResponse) {
                        // Success!
                        Log.d("APD", "linkedin response for data: " + apiResponse.getResponseDataAsJson().toString());

                        final JSONObject obj = apiResponse.getResponseDataAsJson();

                        String firstName = "";
                        String lastName = "";
                        String emailAddress = "";

                        try {
                            if (obj.has("firstName")) {
                                firstName = obj.getString("firstName");
                            }

                            if (obj.has("lastName")) {
                                lastName = obj.getString("lastName");
                            }

                            if (obj.has("emailAddress")) {
                                emailAddress = obj.getString("emailAddress");
                            }
                        } catch (Exception ex) {
                            Log.d("APD", ex.getMessage());
                        }

                        final String finalEmailAddress = emailAddress;
                        final String finalFirstName = firstName;
                        final String finalLastName = lastName;

                        final String token = LISessionManager.getInstance(getApplicationContext()).getSession().getAccessToken().toString();

                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        query.whereEqualTo("username", emailAddress);
                        query.findInBackground(new FindCallback<ParseUser>() {
                            public void done(List<ParseUser> objects, ParseException e) {
                                if (e == null) {
                                    ParseUser parseUser = null;
                                    if (objects.size() > 0) {
                                        final ParseUser user = objects.get(0);

                                        user.setUsername(finalEmailAddress);
                                        user.setEmail(finalEmailAddress);
                                        user.put("firstName", finalFirstName);
                                        user.put("lastName", finalLastName);

                                        user.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                new LinkedInImportTask().execute(user.getObjectId(), token);
                                            }
                                        });

                                        parseUser = user;
                                    } else {
                                        final ParseUser user = new ParseUser();

                                        user.setUsername(finalEmailAddress);
                                        user.setEmail(finalEmailAddress);
                                        user.put("firstName", finalFirstName);
                                        user.put("lastName", finalLastName);

                                        user.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                new LinkedInImportTask().execute(user.getObjectId(), token);
                                            }
                                        });

                                        parseUser = user;
                                    }

                                    OConnectBaseActivity.currentPerson = Person.saveFromParseUser(parseUser, false);

                                    Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                    startActivity(i);

                                }
                            }
                        });
                    }

                    @Override
                    public void onApiError(LIApiError liApiError) {
                        // Error making GET request!
                    }
                });
            }

            @Override
            public void onAuthError(LIAuthError error) {
                // Handle authentication errors
            }
        }, true);
    }

    // Build the list of member permissions our LinkedIn session requires
    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE, Scope.W_SHARE);
    }

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public void signInClicked(View sender) {
        final String usernametxt = username.getText().toString();
        String passwordtxt = password.getText().toString();

        dialog = ProgressDialog.show((Context)this, null, "Logging In ...");

        // Send data to Parse.com for verification
        ParseUser.logInInBackground(usernametxt, passwordtxt,
                new LogInCallback() {
                    public void done(ParseUser user, ParseException e) {
                        if (user != null) {
                            OConnectBaseActivity.currentPerson = Person.saveFromParseUser(user, false);
                            AppUtil.setIsSignedIn(SignInActivity2.this, true);
                            AppUtil.setSignedInUserID(SignInActivity2.this, user.getObjectId());

                            dialog.dismiss();

                            try {
                                List<ParseObject> speakerList = ParseQuery.getQuery("Speaker").whereEqualTo("email", usernametxt).find();
                                List<ParseObject> attendeeList = ParseQuery.getQuery("Attendee").whereEqualTo("email", usernametxt).find();

                                final ParseUser finalUser = user;

                                if (speakerList.size() == 0 &&
                                        attendeeList.size() == 0 &&
                                        AppUtil.hasPinPromptEnteredForConference(SignInActivity2.this, AppUtil.getSelectedConferenceID(SignInActivity2.this)) == false
                                        && OConnectBaseActivity.selectedConference.isShouldShowPin() == true &&
                                        AppUtil.hasPinPromptSkippedForConference(SignInActivity2.this, AppUtil.getSelectedConferenceID(SignInActivity2.this)) == false) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity2.this);
                                    builder.setTitle("Please Enter Your PIN To Verify Your Identity");
                                    builder.setMessage("If you have received an email from the organizer with a PIN number, please enter it here to verify your identity.  If you did not receive an email, please Skip this step.");

                                    final EditText input = new EditText(SignInActivity2.this);

                                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                    builder.setView(input);

                                    builder.setPositiveButton("Enter PIN", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                // Realm realm = AppUtil.getRealmInstance(App.getInstance());

                                                /*
                                                ParseQuery<ParseObject> attendeeQuery = ParseQuery.getQuery("Attendee");
                                                ParseQuery<ParseObject> speakerQuery = ParseQuery.getQuery("Speaker");

                                                speakerQuery.whereEqualTo("IOS_code", input.getText().toString()).getFirstInBackground(new GetCallback<ParseObject>() {
                                                    @Override
                                                    public void done(ParseObject object, ParseException e) {
                                                        if (e == null) {

                                                        } else {
                                                            Log.d("APD", e.getMessage());
                                                        }
                                                    }
                                                });

                                                attendeeQuery.whereEqualTo("IOS_code", input.getText().toString()).getFirstInBackground(new GetCallback<ParseObject>() {
                                                    @Override
                                                    public void done(ParseObject object, ParseException e) {
                                                        if (e == null) {

                                                        } else {
                                                            Log.d("APD", e.getMessage());
                                                        }
                                                    }
                                                });
                                                */

                                                List<ParseObject> speakerList = ParseQuery.getQuery("Speaker").whereEqualTo("IOS_code", input.getText().toString()).find();
                                                List<ParseObject> attendeeList = ParseQuery.getQuery("Attendee").whereEqualTo("IOS_code", input.getText().toString()).find();

                                                boolean bUserLinked = false;

                                                if (speakerList.size() > 0) {
                                                    ParseObject speakerObj = speakerList.get(0).fetchIfNeeded();

                                                    speakerObj.fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                                                        @Override
                                                        public void done(ParseObject object, ParseException e) {
                                                            if (e == null) {
                                                                object.put("UserLink", finalUser.getObjectId());

                                                                try {
                                                                    object.save();
                                                                } catch (Exception ex) {
                                                                    Log.d("APD", "Error saving Speaker: " + ex.getMessage());
                                                                }
                                                            }
                                                        }
                                                    });

                                                    bUserLinked = true;
                                                } else if (attendeeList.size() > 0) {
                                                    ParseObject attendeeObj = attendeeList.get(0).fetchIfNeeded();
                                                    attendeeObj.put("UserLink", finalUser.getObjectId());
                                                    attendeeObj.save();
                                                    bUserLinked = true;
                                                }

                                                if (bUserLinked == true) {
                                                    AppUtil.addConferenceForPinPromptEntered(SignInActivity2.this, OConnectBaseActivity.selectedConference.getObjectId());
                                                    SignInActivity2.this.finish();
                                                    Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                                    startActivity(i);
                                                    overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
                                                } else {
                                                    AlertDialog alertDialog = new AlertDialog.Builder(SignInActivity2.this).create();
                                                    alertDialog.setTitle("Error");
                                                    alertDialog.setMessage("Incorrect PIN.  Please try again when logging in.");
                                                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                                            new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    SignInActivity2.this.finish();
                                                                    Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                                                    startActivity(i);
                                                                    overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
                                                                }
                                                            });
                                                    alertDialog.show();
                                                }

                                            } catch (Exception ex) {
                                                Log.d("SignIn2", ex.getMessage());
                                            }
                                        }
                                    });
                                    builder.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity2.this);
                                            builder.setTitle("Are You Sure?");
                                            builder.setMessage("If you received an email with a PIN, we strongly recommend you enter it so that we can verify your identity properly.");

                                            final EditText input = new EditText(SignInActivity2.this);

                                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                            builder.setView(input);

                                            builder.setPositiveButton("Enter PIN", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    try {
                                                        List<ParseObject> speakerList = ParseQuery.getQuery("Speaker").whereEqualTo("IOS_code", input.getText().toString()).find();
                                                        List<ParseObject> attendeeList = ParseQuery.getQuery("Attendee").whereEqualTo("IOS_code", input.getText().toString()).find();

                                                        boolean bUserLinked = false;

                                                        if (speakerList.size() > 0) {
                                                            ParseObject speakerObj = speakerList.get(0);
                                                            speakerObj.put("UserLink", finalUser.getObjectId());
                                                            speakerObj.save();
                                                            bUserLinked = true;
                                                        } else if (attendeeList.size() > 0) {
                                                            ParseObject attendeeObj = attendeeList.get(0);
                                                            attendeeObj.put("UserLink", finalUser.getObjectId());
                                                            attendeeObj.save();
                                                            bUserLinked = true;
                                                        }

                                                        if (bUserLinked == true) {
                                                            AppUtil.addConferenceForPinPromptEntered(SignInActivity2.this, OConnectBaseActivity.selectedConference.getObjectId());
                                                            SignInActivity2.this.finish();
                                                            Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                                            startActivity(i);
                                                            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
                                                        } else {
                                                            AlertDialog alertDialog = new AlertDialog.Builder(SignInActivity2.this).create();
                                                            alertDialog.setTitle("Error");
                                                            alertDialog.setMessage("Incorrect PIN.  Please try again when logging in.");
                                                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                                                    new DialogInterface.OnClickListener() {
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            SignInActivity2.this.finish();
                                                                            Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                                                            startActivity(i);
                                                                            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
                                                                        }
                                                                    });
                                                            alertDialog.show();
                                                        }
                                                    } catch (Exception ex) {
                                                        Log.d("SignIn2", ex.getMessage());
                                                    }
                                                }
                                            });
                                            builder.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    AppUtil.addConferenceForPinPromptSkipped(SignInActivity2.this, AppUtil.getSelectedConferenceID(SignInActivity2.this));
                                                    Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                                    startActivity(i);
                                                    overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
                                                }
                                            });

                                            builder.create().show();

                                        }
                                    });

                                    AlertDialog dialog = builder.create();

                                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                                    dialog.show();
                                } else {
                                    Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                    startActivity(i);
                                    overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );
                                }
                            } catch (Exception ex) {
                                SignInActivity2.this.finish();
                                Intent i = new Intent(SignInActivity2.this, DashboardActivity.class);
                                startActivity(i);
                                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up );

                                Log.d("SignIn2", ex.getMessage());
                            }

                            // SignInActivity2.this.finish();
                            // overridePendingTransition(R.anim.hold, R.anim.disappear_to_bottom);
                        } else {
                            dialog.dismiss();

                            AlertDialog alertDialog = new AlertDialog.Builder(SignInActivity2.this).create();
                            alertDialog.setTitle("Error");
                            alertDialog.setMessage("Incorrect username or password.  Please try again.");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();

                            if (e != null) {
                                Log.d("APD", "Parse Login Error: " + e.getMessage());
                            }
                        }
                    }
                });
    }

    private class ImportTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... args) {

            // params comes from the execute() call: params[0] is the url.
            try {
                HTTPPostHandler httpPostHandler = new HTTPPostHandler();

                return httpPostHandler.performTwitterImport(SignInActivity2.this,"","","","");
            } catch (Exception e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d("IMPORT RESULT", result);

            /*
            try {

            } catch (JSONException e) {
                e.printStackTrace();
            }
            */
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Add this line to your existing onActivityResult() method
        LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, resultCode, data);
    }

    private class FacebookImportTask extends AsyncTask<ParseUser, Void, String> {

        @Override
        protected String doInBackground(ParseUser... args) {

            ParseUser user = args[0];

            AccessToken token = AccessToken.getCurrentAccessToken();

            String tokenStr = "";

            if (token != null) {
                tokenStr = token.getToken();
            }

            String urlStr = getString(R.string.oconnect_base_url_production) + "/functions/facebookProfile";

            try {
                URL url = new URL(urlStr);
                HttpURLConnection client = (HttpURLConnection) url.openConnection();


                client.setRequestMethod("POST");
                client.setRequestProperty("X-Parse-Application-Id","OxZLwjHXEjFjuFExxotUrwvOlxdT2efPN8pv06JI");
                client.setRequestProperty("X-Parse-REST-API-Key","gRM2GVEI6I6HdtRv8inJSW9LUF9ZPInwz9FlIb4r");
                client.setRequestProperty("Cache-Control", "no-cache");
                client.setRequestProperty("Postman-Token", "1a8b6b0e-db51-8041-1e44-bc814dc162ca");
                client.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
                client.setDoOutput(true);


                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("ocUser", user.getObjectId()));
                params.add(new BasicNameValuePair("token", tokenStr));

                OutputStream os = new BufferedOutputStream(client.getOutputStream());
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();

                if(client.getResponseCode() == HttpURLConnection.HTTP_OK){
                    String server_response = readStream(client.getInputStream());
                    Log.d("APD", "Facebook Import Cloud Method Called with Code: " + client.getResponseCode() +
                            " with Response Message: " + server_response);
                }
            } catch (Exception ex) {
                Log.d("APD", ex.getMessage());
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private class TwitterImportTask extends AsyncTask<ParseUser, Void, String> {

        @Override
        protected String doInBackground(ParseUser... args) {

            ParseUser user = args[0];

            String userIdStr = ParseTwitterUtils.getTwitter().getUserId();

            String urlStr = getString(R.string.oconnect_base_url_production) + "/functions/twitterProfile";

            try {
                URL url = new URL(urlStr);
                HttpURLConnection client = (HttpURLConnection) url.openConnection();

                client.setRequestMethod("POST");
                client.setRequestProperty("X-Parse-Application-Id","OxZLwjHXEjFjuFExxotUrwvOlxdT2efPN8pv06JI");
                client.setRequestProperty("X-Parse-REST-API-Key","gRM2GVEI6I6HdtRv8inJSW9LUF9ZPInwz9FlIb4r");
                client.setRequestProperty("Cache-Control", "no-cache");
                client.setRequestProperty("Postman-Token", "1a8b6b0e-db51-8041-1e44-bc814dc162ca");
                client.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
                client.setDoOutput(true);


                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("ocUser", user.getObjectId()));
                params.add(new BasicNameValuePair("userId", userIdStr));

                OutputStream os = new BufferedOutputStream(client.getOutputStream());
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();

                if(client.getResponseCode() == HttpURLConnection.HTTP_OK){
                    String server_response = readStream(client.getInputStream());
                    Log.d("APD", "Twitter Import Cloud Method Called with Code: " + client.getResponseCode() +
                            " with Response Message: " + server_response);
                }

            } catch (Exception ex) {
                Log.d("APD", ex.getMessage());
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private class LinkedInImportTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... args) {
            String urlStr = getString(R.string.oconnect_base_url_production) + "/functions/linkedInProfile";

            try {
                URL url = new URL(urlStr);
                HttpURLConnection client = (HttpURLConnection) url.openConnection();


                client.setRequestMethod("POST");
                client.setRequestProperty("X-Parse-Application-Id", "OxZLwjHXEjFjuFExxotUrwvOlxdT2efPN8pv06JI");
                client.setRequestProperty("X-Parse-REST-API-Key", "gRM2GVEI6I6HdtRv8inJSW9LUF9ZPInwz9FlIb4r");
                client.setRequestProperty("Cache-Control", "no-cache");
                client.setRequestProperty("Postman-Token", "1a8b6b0e-db51-8041-1e44-bc814dc162ca");
                client.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
                client.setDoOutput(true);


                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("ocUser", args[0]));
                params.add(new BasicNameValuePair("token", args[1]));

                OutputStream os = new BufferedOutputStream(client.getOutputStream());
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();

                if (client.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String server_response = readStream(client.getInputStream());
                    Log.d("APD", "LinkedIn Import Cloud Method Called with Code: " + client.getResponseCode() +
                            " with Response Message: " + server_response);
                }
            } catch (Exception ex) {
                Log.d("APD", ex.getMessage());
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}
