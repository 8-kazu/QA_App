package jp.techacademy.hirokazu.hatta.qa_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import jp.techacademy.hirokazu.hatta.qa_app.ui.theme.QA_AppTheme


import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold

import jp.techacademy.hirokazu.hatta.qa_app.navigation.QAAppNavHost
import jp.techacademy.hirokazu.hatta.qa_app.ui.theme.QA_AppTheme


class MainActivity : ComponentActivity() {
    //MainActivity はアプリの エントリーポイント（最初に起動される画面）
    //ComponentActivity を継承している → Compose が使える Activity
    override fun onCreate(savedInstanceState: Bundle?) {//onCreate() は Activity が生成されたときに呼ばれるライフサイクルメソッド
        super.onCreate(savedInstanceState)//super.onCreate() は親クラスの処理を呼ぶ（必須）
        enableEdgeToEdge()//端末の画面端まで UI を表示できるようにする設定 /ステータスバーやナビゲーションバーの下まで描画できる
        setContent {//Compose で 画面の中身を定義するための関数
            QA_AppTheme {//QA_AppTheme はアプリ独自のテーマ（色・フォントなど）
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QAAppNavHost(//QAAppNavHost はアプリの 画面遷移（Navigation）を管理するコンポーザブル
                        modifier = Modifier.padding(innerPadding)//Scaffold が作るバーやナビゲーションの分だけ余白を確保
                    )
                }
            }
        }
    }
}