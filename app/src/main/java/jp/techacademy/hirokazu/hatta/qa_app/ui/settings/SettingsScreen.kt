//このファイルの住所。「設定画面(UI)」がどこにあるかを示す。
package jp.techacademy.hirokazu.hatta.qa_app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*//ボタン・テキスト・入力欄などの部品
import androidx.compose.runtime.*//状態を監視する仕組み
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel//さっきのSettingViewModelをこの画面で使えるようにする

@OptIn(ExperimentalMaterial3Api::class)//Material Design 3 のちょっと実験的な機能を使うことを宣言
@Composable
fun SettingScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = viewModel()
) {
    //ViewModelの中の状態（名前、エラー、読み込み中など）をリアルタイムで受け取る。
    //これで画面が自動更新される。
    val uiState by viewModel.uiState.collectAsState()
    //「アプリの現在の状況（スマホ本体へのアクセス）」を取得する。
    val context = LocalContext.current

    // 表示名を読み込み
    LaunchedEffect(Unit) {
        viewModel.loadDisplayName(context)
    }

    // ログアウト成功時の処理
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    // エラーメッセージ・成功メッセージ表示用のSnackbarHost
    val snackbarHostState = remember { SnackbarHostState() }

    // エラーメッセージ表示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // 成功メッセージ表示
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 表示名変更セクション
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "表示名設定",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 表示名入力
                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = viewModel::updateDisplayName,
                        label = { Text("表示名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )

                    // 表示名変更ボタン
                    Button(
                        onClick = { viewModel.changeDisplayName(context) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("表示名を変更")
                        }
                    }
                }
            }

            // ログアウトセクション
            Card(//Cardは、関連するコンテンツやアクションを一つの視覚的な「まとまり」として区切るために使われます。
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "アカウント",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "アプリからログアウトします",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ログアウトボタン
                    OutlinedButton(
                        onClick = viewModel::logout,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("ログアウト")
                        }
                    }
                }
            }
        }
    }
}