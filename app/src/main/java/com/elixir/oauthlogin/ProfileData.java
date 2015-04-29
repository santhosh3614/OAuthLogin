package com.elixir.oauthlogin;

import android.graphics.Bitmap;

/**
 * Created by santhoshkumar on 29/4/15.
 */
public class ProfileData {
    public ProfileData(String name, Bitmap bitmap) {
        this.name = name;
        this.bitmap = bitmap;
    }

    String name;
    Bitmap bitmap;


}
