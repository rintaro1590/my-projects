package jp.ac.kanto_pc.ishikawa.test2;
import android.content.Intent;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
public class Success extends AppCompatActivity {
    private TextView textID;
    private Button btnBack;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        textID = findViewById(R.id.textID);
        btnBack = findViewById(R.id.btnBack);
        int status = getIntent().getIntExtra("status",0);

        switch(status){
            case 1:  textID.setText("出席しました");  break;
            case 2:  textID.setText("遅刻しました");  break;
            case 3:  textID.setText("早退しました");  break;
            case 4:  textID.setText("退席しました");  break;
        }
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(Success.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}
