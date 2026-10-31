package com.example.atm;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Change PIN: needs the current PIN, then the new PIN twice. */
public class ChangePinActivity extends AppCompatActivity {

    long accNo;
    DatabaseHelper db;
    EditText etOldPin, etNewPin, etConfirmPin;
    Button btnChange, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pin);
        setTitle("Change PIN");

        accNo = getIntent().getLongExtra(Util.EXTRA_ACC_NO, -1);
        db = new DatabaseHelper(this);

        etOldPin = findViewById(R.id.etOldPin);
        etNewPin = findViewById(R.id.etNewPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnChange = findViewById(R.id.btnChange);
        btnBack = findViewById(R.id.btnBack);

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePin();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void changePin() {
        String oldPin = etOldPin.getText().toString().trim();
        String newPin = etNewPin.getText().toString().trim();
        String confirm = etConfirmPin.getText().toString().trim();

        if (!Util.isValidPin(oldPin)) {
            toast("Enter your current 4-digit PIN");
            return;
        }
        if (!Util.isValidPin(newPin)) {
            toast("New PIN must be exactly 4 digits");
            return;
        }
        if (!newPin.equals(confirm)) {
            toast("New PINs do not match");
            return;
        }
        if (newPin.equals(oldPin)) {
            toast("New PIN must be different from the current PIN");
            return;
        }

        if (db.changePin(accNo, oldPin, newPin)) {
            toast("PIN changed successfully");
            finish();
        } else {
            etOldPin.setText("");
            toast("Current PIN is wrong");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
