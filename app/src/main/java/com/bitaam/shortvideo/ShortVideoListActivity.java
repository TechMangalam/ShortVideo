package com.bitaam.shortvideo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.bitaam.shortvideo.adapter.VideoListAdapter;
import com.bitaam.shortvideo.modal.VideoModal;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class ShortVideoListActivity extends AppCompatActivity {

    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView videoRecyclerView;
    FloatingActionButton addNewVideoAcnBtn;
    VideoListAdapter videoListAdapter;
    Toolbar toolbar;
    android.widget.SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_video_list);

        swipeRefreshLayout = findViewById(R.id.post_swipe_refresh);
        addNewVideoAcnBtn = findViewById(R.id.addNewVideoActionBtn);
        videoRecyclerView = findViewById(R.id.video_recycler);

        toolbar = findViewById(R.id.toolbar_post);
        setSupportActionBar(toolbar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(),RecyclerView.VERTICAL,false);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setSmoothScrollbarEnabled(true);

        SnapHelper snapHelper = new PagerSnapHelper();

        videoRecyclerView.setLayoutManager(linearLayoutManager);
        snapHelper.attachToRecyclerView(videoRecyclerView);

        videoListAdapter = new VideoListAdapter(new ArrayList<>(),getApplicationContext());
        videoRecyclerView.setAdapter(videoListAdapter);

        onClickActivities();

        getVideoFromDatabase();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_search,menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (android.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setSubmitButtonEnabled(true);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                videoListAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                videoListAdapter.getFilter().filter(query);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        searchView.setQuery("",true);
        if (!searchView.isIconified()) {
            searchView.clearFocus();
            searchView.setIconified(true);
            return;
        }
        super.onBackPressed();
    }

    private void onClickActivities(){
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //postItemsAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        addNewVideoAcnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!searchView.isIconified()) {
                    searchView.setQuery(null,true);
                    searchView.setIconified(true);
                }
                startActivity(new Intent(ShortVideoListActivity.this, VideoUploadActivity.class));
            }
        });

    }

    private void getVideoFromDatabase(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Videos");
        reference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                VideoModal model = snapshot.getValue(VideoModal.class);
                ((VideoListAdapter) Objects.requireNonNull(videoRecyclerView.getAdapter())).update(model);
            }

            @Override
            public void onChildChanged(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull @NotNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onPause() {
        ((VideoListAdapter) Objects.requireNonNull(videoRecyclerView.getAdapter())).pausePlayer();
        searchView.setQuery("",true);
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
            return;
        }
        super.onPause();
    }
}