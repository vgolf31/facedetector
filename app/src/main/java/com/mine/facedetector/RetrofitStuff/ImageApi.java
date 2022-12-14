package com.mine.facedetector.RetrofitStuff;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public interface ImageApi {
    @Multipart
    @POST("post_image/")
    public Call<ResponseBody> postImage (@Part MultipartBody.Part image);

    @Multipart
    @POST ("post_images/")
    public Call<ResponseBody> postImages (@Part MultipartBody.Part[] images);
}
