package com.example.sfp4.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import com.example.sfp4.R
import com.example.sfp4.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.HashMap

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore

    private lateinit var userID: String
    var role = ""
    val roles = arrayListOf<String>("Teacher", "Student")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()

        binding.tvGoToSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        val spinner : Spinner = findViewById(R.id.spSelectRole)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, roles)

        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                role = roles[position]
                Toast.makeText(applicationContext, "Continue as $role", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        binding.button.setOnClickListener {
            val name = binding.nameEt.text.toString()
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passET.text.toString().trim()
            val confirmPass = binding.confirmPassEt.text.toString().trim()


            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {

                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            userID = firebaseAuth.currentUser!!.uid
                            val documentReference: DocumentReference = fireStore.collection("users").document(userID)

                            val user = HashMap<String, String>()
                            user.put("name", name)
                            user.put("role", role)

                            Log.e("role", role)

                            documentReference.set(user)
                                .addOnSuccessListener {
                                    Log.e("SignUpActivity", "user profile created $userID")
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                                }

                            val intent = Intent(this, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                        else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun btnSignUpClicked(view: View){
        val emailEt = findViewById<EditText>(R.id.emailEt)
    }
}