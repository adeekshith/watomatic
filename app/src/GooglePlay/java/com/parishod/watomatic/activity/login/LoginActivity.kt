package com.parishod.watomatic.activity.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.activity.main.MainActivity
import com.parishod.watomatic.databinding.ActivityLoginBinding
import com.parishod.watomatic.model.preferences.PreferencesManager

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        auth = FirebaseAuth.getInstance()

        setupGoogleSignIn()
        setupViews()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // default added with google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account.idToken?.let { firebaseAuthWithGoogle(it) }
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.email?.let { email ->
                        preferencesManager.isLoggedIn = true
                        preferencesManager.isGuestMode = false
                        preferencesManager.saveString("pref_user_email", email)
                        handleSuccessfulLogin()
                    }
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                checkIfEmailExists(email) { exists ->
                    if (exists) {
                        doSignin(email, password)
                    } else {
                        showSignUpConfirmation(email, password)
                    }
                }
            }
        }

        binding.btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE)
        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.btnContinueAsGuest.setOnClickListener {
            preferencesManager.isGuestMode = true
            navigateToMain()
        }
    }

    fun doSignin(email: String, password: String){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    handleSuccessfulAuth(email)
                } else {
                    val exception = task.exception
                    Toast.makeText(this, "Authentication failed: ${exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    fun checkIfEmailExists(email: String, onResult: (Boolean) -> Unit) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods ?: emptyList()
                    onResult(signInMethods.isNotEmpty()) // true if email exists
                } else {
                    onResult(false)
                }
            }
    }

    private fun showSignUpConfirmation(email: String, password: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Create New Account")
            .setMessage("Would you like to create a new account with this email?\n\n$email")
            .setPositiveButton("Create Account") { _, _ ->
                createNewAccount(email, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    handleSuccessfulAuth(email)
                } else {
                    when {
                        task.exception?.message?.contains("email address is already", ignoreCase = true) == true -> {
                            Toast.makeText(this, "This email is already registered. Please try signing in.", Toast.LENGTH_LONG).show()
                        }
                        task.exception?.message?.contains("credential is incorrect", ignoreCase = true) == true -> {
                            Toast.makeText(this, "Please use a valid email and a password with at least 6 characters", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Failed to create account: ${task.exception?.message}",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun handleSuccessfulAuth(email: String) {
        preferencesManager.isLoggedIn = true
        preferencesManager.isGuestMode = false
        preferencesManager.saveString("pref_user_email", email)
        handleSuccessfulLogin()
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tilEmail.error = null
                binding.tilPassword.error = null
            }
        }

        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_too_short)
            isValid = false
        }

        return isValid
    }

    private fun handleSuccessfulLogin() {
        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
