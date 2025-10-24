package jp.techacademy.hirokazu.hatta.qa_app.ui.login


import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import androidx.datastore.preferences.core.edit
//import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.techacademy.hirokazu.hatta.qa_app.Const
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.dataStore//*





import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.saveDisplayNameToDataStore
import kotlinx.coroutines.flow.first

// DataStore extension
//private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // UI State
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun updateDisplayName(displayName: String) {
        _uiState.value = _uiState.value.copy(displayName = displayName)
    }

    fun toggleCreateAccountMode() {
        _uiState.value = _uiState.value.copy(
            isCreateAccountMode = !_uiState.value.isCreateAccountMode,
            errorMessage = null
        )
    }

    fun signIn(context: Context) {
        val currentState = _uiState.value

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "メールアドレスとパスワードを入力してください")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(currentState.email, currentState.password).await()

                // ログイン成功時にFirestoreから表示名を取得してDataStoreに保存
                auth.currentUser?.let { user ->
                    val userDoc = firestore.collection(Const.UsersPATH).document(user.uid).get().await()
                    val displayName = userDoc.getString(Const.NameKEY) ?: ""
                    saveDisplayNameToDataStore(context, displayName)
                }

                _uiState.value = currentState.copy(
                    isLoading = false,
                    isLoggedIn = true
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "ログインに失敗しました: ${e.message}"
                )
            }
        }
    }

    fun createAccount(context: Context) {
        val currentState = _uiState.value

        if (currentState.email.isBlank() || currentState.password.isBlank() || currentState.displayName.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "すべての項目を入力してください")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // アカウント作成
                val result = auth.createUserWithEmailAndPassword(currentState.email, currentState.password).await()

                // Firestoreに表示名を保存
                result.user?.let { user ->
                    firestore.collection(Const.UsersPATH).document(user.uid)
                        .set(mapOf(Const.NameKEY to currentState.displayName))
                        .await()

                    // DataStoreに表示名を保存
                    saveDisplayNameToDataStore(context, currentState.displayName)
                }

                _uiState.value = currentState.copy(
                    isLoading = false,
                    isLoggedIn = true
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "アカウント作成に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun checkAuthState(context: Context) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // ログイン済みの場合、Firestoreから表示名を取得
                try {
                    val userDoc = firestore.collection(Const.UsersPATH).document(currentUser.uid).get().await()
                    val displayName = userDoc.getString(Const.NameKEY) ?: ""
                    saveDisplayNameToDataStore(context, displayName)

                    _uiState.value = _uiState.value.copy(isLoggedIn = true)
                } catch (e: Exception) {
                    // エラーの場合はログアウト状態にする
                    _uiState.value = _uiState.value.copy(isLoggedIn = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoggedIn = false)
            }
        }
    }

   /* private suspend fun saveDisplayNameToDataStore(context: Context, displayName: String) {
        val displayNameKey = stringPreferencesKey("display_name")
        context.dataStore.edit { preferences ->
            preferences[displayNameKey] = displayName
        }
    }*/


    private suspend fun saveDisplayNameToDataStore(context: Context, displayName: String) {
        jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.saveDisplayNameToDataStore(context, displayName)
    }

    suspend fun getDisplayNameFromDataStore(context: Context): String {
        val displayNameKey = stringPreferencesKey("display_name")
        val preferences = context.dataStore.data.first()
        return preferences[displayNameKey] ?: ""
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

}


data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isCreateAccountMode: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)