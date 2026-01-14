package com.example.smart_attendance_system;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudFaceRecognitionService {

    private static final String TAG = "CloudFaceRecognition";

    // AWS Rekognition Configuration
    private static final String AWS_REGION = "us-east-1";
    private static final String AWS_ACCESS_KEY = "YOUR_AWS_ACCESS_KEY";
    private static final String AWS_SECRET_KEY = "YOUR_AWS_SECRET_KEY";

    // Azure Face API Configuration
    private static final String AZURE_ENDPOINT = "https://YOUR_REGION.api.cognitive.microsoft.com";
    private static final String AZURE_SUBSCRIPTION_KEY = "YOUR_AZURE_SUBSCRIPTION_KEY";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;

    public interface CloudRecognitionCallback {
        void onSuccess(CloudRecognitionResult result);
        void onError(String error);
    }

    public static class CloudRecognitionResult {
        public boolean isMatch;
        public float confidence;
        public String faceId;
        public String provider;

        public CloudRecognitionResult(boolean isMatch, float confidence, String faceId, String provider) {
            this.isMatch = isMatch;
            this.confidence = confidence;
            this.faceId = faceId;
            this.provider = provider;
        }
    }

    public CloudFaceRecognitionService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // Azure Face API Implementation
    public void verifyFaceWithAzure(String referenceFaceBase64, String currentFaceBase64,
                                    String enrollmentNo, CloudRecognitionCallback callback) {

        try {
            // Step 1: Create person group if not exists
            createPersonGroupIfNotExists(enrollmentNo, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Failed to create person group: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() || response.code() == 409) { // 409 = already exists
                        // Step 2: Add reference face to person group
                        addFaceToPersonGroup(enrollmentNo, referenceFaceBase64, callback);
                    } else {
                        callback.onError("Failed to create person group: " + response.message());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in Azure face verification: " + e.getMessage());
            callback.onError("Azure verification error: " + e.getMessage());
        }
    }

    private void createPersonGroupIfNotExists(String personGroupId, Callback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", "Student_" + personGroupId);
            requestBody.put("userData", "Student face group for " + personGroupId);

            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(AZURE_ENDPOINT + "/face/v1.0/persongroups/" + personGroupId)
                    .put(body)
                    .addHeader("Ocp-Apim-Subscription-Key", AZURE_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(callback);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating person group request: " + e.getMessage());
        }
    }

    private void addFaceToPersonGroup(String personGroupId, String faceBase64,
                                      CloudRecognitionCallback callback) {
        try {
            // First create person in group
            JSONObject personRequest = new JSONObject();
            personRequest.put("name", "Student_" + personGroupId);
            personRequest.put("userData", "Student " + personGroupId);

            RequestBody personBody = RequestBody.create(personRequest.toString(), JSON);
            Request personCreateRequest = new Request.Builder()
                    .url(AZURE_ENDPOINT + "/face/v1.0/persongroups/" + personGroupId + "/persons")
                    .post(personBody)
                    .addHeader("Ocp-Apim-Subscription-Key", AZURE_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(personCreateRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Failed to create person: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject personResponse = new JSONObject(responseBody);
                            String personId = personResponse.getString("personId");

                            // Add face to person
                            addFaceToPerson(personGroupId, personId, faceBase64, callback);

                        } catch (JSONException e) {
                            callback.onError("Error parsing person creation response: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Failed to create person: " + response.message());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError("Error creating person request: " + e.getMessage());
        }
    }

    private void addFaceToPerson(String personGroupId, String personId, String faceBase64,
                                 CloudRecognitionCallback callback) {
        try {
            JSONObject faceRequest = new JSONObject();
            faceRequest.put("url", "data:image/jpeg;base64," + faceBase64);

            RequestBody faceBody = RequestBody.create(faceRequest.toString(), JSON);
            Request faceAddRequest = new Request.Builder()
                    .url(AZURE_ENDPOINT + "/face/v1.0/persongroups/" + personGroupId +
                            "/persons/" + personId + "/persistedFaces")
                    .post(faceBody)
                    .addHeader("Ocp-Apim-Subscription-Key", AZURE_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(faceAddRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Failed to add face: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Train the person group
                        trainPersonGroup(personGroupId, callback);
                    } else {
                        callback.onError("Failed to add face: " + response.message());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError("Error creating face add request: " + e.getMessage());
        }
    }

    private void trainPersonGroup(String personGroupId, CloudRecognitionCallback callback) {
        Request trainRequest = new Request.Builder()
                .url(AZURE_ENDPOINT + "/face/v1.0/persongroups/" + personGroupId + "/train")
                .post(RequestBody.create("", JSON))
                .addHeader("Ocp-Apim-Subscription-Key", AZURE_SUBSCRIPTION_KEY)
                .build();

        client.newCall(trainRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to train person group: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Training started successfully
                    callback.onSuccess(new CloudRecognitionResult(true, 1.0f, personGroupId, "Azure"));
                } else {
                    callback.onError("Failed to train person group: " + response.message());
                }
            }
        });
    }

    // Identify face against person group
    public void identifyFace(String faceBase64, String personGroupId, CloudRecognitionCallback callback) {
        try {
            // First detect face
            JSONObject detectRequest = new JSONObject();
            detectRequest.put("url", "data:image/jpeg;base64," + faceBase64);

            RequestBody detectBody = RequestBody.create(detectRequest.toString(), JSON);
            Request detectFaceRequest = new Request.Builder()
                    .url(AZURE_ENDPOINT + "/face/v1.0/detect")
                    .post(detectBody)
                    .addHeader("Ocp-Apim-Subscription-Key", AZURE_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(detectFaceRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Failed to detect face: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONArray faces = new JSONArray(responseBody);

                            if (faces.length() > 0) {
                                JSONObject face = faces.getJSONObject(0);
                                String faceId = face.getString("faceId");

                                // Identify the face
                                performIdentification(faceId, personGroupId, callback);
                            } else {
                                callback.onError("No face detected in image");
                            }

                        } catch (JSONException e) {
                            callback.onError("Error parsing face detection response: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Face detection failed: " + response.message());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError("Error creating face detection request: " + e.getMessage());
        }
    }

    private void performIdentification(String faceId, String personGroupId, CloudRecognitionCallback callback) {
        try {
            JSONObject identifyRequest = new JSONObject();
            JSONArray faceIds = new JSONArray();
            faceIds.put(faceId);
            identifyRequest.put("faceIds", faceIds);
            identifyRequest.put("personGroupId", personGroupId);
            identifyRequest.put("maxNumOfCandidatesReturned", 1);
            identifyRequest.put("confidenceThreshold", 0.5);

            RequestBody identifyBody = RequestBody.create(identifyRequest.toString(), JSON);
            Request identifyFaceRequest = new Request.Builder()
                    .url(AZURE_ENDPOINT + "/face/v1.0/identify")
                    .post(identifyBody)
                    .addHeader("Ocp-Apim-Subscription-Key", AZURE_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(identifyFaceRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Failed to identify face: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONArray results = new JSONArray(responseBody);

                            if (results.length() > 0) {
                                JSONObject result = results.getJSONObject(0);
                                JSONArray candidates = result.getJSONArray("candidates");

                                if (candidates.length() > 0) {
                                    JSONObject candidate = candidates.getJSONObject(0);
                                    double confidence = candidate.getDouble("confidence");
                                    String personId = candidate.getString("personId");

                                    boolean isMatch = confidence >= 0.7; // Confidence threshold
                                    callback.onSuccess(new CloudRecognitionResult(isMatch, (float)confidence, personId, "Azure"));
                                } else {
                                    callback.onSuccess(new CloudRecognitionResult(false, 0.0f, null, "Azure"));
                                }
                            } else {
                                callback.onSuccess(new CloudRecognitionResult(false, 0.0f, null, "Azure"));
                            }

                        } catch (JSONException e) {
                            callback.onError("Error parsing identification response: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Face identification failed: " + response.message());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError("Error creating identification request: " + e.getMessage());
        }
    }

    // AWS Rekognition Implementation (Placeholder)
    public void verifyFaceWithAWS(String referenceFaceBase64, String currentFaceBase64,
                                  String collectionId, CloudRecognitionCallback callback) {
        // This would require AWS SDK implementation
        // For now, return error indicating it needs AWS SDK
        callback.onError("AWS Rekognition requires AWS SDK implementation");
    }

    // Cleanup method
    public void cleanup() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}