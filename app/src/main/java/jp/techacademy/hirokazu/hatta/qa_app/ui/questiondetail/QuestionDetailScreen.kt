package jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    questionUid: String,
    genreId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToAnswerSend: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: QuestionDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // 初期化 - 質問詳細と回答一覧を読み込み
    LaunchedEffect(questionUid, genreId) {
        viewModel.loadQuestionDetail(questionUid, genreId)
    }

    // エラーメッセージ表示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.question?.title ?: "質問詳細")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            // ログインユーザーのみ回答投稿可能
            if (currentUser != null) {
                FloatingActionButton(
                    onClick = { onNavigateToAnswerSend(questionUid, genreId) }
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "回答する")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                // 初回読み込み時のローディング
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
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
            }

            uiState.question == null -> {
                // 質問が見つからない場合
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "質問が見つかりません",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "質問が削除されたか、存在しない可能性があります",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                // 質問詳細と回答一覧を表示
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {

                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser

                    // FirebaseUser → 自作Userに変換
                    val currentUser1 = currentUser?.let {
                        jp.techacademy.hirokazu.hatta.qa_app.data.User(
                            uid = it.uid,
                            displayName = it.displayName ?: "名無しユーザー"
                        )
                    }


                    // 質問詳細ヘッダー
                    item {
                        QuestionDetailHeader(question = uiState.question!!,
                            user = currentUser1!! ,
                            viewModel = viewModel
                        )//ここは別のファイルに書いてある
                        /*val question = uiState.question
                        val user = uiState.user

                        if (question != null && user != null) {
                            QuestionDetailHeader(
                                question = question,
                                user = user,
                                viewModel = viewModel
                            )
                        } else {
                            // ローディング表示など
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }*/
                    }

                    // 回答一覧のタイトル
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "回答一覧 (${uiState.answers.size}件)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // 回答が存在しない場合のメッセージ
                    if (uiState.answers.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = "まだ回答がありません。\n最初の回答を投稿してみましょう！",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        // 回答アイテム一覧
                        items(uiState.answers, key = { it.answerUid }) { answer ->
                            AnswerItemCard(answer = answer)//ここは別のファイルに書いてある
                        }
                    }
                }
            }
        }
    }
}