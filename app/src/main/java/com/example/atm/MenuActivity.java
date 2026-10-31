package com.example.atm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 2: main ATM menu shown after a successful login. */
public class MenuActivity extends AppCompatActivity {

    long accNo;
    TextView tvWelcome;
    Button btnBalance, btnDeposit, btnWithdraw, btnTransfer, btnStatement, btnChangePin, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        accNo = getIntent().getLongExtra(Util.EXTRA_ACC_NO, -1);
        if (accNo < 0) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        DatabaseHelper db = new DatabaseHelper(this);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + db.getName(accNo) + "\nAccount " + accNo);

        btnBalance = findViewById(R.id.btnBalance);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnTransfer = findViewById(R.id.btnTransfer);
        btnStatement = findViewById(R.id.btnStatement);
        btnChangePin = findViewById(R.id.btnChangePin);
        btnLogout = findViewById(R.id.btnLogout);

        btnBalance.setOnClickListener(open(BalanceActivity.class, null));
        btnDeposit.setOnClickListener(open(TransactionActivity.class, TransactionActivity.MODE_DEPOSIT));
        btnWithdraw.setOnClickListener(open(TransactionActivity.class, TransactionActivity.MODE_WITHDRAW));
        btnTransfer.setOnClickListener(open(TransferActivity.class, null));
        btnStatement.setOnClickListener(open(StatementActivity.class, null));
        btnChangePin.setOnClickListener(open(ChangePinActivity.class, null));

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MenuActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    /** Builds a click listener that opens a screen and passes it the account number. */
    private View.OnClickListener open(final Class<?> screen, final String mode) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, screen);
                intent.putExtra(Util.EXTRA_ACC_NO, accNo);
                if (mode != null) {
                    intent.putExtra(TransactionActivity.EXTRA_MODE, mode);
                }
                startActivity(intent);
            }
        };
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
