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

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var loginButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val database = AppDatabase.getDatabase(this)
        authRepository = LocalAuthRepository(database.userDao())

        emailLayout = findViewById(R.id.emailLayout)
        emailInput = findViewById(R.id.emailInput)
        passwordLayout = findViewById(R.id.passwordLayout)
        passwordInput = findViewById(R.id.passwordInput)
        errorText = findViewById(R.id.errorText)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener { attemptLogin() }

        findViewById<MaterialButton>(R.id.registerLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        emailLayout.error = null
        passwordLayout.error = null
        errorText.visibility = View.GONE

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty()) {
            emailLayout.error = getString(R.string.error_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = getString(R.string.error_email_invalid)
            return
        }
        if (password.isEmpty()) {
            passwordLayout.error = getString(R.string.error_password_required)
            return
        }

        loginButton.isEnabled = false

        lifecycleScope.launch {
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    sessionManager.saveSession(result.user.id)
                    navigateToMain()
                }
                is AuthResult.Error -> {
                    loginButton.isEnabled = true
                    errorText.text = result.message
                    errorText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
