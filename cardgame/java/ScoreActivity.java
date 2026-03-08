package jp.ac.kanto_pc.rintaro.kadai; // あなたのプロジェクト名に合わせてください

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.view.View;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.Toast;
import android.content.res.TypedArray;

import android.view.Gravity;

public class ScoreActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "GameScores";
    private static final String USER_LIST_KEY = "UserNames";

    // UI要素を定義
    private TextView scoreListTextView;
    //成績リストを動的に追加するコンテナ
    private LinearLayout scoreListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score); // ステップ1.2で作成するXMLをセット

        //scoreListTextView = findViewById(R.id.score_list_textview);
        scoreListContainer = findViewById(R.id.score_list_container);

        // 成績の読み込みと表示
        loadAndDisplayAllScores();
    }

    // --- 成績の読み込みと表示ロジック ---

    private void loadAndDisplayAllScores() {
        // ... (SharedPreferencesから userNames を取得する既存のロジックはそのまま) ...

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String userListString = settings.getString(USER_LIST_KEY, "");

        List<String> userNames = new ArrayList<>();
        if (!userListString.isEmpty()) {
            userNames.addAll(Arrays.asList(userListString.split(",")));
        }

        // 既存のリストをクリア (画面更新のため)
        scoreListContainer.removeAllViews();

        if (userNames.isEmpty()) {
            // スコアがない場合の表示
            TextView noScoreView = new TextView(this);
            noScoreView.setText("まだ成績が保存されていません。");
            noScoreView.setPadding(0, 16, 0, 16);
            scoreListContainer.addView(noScoreView);
            return;
        }

        // 1. 全ユーザーの成績データを収集（以前のロジックと同じ）
        List<List<Object>> allScores = new ArrayList<>();
        for (String userName : userNames) {
            int wins = settings.getInt(userName + "_WINS", 0);
            int loses = settings.getInt(userName + "_LOSES", 0);
            List<Object> entry = new ArrayList<>();
            entry.add(wins);
            entry.add(userName);
            entry.add(loses);
            allScores.add(entry);
        }

        // 2. 勝利数で降順、同数の場合は敗北数で昇順にソートする（以前のロジックと同じ）
        Collections.sort(allScores, new Comparator<List<Object>>() {
            @Override
            public int compare(List<Object> score1, List<Object> score2) {
                Integer wins1 = (Integer) score1.get(0);
                Integer wins2 = (Integer) score2.get(0);
                Integer loses1 = (Integer) score1.get(2);
                Integer loses2 = (Integer) score2.get(2);

                int winComparison = wins2.compareTo(wins1);

                if (winComparison != 0) {
                    return winComparison;
                } else {
                    return loses1.compareTo(loses2);
                }
            }
        });

        // 3. ランキングタイトルを追加
        TextView titleView = new TextView(this);
        titleView.setText("---ランキング---");
        titleView.setTextSize(18);
        titleView.setPadding(0, 0, 0, 16);
        titleView.setTextColor(getColor(android.R.color.black));
        // ★ 中央揃えにするための重要な修正 ★
        //1. レイアウトパラメータを設定
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, // 幅を親要素に合わせる
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleView.setLayoutParams(params);

        // 2. テキストを中央に寄せる
        titleView.setGravity(Gravity.CENTER_HORIZONTAL); // テキストコンテンツを水平方向の中央に揃える
        scoreListContainer.addView(titleView);

        // 4. 動的なリストの作成とリスナーの設定
        for (int i = 0; i < allScores.size(); i++) {
            List<Object> scoreEntry = allScores.get(i);
            int rank = i + 1;
            int wins = (Integer) scoreEntry.get(0);
            String userName = (String) scoreEntry.get(1);
            int loses = (Integer) scoreEntry.get(2);

            // 表示文字列の生成
            String rankPrefix;
            if (rank == 1) {
                rankPrefix = "🥇 1位: ";
            } else if (rank == 2) {
                rankPrefix = "🥈 2位: ";
            } else if (rank == 3) {
                rankPrefix = "🥉 3位: ";
            } else {
                rankPrefix = String.format("%2d位: ", rank);
            }

            final String scoreText = rankPrefix + userName + "さん (" + wins + "勝 - " + loses + "敗)";

            // タップ可能な TextView を作成
            TextView entryView = new TextView(this);
            entryView.setText(scoreText);
            entryView.setTextSize(16);
            entryView.setPadding(8, 10, 8, 10);
            // ★ 修正箇所: 属性からリソースIDを解決する ★
            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            TypedArray typedArray = obtainStyledAttributes(attrs);
            int backgroundResource = typedArray.getResourceId(0, 0);
            typedArray.recycle(); // 忘れずにリサイクル

            // 取得したリソースIDを設定
            entryView.setBackgroundResource(backgroundResource);

            entryView.setTextColor(getColor(android.R.color.black)); // getColor()は以前修正済み
            entryView.setLineSpacing(0, 1.2f);

            // 削除処理のためのタグ付け
            entryView.setTag(userName);

            // ★ リスナー設定: 長押しされたら削除ダイアログを表示 ★
            entryView.setOnLongClickListener(v -> {
                String userToDelete = (String) v.getTag();

                // 確認ダイアログを表示
                showDeleteConfirmationDialog(userToDelete);

                return true; // イベントを消費し、他のクリックイベントが動作しないようにする
            });

            scoreListContainer.addView(entryView);

            // 3位以下の区切り線 (タップ可能項目なので、線もタップされると問題があるため削除)
            // 代わりに、薄い仕切り線をプログラムで追加
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1)); // 高さ1dpの線
            divider.setBackgroundColor(getColor(android.R.color.darker_gray));
            scoreListContainer.addView(divider);
        }
    }

    // 削除確認ダイアログの表示
    private void showDeleteConfirmationDialog(String userName) {
        new AlertDialog.Builder(this)
                .setTitle("成績削除の確認")
                .setMessage(userName + "さんのベスト成績を削除しますか？\n（この操作は元に戻せません）")
                .setPositiveButton("削除する", (dialog, which) -> {
                    // 削除実行
                    deleteUserScore(userName);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
    // ユーザーの成績をSharedPreferencesから完全に削除
    // ScoreActivity.java

    // ユーザーの成績をSharedPreferencesから完全に削除
    private void deleteUserScore(String userName) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        // 1. 成績データを削除
        editor.remove(userName + "_WINS");
        editor.remove(userName + "_LOSES");

        // 2. ユーザーリストから名前を削除
        String userListString = settings.getString(USER_LIST_KEY, "");
        List<String> userNames = new ArrayList<>();
        if (!userListString.isEmpty()) {
            userNames.addAll(Arrays.asList(userListString.split(",")));
        }

        userNames.remove(userName); // ユーザー名をリストから削除

        // 3. 更新されたリストを保存 (★ 修正箇所：String.joinをStringBuilderで置き換え ★)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userNames.size(); i++) {
            sb.append(userNames.get(i));
            if (i < userNames.size() - 1) {
                sb.append(",");
            }
        }
        String updatedListString = sb.toString();
        editor.putString(USER_LIST_KEY, updatedListString);

        // 4. 適用して保存
        editor.apply();

        // 5. 画面を再描画して、削除されたユーザーを非表示にする
        loadAndDisplayAllScores();

        // 削除完了メッセージ
        Toast.makeText(this, userName + "さんの成績を削除しました。", Toast.LENGTH_SHORT).show();
    }

}