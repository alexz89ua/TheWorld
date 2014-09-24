package com.alexz.theworld;

import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alexz.theworld.entity.QuestionEntity;
import com.alexz.theworld.entity.Questions;
import com.alexz.theworld.utils.RippleDrawable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.util.ArrayList;


public class MainMapActivity extends BaseSpiceActivity implements RecognitionListener, View.OnClickListener {
    private SpeechRecognizer speechRecognizer;
    private View mDecorView;
    private ImageButton game;
    private String writeAnswer;
    private LatLng focus;
    private int zoom;
    private GoogleMap map;
    private RelativeLayout card;
    private boolean listenSpeech = false;
    private ProgressBar speechProgress;
    private TextView textResult, tvQuestion;
    private ImageView image;
    private OnResultTaskListener onResultTaskListener = new OnResultTaskListener();
    private ArrayList<QuestionEntity> questionsArray;
    private Handler myHandler;
    private StreetViewPanorama mSvp;
    private LatLng SAN_FRAN = new LatLng(37.765927, -122.449972);
    private RelativeLayout streetView;
    private int questionNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDecorView = getWindow().getDecorView();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(this);
        myHandler = new Handler();
        setUpStreetViewPanoramaIfNeeded(savedInstanceState);

        // no sleep fot this screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get a handle to the Map Fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SAN_FRAN, 13));

        map.addMarker(new MarkerOptions()
                .title("New York City")
                .position(SAN_FRAN));

        initViews();

        GetRequest getRequest = new GetRequest();
        getSpiceManager().execute(getRequest, onResultTaskListener);
    }


    private void setUpStreetViewPanoramaIfNeeded(Bundle savedInstanceState) {
        if (mSvp == null) {
            mSvp = ((StreetViewPanoramaFragment)
                    getFragmentManager().findFragmentById(R.id.streetviewpanorama))
                    .getStreetViewPanorama();
            mSvp.setPosition(SAN_FRAN);
            streetView = (RelativeLayout) findViewById(R.id.street_view);
        }
    }


    private void initViews() {

        card = (RelativeLayout) findViewById(R.id.card);
        RippleDrawable.createRipple(card, getResources().getColor(R.color.material_blue_600));
        ImageButton speech = (ImageButton) findViewById(R.id.speech);
        speech.setOnClickListener(this);
        card.setOnClickListener(this);
        speechProgress = (ProgressBar) findViewById(R.id.speechProgress);
        textResult = (TextView) findViewById(R.id.text_result);
        tvQuestion = (TextView) findViewById(R.id.question);
        image = (ImageView) findViewById(R.id.image);
        game = (ImageButton) findViewById(R.id.game);
        game.setOnClickListener(this);

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                initQuestion();
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mSvp.setPosition(latLng, 1000000);
                streetView.setVisibility(View.VISIBLE);
            }
        });
    }


    @Override
    public void onBackPressed() {
        if (streetView.getVisibility() == View.VISIBLE) {
            streetView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            if (hasFocus) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.card:
                card.setVisibility(View.GONE);
                break;
            case R.id.speech:
                if (!listenSpeech) {
                    speechRecognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
                    listenSpeech = true;
                    speechProgress.setVisibility(View.VISIBLE);
                } else {
                    speechRecognizer.stopListening();
                    listenSpeech = false;
                    speechProgress.setVisibility(View.GONE);
                }
                break;
            case R.id.game:
                openQuestion();
                break;
        }
    }


    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d("Loger", "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("Loger", "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.d("Loger", "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d("Loger", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("Loger", "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        Log.d("Loger", "onError");
        listenSpeech = false;
        speechProgress.setVisibility(View.GONE);
    }

    @Override
    public void onResults(Bundle results) {
        Log.d("Loger", "onResults");
        ArrayList strlist = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < strlist.size(); i++) {
            Log.d("Loger", "result=" + strlist.get(i));
        }
        String result = (String) strlist.get(0);
        textResult.setText(result.substring(0, 1).toUpperCase() + result.substring(1));
        speechRecognizer.stopListening();
        listenSpeech = false;
        speechProgress.setVisibility(View.GONE);

        if (writeAnswer.contains(result)) {

            final Runnable next = new Runnable() {
                @Override
                public void run() {
                    card.setVisibility(View.GONE);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(focus, zoom));

                    map.addMarker(new MarkerOptions()
                            .title(writeAnswer)
                            .position(focus));
                    initQuestion();
                }
            };
            myHandler.postDelayed(next, 1000);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d("Loger", "onPartialResults");
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d("Loger", "onEvent");
    }


    public final class OnResultTaskListener implements RequestListener<Questions> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            Log.i("Loger", "Error: " + spiceException.getMessage());
        }

        @Override
        public void onRequestSuccess(Questions ansver) {
            if (ansver != null && ansver.questions.size() > 0) {
                questionsArray = ansver.questions;
            }
        }
    }


    private void initQuestion() {
        final Runnable question = new Runnable() {
            @Override
            public void run() {
                openQuestion();
            }

        };
        myHandler.postDelayed(question, 4000);
    }


    private void openQuestion() {
        final Runnable speech = new Runnable() {
            @Override
            public void run() {
                if (!listenSpeech) {
                    speechRecognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
                    listenSpeech = true;
                    speechProgress.setVisibility(View.VISIBLE);
                }
            }
        };


        tvQuestion.setText(questionsArray.get(questionNum).question);
        writeAnswer = questionsArray.get(questionNum).answer.toLowerCase();
        focus = new LatLng(questionsArray.get(questionNum).lat, questionsArray.get(questionNum).lon);
        zoom = questionsArray.get(questionNum).zoom;
        textResult.setText("");
        image.setImageDrawable(getResources().getDrawable(R.drawable.gallery));
        ImageLoader.getInstance().displayImage(questionsArray.get(questionNum).image_url, image);
        card.setVisibility(View.VISIBLE);

        questionNum++;

        if (questionNum > questionsArray.size()-1) {
            questionNum = 0;
        }

        myHandler.postDelayed(speech, 4000);

    }

}
