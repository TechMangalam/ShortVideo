package com.bitaam.shortvideo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bitaam.shortvideo.modal.VideoModal;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VideoUploadActivity extends AppCompatActivity {

    TextInputEditText titleEdt,tagEdt;
    ImageButton removeVideoBtn,uploadVideoBtn,attachVideoBtn,attachVideoCameraBtn;
    PlayerView exoPlayerView;
    SimpleExoPlayer player;

    ArrayList<Uri> imgUriList;
    Uri imgUri;
    String offlinePath;
    ArrayList<String> dataUrls;
    ArrayList<String> offlineDataUrls;
    File fileVideo;


    ProgressDialog progressDialog;
    Uri fileUri;
    AlertDialog.Builder builder;

    private final int ATTACH_VIDEO = 111;
    private final int ATTACH_VIDEO_CAMERA = 222;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_upload);

        titleEdt = findViewById(R.id.title_input_field);
        tagEdt = findViewById(R.id.tag_input_field);
        removeVideoBtn = findViewById(R.id.removeAttachedVideoBtn);
        uploadVideoBtn = findViewById(R.id.uploadVideoBtn);
        attachVideoBtn = findViewById(R.id.videoAttachImageBtn);
        attachVideoCameraBtn = findViewById(R.id.cameraAttachImageBtn);

        exoPlayerView  = findViewById(R.id.exoplayer_post);
        player = new SimpleExoPlayer.Builder(getApplicationContext()).build();
        exoPlayerView.setPlayer(player);

        imgUriList = new ArrayList<>();
        dataUrls = new ArrayList<>();
        offlineDataUrls = new ArrayList<>();

        builder = new AlertDialog.Builder(this);

        onClickActions();

    }

    private void onClickActions() {

        removeVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.stop();
                player.removeMediaItem(0);
                fileVideo = null;
                exoPlayerView.setVisibility(View.GONE);
                removeVideoBtn.setVisibility(View.GONE);

            }
        });

        attachVideoCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                offlinePath=null;
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takeVideoIntent, ATTACH_VIDEO_CAMERA);

                }

            }
        });

        attachVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,"Select video"),ATTACH_VIDEO);
            }
        });

        uploadVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert("Posting");

                uploadFileToDatabase(System.currentTimeMillis()+"_post.mp4",fileVideo);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
            switch (requestCode){

                case ATTACH_VIDEO:
                case ATTACH_VIDEO_CAMERA:
                    assert data != null;
                    if (data.getData() != null){
                        Uri mVideoURI = data.getData();

                        if ((getFileSize(mVideoURI)/(1024*1024))<=200) {

                            File file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath());
                            dataUrls.clear();
                            alert("Processing");
                            //progressDialog.setTitle("Processing");
                            removeVideoBtn.setVisibility(View.VISIBLE);
                            new CompressVideo().execute("false", mVideoURI.toString(), file.getPath());

                        }else{
                            Toast.makeText(this, "Choose file having size less than 200 MB", Toast.LENGTH_SHORT).show();
                        }

                    }
                    break;
            }

    }

    private double getFileSize(Uri fileUri) {
        Cursor returnCursor = getContentResolver().
                query(fileUri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        return returnCursor.getLong(sizeIndex);
    }

    private void uploadFileToDatabase(String fileName,File file){

        alert("Posting");
        if (player.isPlaying()){
            player.stop();
        }
        player.release();
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);

        String dataParent = "Post Videos";

        final StorageReference fileRef = FirebaseStorage.getInstance().getReference(dataParent).child(fileName);
        fileRef.putFile(Uri.fromFile(file)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //Toast.makeText(getApplicationContext(),"Image Uploaded",Toast.LENGTH_SHORT).show();
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        dataUrls.add(0,uri.toString());
                        createPostFileType(dataUrls.get(0));
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull @NotNull UploadTask.TaskSnapshot snapshot) {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                progressDialog.setProgress((int)progress);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.setMessage(e.getLocalizedMessage());
                Log.e("Failure",e.getLocalizedMessage());
                Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
            }
        });

    }

    private void createPostFileType(String url) {

        //String dateTime = new SimpleDateFormat("MMM dd,yy h:mm aa", Locale.getDefault()).format(new Date());;
        VideoModal model = new VideoModal();
        String title = titleEdt.getText().toString();
        String tag = tagEdt.getText().toString();


        if (!title.isEmpty()){
            model.setTitle(title);
        }else{
            model.setTitle("na");
        }
        if (!tag.isEmpty()){
            model.setTag(tag);
        }else{
            model.setTag("na");
        }

        model.setVideoUrl(url);

        sendPostToDatabase(model);

    }

    private void sendPostToDatabase(VideoModal model) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
        String key = databaseReference.push().getKey();

        assert key != null;
        databaseReference.child(key).setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {

            }
        }).addOnCompleteListener(task -> {
            progressDialog.setMessage("Posted");
            progressDialog.dismiss();
            finish();
        });

    }

    public void alert(String title) {

        progressDialog = new ProgressDialog(VideoUploadActivity.this);
        progressDialog.setTitle(title);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please Wait ..");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
    }

    @Override
    public void onBackPressed() {
        player.stop();
        player.release();
        super.onBackPressed();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class CompressVideo extends AsyncTask<String,String,String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... strings) {

            String videoPath = null;

            try {
                Uri uri = Uri.parse(strings[1]);

                videoPath = SiliCompressor.with(VideoUploadActivity.this)
                        .compressVideo(uri,strings[2]);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            return videoPath;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            fileVideo = new File(s);
            Uri uri = Uri.fromFile(fileVideo);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            exoPlayerView.setVisibility(View.VISIBLE);
            progressDialog.dismiss();
            player.play();
            offlineDataUrls.add(0,uri.toString());

            float size = fileVideo.length()/1024f;

        }
    }


}