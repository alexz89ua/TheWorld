package com.alexz.theworld;

import android.net.Uri;
import android.util.Log;

import com.alexz.theworld.entity.Questions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.octo.android.robospice.request.SpiceRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by alexandr on 01.07.14.
 */
public class GetRequest extends SpiceRequest<Questions> {

    private static final String TAG = "Loger";
    private String url = "https://raw.githubusercontent.com/alexz89ua/TheWorld/master/questions/quest.txt";


    public GetRequest() {
        super(Questions.class);
        this.setPriority(PRIORITY_HIGH);
    }

    @Override
    public Questions loadDataFromNetwork() throws Exception {

        Log.i(TAG, url);

        Uri.Builder uriBuilder = Uri.parse(url).buildUpon();
        String url = uriBuilder.build().toString();

        Questions questionEntities = new Questions();

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            Log.i(TAG, url + " CODE: " + response.getStatusLine().getStatusCode());
            String result = EntityUtils.toString(entity, HTTP.UTF_8);
            Log.i(TAG, "Result: " + result);
            entity.consumeContent();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            questionEntities = gson.fromJson(result, Questions.class);


        } catch (ConnectTimeoutException e) {
            //Here Connection TimeOut excepion
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            //Here Socket TimeOut excepion
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            //Here Socket TimeOut excepion
            e.printStackTrace();
        }
        return questionEntities;
    }

}