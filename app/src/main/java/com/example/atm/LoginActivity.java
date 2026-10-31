package com.example.atm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 1: PIN login. Three wrong PINs lock the account for 60 seconds. */
public class LoginActivity extends AppCompatActivity {

    EditText etAccount, etPin;
    Button btnLogin, btnCreateAccount;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);
        etAccount = findViewById(R.id.etAccount);
        etPin = findViewById(R.id.etPin);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
            }
        });
    }

    private void login() {
        String accText = etAccount.getText().toString().trim();
        String pin = etPin.getText().toString().trim();

        if (accText.isEmpty() || pin.isEmpty()) {
            toast("Enter your account number and PIN");
            return;
        }
        long accNo = Util.parseNumber(accText);
        if (accNo < 0) {
            toast("Account number must contain digits only");
            return;
        }
        if (!Util.isValidPin(pin)) {
            toast("PIN must be exactly 4 digits");
            return;
        }

        DatabaseHelper.LoginResult result = db.login(accNo, pin);
        switch (result.status) {
            case DatabaseHelper.LOGIN_OK:
                etPin.setText("");
                Intent intent = new Intent(this, MenuActivity.class);
                intent.putExtra(Util.EXTRA_ACC_NO, accNo);
                startActivity(intent);
                finish();
                break;
            case DatabaseHelper.LOGIN_NO_ACCOUNT:
                toast("Account not found");
                break;
            case DatabaseHelper.LOGIN_LOCKED:
                etPin.setText("");
                toast("Too many wrong PINs. Account locked. Try again in "
                        + result.secondsLeft + " seconds");
                break;
            default:
                etPin.setText("");
                toast("Wrong PIN. Attempts left: " + result.attemptsLeft);
                break;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
