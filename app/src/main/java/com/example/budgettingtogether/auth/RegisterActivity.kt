package com.example.budgettingtogether.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.MainActivity
import com.example.budgettingtogether.R
import com.example.budgettingtogether.core.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var registerButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val database = AppDatabase.getDatabase(this)
        authRepository = LocalAuthRepository(database.userDao())
        sessionManager = SessionManager(this)

        emailLayout = findViewById(R.id.emailLayout)
        emailInput = findViewById(R.id.emailInput)
        usernameLayout = findViewById(R.id.usernameLayout)
        usernameInput = findViewById(R.id.usernameInput)
        passwordLayout = findViewById(R.id.passwordLayout)
        passwordInput = findViewById(R.id.passwordInput)
        errorText = findViewById(R.id.errorText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener { attemptRegister() }

        findViewById<MaterialButton>(R.id.loginLink).setOnClickListener {
            finish()
        }
    }

    private fun attemptRegister() {
        emailLayout.error = null
        usernameLayout.error = null
        passwordLayout.error = null
        errorText.visibility = View.GONE

        val email = emailInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty()) {
            emailLayout.error = getString(R.string.error_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = getString(R.string.error_email_invalid)
            return
        }
        if (username.isEmpty()) {
            usernameLayout.error = getString(R.string.error_username_required)
            return
        }
        if (username.length < 3) {
            usernameLayout.error = getString(R.string.error_username_too_short)
            return
        }
        if (password.isEmpty()) {
            passwordLayout.error = getString(R.string.error_password_required)
            return
        }
        if (password.length < 6) {
            passwordLayout.error = getString(R.string.error_password_too_short)
            return
        }

        registerButton.isEnabled = false

        lifecycleScope.launch {
            when (val result = authRepository.register(email, username, password)) {
                is AuthResult.Success -> {
                    sessionManager.saveSession(result.user.id)
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is AuthResult.Error -> {
                    registerButton.isEnabled = true
                    errorText.text = result.message
                    errorText.visibility = View.VISIBLE
                }
            }
        }
    }
}
