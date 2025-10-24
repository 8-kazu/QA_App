package jp.techacademy.hirokazu.hatta.qa_app.ui.main


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import jp.techacademy.hirokazu.hatta.qa_app.Const

//import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(//MainScreen は アプリのメイン画面を作る関数。
    onLogout: () -> Unit,//onLogout はログアウトしたときの処理を渡すための箱。
    onNavigateToSettings: () -> Unit = {},//onNavigateToSettings は設定画面に移動する処理（省略可）。
    onNavigateToQuestionSend: (Int) -> Unit = {},//は質問作成画面に移動する処理（ジャンルIDを渡す）。
    onNavigateToQuestionDetail: (String, Int) -> Unit = { _, _ -> },//質問詳細画面に移動する処理（質問IDとジャンルIDを渡す）。
    onNavigateToFavoriteQuestion: () -> Unit = {  },//お気に入り質問一覧画面に行く
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()//viewModel は画面の情報を管理する箱（データや状態を持つ）。
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

    // ドロワーの状態を管理
    val drawerState = rememberDrawerState(//左から出てくる ジャンル選択メニュー（ドロワー） の状態を作る。
        initialValue = if (uiState.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed//開いているか閉じているかを初期化。
    )

    // ViewModelの状態変化に応じてドロワーの状態を同期
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }


    //左のスライドメニュー を作る。
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                selectedGenre = uiState.selectedGenre,

                //ジャンルを選んだら選択状態を更新してメニューを閉じる。///////////////////////////////////////////////////////////////////////////////
                onGenreSelected = { genreId ->
                    viewModel.selectGenre(genreId)
                    viewModel.closeDrawer()
                }
            )
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {//画面上部の タイトルバー を作る
                        Text(
                            if (uiState.selectedGenre == 0) "Q&Aアプリ"
                            else viewModel.getGenreName(uiState.selectedGenre)//選択中のジャンル名をタイトルに表示。
                        )
                    },
                    navigationIcon = {//左上に ハンバーガーメニュー を配置。
                        //押すとドロワーを開閉。
                        IconButton(onClick = { viewModel.toggleDrawer() }) {
                            Icon(Icons.Default.Menu, contentDescription = "メニュー")
                        }
                    },
                    actions = {//右側のボタン。


                        //お気に入り画面へ遷移
                        IconButton(onClick = {onNavigateToFavoriteQuestion()}) {
                            Icon(Icons.Default.Favorite, contentDescription = "お気に入り")
                        }


                        //「更新ボタン」で質問リストを更新。
                        IconButton(onClick = { viewModel.refreshQuestions() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "更新")
                        }

                        //「設定ボタン」で設定画面へ移動。
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "設定")
                        }
                    }
                )
            },
            floatingActionButton = {//右下の ＋ボタン（質問作成）。
                FloatingActionButton(
                    onClick = {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {//ログイン済みなら質問作成画面へ。
                            onNavigateToQuestionSend(uiState.selectedGenre)
                        } else {//未ログインならログアウト処理を呼ぶ。
                            onLogout()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "質問作成")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }//エラーメッセージを表示するスナックバーを設定。
        ) { innerPadding ->//innerPadding はタイトルバーやFABでの余白を確保するためのもの。
            QuestionListContent(//メインの 質問一覧を表示する部分。
                questions = uiState.questions,
                isLoading = uiState.isLoading,
                //質問を押したら詳細画面へ移動。
                onQuestionClick = { question ->
                    onNavigateToQuestionDetail(question.questionUid, uiState.selectedGenre)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun DrawerContent(//左メニューの中身）
    selectedGenre: Int,
    onGenreSelected: (Int) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ジャンル選択",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))


            //選択できるジャンルのリストを作る。
            val genres = listOf(
                Const.GENRE_HOBBY to "趣味",
                Const.GENRE_LIFE to "生活",
                Const.GENRE_HEALTH to "健康",
                Const.GENRE_COMPUTER to "コンピューター"
            )


            //各ジャンルを クリック可能なメニューアイテム として表示。
            genres.forEach { (genreId, genreName) ->
                NavigationDrawerItem(
                    label = { Text(genreName) },
                    selected = selectedGenre == genreId,
                    onClick = { onGenreSelected(genreId) },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuestionListContent(//（質問リストの表示）
    questions: List<jp.techacademy.hirokazu.hatta.qa_app.data.Question>,
    isLoading: Boolean,
    //viewModel: MainViewModel,
    onQuestionClick: (jp.techacademy.hirokazu.hatta.qa_app.data.Question) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = uiState.currentUser


    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading && questions.isEmpty() -> {//質問がまだ読み込まれていない場合、読み込み中表示をする
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
            questions.isEmpty() -> {
                // 質問が存在しない場合、案内メッセージを表示。
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "まだ質問がありません",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "このジャンルで最初の質問を投稿してみましょう！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            //ここが質問一覧にとって大事なところ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
            else -> {//質問がある場合、縦スクロール可能なリストで表示。
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {



                    items(questions, key = { it.questionUid }) { question ->//questions リストの各質問を 1つずつ取り出して表示。
                        QuestionItemCard(//質問1件ごとに カード形式で表示する。
                            question = question,
                            user = currentUser!! ,//お気に入り機能に使う
                            viewModel = viewModel,
                            onClick = { onQuestionClick(question) }//カードをクリックしたら onQuestionClick（質問詳細画面に飛ぶ処理）を呼ぶ。
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