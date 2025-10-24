package jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail

import jp.techacademy.hirokazu.hatta.qa_app.data.Answer
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import jp.techacademy.hirokazu.hatta.qa_app.data.User
data class QuestionDetailUiState(
    val question: Question? = null,
    val answers: List<Answer> = emptyList(),
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,


)
