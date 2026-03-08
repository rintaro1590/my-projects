package jp.ac.kanto_pc.rintaro.kadai;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_MY_HISTORY = "jp.ac.kanto_pc.rintaro.kadai.MY_HISTORY";
    public static final String EXTRA_CPU_HISTORY = "jp.ac.kanto_pc.rintaro.kadai.CPU_HISTORY";

    private GridLayout myGrid, cpuGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history); // ステップ2で作成するXML

        // 戻るボタン (Upナビゲーション) を有効にする
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("対戦履歴");
        }

        myGrid = findViewById(R.id.my_history_grid);
        cpuGrid = findViewById(R.id.cpu_history_grid);

        // MainActivityからデータを受け取る
        ArrayList<Integer> myHistory = getIntent().getIntegerArrayListExtra(EXTRA_MY_HISTORY);
        ArrayList<Integer> cpuHistory = getIntent().getIntegerArrayListExtra(EXTRA_CPU_HISTORY);

        if (myHistory != null && !myHistory.isEmpty()) {
            displayHistory(myHistory, myGrid);
        } else {
            ((TextView) findViewById(R.id.my_history_label)).setText("あなた: まだカードを出していません。");
        }

        if (cpuHistory != null && !cpuHistory.isEmpty()) {
            displayHistory(cpuHistory, cpuGrid);
        } else {
            ((TextView) findViewById(R.id.cpu_history_label)).setText("CPU: まだカードを出していません。");
        }
    }

    /**
     * リスト内のカードをグリッドに表示する
     */
    private void displayHistory(ArrayList<Integer> history, GridLayout gridLayout) {
        // カードの幅と高さをDPで定義 (例: 60dp x 90dp)
        int cardWidthPx = dpToPx(55);
        int cardHeightPx = dpToPx(85);

        // 履歴をラウンド順（左から右）に表示
        for (int i = 0; i < history.size(); i++) {
            int resourceId = history.get(i);

            // カードの画像ビューを作成
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(resourceId);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // レイアウトパラメータを設定
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cardWidthPx;
            params.height = cardHeightPx;
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4)); // カード間のマージン

            imageView.setLayoutParams(params);

            gridLayout.addView(imageView);
        }
    }

    /**
     * DPをピクセルに変換するユーティリティ
     */
    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }

    // 戻るボタンが押された時の処理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // このActivityを終了し、前の画面に戻る
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}