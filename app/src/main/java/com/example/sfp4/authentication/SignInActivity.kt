 package com.example.sfp4.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import com.example.sfp4.DonationActivity
import com.example.sfp4.MainActivity
import com.example.sfp4.student_home.MainActivityStudent
import com.example.sfp4.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

 class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore

    var userID = "null"
    var role = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()

        if(firebaseAuth.currentUser != null){
            userID = firebaseAuth.currentUser!!.uid
            val documentReference = fireStore.collection("users").document(userID)

            documentReference.get()
                .addOnSuccessListener { document ->
                    if(document != null) {
                        role = document.getString("role")!!.toString()
                        Toast.makeText(this, "Signing In as $role", Toast.LENGTH_LONG).show()

                        if(role == "Teacher"){
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                        else if(role == "Student"){
                            val intent = Intent(this, MainActivityStudent::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                    }
                }

//            Handler(Looper.getMainLooper()).postDelayed({
//                if(role == "Teacher"){
//                    val intent = Intent(this, MainActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    startActivity(intent)
//                }
//                else if(role == "Student"){
//                    val intent = Intent(this, MainActivityStudent::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    startActivity(intent)
//                }
//            }, 2000)
        }

        binding.tvGoToSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passET.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        if(userID == "null"){
                            userID = firebaseAuth.currentUser!!.uid
                        }
                        val documentReference = fireStore.collection("users").document(userID)

                        documentReference.get()
                            .addOnSuccessListener { document ->
                                if(document != null) {
                                    role = document.getString("role")!!.toString()
                                    Toast.makeText(this, "Signing In as $role", Toast.LENGTH_LONG).show()

                                    if(role == "Teacher"){
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        startActivity(intent)
                                    }
                                    else if(role == "Student"){
                                        val intent = Intent(this, MainActivityStudent::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        startActivity(intent)
                                    }
                                }
                            }

                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
    }

     fun btnDonationClicked(view: View) {
         val intent = Intent(this, DonationActivity::class.java)
         startActivity(intent)
     }

 }