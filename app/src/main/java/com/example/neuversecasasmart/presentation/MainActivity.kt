package com.example.neuversecasasmart.presentation

import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.example.neuversecasasmart.R
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
import kotlin.concurrent.thread


class MainActivity   : ComponentActivity(), MqttCallbackExtended {

    var client: MqttClient? = null
    var mqttOptions: MqttConnectOptions? = null
    val id:String?= UUID.randomUUID().toString()
    private val menuItems: java.util.ArrayList<MenuItem> = java.util.ArrayList<MenuItem>()
    var recyclerViewG: WearableRecyclerView? = null
    var mainMenuAdapter: MainMenuAdapter? = null
    val poolsG: java.util.ArrayList<Pool> = java.util.ArrayList<Pool>()
    val poolsGInic: java.util.ArrayList<Pool> = java.util.ArrayList<Pool>()
    var inicializado: Boolean = false
    private val lock = Any()
    var total: Int = 0
    //private var menuInfo: MenuItem? = null
    //private var menuErro: MenuItem? = null

    override fun onDestroy(){
        super.onDestroy()
        client?.disconnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView: WearableRecyclerView = findViewById(R.id.main_menu_view)
        recyclerViewG= recyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.isEdgeItemsCenteringEnabled = true
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        if(!inicializado) {
            inicializado = true
            mainMenuAdapter =
                MainMenuAdapter(this, menuItems, object : MainMenuAdapter.AdapterCallback {
                    override fun onItemClicked(menuPosition: Int?) {
                        if (menuPosition != null) {
                            val menu: MenuItem = menuItems.get(menuPosition)
                            val dispositivo: Dispositivo = menu.dispositivo

                            if (dispositivo.genero.equals(TipoIOT.CONTROLELAMPADA) ||
                                dispositivo.genero.equals(TipoIOT.INTERRUPTOR)) {
                                if (dispositivo.status.equals(Status.ON)) {
                                    dispositivo.status = Status.OFF
                                    //menu.image = R.drawable.b983w
                                } else {
                                    dispositivo.status = Status.ON
                                    //menu.image = R.drawable.lacessa
                                }
                                //recyclerViewG?.refreshDrawableState()
                            }
                            else if (dispositivo.genero.equals(TipoIOT.PUSHBUTTON)) {
                                dispositivo.status = Status.PUSHON;
                                menu.image = R.drawable.branco;
                            }
                            val pools: ArrayList<Pool> = ArrayList()
                            val pool: Pool = Pool()
                            pool.id = dispositivo.idPool
                            pool.dispositivos.add(dispositivo)
                            pools.add(pool)
                            val gson: Gson = GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss")
                                .excludeFieldsWithoutExposeAnnotation().create()
                            val jSon = gson.toJson(pools)
                            val topic = "br/com/neuverse/servidores/" + pool.id + "/atualizar"
                            client?.publish(topic, jSon.toByteArray(), 0, false)
                        }
                    }
                })
            //menuInfo = MenuItem(R.drawable.img, "0", null)
            //menuItems.add(menuInfo!!)
            //menuErro = MenuItem(R.drawable.img, "Ok", null)
            //menuItems.add(menuErro!!)
            recyclerView.adapter = mainMenuAdapter
            try {
                thread {
                    val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
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
                    Thread.sleep(5000)
                    client?.publish(
                        "br/com/neuverse/geral/info", id?.toByteArray(), 0,
                        false
                    )
                }
            } catch (exception: Exception) {
                println()
            }
        }
    }

    override fun connectionLost(cause: Throwable?) {
        runOnUiThread {
            //menuErro?.text = cause!!.message.toString()
            mainMenuAdapter?.notifyDataSetChanged()
        }
        println()
    }

