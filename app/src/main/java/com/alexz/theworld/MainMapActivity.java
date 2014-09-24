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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.util.ArrayList;
import java.util.Random;


public class MainMapActivity extends BaseSpiceActivity implements RecognitionListener, View.OnClickListener {
    private SpeechRecognizer speechRecognizer;
    private View mDecorView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDecorView = getWindow().getDecorView();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(this);
        myHandler = new Handler();

        // Get a handle to the Map Fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        map.setMyLocationEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);

        LatLng sydney = new LatLng(-33.867, 151.206);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));

        map.addMarker(new MarkerOptions()
                .title("Sydney")
                .snippet("The most populous city in Australia.")
                .position(sydney));

        initViews();

        GetRequest getRequest = new GetRequest();
        getSpiceManager().execute(getRequest, onResultTaskListener);
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

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                initQuestion();
            }
        });
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
            myHandler.postDelayed(next, 5000);
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
                initQuestion();
            }
        }
    }


    private void initQuestion() {

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

        final Runnable question = new Runnable() {
            @Override
            public void run() {
                int number = new Random().nextInt(questionsArray.size());

                tvQuestion.setText(questionsArray.get(number).question);
                writeAnswer = questionsArray.get(number).answer.toLowerCase();
                focus = new LatLng(questionsArray.get(number).lat, questionsArray.get(number).lon);
                zoom = questionsArray.get(number).zoom;
                textResult.setText("");
                image.setImageDrawable(getResources().getDrawable(R.drawable.gallery));
                ImageLoader.getInstance().displayImage(questionsArray.get(number).image_url, image);
                card.setVisibility(View.VISIBLE);

                myHandler.postDelayed(speech, 5000);
            }

        };

        myHandler.postDelayed(question, 7000);
    }

}
