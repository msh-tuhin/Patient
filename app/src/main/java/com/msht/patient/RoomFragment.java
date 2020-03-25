package com.msht.patient;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RoomFragment extends Fragment {

    final String ROOM_NAME = "sweet_home";
    final String IDENTITY = "patient";
    String twilio_access_token;

    LocalAudioTrack localAudioTrack;
    LocalVideoTrack localVideoTrack;
    CameraCapturer cameraCapturer;
    Room room;

    VideoView localVideoView;
    VideoView remoteVideoView;

    public RoomFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localVideoView = view.findViewById(R.id.local_video);
        remoteVideoView = view.findViewById(R.id.remote_video);

        createLocalMediaTracks();
        createRoom();
    }

    private void createLocalMediaTracks(){
        localAudioTrack = LocalAudioTrack.create(getActivity(), true);
        cameraCapturer = new CameraCapturer(getActivity(), CameraCapturer.CameraSource.FRONT_CAMERA);
        localVideoTrack = LocalVideoTrack.create(getActivity(), true, cameraCapturer);
        localVideoTrack.addRenderer(localVideoView);
    }

    private void createRoom(){
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.authenticator(new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic("SK0945016da992ba278f419ba5fe41f32e", "9PMCXCuDCqcV6lgkJdk8AKTagKmK5yks");
                return response.request().newBuilder().header("Authorization", credential).build();
            }
        });

        OkHttpClient client = clientBuilder.build();
        String json = "{\"UniqueName\":" + ROOM_NAME + "}";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url("https://video.twilio.com/v1/Rooms")
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("room", "creation failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 201){
                    Log.i("room", "created successfully");
                    // connectToRoom();
                    ((ConvoActivity)RoomFragment.this.getActivity()).tv.setText("Generating Access Token");
                    getAccessTokenAndConnectRoom();
                }
            }
        });
    }

    private void getAccessTokenAndConnectRoom(){
        OkHttpClient client = new OkHttpClient();
        String json = "{\"room\":\"" + ROOM_NAME + "\",\"identity\":\""+IDENTITY+ "\"}";
        Log.i("json", json);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url("https://us-central1-daktar-bondhu-test.cloudfunctions.net/getAccessToken")
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("access_token", "couldn't fetch token");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                twilio_access_token = response.body().string();
                Log.i("access_token", twilio_access_token);
                Log.i("access_token", "fetched successfully");
                ((ConvoActivity)RoomFragment.this.getActivity()).tv.setText("Connecting to room");
                connectToRoom();
            }
        });
    }

    private void connectToRoom() {
        List<LocalAudioTrack> localAudioTracks = new ArrayList<>();
        localAudioTracks.add(localAudioTrack);
        List<LocalVideoTrack> localVideoTracks = new ArrayList<>();
        localVideoTracks.add(localVideoTrack);
        ConnectOptions connectOptions = new ConnectOptions.Builder(twilio_access_token)
                .roomName(ROOM_NAME)
                .audioTracks(localAudioTracks)
                .videoTracks(localVideoTracks)
                .build();
        room = Video.connect(getActivity(), connectOptions, new Room.Listener() {
            @Override
            public void onConnected(@NonNull Room room) {
                ((ConvoActivity)RoomFragment.this.getActivity()).tv.setText("Joined room: " + room.getName());
                Log.i("identity", room.getLocalParticipant().getIdentity());
                Log.i("room_name", room.getName());
                for(RemoteParticipant remoteParticipant : room.getRemoteParticipants()){
                    addRemoteParticipant(remoteParticipant);
                }
                room.getLocalParticipant().publishTrack(localVideoTrack);
                room.getLocalParticipant().publishTrack(localAudioTrack);
            }

            @Override
            public void onConnectFailure(@NonNull Room room, @NonNull TwilioException twilioException) {
                Log.i("connection", "failed to connect");
                Log.i("coode", Integer.toString(twilioException.getCode()));
                Log.i("reason", twilioException.getExplanation());
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onReconnected(@NonNull Room room) {

            }

            @Override
            public void onDisconnected(@NonNull Room room, @Nullable TwilioException twilioException) {

            }

            @Override
            public void onParticipantConnected(@NonNull Room room, @NonNull RemoteParticipant remoteParticipant) {
                addRemoteParticipant(remoteParticipant);
            }

            @Override
            public void onParticipantDisconnected(@NonNull Room room, @NonNull RemoteParticipant remoteParticipant) {

            }

            @Override
            public void onRecordingStarted(@NonNull Room room) {

            }

            @Override
            public void onRecordingStopped(@NonNull Room room) {

            }
        });
    }

    private void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        if (remoteParticipant.getRemoteVideoTracks().size() > 0) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                remoteVideoTrackPublication.getRemoteVideoTrack().addRenderer(remoteVideoView);
            }
        }
        remoteParticipant.setListener(remoteParticipantListener());
    }

    private RemoteParticipant.Listener remoteParticipantListener(){
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackPublished(@NonNull RemoteParticipant remoteParticipant,
                                              @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackUnpublished(@NonNull RemoteParticipant remoteParticipant,
                                                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackSubscribed(@NonNull RemoteParticipant remoteParticipant,
                                               @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                                               @NonNull RemoteAudioTrack remoteAudioTrack) {

            }

            @Override
            public void onAudioTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant,
                                                       @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                       @NonNull TwilioException twilioException) {

            }

            @Override
            public void onAudioTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant,
                                                 @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                 @NonNull RemoteAudioTrack remoteAudioTrack) {

            }

            @Override
            public void onVideoTrackPublished(@NonNull RemoteParticipant remoteParticipant,
                                              @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackUnpublished(@NonNull RemoteParticipant remoteParticipant,
                                                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackSubscribed(@NonNull RemoteParticipant remoteParticipant,
                                               @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                                               @NonNull RemoteVideoTrack remoteVideoTrack) {
                remoteVideoTrack.addRenderer(remoteVideoView);
            }

            @Override
            public void onVideoTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant,
                                                       @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                       @NonNull TwilioException twilioException) {

            }

            @Override
            public void onVideoTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant,
                                                 @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                 @NonNull RemoteVideoTrack remoteVideoTrack) {

            }

            @Override
            public void onDataTrackPublished(@NonNull RemoteParticipant remoteParticipant,
                                             @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {

            }

            @Override
            public void onDataTrackUnpublished(@NonNull RemoteParticipant remoteParticipant,
                                               @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {

            }

            @Override
            public void onDataTrackSubscribed(@NonNull RemoteParticipant remoteParticipant,
                                              @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
                                              @NonNull RemoteDataTrack remoteDataTrack) {

            }

            @Override
            public void onDataTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant,
                                                      @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
                                                      @NonNull TwilioException twilioException) {

            }

            @Override
            public void onDataTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant,
                                                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
                                                @NonNull RemoteDataTrack remoteDataTrack) {

            }

            @Override
            public void onAudioTrackEnabled(@NonNull RemoteParticipant remoteParticipant,
                                            @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackDisabled(@NonNull RemoteParticipant remoteParticipant,
                                             @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onVideoTrackEnabled(@NonNull RemoteParticipant remoteParticipant,
                                            @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackDisabled(@NonNull RemoteParticipant remoteParticipant,
                                             @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }
        };
    }
}
