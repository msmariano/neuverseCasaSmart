package com.example.neuversecasasmart.presentation


import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.UUID


class MqttClientHelper(context: Context?) : MqttCallbackExtended {

    companion object {
        const val TAG = "MqttClientHelper"
    }
    var client: MqttClient? = null
    var mqttOptions: MqttConnectOptions? = null
    var id:String?=UUID.randomUUID().toString()
    init {
        try {
            val policy = ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            client = MqttClient(
                "ssl://f897f821.ala.us-east-1.emqxsl.com:8883",
                id,
                MqttDefaultFilePersistence(System.getProperty("java.io.tmpdir"))
            )
            client!!.setCallback(this)
            mqttOptions = MqttConnectOptions()
            mqttOptions!!.maxInflight = 200
            mqttOptions!!.connectionTimeout = 0
            mqttOptions!!.keepAliveInterval = 60
            mqttOptions!!.isAutomaticReconnect = true
            mqttOptions!!.isCleanSession = true
            mqttOptions!!.userName = "neuverse"
            mqttOptions!!.password = "M@r040370".toCharArray()
            mqttOptions!!.sslHostnameVerifier = null
            client!!.connect(mqttOptions)

        }
        catch (exception:Exception){
            println()
        }
    }

    fun listarIots(){
        try {
            client?.publish("br/com/neuverse/geral/info", id?.toByteArray(), 0, false)
        }
        catch (exception:Exception){
            println()
        }
    }

    override fun connectionLost(cause: Throwable?) {
        println(cause!!.message.toString())
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        val gson: Gson = GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create()
        val listType = object : TypeToken<ArrayList<Pool?>?>() {}.type
        var json: String = String(message!!.payload)
        val pools: List<Pool> = gson.fromJson(json, listType)

        println(topic)
        println(String(message!!.payload))
        if (topic.equals("")){
        }
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        println()
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        try {
            client?.subscribe("br/com/neuverse/servidores/events")
            client?.subscribe("br/com/neuverse/geral/lista")
        }
        catch (exception:Exception){
            println()
        }
    }
}

