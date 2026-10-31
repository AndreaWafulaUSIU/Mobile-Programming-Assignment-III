package com.example.atm;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** Mini statement: the last 5 transactions, newest first. */
public class StatementActivity extends AppCompatActivity {

    private static final int HOW_MANY = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statement);
        setTitle("Mini Statement");

        long accNo = getIntent().getLongExtra(Util.EXTRA_ACC_NO, -1);
        DatabaseHelper db = new DatabaseHelper(this);

        TextView tvHeader = findViewById(R.id.tvHeader);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        ListView lvStatement = findViewById(R.id.lvStatement);
        Button btnBack = findViewById(R.id.btnBack);

        tvHeader.setText("Account " + accNo + " - last " + HOW_MANY + " transactions");

        List<String> lines = db.getMiniStatement(accNo, HOW_MANY);
        if (lines.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        }
        lvStatement.setAdapter(new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, lines));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
