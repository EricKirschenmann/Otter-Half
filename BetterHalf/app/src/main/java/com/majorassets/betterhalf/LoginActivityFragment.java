package com.majorassets.betterhalf;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.majorassets.betterhalf.Database.DataItemRepository;
import com.majorassets.betterhalf.Database.DataProvider;
import com.majorassets.betterhalf.Model.BaseDataItem;
import com.majorassets.betterhalf.Model.Entertainment.MovieItem;
import com.majorassets.betterhalf.Model.Subcategory;
import com.majorassets.betterhalf.Model.User;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class LoginActivityFragment extends Fragment {

    private Button mLoginButton;
    private EditText mEmailEdit;
    private EditText mPasswordEdit;
    private TextView mNewUserTxt;
    private TextView mResponseTxt;
    private ProgressBar mLoginProgressBar;

    private DataProvider db;
    private Firebase mRootRef;
    private Firebase mUserRef;
    private Firebase mUserDataRef;
    private String mEmail;
    private String mPassword;
    private String mUsername;

    private Map<Subcategory, List<BaseDataItem>> userDataList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        //TODO: somehow "wipe" savedInstanceState
        //TODO: don't allow backward navigation to Login screen
        View view =  inflater.inflate(R.layout.fragment_login, container, false);

        initializeUIComponents(view);
        Firebase.setAndroidContext(getContext());
        db = DataProvider.getDataProvider();
        CreateAndControlEvents();
        return view;
    }

    //wire up all the view components from the layout XMLs
    private void initializeUIComponents(View view)
    {
        mResponseTxt = (TextView) view.findViewById(R.id.response_txt);
        mNewUserTxt = (TextView) view.findViewById(R.id.newUser_txt);
        mLoginProgressBar = (ProgressBar) view.findViewById(R.id.login_progressBar);

        mEmailEdit = (EditText) view.findViewById(R.id.email_edit);
        mPasswordEdit = (EditText) view.findViewById(R.id.password_edit);
        mLoginButton = (Button) view.findViewById(R.id.login_btn);

        userDataList = new HashMap<>();
    }

    private void CreateAndControlEvents()
    {
        ////// SETTING ONCLICK LISTENERS ////////
        mEmailEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mEmailEdit.setText("dgblanks@gmail.com"); //temp for testing
            }
        });

        mPasswordEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                mPasswordEdit.setText("test"); //temp password for testing
            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AttemptLogin();
            }
        });

        mNewUserTxt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mLoginButton.setText(R.string.signup_txt);
            }
        });
    }

    private void AttemptLogin()
    {
        mRootRef = db.getFirebaseInstance();

        mEmail = mEmailEdit.getText().toString();
        mPassword = mPasswordEdit.getText().toString();
        mUsername = GenerateUsername(mEmail);

        //TODO: implement progress bar
        //Attempt Login
        if(mLoginButton.getText().toString().equals("Login"))
            LoginWithPassword(mEmail, mPassword);
        else if (mLoginButton.getText().toString().equals("Sign Up"))
            CreateNewAccount(mEmail, mPassword);
    }

    //use Firebase user authentication with an email and password
    private void LoginWithPassword(String email, String password)
    {
        mRootRef.authWithPassword(email, password, new Firebase.AuthResultHandler()
        {
            @Override
            public void onAuthenticated(AuthData authData) {
                //get the reference for a user's data and parse it out into HashMap
                mUserDataRef = db.getUserDataInstance(mUsername);
                GetUserData(mUserDataRef);

                //TODO: read User object from SQLite
                User user = new User();
                user.setEmail(mEmail);

                // THIS IS TEMPORARY TO MOVE FORWARD - must be read from SQLite//
                DataItemRepository userRepo = DataItemRepository.getDataItemRepository();
                userRepo.setDataItems(userDataList);
                user.setDataItemRepository(userRepo);

                //start the home activity
                Intent homeIntent = new Intent(getContext(), HomeActivity.class);
                startActivity(homeIntent);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                //TODO: handle invalid credentials or no account
                mResponseTxt.setText(firebaseError.getMessage());
            }
        });
    }

    private void CreateNewAccount(final String email, final String password)
    {
        //Attempt to create a new user
        mRootRef.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>()
        {
            @Override
            public void onSuccess(Map<String, Object> result)
            {
                //TODO: log user in with first-time welcome screen
                mLoginButton.setText(R.string.login_txt);

                //TODO: store User object in SQLite
                User user = new User();
                user.setEmail(mEmail);

                LoginWithPassword(mEmail, mPassword);
            }

            @Override
            public void onError(FirebaseError firebaseError)
            {
                //TODO: handle account creation errors
                mResponseTxt.setText(firebaseError.getMessage());
            }
        });
    }

    private void GetUserData(Firebase ref)
    {
        ref.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                String parent;
                DataSnapshot next;
                Subcategory subcategory;
                BaseDataItem item;
                //"drill down" to leaf nodes
                while(dataSnapshot.hasChildren())
                {
                    parent = dataSnapshot.getKey();
                    next = dataSnapshot.getChildren().iterator().next();
                    subcategory = Subcategory.GetTypeFromString(parent);
                    switch (subcategory)
                    {
                        //TODO: parse out datasnapshot into separate objects
                        case MOVIE:
                            item = new MovieItem(next.getKey(), next.getValue().toString());
                            AddDataItem(subcategory, item);
                            dataSnapshot = next;
                            break;
                        case MUSIC:
                            dataSnapshot = next;
                            break;
                        default:
                            dataSnapshot = next; //parent was some other folder; keep going
                            break; //error check here
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {

            }
        });
    }

    private void AddDataItem(Subcategory subcategory, BaseDataItem item)
    {
        List<BaseDataItem> list;
        //if there are no entries for a movie then the list will be null
        if(userDataList.get(subcategory) == null)
        {
            list = new ArrayList<>(); // use an empty list
            list.add(item);
            userDataList.put(subcategory, list); //create new entry for movies
        }
        else //add to an already define list
        {
            list = userDataList.get(subcategory);
            list.add(item);
        }
    }

    /* UTILITY */
    @NonNull
    private String GenerateUsername(String email)
    {
        return email.substring(0, email.indexOf('@'));
    }
}
