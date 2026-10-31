package com.example.atm;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Balance enquiry. */
public class BalanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);
        setTitle("Balance Enquiry");

        long accNo = getIntent().getLongExtra(Util.EXTRA_ACC_NO, -1);
        DatabaseHelper db = new DatabaseHelper(this);

        TextView tvAccountInfo = findViewById(R.id.tvAccountInfo);
        TextView tvBalance = findViewById(R.id.tvBalance);
        Button btnBack = findViewById(R.id.btnBack);

        tvAccountInfo.setText(db.getName(accNo) + "\nAccount " + accNo);
        tvBalance.setText(Util.money(db.getBalance(accNo)));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
