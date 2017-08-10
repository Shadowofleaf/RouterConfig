/*
 * Copyright 2017 Aleksandr Tarakanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.atzcx.appverupdater;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.github.atzcx.appverupdater.enums.UpdateErrors;
import com.github.atzcx.appverupdater.interfaces.DownloadListener;
import com.github.atzcx.appverupdater.interfaces.RequestListener;
import com.github.atzcx.appverupdater.models.Update;
import com.github.atzcx.appverupdater.utils.DialogUtils;
import com.github.atzcx.appverupdater.utils.UpdaterUtils;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.ThinDownloadManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AsyncClient {


    /**
     * Information from the server about new ads app
     */

    public static class AsyncStringRequest {

        private Context context;
        private String url;
        private RequestListener listener;
        private OkHttpClient client;
        private Response response;
        private Request request;

        public AsyncStringRequest(Context context, String url, RequestListener listener) {
            this.context = context;
            this.url = url;
            this.listener = listener;
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS).build();

        }

        public void execute() {

            if (listener == null || context == null || client == null) {
                return;
            } else if (UpdaterUtils.isNetworkAvailable(context)) {
                if (url == null || url.length() == 0) {
                    throw new RuntimeException("Argument Url cannot be null or empty");
                }
            } else {
                listener.onFailure(UpdateErrors.NETWORK_NOT_AVAILABLE);
                return;
            }


            request = new Request.Builder()
                    .url(this.url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    listener.onFailure(UpdateErrors.ERROR_CHECKING_UPDATES);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {

                        if (response != null) {
                            try {

                                Update updateModel = JSONParser.parse(new JSONObject(response.body().string()));

                                if (updateModel != null) {
                                    listener.onSuccess(updateModel);
                                }

                            } catch (IOException | JSONException e) {
                                listener.onFailure(UpdateErrors.ERROR_CHECKING_UPDATES);
                            }
                        } else {
                            listener.onFailure(UpdateErrors.FILE_JSON_NO_DATA);
                        }

                    } else {

                        if (response.code() == 404) {
                            listener.onFailure(UpdateErrors.JSON_FILE_IS_MISSING);
                        }

                    }
                }
            });

        }

    }


    /**
     * Download the updates from the server
     */

    public static class AsyncDownloadRequest {

        private Context context;
        private String url;

        private CharSequence message;
        private String downloadFileName;
        private DownloadListener listener;
        private ProgressDialog progressDialog;

        private DownloadRequest downloadRequest;

        public AsyncDownloadRequest(final Context context, String url, CharSequence message, String downloadFileName, DownloadListener listener) {
            this.context = context;
            this.url = url;
            this.message = message;
            this.downloadFileName = downloadFileName;
            this.listener = listener;
            this.progressDialog = DialogUtils.showDownloadProgressDialog(context, this.message);
        }


        public void execute() {

            if (listener == null || context == null) {
                return;
            } else if (UpdaterUtils.isNetworkAvailable(context)) {
                if (url == null || url.length() == 0) {
                    throw new RuntimeException("Argument Url cannot be null or empty");
                }
            } else {
                listener.onFailure(UpdateErrors.NETWORK_NOT_AVAILABLE);
                return;
            }

            this.progressDialog.show();

            Uri downloadUri = Uri.parse(this.url);

            final File SDCardRoot = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download");

             if (SDCardRoot.exists() == false) {
                 SDCardRoot.mkdirs();
             }

            File file = new File(SDCardRoot, downloadFileName);

            Uri destinationUri = Uri.parse(file.getPath());

            ThinDownloadManager downloadManager = new ThinDownloadManager(5);

            downloadRequest = new DownloadRequest(downloadUri)
                    .setDestinationURI(destinationUri);

            downloadManager.add(downloadRequest);

            downloadRequest.setStatusListener(new DownloadStatusListenerV1() {
                @Override
                public void onDownloadComplete(DownloadRequest downloadRequest) {
                    AsyncDownloadRequest.this.progressDialog.dismiss();
                    listener.onSuccess(new File(SDCardRoot, downloadFileName));
                }

                @Override
                public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
                    listener.onFailure(UpdateErrors.ERROR_DOWNLOADING_UPDATES);
                    AsyncDownloadRequest.this.progressDialog.dismiss();
                }

                @Override
                public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
                    AsyncDownloadRequest.this.progressDialog.setMessage(AsyncDownloadRequest.this.message + " - " + progress + "%");
                }
            });

        }

        public DownloadRequest get(){
            return downloadRequest;
        }

    }

}
