package net.evendanan.ironhenry.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

public class StorynoryServiceFactory {
    public static StorynoryService createService() {
        Gson gson = new GsonBuilder().create();

        return new RestAdapter.Builder()
                .setEndpoint("http://www.storynory.com")
                .setConverter(new GsonConverter(gson))
                .build().create(StorynoryService.class);
    }
}
