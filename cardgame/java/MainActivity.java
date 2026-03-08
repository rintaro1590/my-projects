package jp.ac.kanto_pc.rintaro.kadai; // (あなたのプロジェクト名に合わせる)

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    // --- 新しいCardクラスを内部に定義（推奨） ---
    /**
     * カードのマークと数字を保持するクラス
     */
    private static class Card {
        public final int value;      // 数字 (1-5)
        public final int markType;   // マーク (0: クラブ, 1: ダイヤ)
        public final int resourceId; // 画像リソースID

        public Card(int value, int markType, int resourceId) {
            this.value = value;
            this.markType = markType;
            this.resourceId = resourceId;
        }
    }

    // --- クラス変数 ---
    private List<Card> myDeck;
    private List<Card> cpuDeck;
    private TextView resultText;
    private ImageView playerCardPlayed, cpuCardPlayed;
    private Button startButton, endGameButton, replayGameButton;
    private Button menuShowCardHistoryButton;
    private List<Integer> myPlayedCardHistory = new ArrayList<>(); // 自分のプレイ履歴 (画像ID)
    private List<Integer> cpuPlayedCardHistory = new ArrayList<>(); // CPUのプレイ履歴 (画像ID)
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private int myWins = 0;
    private int cpuWins = 0;

    // マーク定数
    private static final int MARK_CLUB = 0;    // b
    private static final int MARK_DIAMOND = 1; // d
    private static final int MARK_SPADE = 2;    // s
    private static final int MARK_HEART = 3; // h

    // --- SoundPool関連の変数 ---
    private SoundPool soundPool;
    private int clickSoundId;

    private SoundPool soundPool2;
    private int clickCardId;
    private SoundPool victory;
    private int victoryId;
    private SoundPool lose;
    private int loseId;
    private SoundPool eve;
    private int eveId;

    // --- SharedPreferences を使った成績の保存・読み込みヘルパー ---
    private static final String PREFS_NAME = "GameScores";
    private static final String USER_LIST_KEY = "UserNames"; // ユーザー名リストを保存するキー
    // 現在のプレイヤー名を保持する変数 (もし無ければ追加)
    private String currentPlayerName = "";
    //履歴表示ボタン
    private Button menuShowHistoryButton;
    private Button playButton;

    //のぞき見機能のための変数
    private Button cheatButton;
    private LinearLayout cheatViewContainer;
    private LinearLayout cpuCheatCards;
    private LinearLayout playerCheatCards;
    // ★ 追記: のぞき見機能の使用状態を管理する変数 ★
    private boolean cheatUsed = false;

    // --- ライフサイクルメソッド ---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // XMLレイアウト (activity_main.xml) を読み込み
        setContentView(R.layout.activity_main); 

        // UI要素の関連付け
        resultText = findViewById(R.id.resultText);
        playerCardPlayed = findViewById(R.id.playerCardPlayed);
        cpuCardPlayed = findViewById(R.id.cpuCardPlayed);
        startButton = findViewById(R.id.startButton);
        endGameButton = findViewById(R.id.endGameButton);
        replayGameButton = findViewById(R.id.replayGameButton);
        playButton = findViewById(R.id.playButton);

        //のぞき見ボタンの関連付け
        cheatButton = findViewById(R.id.cheatButton);
        cheatViewContainer = findViewById(R.id.cheatViewContainer);
        cpuCheatCards = findViewById(R.id.cpuCheatCards);
        playerCheatCards = findViewById(R.id.playerCheatCards);

        // ナビゲーションドロワーのボタンを設定
        //ドロワー内の履歴表示ボタンの関連付け
        menuShowHistoryButton = findViewById(R.id.menu_show_history_button); // 順位を見る
        menuShowCardHistoryButton = findViewById(R.id.menu_show_card_history_button);

        cheatButton.setVisibility(View.GONE);
        // のぞき見ボタンのクリックリスナー
        cheatButton.setOnClickListener(v -> {
            playSound();
            toggleCheatView(); // のぞき見表示/非表示を切り替えるメソッドを呼び出す
        });


        //HistoryActivityへの遷移ロジック
        menuShowCardHistoryButton.setOnClickListener(v -> {
            playSound(); // クリック音
            drawerLayout.closeDrawer(GravityCompat.START); // ドロワーを閉じる

            // HistoryActivity を起動する Intent を作成
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);

            // 履歴データを新しい画面に渡す
            intent.putIntegerArrayListExtra(HistoryActivity.EXTRA_MY_HISTORY, (ArrayList<Integer>) myPlayedCardHistory);
            intent.putIntegerArrayListExtra(HistoryActivity.EXTRA_CPU_HISTORY, (ArrayList<Integer>) cpuPlayedCardHistory);
            startActivity(intent);
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open, 
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState(); // ハンバーガーアイコンを回転アニメーションで表示

        // ドロワー内のメニューアイテムの関連付け (前回のコードから移動)
        Button menureplayGameButton = findViewById(R.id.menu_replay_game);
        Button menuExitAppButton = findViewById(R.id.menu_exit_app);

        //ドロワー内の履歴表示ボタンの関連付け
        menuShowHistoryButton = findViewById(R.id.menu_show_history_button);

        // ★ 履歴表示ボタンのリスナー設定を新しいActivityの起動に変更 ★
        menuShowHistoryButton.setOnClickListener(v -> {
            playSound(); // クリック音

            // ScoreActivity を起動する Intent を作成
            Intent intent = new Intent(MainActivity.this, ScoreActivity.class);
            startActivity(intent); // アクティビティを開始
        });
        // (1) ゲームを起動すると、緑背景に裏面のトランプ2枚と開始マークを表示する [cite: 8]
        resultText.setText("開始マークをタッチしてください");
        endGameButton.setVisibility(View.GONE); // 終了ボタンは非表示にしておく
        replayGameButton.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE); // スタートボタンを一時的に「名前入力」のトリガーとして使う

        // ---SoundPoolの初期化 ---
        initializeSound();
        
        // ---開始ボタンのリスナー (音を鳴らす) ---
        startButton.setOnClickListener(v -> {
            playSound();
            // ゲーム開始ではなく、名前入力ダイアログを表示する
            showNameInputDialogAndStart();
        });

        menureplayGameButton.setOnClickListener(v -> {
            playSound();
            drawerLayout.closeDrawer(GravityCompat.START); // ドロワーを閉じる
            startGame(); // ゲーム開始処理を呼び出す
        });

        menuExitAppButton.setOnClickListener(v -> {
            playSound();
            finish(); // アプリケーションを終了
        });

    }
    // ユーザー名入力ダイアログを表示し、ゲーム開始の準備をする
    private void showNameInputDialogAndStart() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("プレイヤー名入力");
        builder.setMessage("ゲームを開始するプレイヤーの名前を入力してください。\n（成績を記録したい場合のみ）"); // メッセージも分かりやすく変更

        final EditText input = new EditText(this);
        builder.setView(input);

        // 【1. 記録保存モード】
        builder.setPositiveButton("ゲーム開始", (dialog, which) -> { // ボタン名も明確に変更
            String userName = input.getText().toString().trim();
            if (userName.isEmpty()) {
                userName = "名無し";
            }

            // 1. 現在のプレイヤー名を設定
            currentPlayerName = userName;

            // 2. 結果表示エリアにプレイヤー名を表示
            resultText.setText(currentPlayerName + "さん、カードをタップしてゲームを開始してください！");

            // 3. カードを配り始める
            startGame();
        });

        // 【2. 記録なしモード】
        builder.setNegativeButton("キャンセル", (dialog, which) -> { // ボタン名を変更
            // 記録を保存しないモードでゲームを開始する
            currentPlayerName = ""; //空文字を設定することで、endGame()でsaveScore()をスキップさせる
            dialog.cancel(); // ダイアログを閉じる

            // 1. 結果表示エリアにメッセージを表示
            resultText.setText("カードをタップしてゲームを開始してください！ (今回の成績は保存されません)");

            // 2. カードを配り始める
            startGame();
        });

        builder.show();
    }

    /**
     * ユーザー名と成績をSharedPreferencesに保存し、ユーザーリストを更新する。
     * 新しい勝利数が過去の最高勝利数を上回った場合のみ、スコアを上書きする。
     * @return 成績が更新された場合は true、それ以外は false
     */
    private boolean saveScore(String userName, int newWins, int newLoses) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        boolean scoreUpdated = false;

        // 1. 現在保存されている過去の最高勝利数を読み込む (存在しない場合は 0)
        int oldWins = settings.getInt(userName + "_WINS", 0);

        // 2. 新しい成績が既存のベストスコアよりも優れているかチェック
        if (newWins > oldWins) {
            // ★ ベストスコアを更新！新しい成績を保存
            editor.putInt(userName + "_WINS", newWins);
            editor.putInt(userName + "_LOSES", newLoses);
            scoreUpdated = true; // 記録が更新された
        }

        // 3. ユーザー名をリストに追加・更新 (ここは成績の良し悪しに関わらず実行)
        String userListString = settings.getString(USER_LIST_KEY, "");
        List<String> userNames = new ArrayList<>();
        if (!userListString.isEmpty()) {
            Collections.addAll(userNames, userListString.split(","));
        }

        boolean userListUpdated = false;
        if (!userNames.contains(userName)) {
            userNames.add(userName);
            String updatedListString = String.join(",", userNames);
            editor.putString(USER_LIST_KEY, updatedListString);
            userListUpdated = true;
        }

        // 4. 変更があった場合のみ適用
        if (scoreUpdated || userListUpdated) {
            editor.apply();
        }

        //記録更新の有無を返す
        return scoreUpdated;
    }


    public void onBackPressed() {
        // ドロワーが開いている場合は閉じる
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // ドロワーが閉じていれば通常通り戻る
            super.onBackPressed();
        }
    }

    // --- SoundPoolの初期化 ---
    private void initializeSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);
            soundPool = builder.build();

            SoundPool.Builder builder2 = new SoundPool.Builder();
            builder2.setMaxStreams(1);
            soundPool2 = builder2.build();

            SoundPool.Builder builder3 = new SoundPool.Builder();
            builder3.setMaxStreams(1);
            victory = builder3.build();

            SoundPool.Builder builder4 = new SoundPool.Builder();
            builder4.setMaxStreams(1);
            lose = builder4.build();

            SoundPool.Builder builder5 = new SoundPool.Builder();
            builder5.setMaxStreams(1);
            eve = builder5.build();
        } else {
            // API 21未満の場合
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            soundPool2 = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            victory = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            lose = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            eve = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }

        // サウンドファイルをロード（ファイル名は res/raw/click_sound に合わせる）
        clickSoundId = soundPool.load(this, R.raw.click_sound, 1);
        clickCardId = soundPool2.load(this, R.raw.click_cards, 1);
        victoryId = victory.load(this, R.raw.victory, 1);
        loseId = lose.load(this, R.raw.lose, 1);
        eveId = eve.load(this, R.raw.eve, 1);
    }

    // --- 効果音の再生メソッド ---
    private void playSound() {
        if (soundPool != null && clickSoundId != 0) {
            // サウンドID, 左音量, 右音量, 優先度, ループ回数, 再生速度
            soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f); 
        }

    }

    private void Sound_card(){
        if (soundPool2 != null && clickCardId != 0) {
            // サウンドID, 左音量, 右音量, 優先度, ループ回数, 再生速度
            soundPool2.play(clickCardId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }
    private void Sound_victory(){
        if (victory != null && victoryId != 0) {
            // サウンドID, 左音量, 右音量, 優先度, ループ回数, 再生速度
            victory.play(victoryId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }
    private void Sound_lose(){
        if (lose != null && loseId != 0) {
            // サウンドID, 左音量, 右音量, 優先度, ループ回数, 再生速度
            lose.play(loseId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    private void Sound_eve(){
        if (eve != null && eveId != 0) {
            // サウンドID, 左音量, 右音量, 優先度, ループ回数, 再生速度
            eve.play(eveId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    // --- ゲーム開始処理 ---
    private void startGame() {
        startButton.setVisibility(View.GONE); // 開始マークを非表示
        endGameButton.setVisibility(View.GONE);
        replayGameButton.setVisibility(View.GONE);

        myPlayedCardHistory.clear();
        cpuPlayedCardHistory.clear();

        //のぞき見機能のリセット
        cheatUsed = false; // 使用フラグをリセット
        cheatButton.setVisibility(View.VISIBLE); // ボタンを再表示（有効化）

        initializeDecks();

        // カードを裏面に戻し、アニメーションの初期位置を設定

        // 1. カードを裏面にする
        playerCardPlayed.setImageResource(R.drawable.ura);
        cpuCardPlayed.setImageResource(R.drawable.ura);

        // 2. アニメーションのために初期位置を設定
        // CPUカードを上方向へ大きく移動 (最終位置 Y=0 から-1000f)
        cpuCardPlayed.setTranslationY(-1000f);
        // プレイヤーカードを下方向へ大きく移動 (最終位置 Y=0 から+1000f)
        playerCardPlayed.setTranslationY(1000f);

        // 3. アニメーションを開始
        animateDeal();

    }

    // --- カードの配りアニメーション ---
    private void animateDeal() {
        long duration = 500; // アニメーション時間 (0.5秒)

        // CPUカード (上) を最終位置へアニメーション
        cpuCardPlayed.animate()
                .translationY(0f) // 最終位置（制約で決められた場所）へ移動
                .setDuration(duration)
                .start();

        // プレイヤーカード (下) を最終位置へアニメーション
        playerCardPlayed.animate()
                .translationY(0f) // 最終位置へ移動
                .setDuration(duration)
                .withEndAction(() -> {
                    // ★ アニメーションが完了した後に実行する処理 ★

                    // カードタップでラウンド開始のリスナーを設定
                    playerCardPlayed.setOnClickListener(v -> {
                        Sound_card();
                        playRound();
                    });

                    // resultTextを更新
                    resultText.setText("山札の残り: " + myDeck.size() + "枚");
                })
                .start();
    }

    // --- 山札初期化とシャッフル ---
    private void initializeDecks() {
        // カードをリストに格納し、シャッフル
        List<Card> allCards = new ArrayList<>();

        // 1. クラブ (b) のカードを作成 (数字1から5)
        for (int value = 1; value <= 5; value++) {
            int resourceId = getClubResource(value);
            if (resourceId != 0) {
                allCards.add(new Card(value, MARK_CLUB, resourceId));
            }
        }

        // 2. ダイヤ (d) のカードを作成 (数字1から4のみ提供されているため4まで)
        for (int value = 1; value <= 5; value++) {
            int resourceId = getDiamondResource(value);
            if (resourceId != 0) {
                allCards.add(new Card(value, MARK_DIAMOND, resourceId));
            }
        }

        // 3. スペード (s) のカードを作成 (数字1から5)
        for (int value = 1; value <= 5; value++) {
            int resourceId = getSPADEResource(value);
            if (resourceId != 0) {
                allCards.add(new Card(value, MARK_SPADE, resourceId));
            }
        }

        // 4. ハート (h) のカードを作成 (数字1から5)
        for (int value = 1; value <= 5; value++) {
            int resourceId = getHEARTResource(value);
            if (resourceId != 0) {
                allCards.add(new Card(value, MARK_HEART, resourceId));
            }
        }
        
        Collections.shuffle(allCards);

        // 山札を分割 (5枚ずつ)
        int half = allCards.size() / 2;
        myDeck = new ArrayList<>(allCards.subList(0, half));
        cpuDeck = new ArrayList<>(allCards.subList(half, allCards.size()));
        
        myWins = 0;
        cpuWins = 0;
        resultText.setText("山札の残り: " + myDeck.size() + "枚");

        playerCardPlayed.setVisibility(View.VISIBLE);
        cpuCardPlayed.setVisibility(View.VISIBLE);
        // カードを裏面に戻す
        playerCardPlayed.setImageResource(R.drawable.ura);
        cpuCardPlayed.setImageResource(R.drawable.ura);
    }

    // --- 1ゲームの実行 (タッチ時の処理) ---
    private void playRound() {
        //前回の覗き見ビューが開いていたら閉じる処理 ★
        if (cheatViewContainer.getVisibility() == View.VISIBLE) {
            closeCheatView();
        }
        // 山札が空になったらゲーム終了
        if (myDeck.isEmpty() || cpuDeck.isEmpty()) {
            endGame();
            return;
        }

        // 山札からカードを引く
        Card myCard = myDeck.remove(0);
        Card cpuCard = cpuDeck.remove(0);

        // 履歴リストに今回出したカードを追加
        myPlayedCardHistory.add(myCard.resourceId);
        cpuPlayedCardHistory.add(cpuCard.resourceId);

        // 確定したマークの画像を表示
        playerCardPlayed.setImageResource(myCard.resourceId);
        cpuCardPlayed.setImageResource(cpuCard.resourceId); // CPUはまだ裏面のまま

        resultText.setText("カードをクリックして勝敗を確定"); // メッセージ変更
        playButton.setVisibility(View.GONE); // 次のラウンドボタンを非表示

        // 勝敗判定は、Cardオブジェクトの value を使う
        String result;
        if (myCard.value > cpuCard.value) {
            result = "勝ち";
            myWins++;
        } else if (myCard.value < cpuCard.value) {
            result = "負け";
            cpuWins++;
        } else {
            result = "あいこ";
        }
        //勝敗が確定したら覗き見ビューを閉じる
        closeCheatView();

        resultText.setText(result + "！\n山札の残り: " + myDeck.size() + "枚");

        updateCheatButtonState();
    }
    
   // --- クラブの画像リソース取得ヘルパー ---
    private int getClubResource(int value) {
        switch (value) {
            case 1: return R.drawable.b1;
            case 2: return R.drawable.b2;
            case 3: return R.drawable.b3;
            case 4: return R.drawable.b4;
            case 5: return R.drawable.b5;
            default: return 0;
        }
    }
    
    // --- ダイヤの画像リソース取得ヘルパー ---
    private int getDiamondResource(int value) {
        switch (value) {
            case 1: return R.drawable.d1;
            case 2: return R.drawable.d2;
            case 3: return R.drawable.d3;
            case 4: return R.drawable.d4;
            case 5: return R.drawable.d5;
            default: return 0; // d5がないため、数字5のダイヤは含めない
        }
    }

    // --- スペードの画像リソース取得ヘルパー ---
    private int getSPADEResource(int value) {
        switch (value) {
            case 1: return R.drawable.s1;
            case 2: return R.drawable.s2;
            case 3: return R.drawable.s3;
            case 4: return R.drawable.s4;
            case 5: return R.drawable.s5;
            default: return 0;
        }
    }
    
    // --- ハートの画像リソース取得ヘルパー ---
    private int getHEARTResource(int value) {
        switch (value) {
            case 1: return R.drawable.h1;
            case 2: return R.drawable.h2;
            case 3: return R.drawable.h3;
            case 4: return R.drawable.h4;
            case 5: return R.drawable.h5;
            default: return 0;
        }
    }


    // --- ゲーム終了処理 ---
    private void endGame() {
        // (4) 裏面のカードはなくなり [cite: 12]
        playerCardPlayed.setOnClickListener(null); // タッチイベントを無効化
        playerCardPlayed.setVisibility(View.GONE);
        cpuCardPlayed.setVisibility(View.GONE);
        playerCardPlayed.clearAnimation();

        // 全体の勝敗結果の判定
        String overallResult;
        if (myWins > cpuWins) {
            overallResult = "あなたの勝利！ (" + myWins + "勝-" + cpuWins + "敗)";
            Sound_victory();
        } else if (myWins < cpuWins) {
            overallResult = "CPUの勝利... (" + myWins + "勝-" + cpuWins + "敗)";
            Sound_lose();
        } else {
            overallResult = "引き分け！ (" + myWins + "勝-" + cpuWins + "敗)";
            Sound_eve();
        }

        // (4) 全体の勝敗結果とゲーム終了のボタンを表示する [cite: 12]
        resultText.setText(overallResult);

        endGameButton.setVisibility(View.VISIBLE);
        // ---終了ボタンのリスナー (音を鳴らす) ---
        endGameButton.setOnClickListener(v -> {
            playSound(); //終了ボタンが押されたら音を鳴らす
            finish(); // アプリケーションを終了
        });

        replayGameButton.setVisibility(View.VISIBLE);
        // ---再チャレンジボタンのリスナー (音を鳴らす) ---
        replayGameButton.setOnClickListener(v->{
            playSound(); //再チャレンジのボタンが押されたら音を鳴らす
            startGame(); //ゲームの再スタート
        });

        boolean recordUpdated = false; // 記録更新フラグ

        if (!currentPlayerName.isEmpty()) {
            // ★ saveScoreを呼び出し、記録更新の有無を受け取る
            recordUpdated = saveScore(currentPlayerName, myWins, cpuWins);
        }

        // ★ 記録更新メッセージの追加 ★
        if (recordUpdated) {
            // 既存の結果テキストに記録更新メッセージを追記
            String currentText = resultText.getText().toString();
            resultText.setText(currentText + "\n\n🏆 ベスト記録更新！ 🏆");
        }
    }

    // --- ライフサイクル終了時のSoundPool解放 ---
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release(); 
            soundPool = null;
        }
    }

    private void toggleCheatView() {
        if (cheatViewContainer.getVisibility() == View.VISIBLE) {
            // ★ 修正: 表示されている場合は何もしないか、あるいは次のラウンドで閉じるようにする ★
            // ここでの閉じる処理は、カードクリック時または次のラウンドで行うため、削除または簡略化します
            // 現在は表示されているので、そのままリターン
            return;

        } else {
            // 現在非表示の場合は表示する
            if (myDeck.size() < 4 || cpuDeck.size() < 4) {
                return;
            }

            // カードをロードして表示
            loadCheatCards(cpuDeck, cpuCheatCards);
            loadCheatCards(myDeck, playerCheatCards);
            cheatViewContainer.setVisibility(View.VISIBLE);

            //一度使ったら即座にボタンを非表示にする処理をここで実行
            cheatUsed = true;
            cheatButton.setVisibility(View.GONE); // ボタンを非表示にする
        }
    }
    /**
     * 山札から3枚をシャッフルして抽出し、指定されたLinearLayoutに表示する
     * @param deck 対象の山札 (myDeck または cpuDeck)
     * @param container カードを表示するLinearLayout (cpuCheatCards または playerCheatCards)
     */
    private void loadCheatCards(List<Card> deck, LinearLayout container) {
        // 既存のビューをクリア
        container.removeAllViews();

        // 1. 山札から最大3枚を抽出
        List<Card> tempCards = new ArrayList<>();
        int count = Math.min(3, deck.size()); // 山札が3枚未満の場合は、その枚数だけ取る

        for (int i = 0; i < count; i++) {
            // 山札の0番目からi番目までのカードをコピーしてリストに入れる
            // 山札を直接変更しないように注意
            tempCards.add(deck.get(i));
        }

        // 2. 抽出したカードをシャッフル (毎回ランダムな順序で表示)
        Collections.shuffle(tempCards);

        // 3. ImageViewを作成し、コンテナに追加
        for (Card card : tempCards) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(card.resourceId);

            // レイアウトパラメータの設定 (小さめに表示)
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.cheat_card_width), // R.dimen.cheat_card_width を後で定義します
                    (int) getResources().getDimension(R.dimen.cheat_card_height) // R.dimen.cheat_card_height を後で定義します
            );
            params.setMargins(8, 0, 8, 0); // 左右に少しマージンを設定
            imageView.setLayoutParams(params);

            container.addView(imageView);
        }
    }
    /**
     * 山札の枚数と使用状況に基づいて、のぞき見ボタンの表示状態を更新する
     */
    private void updateCheatButtonState() {
        // 1. 既に1回使用済みの場合
        if (cheatUsed) {
            cheatButton.setVisibility(View.GONE);
            return;
        }

        // 2. 山札の枚数が4枚未満になった場合
        if (myDeck.size() < 4 || cpuDeck.size() < 4) {
            cheatButton.setVisibility(View.GONE);
            // 表示されている場合は閉じる
            if (cheatViewContainer.getVisibility() == View.VISIBLE) {
                toggleCheatView();
            }
            return;
        }

        // 3. 上記に該当しない場合 (未だ使っておらず、4枚以上ある)
        cheatButton.setVisibility(View.VISIBLE);
    }
    /*
     * のぞき見ビューを閉じる（カードクリックと次のラウンドボタンで共通利用）
     */
    private void closeCheatView() {
        if (cheatViewContainer.getVisibility() == View.VISIBLE) {
            cheatViewContainer.setVisibility(View.GONE);
            cpuCheatCards.removeAllViews();
            playerCheatCards.removeAllViews();
            // ボタンのテキスト更新も不要
        }
    }
}