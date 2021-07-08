package com.user.kasironline.activity

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.GlideApp
import com.user.kasironline.R
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.utils.Constant.PLAY_SERVICES_RESOLUTION_REQUEST
import com.user.kasironline.utils.Validation.Companion.validateFields
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        prepare()
        checkPlayServices()
        checkUser()
        btn_login.setOnClickListener {
            val myUsername = input_username.text.toString()
            val myPassword = input_password.text.toString()

            if(validateFields(myUsername) || validateFields(myPassword)){
                Toast.makeText(this, "Username atau Passowrd tidak boleh kosong!", Toast.LENGTH_SHORT)
                    .show()
            }else{
                btn_login.showProgress { progressColor = Color.WHITE }
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(myUsername)
                    .get()
                    .addOnSuccessListener {
                        btn_login.hideProgress(R.string.btn_login)
                        val password = it.getString("password").toString()
                        if (myPassword == password) {
                            SharedPrefManager.getInstance(this)?.setStatus(true)
                            SharedPrefManager.getInstance(this)?.setUsername(myUsername)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }else{
                            Toast.makeText(this, resources.getString(R.string.account_wrong), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }.addOnFailureListener {
                        btn_login.hideProgress(R.string.btn_login)
                        Toast.makeText(this, resources.getString(R.string.account_wrong), Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }
    }

    private fun prepare() {
        GlideApp.with(this)
            .load(R.drawable.royal_truss)
            .into(img_logo)

        bindProgressButton(btn_login)
        btn_login.attachTextChangeAnimator()
    }

    private fun checkUser() {
        val getStatus = SharedPrefManager.getInstance(this)?.getStatus
        if (getStatus == true) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun checkPlayServices() {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(
                    this, result,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                ).show()
            }
        }
    }
}