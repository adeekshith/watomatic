package com.parishod.watomatic.activity.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.githubUesrsResponse.GithubUsersResponse;
import com.parishod.watomatic.network.GetGitUserInterface;
import com.parishod.watomatic.network.RetrofitInstance;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView text_view;
    private String all_contributers = "";
    private TextView privacy_policy;
    private TextView developer_link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        text_view = findViewById(R.id.contributers);
        privacy_policy = findViewById(R.id.privacyPolicyLabel);
        developer_link = findViewById(R.id.developerLink);

        privacy_policy.setOnClickListener(this);
        developer_link.setOnClickListener(this);
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance();
        GetGitUserInterface getGitUserInterface = retrofit.create(GetGitUserInterface.class);

        Call<List<GithubUsersResponse>> call = getGitUserInterface.getContriUsers();
        call.enqueue(new Callback<List<GithubUsersResponse>>() {
            @Override
            public void onResponse(Call<List<GithubUsersResponse>> call, Response<List<GithubUsersResponse>> response) {
                if (response.isSuccessful()) {
                    List<GithubUsersResponse> list = response.body();
                    for (GithubUsersResponse i : list) {
                        all_contributers += i.getLogin() + ", ";
                    }
                    text_view.setText(all_contributers);
                }
            }

            @Override
            public void onFailure(Call<List<GithubUsersResponse>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.privacyPolicyLabel) {
            String url = "https://adeekshith.github.io/watomatic/#/privacy-policy.md";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }

        if (view.getId() == R.id.developerLink) {
            String url = "https://twitter.com/adeekshith";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    }
}