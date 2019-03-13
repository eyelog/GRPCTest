package ru.eyelog.grpctest

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import io.grpc.Context
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        send_button.setOnClickListener {
            (getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(host_edit_text.getWindowToken(), 0)
            send_button.isEnabled = false
            grpc_response_text.text = ""

            GrpcTask(this).execute(
                host_edit_text.text.toString(),
                message_edit_text.text.toString(),
                port_edit_text.text.toString()
            )
        }
    }

    class GrpcTask(activity: AppCompatActivity) : AsyncTask<String, Void, String>() {

        var activityReference: WeakReference<AppCompatActivity>
        lateinit var channel: ManagedChannel


        init {
            activityReference = WeakReference(activity)
        }

        override fun doInBackground(vararg params: String?): String {
            val host = params[0]
            val message = params[1]
            val stPort = params[2]
            val port = if (TextUtils.isEmpty(stPort)) 0 else Integer.valueOf(stPort)

            try {
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub = GreeterGrpc.newBlockingStub(channel)
                val request = HelloRequest.newBuilder().setName(message).build()
                val reply = stub.sayHello(request)
                return reply.message
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()
                return String.format("Failed... : %n%s", sw)
            }
        }

        override fun onPostExecute(result: String?) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Thread.currentThread().interrupt()
            }

            val activity = activityReference.get()
            if (activity == null) {
                return
            }

            activity.grpc_response_text.setText(result)
            activity.send_button.isEnabled = true
        }
    }
}
