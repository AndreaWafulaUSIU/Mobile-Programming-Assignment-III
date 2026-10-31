package com.example.atm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Opens a new account with a PIN and an opening deposit. */
public class CreateAccountActivity extends AppCompatActivity {

    private static final long MIN_OPENING = 1000;

    EditText etName, etNewPin, etConfirmPin, etOpening;
    Button btnCreate;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        setTitle("New Account");

        db = new DatabaseHelper(this);
        etName = findViewById(R.id.etName);
        etNewPin = findViewById(R.id.etNewPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        etOpening = findViewById(R.id.etOpening);
        btnCreate = findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    private void createAccount() {
        String name = etName.getText().toString().trim();
        String pin = etNewPin.getText().toString().trim();
        String confirm = etConfirmPin.getText().toString().trim();
        long opening = Util.parseNumber(etOpening.getText().toString());

        if (name.isEmpty()) {
            toast("Enter your name");
            return;
        }
        if (!Util.isValidPin(pin)) {
            toast("PIN must be exactly 4 digits");
            return;
        }
        if (!pin.equals(confirm)) {
            toast("PINs do not match");
            return;
        }
        if (opening < MIN_OPENING) {
            toast("Opening deposit must be at least " + Util.money(MIN_OPENING));
            return;
        }

        long accNo = db.createAccount(name, pin, opening);

        new AlertDialog.Builder(this)
                .setTitle("Account created")
                .setMessage("Your account number is " + accNo + ".\nUse it with your PIN to log in.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
