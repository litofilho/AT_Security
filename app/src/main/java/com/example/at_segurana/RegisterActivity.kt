package com.example.at_segurana

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_register.*
import java.io.File
import java.security.MessageDigest


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RegisterActivity : AppCompatActivity() {

    private lateinit var mInterstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        MobileAds.initialize(this){}
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        mInterstitialAd.adListener = object : AdListener(){
            override fun onAdFailedToLoad(errorCode: Int){
                Log.w("TAG", "The interstitial not loaded. Error: $errorCode")
            }
        }

        cpfEditText.addTextChangedListener(Mask.mask("###.###.###-##", cpfEditText))
    }

    fun register(view: View) {

        if(validateName()){
            if(validateCPF()){
                if(validateEmail()){
                    if(validatePassword()){
                        saveData()
                        callAd()
                    }else
                        Toast.makeText(this, "As senhas digitadas não são iguais", Toast.LENGTH_LONG).show()
                }else
                    Toast.makeText(this, "O campo Email não é válido", Toast.LENGTH_LONG).show()
            }else
                Toast.makeText(this, "O campo CPF não é válido", Toast.LENGTH_LONG).show()
        }else
            Toast.makeText(this, "O campo Nome não é válido", Toast.LENGTH_LONG).show()
    }

    private fun saveData(){

        val data = object {
            val name = nameEditText.text.toString()
            val cpf = cpfEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = getHashForString(passwordEditText.text.toString())
        }

        val fileName = "data.txt"
        val string = "Nome: ${data.name}\tCPF: ${data.cpf}\tLogin: ${data.email}\tSenha: ${data.password}"
        val file = File(filesDir,fileName)

        try {
            file.bufferedWriter().use { out ->
                out.appendln(string)
                Log.d("WRITE",string)
            }
        }catch(e: Exception){
            Log.w("TAG",e.message)
        }
    }

    fun getHashForString(stringToHash: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val result = digest.digest(stringToHash.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in result) {
            sb.append(String.format("%02X", b))
        }
        val hashedString = sb.toString()
        return hashedString
    }

    fun callAd() = mInterstitialAd.show()

    fun validateName() = !nameEditText.text.toString().isEmpty()

    fun validateEmail() = !emailEditText.text.toString().isEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailEditText.text.toString()).matches()

    fun validatePassword() = passwordEditText.text.toString() == confirmPasswordEditText.text.toString()

    fun validateCPF() : Boolean{
        val cpf = cpfEditText.text.toString()
        val cpfClean = cpf.replace(".", "").replace("-", "")

        //## check if size is eleven
        if (cpfClean.length != 11)
            return false

        //## check if is number
        try {
            val number  = cpfClean.toLong()
        }catch (e : Exception){
            return false
        }

        //continue
        val dvCurrent10 = cpfClean.substring(9,10).toInt()
        val dvCurrent11= cpfClean.substring(10,11).toInt()

        //the sum of the nine first digits determines the tenth digit
        val cpfNineFirst = IntArray(9)
        var i = 9
        while (i > 0 ) {
            cpfNineFirst[i-1] = cpfClean.substring(i-1, i).toInt()
            i--
        }
        //multiple the nine digits for your weights: 10,9..2
        val sumProductNine = IntArray(9)
        var weight = 10
        var position = 0
        while (weight >= 2){
            sumProductNine[position] = weight * cpfNineFirst[position]
            weight--
            position++
        }
        //Verify the nineth digit
        var dvForTenthDigit = sumProductNine.sum() % 11
        dvForTenthDigit = 11 - dvForTenthDigit //rule for tenth digit
        if(dvForTenthDigit > 9)
            dvForTenthDigit = 0
        if (dvForTenthDigit != dvCurrent10)
            return false

        //### verify tenth digit
        val cpfTenFirst = cpfNineFirst.copyOf(10)
        cpfTenFirst[9] = dvCurrent10
        //multiple the nine digits for your weights: 10,9..2
        val sumProductTen = IntArray(10)
        var w = 11
        var p = 0
        while (w >= 2){
            sumProductTen[p] = w * cpfTenFirst[p]
            w--
            p++
        }
        //Verify the nineth digit
        var dvForeleventhDigit = sumProductTen.sum() % 11
        dvForeleventhDigit = 11 - dvForeleventhDigit //rule for tenth digit
        if(dvForeleventhDigit > 9)
            dvForeleventhDigit = 0
        if (dvForeleventhDigit != dvCurrent11)
            return false

        return true
    }
}

