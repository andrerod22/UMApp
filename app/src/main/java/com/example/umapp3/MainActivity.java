package com.example.umapp3;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    // TODO: [Optional] Authenticate users via firebase (have to create a user fragment) using: FirebaseAuth firebaseAuth;
    ActionBar actionBar;
    EditText titleEt, descriptionEt;
    ImageView imageIv;
    Button uploadButton;

    // Set the action bar

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;

    //permissions array
    String[] cameraPermissions;
    String[] storagePermissions;

    //image constants
    private static final int IMAGE_FROM_CAMERA_CODE = 300;
    private static final int IMAGE_FROM_STORAGE_CODE = 400;

    //image selected uri variable
    Uri image_rui = null;

    // bar of progression
    ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create New Post");
        //enable back button for the actionbar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //initialize the permissions to access user OS hardware
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressBar = new ProgressDialog(this);

        //firebaseAuth = firebaseAuth.getInstance(); // we need to create a User class for this
        // verifyUserStatus() // also needed if we want to authorize users
        actionBar.setSubtitle("No User Logged");
        titleEt = findViewById(R.id.pTitleEt);
        descriptionEt = findViewById(R.id.pDescriptionEt);
        imageIv = findViewById(R.id.pImageIv);
        uploadButton = findViewById(R.id.pUploadButton);

        //get image from camera gallery on click
        imageIv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                imageSelection();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //fetch data from title and description boxes in our application:
                String title = titleEt.getText().toString().trim();
                String description = descriptionEt.getText().toString().trim();
                if(TextUtils.isEmpty(title)){
                    Toast.makeText(MainActivity.this, "Title required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(description)){
                    Toast.makeText(MainActivity.this, "Description required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (image_rui == null){
                    // post without image
                    uploadData(title, description, "noImage");
                }
                else {
                    // post with image
                    uploadData(title, description, String.valueOf(image_rui));
                }
            }
        });
    }

    private void uploadData(String title, String description, String uri) {
        progressBar.setMessage("Publishing post...");
        progressBar.show();

        //post-image name, post-id, post-publish-time
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathway = "Posts/" + "post_" + timeStamp;
        if(uri.equals("noImage")){
            //post w/o image
            HashMap<Object, String> hashMap = new HashMap<>();
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescription", description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", timeStamp);

            //path to store post data
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
            // put data in this ref
            ref.child(timeStamp).setValue(hashMap)
                    // success in adding post to database
                    .addOnSuccessListener(v -> {
                        progressBar.dismiss();
                        Toast.makeText(MainActivity.this, "Post successfully added!", Toast.LENGTH_SHORT).show();
                        //reset views
                        titleEt.setText("");
                        descriptionEt.setText("");
                        imageIv.setImageURI(null);
                        image_rui = null;
                    })
                    // failed to add post to the database
                    .addOnFailureListener(e -> {
                        progressBar.dismiss();
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
        else{
            //post w/ image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathway);
            ref.putFile(Uri.parse(uri))
                    //Successful upload
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapShot) {

                            Task<Uri> uriTask = taskSnapShot.getStorage().getDownloadUrl();
                            while(!uriTask.isSuccessful());

                            String downloadUri = uriTask.getResult().toString();

                            if(uriTask.isSuccessful()){

                                //received uploaded post to firebase database

                                HashMap<Object, String> hashMap = new HashMap<>();
                                hashMap.put("pId", timeStamp);
                                hashMap.put("pTitle", title);
                                hashMap.put("pDescription", description);
                                hashMap.put("pImage", downloadUri);
                                hashMap.put("pTime", timeStamp);

                                //path to store post data
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                                // put data in this ref
                                ref.child(timeStamp).setValue(hashMap)
                                        // success in adding post to database
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void v){
                                                progressBar.dismiss();
                                                Toast.makeText(MainActivity.this, "Post successfully added!", Toast.LENGTH_SHORT).show();
                                                titleEt.setText("");
                                                descriptionEt.setText("");
                                                imageIv.setImageURI(null);
                                                image_rui = null;
                                            }
                                        })
                                        // failed to add post to the database
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e){
                                                progressBar.dismiss();
                                                Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    })
                    //failure to upload
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.dismiss();
                            Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void imageSelection() {
        // Provide options for users camera & gallery
        String[] imageOptions = {"Camera", "Gallery"};

        // Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");

        builder.setItems(imageOptions, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // handler for options
                // TODO: [Optional] add verification when choosing images or photo
                switch(which){
                    case 0:{
                        // camera chosen
                        if(!checkCameraPermission()){
                            requestCameraPermission();
                        }
                        else {
                            utilizeCamera();
                        }
                    }
                    break;
                    case 1:{
                        // image gallery chosen
                        if(!checkStoragePermission()){
                            requestStoragePermission();
                        }
                        else{
                            selectPhoto();
                        }

                    }
                    break;
                }
            }
        });


        builder.create().show();

    }

    // STORAGE PERMISSIONS
    private boolean checkStoragePermission() {
        // during runtime
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission(){
        // during runtime
        ActivityCompat.requestPermissions(this, storagePermissions, GALLERY_REQUEST_CODE);
    }

    // CAMERA PERMISSIONS
    private boolean checkCameraPermission() {
        // Check camera permissions during runtime
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return cameraResult && storageResult;
    }

    private void requestCameraPermission(){
        // Request camera permissions during runtime
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }


    @Override
    protected void onStart(){
        // TODO: (Optional) Verify if user is still logged in
        super.onStart();
        // verifyUserStatus()
    }

    @Override
    protected void onResume(){
        // TODO: (See OnStart)
        super.onResume();
        // verifyUserStatus()
    }
    @Override
    public boolean onSupportNavigateUp() {
        // Go to previous screen (activity)
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_create_post).setVisible(false);
        // menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    // TODO: Finish items selected logic for post creation
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_create_post) {
            startActivity(new Intent(this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    utilizeCamera();
                    /*
                    if (cameraAccepted && storageAccepted){
                        utilizeCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera & Storage permissions required.", Toast.LENGTH_SHORT).show();
                    }
                    */
                }
            }
            break;
            case GALLERY_REQUEST_CODE:{
                if(grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    selectPhoto();
                    /*
                    if(storageAccepted){
                        selectPhoto();
                    }
                    else{
                        Toast.makeText(this, "Storage permissions required.", Toast.LENGTH_SHORT).show();
                    }
                    */
                }
            }
            break;
        }
    }

    private void selectPhoto() {
        //intent to pick image from gallery:
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        // startStorageAccessForResult.launch(intent);
        startActivityForResult(intent, IMAGE_FROM_STORAGE_CODE);

    }

    /*
    ActivityResultLauncher<Intent> startStorageAccessForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result){
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        image_rui = data.getData();
                        // give imageview
                        imageIv.setImageURI(image_rui);
                    }

                }
            });
     */
    private void utilizeCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Place Holder");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Place Holder");
        Uri image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_rui);
        // startCameraForResult.launch(intent);
        startActivityForResult(intent, IMAGE_FROM_CAMERA_CODE);
    }

    /*
    ActivityResultLauncher<Intent> startCameraForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result){
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        image_rui = data.getData();
                        // give imageview
                        imageIv.setImageURI(image_rui);
                    }

                }
            });

    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_FROM_CAMERA_CODE){
                assert data != null;
                image_rui = data.getData();
                //give imageview
                imageIv.setImageURI(image_rui);
            }
            else if(requestCode == IMAGE_FROM_STORAGE_CODE){
                imageIv.setImageURI(image_rui);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}