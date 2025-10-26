package jp.techacademy.hirokazu.hatta.qa_app.ui.FavoriteQuestion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.techacademy.hirokazu.hatta.qa_app.Const


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(//MainScreen は アプリのメイン画面を作る関数。
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoriteQuestionViewModel = viewModel()//viewModel は画面の情報を管理する箱（データや状態を持つ）。
) {
    val context = LocalContext.current//context は現在の画面情報（Androidで必要）。
    val uiState by viewModel.uiState.collectAsState()//uiState は画面に表示する状態情報を取り出す。

    // ユーザー情報を読み込み
    LaunchedEffect(Unit) {//画面が表示されたら初回に実行される処理。
        viewModel.loadUserInfo(context)
        viewModel.selectGenre(Const.GENRE_HOBBY) // デフォルトで趣味ジャンルを選択
    }

    // エラーメッセージ表示用のSnackbarHost(画面下に出す)
    val snackbarHostState = remember { SnackbarHostState() }

    // エラーメッセージ表示
    LaunchedEffect(uiState.errorMessage) {//エラーメッセージが変わったら実行。
        uiState.errorMessage?.let { message ->//メッセージがあればスナックバーに表示して、状態をクリアする。
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("お気に入り画面") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        FavoriteQuestionListContent(
            isLoading = uiState.isLoading,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}




@Composable
 fun FavoriteQuestionListContent(//（質問リストの表示）
   isLoading: Boolean,
   viewModel: FavoriteQuestionViewModel,
    modifier: Modifier = Modifier
) {
    // ここで favoriteQuestions を取得
    val favoriteQuestions by viewModel.favoriteQuestions.collectAsState()//collectAsStateは、Flow を Compose の State に変換 してくれる関数・これにより、Flow の値が変わるたびに Compose の画面が自動で再描画される

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading && favoriteQuestions.isEmpty() -> {//質問がまだ読み込まれていない場合、読み込み中表示をする
                // 初回読み込み時のローディング
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "質問を読み込んでいます...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            favoriteQuestions.isEmpty() -> {
                // 質問が存在しない場合、案内メッセージを表示。
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ここは、お気に入り質問の一覧画面です。",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "今は、お気に入りの質問がありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
///ここが、質問一覧に大事
            else -> {
                //質問がある場合、縦スクロール可能なリストで表示。
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(//質問一覧の一個一個の質問
                            favoriteQuestions,//これは「表示したいデータのリスト」です。
                            key = { it.questionUid }//it は Question オブジェクト,it.questionUid は質問の固有IDです。（items() の中の it は、「現在描画しているリストの要素（1つのアイテム）」 を指します。）
                            //「各質問の一意のキーとして questionUid を使う」 という意味です。
                        ) { favoriteQuestion ->//favoriteQuestions リストの各質問を 1つずつ取り出して表示。
                            FavoriteQuestionItemCard(//質問1件ごとに カード形式で表示する。
                                favoriteQuestion = favoriteQuestion,//引数に、取り出したfavoriteQuestionを渡す
                            )
                        }

                        // リアルタイム更新時のローディング表示
                        if (isLoading) {//リストを更新中（リアルタイム更新中）のときの処理。
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(//ローディング中の くるくる回るマーク（インジケーター） を表示。
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

