package nyc.c4q.ashiquechowdhury.auxx.joinandcreate;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.IOException;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;
import nyc.c4q.ashiquechowdhury.auxx.ArtistAdapter;
import nyc.c4q.ashiquechowdhury.auxx.R;
import nyc.c4q.ashiquechowdhury.auxx.SongTrackClickListener;
import nyc.c4q.ashiquechowdhury.auxx.model.PlaylistTrack;
import nyc.c4q.ashiquechowdhury.auxx.model.SpotifyService;
import nyc.c4q.ashiquechowdhury.auxx.model.artistModel.ArtistResponse;
import nyc.c4q.ashiquechowdhury.auxx.model.artistModel.Track;
import nyc.c4q.ashiquechowdhury.auxx.util.SongListHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

/**
 * Created by SACC on 3/6/17.
 */

public class CurrentSongInfoFragment extends Fragment implements View.OnClickListener, SongTrackClickListener{

    private CircleImageView artistPictureIV;

    private String roomName = "musicList";
    private FirebaseDatabase database;
    private DatabaseReference reference;
    private ImageView albumArtWorkIv;
    private TextView songNameTv;
    private TextView artistNameTv;
    private TextView albumNameTv;
    private ImageButton likeButton;
    private ImageButton vetoButton;
    private final String CHOSEN_TRACK_KEY = "chosen track";
    private PlaylistTrack track;
    RecyclerView recyclerView;
    ArtistAdapter artistAdapter;
    View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_currentsong_info, container, false);
        initializeViews();

        Bundle getArgumentsBundle = getArguments();
        if(getArgumentsBundle != null) {
            roomName = getArgumentsBundle.getString(JoinRoomActivity.ROOMNAMEKEY);
        }

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        Bundle bundle = getArguments();
        if(bundle.getSerializable(CHOSEN_TRACK_KEY) != null){
            track = (PlaylistTrack) bundle.getSerializable(CHOSEN_TRACK_KEY);
            Glide.with(getContext()).load(track.getAlbumArt()).into(albumArtWorkIv);
            songNameTv.setText(track.getTrackName());
            artistNameTv.setText(track.getArtistName());
            albumNameTv.setText(track.getAlbumName());
            getTracks(track);
        } else {
            if (SongListHelper.getCurrentlyPlayingSong() != null) {
                track = SongListHelper.getCurrentlyPlayingSong();
                Glide.with(getContext()).load(SongListHelper.getCurrentlyPlayingSong().getAlbumArt()).into(albumArtWorkIv);
                songNameTv.setText(SongListHelper.getCurrentlyPlayingSong().getTrackName());
                artistNameTv.setText(SongListHelper.getCurrentlyPlayingSong().getArtistName());
                albumNameTv.setText(SongListHelper.getCurrentlyPlayingSong().getAlbumName());
                getTracks(SongListHelper.getCurrentlyPlayingSong());
                roomName = SongListHelper.roomName;
            } else {
                view = inflater.inflate(R.layout.empty_current_song_layout, container, false);

            }
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.like_button_info_fragment:
                Toasty.success(getActivity().getApplicationContext(), "You Liked This Song", Toast.LENGTH_SHORT, true).show();
                break;
            case R.id.veto_button_info_fragment:
                Toasty.error(getActivity().getApplicationContext(), "You Vetoed This Song", Toast.LENGTH_SHORT, true).show();
                DatabaseReference songReference = reference.child(roomName).child(track.getFirebaseKey());
                userVetoSong(songReference);
                break;
        }

    }


    public void initializeViews() {
        albumArtWorkIv = (ImageView) view.findViewById(R.id.album_art);
        songNameTv = (TextView) view.findViewById(R.id.song_name);
        artistNameTv = (TextView) view.findViewById(R.id.artist_name);
        albumNameTv = (TextView) view.findViewById(R.id.album_name);
        likeButton = (ImageButton) view.findViewById(R.id.like_button_info_fragment);
        vetoButton = (ImageButton) view.findViewById(R.id.veto_button_info_fragment);
        recyclerView = (RecyclerView) view.findViewById(R.id.more_from_this_artist_recyclerview);
        artistPictureIV = (CircleImageView) view.findViewById(R.id.artist_picture);
        likeButton.setOnClickListener(this);
        vetoButton.setOnClickListener(this);
    }

    private void getTracks(PlaylistTrack track) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SpotifyService spotifyService = retrofit.create(SpotifyService.class);
        Call<ArtistResponse> httpRequest = spotifyService.getArtistTopTracks(track.getArtistId(), "US");
        httpRequest.enqueue(new Callback<ArtistResponse>() {
            @Override
            public void onResponse(Call<ArtistResponse> call, Response<ArtistResponse> response) {
                try {
                    if (response.isSuccessful()) {
                        findTracks(response.body().getTracks());

                    } else {
                        Log.d(TAG, "Error" + response.errorBody().string());
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ArtistResponse> call, Throwable t) {
                Log.d("failure", "no connection");

            }
        });
    }

    void findTracks(List<Track> tracklist) {
        artistAdapter = new ArtistAdapter(tracklist, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(artistAdapter);
    }

    @Override
    public void songClicked(Track track) {
        PlaylistTrack myTrack = SongListHelper.transformAndAdd(track);
        reference.child(roomName).push().setValue(myTrack);
    }

    public void userVetoSong(DatabaseReference songRef) {
        songRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                PlaylistTrack track = mutableData.getValue(PlaylistTrack.class);
                track.setVetos(track.getVetos() + 1);

                mutableData.setValue(track);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d(TAG, "postTransaction:onComplete:" + databaseError);
            }
        });

    }
}
