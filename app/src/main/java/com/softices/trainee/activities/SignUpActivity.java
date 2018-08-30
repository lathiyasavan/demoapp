package com.softices.trainee.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.softices.trainee.R;
import com.softices.trainee.database.DbHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.CAMERA;
import static com.softices.trainee.database.DbHelper.TABLE_NAME;
import static com.softices.trainee.database.DbHelper.USER_TABLE_COLUMN_EMAIL;
import static com.softices.trainee.database.DbHelper.USER_TABLE_COLUMN_FIRST_NAME;
import static com.softices.trainee.database.DbHelper.USER_TABLE_COLUMN_LAST_NAME;
import static com.softices.trainee.database.DbHelper.USER_TABLE_COLUMN_MOBILE_NUMBER;
import static com.softices.trainee.database.DbHelper.USER_TABLE_COLUMN_PASSWORD;
import static com.softices.trainee.database.DbHelper.cValues;
import static com.softices.trainee.methods.L.isValidEmail;
import static com.softices.trainee.methods.L.isValidFirstName;
import static com.softices.trainee.methods.L.isValidLastName;
import static com.softices.trainee.methods.L.isValidMobileNumber;
import static com.softices.trainee.methods.L.isValidPassword;

@TargetApi(Build.VERSION_CODES.M)
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    public EditText edtFirstName, edtLastName, edtEmail, edtMobileNumber, edtPassword,
            edtConfirmPassword;
    public Button btnSignUp;
    public TextView txtLoginNow;
    public DbHelper dbHelper;
    public RadioGroup radioGroup;
    public Toolbar toolbar;
    Bitmap myBitmap;
    Uri picUri;


    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();

    private final static int ALL_PERMISSIONS_RESULT = 107;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CircleImageView circleImageView = (CircleImageView) findViewById(R.id.img_profile);
        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(getPickImageChooserIntent(), 200);
            }
        });


        permissions.add(CAMERA);
        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }


        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }


    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "profile.png"));
        }
        return outputFileUri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Bitmap bitmap;
        if (resultCode == Activity.RESULT_OK) {

            ImageView imageView = (ImageView) findViewById(R.id.img_profile);

            if (getPickImageResultUri(data) != null) {
                picUri = getPickImageResultUri(data);

                try {
                    myBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);
                    CircleImageView croppedImageView = (CircleImageView) findViewById(R.id.image);
                    croppedImageView.setImageBitmap(myBitmap);
                    imageView.setImageBitmap(myBitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {


                bitmap = (Bitmap) data.getExtras().get("data");

                myBitmap = bitmap;
                CircleImageView croppedImageView = (CircleImageView) findViewById(R.id.image);
                if (croppedImageView != null) {
                    croppedImageView.setImageBitmap(myBitmap);
                }

                imageView.setImageBitmap(myBitmap);

            }

        }

    }


    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }


        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on scren orientation
        // changes
        outState.putParcelable("pic_uri", picUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        picUri = savedInstanceState.getParcelable("pic_uri");
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (hasPermission(perms)) {

                    } else {

                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                                                //Log.d("API123", "permisionrejected " + permissionsRejected.size());

                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }




    public void init() {
        edtFirstName = (EditText) findViewById(R.id.edt_first_name);
        edtLastName = (EditText) findViewById(R.id.edt_last_name);
        edtEmail = (EditText) findViewById(R.id.edt_signup_email);
        edtMobileNumber = (EditText) findViewById(R.id.edt_mobile_number);
        edtPassword = (EditText) findViewById(R.id.edt_signup_password);
        edtConfirmPassword = (EditText) findViewById(R.id.edt_signup_confirmPassword);
        btnSignUp = (Button) findViewById(R.id.btn_signup);
        txtLoginNow = (TextView) findViewById(R.id.txt_login_now);
        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        btnSignUp.setOnClickListener(this);
        txtLoginNow.setOnClickListener(this);
        dbHelper = new DbHelper(this);
        edtFirstName.setText("savan");
        edtLastName.setText("lathiya");
        edtEmail.setText("aaa@aaa.aaa");
        edtMobileNumber.setText("8488047726");
        edtPassword.setText("123456");
        edtConfirmPassword.setText("123456");
    }


    public void clickedSignUp() {
        String firstName = edtFirstName.getText().toString();
        String lastName = edtLastName.getText().toString();
        String email = edtEmail.getText().toString();
        String mobileNumber = edtMobileNumber.getText().toString();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();
        int selectedRadioButton = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) findViewById(selectedRadioButton);

        if (!isValidFirstName(firstName)) {
            Toast.makeText(SignUpActivity.this, "Please Enter valid First Name!",
                    Toast.LENGTH_LONG).show();
        } else if (!isValidLastName(lastName)) {
            Toast.makeText(SignUpActivity.this, "Please Enter valid Last Name",
                    Toast.LENGTH_LONG).show();
        } else if (!isValidEmail(email)) {
            Toast.makeText(SignUpActivity.this, "Please Enter valid Email!",
                    Toast.LENGTH_LONG).show();
        } else if (!isValidMobileNumber(mobileNumber)) {
            Toast.makeText(SignUpActivity.this, "Please Enter valid Mobile Number",
                    Toast.LENGTH_LONG).show();
        } else if (!isValidPassword(password)) {
            Toast.makeText(SignUpActivity.this, "Please Enter valid Password",
                    Toast.LENGTH_LONG).show();
        } else if (!password.equals(confirmPassword)) {
            Toast.makeText(SignUpActivity.this, "Password and Confirm Password " +
                    "must be same!", Toast.LENGTH_LONG).show();
        } else if (dbHelper.insertDataIntoDatabase(firstName, lastName, email, mobileNumber,
                radioButton.getText().toString(), password)) {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            Toast.makeText(SignUpActivity.this, "User register Successfully.!", Toast.LENGTH_LONG).show();
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(SignUpActivity.this, "Something went Wrong.!",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_signup:
                clickedSignUp();
                break;
            case R.id.txt_login_now:
                Intent txtLoginNow = new Intent(SignUpActivity.this,
                        SignInActivity.class);
                startActivity(txtLoginNow);
                break;
            default:
                break;
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
