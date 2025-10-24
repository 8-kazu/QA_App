package jp.techacademy.hirokazu.hatta.qa_app.ui.settings

data class SettingUiState(
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null

)
