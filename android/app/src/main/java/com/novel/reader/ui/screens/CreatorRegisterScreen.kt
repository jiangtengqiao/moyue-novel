package com.novel.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.model.CreatorRegisterRequest
import com.novel.reader.data.repository.NovelRepository
import com.novel.reader.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatorRegisterViewModel @Inject constructor(
    private val repository: NovelRepository,
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun register(
        penName: String,
        realName: String,
        intro: String,
        email: String,
        phone: String,
    ) {
        if (penName.isBlank()) {
            _error.value = "请输入笔名"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val social = mutableMapOf<String, String>()
                if (phone.isNotBlank()) social["phone"] = phone
                repository.registerCreator(CreatorRegisterRequest(
                    penName = penName,
                    realName = realName.ifBlank { null },
                    introduction = intro.ifBlank { null },
                    contactEmail = email.ifBlank { null },
                    contactPhone = phone.ifBlank { null },
                    socialAccounts = social.ifEmpty { null },
                ))
                _success.value = true
            } catch (e: Exception) {
                _error.value = "注册失败: ${e.message}"
            }
            _loading.value = false
        }
    }
}

@Composable
fun CreatorRegisterScreen(
    viewModel: CreatorRegisterViewModel = hiltViewModel(),
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()

    var penName by remember { mutableStateOf("") }
    var realName by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(success) { if (success) onSuccess() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("创作者注册", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(8.dp))
        Text(
            "完善以下信息，成为墨阅创作者。\n你的读者将通过这些信息了解你。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = penName, onValueChange = { penName = it },
            label = { Text("笔名 *") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = realName, onValueChange = { realName = it },
            label = { Text("真实姓名(选填)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = intro, onValueChange = { intro = it },
            label = { Text("个人简介(选填)") },
            modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("联系邮箱(选填)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("联系电话(选填)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.register(penName, realName, intro, email, phone) },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InkBlack, contentColor = PaperWhite),
        ) {
            if (loading) {
                CircularProgressIndicator(color = PaperWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("提交注册", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
