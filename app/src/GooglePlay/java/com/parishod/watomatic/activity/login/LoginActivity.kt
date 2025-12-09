package com.parishod.watomatic.activity.login

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    companion object {
        private const val PREF_USER_EMAIL = "pref_user_email"
        private const val EMAIL_ALREADY_EXISTS = "email address is already"
        private const val INVALID_CREDENTIALS = "credential is incorrect"
    }

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
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        // Try to request ID token if the resource exists (usually added via google-services.json)
        val webClientIdRes = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (webClientIdRes != 0) {
            gsoBuilder.requestIdToken(getString(webClientIdRes)) // default added with google-services.json
        }
        val gso = gsoBuilder.build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            Toast.makeText(this, getString(R.string.google_sign_in_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                authResult.user?.email?.let { email ->
                    handleSuccessfulAuth(email)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
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
            handleLoginButtonClick()
        }

        binding.btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE)
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnContinueAsGuest.setOnClickListener {
            continueAsGuest()
        }
    }

    private fun handleLoginButtonClick() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        if (validateInputs(email, password)) {
            checkIfEmailExists(email) { exists ->
                if (exists) {
                    doSignin(email, password)
                } else {
                    createNewAccount(email, password)
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun continueAsGuest() {
        preferencesManager.isGuestMode = true
        navigateToMain()
    }

    private fun doSignin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                checkAndSendEmailVerification(it.user)
                it.user?.getIdToken(true)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val idToken = task.result?.token
                            // Use the token here
                            Log.d("firebase","Firebase ID Token: $idToken")
                            preferencesManager.firebaseToken = idToken
                        } else {
                            // Handle error
                            task.exception?.printStackTrace()
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, getString(R.string.authentication_failed_with_message, exception.message),
                    Toast.LENGTH_LONG).show()
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
        AlertDialog.Builder(this)
            .setTitle(R.string.create_new_account)
            .setMessage(getString(R.string.create_new_account_prompt, email))
            .setPositiveButton(R.string.create_account) { _, _ ->
                createNewAccount(email, password)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createNewAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                checkAndSendEmailVerification(it.user)
            }
            .addOnFailureListener { exception ->
                val message = when {
                    exception.message?.contains(EMAIL_ALREADY_EXISTS, ignoreCase = true) == true -> {
                        doSignin(email, password)
//                        getString(R.string.email_already_registered)
                        ""
                    }
                    exception.message?.contains(INVALID_CREDENTIALS, ignoreCase = true) == true -> {
                        getString(R.string.invalid_email_or_password_length)
                    }
                    else -> {
                        getString(R.string.create_account_failed, exception.message)
                    }
                }
                if(message.isNotEmpty())
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun checkAndSendEmailVerification(user: FirebaseUser?) {
        if (user != null && !user.isEmailVerified) {
            user.sendEmailVerification()
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.verification_email_sent, user.email), Toast.LENGTH_SHORT).show()
                    showVerificationDialog(user)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, getString(R.string.verification_email_failed, exception.message), Toast.LENGTH_SHORT).show()
                }
        } else {
            user?.email?.let { handleSuccessfulAuth(it) }
        }
    }

    private fun showVerificationDialog(user: FirebaseUser) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.verify_your_email))
        builder.setMessage(getString(R.string.verification_dialog_message, user.email))

        builder.setPositiveButton(getString(R.string.i_ve_verified)) { _, _ ->
            user.reload()
                .addOnSuccessListener {
                    if (user.isEmailVerified) {
                        Toast.makeText(this, getString(R.string.email_verified), Toast.LENGTH_SHORT).show()
                        user.email?.let { handleSuccessfulAuth(it) }
                        user?.getIdToken(true)?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val idToken = task.result?.token
                                // Use the token here
                                Log.d("firebase","Firebase ID Token: $idToken")
                                preferencesManager.firebaseToken = idToken
                            } else {
                                // Handle error
                                task.exception?.printStackTrace()
                            }
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.email_not_verified_yet), Toast.LENGTH_SHORT).show()
                        showVerificationDialog(user)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, getString(R.string.verification_check_failed, exception.message), Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNeutralButton(getString(R.string.resend_email)) { _, _ ->
            user.sendEmailVerification()
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.verification_email_resent, user.email), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, getString(R.string.failed_to_resend_email, exception.message), Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton(getString(R.string.contact_permission_dialog_cancel)) { dialog, _ -> dialog.dismiss() }

        builder.setCancelable(false)
        builder.show()
    }

    private fun handleSuccessfulAuth(email: String) {
        preferencesManager.isLoggedIn = true
        preferencesManager.isGuestMode = false
        preferencesManager.saveString(PREF_USER_EMAIL, email)
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