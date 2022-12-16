package com.benhuang.baobaobluetoothprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    static String TAG = "LOGIN-ACTIVE";

    EditText mAccountInput, mPasswdInput;
    Button mLoginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginBtn = findViewById(R.id.login_btn);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountInput = findViewById(R.id.account_input);
                mPasswdInput = findViewById(R.id.passwd_input);

                String account = mAccountInput.getText().toString();
                String passwd = mPasswdInput.getText().toString();

                Bundle mBundle = new Bundle();
                mBundle.putString("account", account);
                Intent mBackIntent = new Intent();
                mBackIntent.putExtras(mBundle);
                setResult(Activity.RESULT_OK, mBackIntent);
                finish();
            }
        });


    }
}