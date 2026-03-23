package com.parishod.watomatic.activity.login

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.ActionCodeSettings
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
    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private var subscriptionManager: com.parishod.watomatic.model.subscription.SubscriptionManager? = null

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
        subscriptionManager = com.parishod.watomatic.model.subscription.SubscriptionManagerImpl(this, preferencesManager)
        auth = FirebaseAuth.getInstance()

        setupGoogleSignIn()
        setupViews()
        setupClickListeners()
        setupTextWatchers()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if(preferencesManager.isLoggedIn){
            navigateToMain()
            return
        }
        val link = intent?.data?.toString()
        if (link != null && auth.isSignInWithEmailLink(link)) {
            val email = preferencesManager.getString(PREF_USER_EMAIL, "")
            if (email.isNotEmpty()) {
                completeSignInWithEmailLink(email, link)
            } else {
                // Email missing from prefs (e.g. user clicked link on different device/browser)
                showEmailInputDialog(link)
            }
        }
    }

    private fun showEmailInputDialog(link: String) {
        val emailInput = android.widget.EditText(this)
        emailInput.hint = "Confirm your email"
        emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(this)
            .setTitle("Confirm Email")
            .setMessage("Please enter your email to complete the sign-in.")
            .setView(emailInput)
            .setPositiveButton("Verify") { _, _ ->
                val email = emailInput.text.toString()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    completeSignInWithEmailLink(email, link)
                } else {
                    Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                    showEmailInputDialog(link)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeSignInWithEmailLink(email: String, link: String) {
        showLoading()
        auth.signInWithEmailLink(email, link)
            .addOnSuccessListener { authResult ->
                authResult.user?.email?.let { verifiedEmail ->
                    handleSuccessfulAuth(verifiedEmail)
                } ?: hideLoading()

                authResult.user?.getIdToken(true)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        preferencesManager.firebaseToken = task.result?.token
                    }
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupGoogleSignIn() {
        credentialManager = CredentialManager.create(this)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                authResult.user?.email?.let { email ->
                    handleSuccessfulAuth(email)
                } ?: hideLoading()
            }
            .addOnFailureListener {
                hideLoading()
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

        if (validateInputs(email)) {
            showLoading()
            sendSignInLink(email)
        }
    }

    private fun sendSignInLink(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://atomatic-cd91f.firebaseapp.com/finishSignIn")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                packageName,
                true, // installIfNotAvailable
                null // minimumVersion
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnSuccessListener {
                hideLoading()
                preferencesManager.saveString(PREF_USER_EMAIL, email)
                Toast.makeText(this, "Verification link sent to $email", Toast.LENGTH_LONG).show()
                showVerificationSentDialog(email)
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showVerificationSentDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Check your email")
            .setMessage("We've sent a verification link to $email. Please click the link in your email to sign in.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun signInWithGoogle() {
        showLoading()

        val webClientIdRes = resources.getIdentifier("default_web_client_id", "string", packageName)
        val webClientId = if (webClientIdRes != 0) getString(webClientIdRes) else ""

        if (webClientId.isEmpty()) {
            hideLoading()
            Toast.makeText(this, "Web Client ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                hideLoading()
                if (e !is GetCredentialCancellationException) {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.google_sign_in_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleSignIn(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: GoogleIdTokenParsingException) {
                hideLoading()
                Toast.makeText(this, getString(R.string.google_sign_in_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        } else {
            hideLoading()
            Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun continueAsGuest() {
        preferencesManager.isGuestMode = true
        navigateToMain()

        //testing create anonymous user for non logged case to get firebase tiken
        /*if (auth.currentUser != null) {
            preferencesManager.isGuestMode = true
            navigateToMain()
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener {
                preferencesManager.isGuestMode = true
                navigateToMain()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.authentication_failed_with_message, e.message), Toast.LENGTH_SHORT).show()
            }*/
    }

    private fun showVerificationDialog(user: FirebaseUser) {
        // No longer used in Email Link flow
    }

    private fun handleSuccessfulAuth(email: String) {
        preferencesManager.isLoggedIn = true
        preferencesManager.isGuestMode = false
        preferencesManager.saveString(PREF_USER_EMAIL, email)
        handleSuccessfulLogin()
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false
        binding.btnContinueAsGuest.isEnabled = false
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
        binding.btnLogin.isEnabled = true
        binding.btnGoogleSignIn.isEnabled = true
        binding.btnContinueAsGuest.isEnabled = true
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tilEmail.error = null
            }
        }

        binding.etEmail.addTextChangedListener(textWatcher)
    }

    private fun validateInputs(email: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        return isValid
    }

    private fun handleSuccessfulLogin() {
        // Refresh subscription status before navigating
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                subscriptionManager?.refreshSubscriptionStatus()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}