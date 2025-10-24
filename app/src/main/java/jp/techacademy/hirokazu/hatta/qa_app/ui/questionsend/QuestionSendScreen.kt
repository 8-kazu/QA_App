package jp.techacademy.hirokazu.hatta.qa_app.ui.questionsend

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*//画面のレイアウト（縦並び・横並びなど）を作るための基本パーツを読み込んでいる。
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//画像の形を切り抜いたり（丸くするなど）、どのように表示するかを決めるため。
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
//キーボードの動作（改行する・次の入力欄に進むなど）を設定するため。
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
//画像をネットやスマホ内から読み込んで表示するためのライブラリ（Coil）を使う。
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
//定数（ジャンルの番号など）をまとめたファイルを呼び出す。
import jp.techacademy.hirokazu.hatta.qa_app.Const

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSendScreen(
    genreId: Int,
    onNavigateBack: () -> Unit,//onNavigateBack：戻るボタンを押したときに呼ばれる関数
    modifier: Modifier = Modifier,
    viewModel: QuestionSendViewModel = viewModel()
) {
    val context = LocalContext.current//今の画面情報を取る。
    val uiState by viewModel.uiState.collectAsState()//画面の状態（入力内容・エラーなど）を覚えておく。
    val snackbarHostState = remember { SnackbarHostState() }//「投稿しました」などのメッセージを出す準備。

    // 初期化(画面を開いたときに、ジャンルIDを使って初期設定をする。)
    LaunchedEffect(genreId) {
        viewModel.initialize(genreId)
    }

    // 投稿成功時の処理
    LaunchedEffect(uiState.isQuestionPosted) {
        if (uiState.isQuestionPosted) {//「投稿完了」になったら
            snackbarHostState.showSnackbar("質問を投稿しました")//メッセージを表示して、
            onNavigateBack()//前の画面に戻る。
        }
    }

    // 画像選択用のランチャー
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateSelectedImageUri(it)//選ばれた画像の場所（URI）をViewModelに渡す。
        }
    }

    // カメラ撮影用のランチャー（簡略化版）
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // カメラからの画像取得は今回は簡略化
        // 実際のアプリでは、一時ファイルの作成と処理が必要
    }

    // 画像選択ダイアログの表示状態(「画像選択方法（ギャラリー or カメラ）」のダイアログを出すかどうかを覚えておく変数。)
    var showImageSelectionDialog by remember { mutableStateOf(false) }

    Scaffold(//画面の上にタイトル「質問作成」と戻るボタンを表示。
        topBar = {
            TopAppBar(
                title = { Text("質問作成") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = {//下にメッセージを出す場所を設定。
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),//スクロールできるようにしている。
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // エラーがあったら赤いカードでエラーメッセージ表示
            uiState.errorMessage?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 質問タイトル入力
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("質問タイトル") },
                placeholder = { Text("質問のタイトルを入力してください") },
                supportingText = { Text("${uiState.title.length}/100") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = uiState.title.length > 100//100文字を超えるとエラーになる。
            )

            // 質問本文入力
            OutlinedTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                label = { Text("質問内容") },
                placeholder = { Text("質問の詳細を入力してください") },
                supportingText = { Text("${uiState.body.length}/1000") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = uiState.body.length > 1000//1000文字を超えるとエラー表示
            )

            // ジャンル選択（読み取り専用で表示）
            OutlinedTextField(
                value = getGenreName(uiState.selectedGenre),
                onValueChange = { },
                label = { Text("ジャンル") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            // 画像選択エリア
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = { showImageSelectionDialog = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.selectedImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uiState.selectedImageUri)
                                .build(),
                            contentDescription = "選択された画像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "画像を追加",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "画像を選択",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 投稿ボタン
            Button(
                onClick = { viewModel.submitQuestion(context) },
                enabled = !uiState.isLoading,//処理中は押せないようにしている。
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("投稿する")
                }
            }
        }
    }

    // 画像選択ダイアログ(「ギャラリーから選ぶ or カメラで撮る」を選択する小さなポップアップを表示。)
    if (showImageSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showImageSelectionDialog = false },
            title = { Text("画像を選択") },
            text = { Text("画像の選択方法を選んでください") },
            confirmButton = {
                TextButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                        showImageSelectionDialog = false
                    }
                ) {
                    Text("ギャラリーから選択")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // カメラ機能は今回は簡略化
                        // 実際のアプリでは、カメラIntentを起動してファイル保存処理を行う
                        showImageSelectionDialog = false
                    }
                ) {
                    Text("カメラで撮影")
                }
            }
        )
    }
}


//数字で管理されているジャンルIDを、人が読める言葉（日本語）に変換して表示する。
private fun getGenreName(genreId: Int): String {
    return when (genreId) {
        Const.GENRE_HOBBY -> "趣味"
        Const.GENRE_LIFE -> "生活"
        Const.GENRE_HEALTH -> "健康"
        Const.GENRE_COMPUTER -> "コンピューター"
        else -> "選択してください"
    }
}