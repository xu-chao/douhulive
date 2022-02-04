package com.xuchao.douhu.ui.about;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.drakeet.about.AbsAboutActivity;
import com.drakeet.about.Card;
import com.drakeet.about.Category;
import com.drakeet.about.Contributor;
import com.drakeet.about.License;
import com.drakeet.about.OnContributorClickedListener;
import com.drakeet.about.OnRecommendationClickedListener;
import com.drakeet.about.Recommendation;
import com.drakeet.about.provided.PicassoImageLoader;
import com.xuchao.douhu.BuildConfig;
import com.xuchao.douhu.R;

import java.util.List;

@SuppressLint("SetTextI18n")
@SuppressWarnings("SpellCheckingInspection")
public class AboutActivity extends AbsAboutActivity
        implements OnRecommendationClickedListener, OnContributorClickedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setImageLoader(new PicassoImageLoader());
        setOnRecommendationClickedListener(this);
        setOnContributorClickedListener(this);
    }

    @Override
    protected void onCreateHeader(@NonNull ImageView icon, @NonNull TextView slogan, @NonNull TextView version) {
        icon.setImageResource(R.mipmap.ic_launcher);
        slogan.setText("斗虎直播");
        version.setText("v" + BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onItemsCreated(@NonNull List<Object> items) {
        items.add(new Category("介绍与帮助"));
        items.add(new Card(getString(R.string.card_content)));
        items.add(new Category("功能特性"));
        items.add(new Category("Open Source Licenses"));
        items.add(new License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"));
        items.add(new License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"));
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_about, menu);
//        MenuItem dayNight = menu.findItem(R.id.menu_night_mode);
//        dayNight.setChecked(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem menuItem) {
//        if (menuItem.getItemId() == R.id.menu_night_mode) {
//            menuItem.setChecked(!menuItem.isChecked());
//            if (menuItem.isChecked()) {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//            } else {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//            }
//            getDelegate().applyDayNight();
//        }
//        return true;
//    }

    @Override
    public boolean onRecommendationClicked(@NonNull View itemView, @NonNull Recommendation recommendation) {
        Toast.makeText(this, "onRecommendationClicked: " + recommendation.appName, Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public boolean onContributorClicked(@NonNull View itemView, @NonNull Contributor contributor) {
        if (contributor.name.equals("test")) {
            Toast.makeText(this, "onContributorClicked: " + contributor.name, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
