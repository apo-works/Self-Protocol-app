package jp.bodyprotocol.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            textSize = 18f
            setPadding(48, 80, 48, 48)
            text = "Body Protocol は、あなたが許可した場合に限り Health Connect から体重・体脂肪率・歩数を読み取ります。これらは減量進捗と運動量の表示だけに使い、アプリ外へ送信しません。権限はいつでも Health Connect の設定から解除できます。"
        })
    }
}
