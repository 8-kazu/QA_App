package jp.techacademy.hirokazu.hatta.qa_app.ui.answersend

data class AnswerSendUiState(
    val body: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAnswerPosted: Boolean = false

)
