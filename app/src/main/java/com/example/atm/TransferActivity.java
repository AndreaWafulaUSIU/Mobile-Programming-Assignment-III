package com.example.atm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Transfer money to another account, with a confirmation step. */
public class TransferActivity extends AppCompatActivity {

    long accNo;
    DatabaseHelper db;
    TextView tvCurrentBalance;
    EditText etToAccount, etAmount;
    Button btnSend, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        setTitle("Transfer");

        accNo = getIntent().getLongExtra(Util.EXTRA_ACC_NO, -1);
        db = new DatabaseHelper(this);

        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        etToAccount = findViewById(R.id.etToAccount);
        etAmount = findViewById(R.id.etAmount);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        showBalance();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndConfirm();
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

    private void checkAndConfirm() {
        final long toAcc = Util.parseNumber(etToAccount.getText().toString());
        final long amount = Util.parseNumber(etAmount.getText().toString());

        if (toAcc < 0) {
            toast("Enter the recipient account number");
            return;
        }
        if (amount < 0) {
            toast("Enter an amount");
            return;
        }
        if (amount == 0) {
            toast("Amount must be greater than zero");
            return;
        }
        if (toAcc == accNo) {
            toast("You cannot transfer to your own account");
            return;
        }
        if (!db.accountExists(toAcc)) {
            toast("Recipient account not found");
            return;
        }
        if (amount > db.getBalance(accNo)) {
            toast("Insufficient funds");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm transfer")
                .setMessage("Send " + Util.money(amount) + " to " + db.getName(toAcc)
                        + " (account " + toAcc + ")?")
                .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (db.transfer(accNo, toAcc, amount)) {
                            toast("Transferred " + Util.money(amount));
                            etToAccount.setText("");
                            etAmount.setText("");
                            showBalance();
                        } else {
                            toast("Transfer failed");
                        }
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
