package com.example.finalprojectyali.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectyali.Extras.GroupRepository;
import com.example.finalprojectyali.ui.Home.MainActivity;

public class JoinLink extends AppCompatActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();              // com.example.finalprojectyali://join?code=AB12
        if (data == null || !"join".equals(data.getHost())) {
            finish(); return;
        }

        String code = data.getQueryParameter("code");
        if (code == null || code.length()!=4) { finish(); return; }

        GroupRepository.joinByCode(code,
                g -> {
                    Toast.makeText(this,"Joined "+g.getName(),Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                },
                err -> {
                    Toast.makeText(this,err,Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}
