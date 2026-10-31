package com.example.atm;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** One screen used for both DEPOSIT and WITHDRAW; the menu tells it which one. */
public class TransactionActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "MODE";
    public static final String MODE_DEPOSIT = "DEPOSIT";
    public static final String MODE_WITHDRAW = "WITHDRAW";

    long accNo;
    boolean isDeposit;
    DatabaseHelper db;
    TextView tvCurrentBalance;
    EditText etAmount;
    Button btnConfirm, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        accNo = getIntent().getLongExtra(Util.EXTRA_ACC_NO, -1);
        isDeposit = MODE_DEPOSIT.equals(getIntent().getStringExtra(EXTRA_MODE));
        db = new DatabaseHelper(this);

        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        etAmount = findViewById(R.id.etAmount);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnBack = findViewById(R.id.btnBack);

        setTitle(isDeposit ? "Deposit" : "Withdraw");
        btnConfirm.setText(isDeposit ? "DEPOSIT" : "WITHDRAW");
        showBalance();

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                process();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showBalance() {
        tvCurrentBalance.setText("Current balance: " + Util.money(db.getBalance(accNo)));
    }

    private void process() {
        long amount = Util.parseNumber(etAmount.getText().toString());
        if (amount < 0) {
            toast("Enter an amount");
            return;
        }
        if (amount == 0) {
            toast("Amount must be greater than zero");
            return;
        }

        if (isDeposit) {
            db.deposit(accNo, amount);
            toast("Deposited " + Util.money(amount));
        } else {
            if (amount > Util.WITHDRAW_LIMIT) {
                toast("Maximum per withdrawal is " + Util.money(Util.WITHDRAW_LIMIT));
                return;
            }
            if (!db.withdraw(accNo, amount)) {
                toast("Insufficient funds");
                return;
            }
            toast("Withdrew " + Util.money(amount));
        }
        etAmount.setText("");
        showBalance();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
