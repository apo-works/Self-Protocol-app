# Body Protocol — Android MVP

減量・筋力維持を一つにまとめた個人用AndroidアプリのMVPです。

## 入っている機能

- Samsung Health → Health Connect 経由で体重・体脂肪率・歩数を読み取り
- 体重の7日平均 / 68kg目標表示
- ジムに行った日を1タップ記録、直近21日表示
- Anytime Fitness向け週3回・40分の全身筋トレ基本メニュー
- 3日ごと18:30に献立チェックイン通知
- 「何が食べたい？」「残っている食材」「外食予定」「ジム予定」を回答した後に3日分を生成
- 平日昼は前夜の取り分け中心、米代を除き1食300円を目安
- 1日2,000kcal / P140g を初期目標
- 3日分の買い物リスト・作り置きポイント
- VITAS / Gold Standard Whey / クレアチンのデイリーチェック
- データは端末内 SharedPreferences に保存（Health Connectの健康データ自体はコピー保存しません）

## Health Connect

AndroidX Health Connect `1.1.0` を使用。読み取り対象は Weight / Body Fat / Steps のみです。
Samsung Health側で Health Connect 連携と対象データの共有をONにしてください。

## ビルド

1. Android Studioでこのフォルダを開く
2. Gradle Sync
3. Galaxy端末をUSBデバッグ接続
4. Run
5. 初回起動で通知・Health Connectの権限を許可

想定: Android 9+ / targetSdk 35。Samsung Health連携を主目的にする場合はAndroid 14/15のGalaxy端末を推奨。

## 3日献立の仕様

初回基準日は 2026-08-23 18:30、以降3日ごとです。端末がExact Alarmを許可していない場合はAndroid都合で通知時刻が多少ずれることがあります。

献立は現時点では完全ローカルのルールベースです。OpenAI APIキー不要で動き、残り物/食べたい系統をキーワードで優先します。将来AI生成に差し替えられる構成です。

## 重要

このMVPはソース一式です。この実行環境にはAndroid SDK/Gradleがないため、ここではAPKの実ビルドまでは検証していません。Android StudioでのGradle Sync時に依存関係の更新を求められた場合はIDEの推奨に従ってください。

## GitHub ActionsでAPKを作る

`.github/workflows/android-debug-apk.yml` を同梱しています。GitHubへpushすると `Build Android Debug APK` が走り、ActionsのArtifactsから `body-protocol-debug-apk` を取得できます。