    override fun  messageArrived(topic: String?, message: MqttMessage?) {
        try {
            if (topic.equals("br/com/neuverse/geral/lista")) {
                val gson: Gson = GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create()
                val listType = object : TypeToken<ArrayList<Pool?>?>() {}.type
                var json: String = String(message!!.payload)
                val poolsIot: List<Pool> = gson.fromJson(json, listType)
                for(pool in poolsIot){
                    for (dispositivo in pool.dispositivos) {
                        dispositivo.idPool = pool.id
                        var menu: MenuItem? = null
                        for(menuItem in menuItems){
                            if (  (menuItem.dispositivo != null) && menuItem.dispositivo.idPool.equals(pool.id) &&
                                menuItem.dispositivo.id.equals(dispositivo.id)){
                                menu = menuItem
                                break
                            }
                        }

                        if (menu == null) {
                            menu = MenuItem(R.drawable.b983w, dispositivo.nick, dispositivo)
                            menuItems.add(menu)
                        }
                        //if(dispositivo.id == 1)
                        //    menuItems.add(MenuItem(R.drawable.img, pool.nick, null))

                        if (dispositivo.genero.equals(TipoIOT.CONTROLELAMPADA)) {
                            if (dispositivo.status.equals(Status.ON))
                                menu.image = R.drawable.lacessa
                            else
                                menu.image = R.drawable.b983w
                        } else if (dispositivo.genero.equals(TipoIOT.PUSHBUTTON)) {
                            menu.image = R.drawable.pushbutton
                        } else if (dispositivo.genero.equals(TipoIOT.INTERRUPTOR)) {
                            if (dispositivo.status.equals(Status.ON) && dispositivo.nivelAcionamento.equals(Status.HIGH))
                                menu.image = R.drawable.intligado
                            else if (dispositivo.status.equals(Status.OFF) && dispositivo.nivelAcionamento.equals(Status.HIGH))
                                menu.image = R.drawable.intdesligado
                            else if (dispositivo.status.equals(Status.ON) && dispositivo.nivelAcionamento.equals(Status.LOW))
                                menu.image = R.drawable.intdesligado
                            else if (dispositivo.status.equals(Status.OFF) && dispositivo.nivelAcionamento.equals(Status.LOW))
                                menu.image = R.drawable.intligado
                        } else if (dispositivo.genero.equals(TipoIOT.NOTIFICACAO)) {
                            if (dispositivo.status.equals(Status.ON))
                                menu.image = R.drawable.notificacao
                            else
                                menu.image = R.drawable.semnotificacao
                        }
                    }
                }
                poolsG.addAll(poolsIot)
                runOnUiThread {
                    total += 1
                    //menuInfo?.text = total.toString()
                    mainMenuAdapter?.notifyDataSetChanged()
                }
            }
            else if ((topic == "br/com/neuverse/servidores/events")){
                handleEvent(String(message!!.payload))
            }
        } catch (exception: Exception) {
            runOnUiThread {
                //menuErro?.text = exception.message
                mainMenuAdapter?.notifyDataSetChanged()
            }
        }
    }

    fun handleEvent(json:String) {

        val gson = GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create()
        val listType = object : TypeToken<java.util.ArrayList<Pool?>?>() {}.type
        val pools = gson.fromJson<List<Pool>>(json, listType)

        for (pool in pools) {
            for (dispositivo  in pool.getDispositivos()) {
                var menu: MenuItem? = null
                for(menuItem in menuItems){
                    if ( (menuItem.dispositivo != null) && menuItem.dispositivo.idPool.equals(pool.id) &&
                        menuItem.dispositivo.id.equals(dispositivo.id)){
                        menu = menuItem
                        break
                    }
                }
                if(menu != null){
                    if (dispositivo.genero.equals(TipoIOT.CONTROLELAMPADA)) {
                        if (dispositivo.status.equals(Status.ON))
                            menu.image = R.drawable.lacessa
                        else
                            menu.image = R.drawable.b983w
                    } else if (dispositivo.genero.equals(TipoIOT.PUSHBUTTON)) {
                        menu.image = R.drawable.pushbutton
                    } else if (dispositivo.genero.equals(TipoIOT.INTERRUPTOR)) {
                        if (dispositivo.status.equals(Status.ON) && dispositivo.nivelAcionamento.equals(Status.HIGH))
                            menu.image = R.drawable.intligado
                        else if (dispositivo.status.equals(Status.OFF) && dispositivo.nivelAcionamento.equals(Status.HIGH))
                            menu.image = R.drawable.intdesligado
                        else if (dispositivo.status.equals(Status.ON) && dispositivo.nivelAcionamento.equals(Status.LOW))
                            menu.image = R.drawable.intdesligado
                        else if (dispositivo.status.equals(Status.OFF) && dispositivo.nivelAcionamento.equals(Status.LOW))
                            menu.image = R.drawable.intligado
                    } else if (dispositivo.genero.equals(TipoIOT.NOTIFICACAO)) {
                        if (dispositivo.status.equals(Status.ON))
                            menu.image = R.drawable.notificacao
                        else
                            menu.image = R.drawable.semnotificacao
                    }
                    runOnUiThread {
                        mainMenuAdapter?.notifyDataSetChanged()
                    }
                }
            }
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




