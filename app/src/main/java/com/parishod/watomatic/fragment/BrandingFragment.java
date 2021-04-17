package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.GithubReleaseNotes;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.network.GetReleaseNotesService;
import com.parishod.watomatic.network.RetrofitInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.ACTION_VIEW;

public class BrandingFragment extends Fragment {
    private ImageButton githubBtn;
    private ImageButton share_layout;
    private Button watomaticSubredditBtn, whatsNewBtn;
    private List<String> whatsNewUrls;
    private int gitHubReleaseNotesId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_branding, container, false);

        githubBtn = view.findViewById(R.id.watomaticGithubBtn);
        share_layout = view.findViewById(R.id.share_btn);
        watomaticSubredditBtn = view.findViewById(R.id.watomaticSubredditBtn);
        whatsNewBtn = view.findViewById(R.id.whatsNewBtn);
        whatsNewBtn.setOnClickListener(v -> {
            launchApp();
        });

        share_layout.setOnClickListener(v -> launchShareIntent());

        watomaticSubredditBtn.setOnClickListener(v -> {
            String url = getString(R.string.watomatic_subreddit_url);
            startActivity(
                    new Intent(ACTION_VIEW).setData(Uri.parse(url))
            );
        });

        githubBtn.setOnClickListener(v -> {
            String url = getString(R.string.watomatic_github_url);
            startActivity(
                    new Intent(ACTION_VIEW).setData(Uri.parse(url))
            );
        });

        getGthubReleaseNotes();

        return view;
    }

    private void launchApp() {
        for(String url: whatsNewUrls){
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
            List<ResolveInfo> list = getActivity().getPackageManager()
                    .queryIntentActivities(intent, 0);
            boolean isLaunched = false;
            //Check for non browser application
            for(ResolveInfo resolveInfo: list) {
                if (!resolveInfo.activityInfo.packageName.contains("com.android")
                    && !resolveInfo.activityInfo.packageName.contains("broswer")
                    && !resolveInfo.activityInfo.packageName.contains("chrome")) {
                    intent.setPackage(resolveInfo.activityInfo.packageName);
                    PreferencesManager.getPreferencesInstance(getActivity()).setGithubReleaseNotesId(gitHubReleaseNotesId);
                    startActivity(intent);
                    isLaunched = true;
                    break;
                }
            }
            if(isLaunched){
                break;
            }
        }
    }

    private void getGthubReleaseNotes() {
        GetReleaseNotesService releaseNotesService = RetrofitInstance.getRetrofitInstance().create(GetReleaseNotesService.class);
        Call<GithubReleaseNotes> call = releaseNotesService.getReleaseNotes();
        call.enqueue(new Callback<GithubReleaseNotes>() {
            @Override
            public void onResponse(Call<GithubReleaseNotes> call, Response<GithubReleaseNotes> response) {
                parseReleaseNotesResponse(response.body());
            }

            @Override
            public void onFailure(Call<GithubReleaseNotes> call, Throwable t) {

            }
        });
    }

    private void parseReleaseNotesResponse(GithubReleaseNotes releaseNotes) {
        gitHubReleaseNotesId = releaseNotes.getId();
        String body = releaseNotes.getBody();
        int gitHubId = PreferencesManager.getPreferencesInstance(getActivity()).getGithubReleaseNotesId();
        //Check local githubid and id received id's are different and if its not minor release
        if((gitHubId == 0 || gitHubId != gitHubReleaseNotesId) && !body.contains("minor-release: true")) {
            //Split the body into separate lines and search for line starting with "view release notes on"
            String[] splitStr = body.split("\n");
            if(splitStr.length > 0) {
                for (String s : splitStr) {
                    if (s.toLowerCase().startsWith("view release notes on")) {
                        whatsNewUrls = extractLinks(s);
                        watomaticSubredditBtn.setVisibility(View.GONE);
                        whatsNewBtn.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        }
    }

    public static List<String> extractLinks(String text) {
        List<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }

        return links;
    }


    private void launchShareIntent() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.share_app_text));
        startActivity(Intent.createChooser(sharingIntent, "Share app via"));
    }
}
