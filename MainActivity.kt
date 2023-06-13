package com.example.racketsensefinal
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity:AppCompatActivity (), SensorEventListener {

    // Vistas:
    private var tvMagneto:ArrayList<TextView> = ArrayList()
    private var tvAceleracion:ArrayList<TextView> = ArrayList()
    private var tvGiro:ArrayList<TextView> = ArrayList()
    private var tvGolpe:ArrayList<TextView> = ArrayList()
    // Botones
    private lateinit var btON:Button
    private lateinit var btOFF:Button
    private lateinit var btDescargar:Button
    // Ids
    private var idMagneto:ArrayList<Int> = arrayListOf(R.id.g_x,R.id.g_y,R.id.g_z)
    private var idAceleracion:ArrayList<Int> = arrayListOf(R.id.a_x,R.id.a_y,R.id.a_z)
    private var idGiro:ArrayList<Int> = arrayListOf(R.id.m_x,R.id.m_y,R.id.m_z)
    private var idGolpe:ArrayList<Int> = arrayListOf(R.id.textGolpe,R.id.textDerecha,R.id.textReves,R.id.textVolea)
    //Sensores
    private lateinit var sensorManager:SensorManager
    private lateinit var sensorMagneto:Sensor
    private lateinit var sensorAcceleration:Sensor
    private lateinit var sensorGiro:Sensor
    //Datos de sensores
    private var magnetoData: SensorData? = null
    private var accelerationData: SensorData? = null
    private var giroData: SensorData? = null
    //GiroData
    private var giroX:Float = 0f
    private var giroY:Float = 0f
    private var giroZ:Float = 0f
    //Time
    private var timeMagneto:Long = 0
    private var timeAcceleration: Long = 0
    private var timeGiro: Long = 0

    private lateinit var recyclerView: RecyclerView
    private lateinit var context: Context
    //File
    private lateinit var file:File
    private var fileWriter: FileWriter? = null

    private var golpeEnCurso = false
    private var GIRevisado = false
    private var MGRevisado = false
    //Contadores
    private var contadorGolpes = 0
    private var contadorDerecha = 0
    private var contadorReves = 0
    private var contadorVolea = 0

    private var posibleDerecha = 0
    private var posibleReves = 0
    private var posibleVolea = 0

    private var giroSuma:Float = 0f
    private var giroSuma2:Float = 0f
    private var giroSumaTemp:Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerView)

        initViews()
        initSensesores()
    }

    private fun initViews() {
        // Inicializar vistas y botones
        for (i in idAceleracion){tvAceleracion.add(findViewById(i))} //aceleración
        for (i in idMagneto){tvMagneto.add(findViewById(i))} //magnetómetro
        for (i in idGiro){tvGiro.add(findViewById(i))} //giroscopio
        for (i in idGolpe){tvGolpe.add(findViewById(i))} //giroscopio
        // Inicializar botón de ON
        btON = findViewById(R.id.start)

        btON.setOnClickListener{
            // Acciones al hacer clic en el botón de ON
            context = applicationContext
            createLogFile(context)
            giroSuma = 0f
            registerListener()
            btON.isEnabled = false
            btOFF.isEnabled = true
        }
        // Inicializar botón de OFF
        btOFF = findViewById(R.id.stop)

        btOFF.setOnClickListener{
            // Acciones al hacer clic en el botón de OFF
            if (btON.isEnabled == false) {//Controlamos el error al pulsar el boton OFF sin pulsar previamente ON
                unregisterListener()
                btON.isEnabled = true
                btOFF.isEnabled = false
                closeLogFile()
            }
        }
        // Inicializar botón de Descarga
        btDescargar = findViewById(R.id.descarga)

        btDescargar.setOnClickListener{
            // Acciones al hacer clic en el botón de Descarga
            val file = File(getExternalFilesDir(null), "sensor_data.csv") // Cambia el nombre del archivo si es necesario
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "text/csv") // Cambia el tipo MIME si el archivo es de otro tipo
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        }
    }

    private fun initSensesores()
    {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
        {
            sensorMagneto = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null)
        {
            sensorAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
        {
            sensorGiro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        }
    }
    private fun registerListener()
    {
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
        {
            sensorManager.registerListener(this, sensorMagneto, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null)
        {
            sensorManager.registerListener(this, sensorAcceleration, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
        {
            sensorManager.registerListener(this, sensorGiro, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterListener()
    {
        sensorManager.unregisterListener(this,sensorMagneto)
        sensorManager.unregisterListener(this,sensorAcceleration)
        sensorManager.unregisterListener(this,sensorGiro)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
        {
            getMagnetoData(event)
        }
        else if (event!!.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            getAccelerationData(event)
        }
        else if (event!!.sensor.type == Sensor.TYPE_GYROSCOPE)
        {
            getGiroData(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    @SuppressLint("SetTextI18n")
    private fun getMagnetoData(e:SensorEvent?) {
        // Si no existe magnetoData, lo creamos con los valores del evento
        if (magnetoData == null) {
            magnetoData = SensorData(e!!.values[0], e!!.values[1], e!!.values[2], e!!.timestamp)
        } else { // Actualizamos los valores de magnetoData con los valores del evento
            magnetoData!!.x1 = e!!.values[0]
            magnetoData!!.x2 = e!!.values[1]
            magnetoData!!.x3 = e!!.values[2]
        }
        if (true) {
            tvMagneto[0].text = "x1: ${"%.2f".format(magnetoData!!.x1)} m/s"
            tvMagneto[1].text = "x2: ${"%.2f".format(magnetoData!!.x2)} m/s"
            tvMagneto[2].text = "x3: ${"%.2f".format(magnetoData!!.x3)} m/s"
            timeMagneto = System.currentTimeMillis()
        }
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)
        val sensor1 = magnetoData!!.x1
        val sensor2 = magnetoData!!.x2
        val sensor3 = magnetoData!!.x3
        // Analizamos el golpe con los datos del sensor magnético
        analizarGolpe("MG",magnetoData!!.x1,magnetoData!!.x2,magnetoData!!.x3)
        val formattedSensor1 = sensor1.toString().replace(".", ",")
        val formattedSensor2 = sensor2.toString().replace(".", ",")
        val formattedSensor3 = sensor3.toString().replace(".", ",")
        // Guarda los datos en el archivo CSV
        guardarEnArchivoCSV("MG",formattedTimestamp, formattedSensor1,
            formattedSensor2, formattedSensor3)
    }

    @SuppressLint("SetTextI18n")
    private fun getAccelerationData(e:SensorEvent?) {
        if (accelerationData == null) {
            accelerationData =
                SensorData(e!!.values[0], e!!.values[1], e!!.values[2], e!!.timestamp)
        } else {
            accelerationData!!.x1 = e!!.values[0]
            accelerationData!!.x2 = e!!.values[1]
            accelerationData!!.x3 = e!!.values[2]
        }
        if (true) {
            tvAceleracion[0].text = "x1: ${"%.2f".format(accelerationData!!.x1)} m/s"
            tvAceleracion[1].text = "x2: ${"%.2f".format(accelerationData!!.x2)} m/s"
            tvAceleracion[2].text = "x3: ${"%.2f".format(accelerationData!!.x3)} m/s"
            timeAcceleration = System.currentTimeMillis()
        }
        // Guarda los datos en el archivo CSV
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)
        val sensor1 = accelerationData!!.x1
        val sensor2 = accelerationData!!.x2
        val sensor3 = accelerationData!!.x3
        analizarGolpe("AC",accelerationData!!.x1,accelerationData!!.x2,accelerationData!!.x3)
        val formattedSensor1 = sensor1.toString().replace(".", ",")
        val formattedSensor2 = sensor2.toString().replace(".", ",")
        val formattedSensor3 = sensor3.toString().replace(".", ",")

        guardarEnArchivoCSV("AC",formattedTimestamp, formattedSensor1,
            formattedSensor2, formattedSensor3)
    }

    @SuppressLint("SetTextI18n")
    private fun getGiroData(e:SensorEvent?)
    {
        if(giroData == null)
        {
            giroData = SensorData(e!!.values[0], e!!.values[1], e!!.values[2], e!!.timestamp)
            timeGiro = System.currentTimeMillis()
        }
        else
        {
            val time = (System.currentTimeMillis() - timeGiro)/(1000f)
            giroData!!.x1 = e!!.values[0]
            giroData!!.x2 = e!!.values[1]
            giroData!!.x3 = e!!.values[2]
            giroX += giroData!!.x1 * time
            giroY += giroData!!.x2 * time
            giroZ += giroData!!.x3 * time
        }
        tvGiro[0].text = "x1: ${"%.2f".format(giroData!!.x1*(180.0/Math.PI))} ª/s \t\t giroX: ${"%.2f".format(giroX*(180.0/Math.PI))} ª"
        tvGiro[1].text = "x2: ${"%.2f".format(giroData!!.x2*(180.0/Math.PI))} ª/s \t\t giroY: ${"%.2f".format(giroY*(180.0/Math.PI))} ª"
        tvGiro[2].text = "x3: ${"%.2f".format(giroData!!.x3*(180.0/Math.PI))} ª/s \t\t giroZ: ${"%.2f".format(giroZ*(180.0/Math.PI))} ª"
        timeGiro = System.currentTimeMillis()

        // Guarda los datos en el archivo CSV
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = formatTimestamp(timestamp)
        val sensor1 = giroData!!.x1
        val sensor2 = giroData!!.x2
        val sensor3 = giroData!!.x3
        analizarGolpe("GI",giroData!!.x1,giroData!!.x2,giroData!!.x3)
        val formattedSensor1 = sensor1.toString().replace(".", ",")
        val formattedSensor2 = sensor2.toString().replace(".", ",")
        val formattedSensor3 = sensor3.toString().replace(".", ",")
        guardarEnArchivoCSV("GI",formattedTimestamp, formattedSensor1,
            formattedSensor2, formattedSensor3)
    }
    private fun createLogFile(context: Context) {
        file = File(getExternalFilesDir(null), "sensor_data.csv")
        fileWriter = FileWriter(file, true)
    }
    private fun closeLogFile() {
        fileWriter?.close()
        fileWriter = null

        val lines = readLastLinesFromFile(file, 10)

        // Configura el adaptador del RecyclerView con las líneas recibidas
        val adapter = MyRecyclerViewAdapter(lines)
        recyclerView.adapter = adapter

        // Establecer el administrador de diseño si aún no se ha establecido
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(this)
        }
    }
    private fun guardarEnArchivoCSV(tipoSensor: String, timestamp: String, sensor1: String, sensor2: String, sensor3: String) {

        val data = "$tipoSensor;$timestamp;$sensor1;$sensor2;$sensor3\n"

        try {
            fileWriter?.append(data)
            fileWriter?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun analizarGolpe(tipoSensor: String, sensor1: Float, sensor2: Float, sensor3: Float){
        if (tipoSensor == "AC") {
            val aceleracionVectorial = calcularAceleracionVectorial(sensor1, sensor2, sensor3)
            if (golpeEnCurso) { //Si el golpe esta en curso
                if (aceleracionVectorial > 22) {// Continúa el golpe
                    giroSuma2 = giroSumaTemp //guardamos el valor del ultimo giroSuma
                } else { // Termina el golpe
                    golpeEnCurso = false
                    contadorGolpes++
                    if((!GIRevisado)&&(giroSuma>8))
                    {//Si no hay valores de GI en el golpe en curso, miramos el ultimo giroSuma
                        posibleReves++
                        posibleReves++
                        posibleReves++
                        giroSuma = 0f
                    }
                    if(giroSuma2>8)
                    {//Comprobamos la giroSuma entre dos valores AC > 22
                        posibleReves++
                        posibleReves++
                        posibleReves++
                        giroSuma2 = 0f
                    }
                    finalizarGolpe()
                }
            } else {
                if (aceleracionVectorial > 22) {
                    golpeEnCurso = true
                    //Sumamos 1 a posibleVolea siguiendo los patrones de las pruebas
                    if((sensor2+sensor3 > -19)&&(sensor2+sensor3<0)&&(sensor1>-10)){
                        posibleVolea++
                    }//Sumamos 2 a posibleDerecha siguiendo los patrones de las pruebas
                    if((sensor2+sensor3 < -19)&&(sensor2+sensor3<-32)&&(sensor1>-10)){
                        posibleDerecha++
                        posibleDerecha++
                    }
                }
            }
        }
        else if (golpeEnCurso)
        {
            when (tipoSensor)
            {
                "GI" ->
                {
                    if(!GIRevisado)
                    {
                        val sumaGI = sensor1+sensor2+sensor3
                        if(sumaGI > 10){
                            posibleReves++
                            posibleReves++
                            posibleReves++
                        }
                        if(giroSuma>8)
                        {
                            posibleReves++
                            posibleReves++
                            posibleReves++
                            giroSuma = 0f
                        }
                        if (sumaGI < -3){
                            posibleDerecha++
                        }
                        if(sumaGI < 1){
                            posibleVolea++
                        }
                    }
                    GIRevisado = true
                }
                "MG" -> {
                    if(!MGRevisado)
                    {
                        if (sensor2 > 5){
                            posibleDerecha++
                        }
                        if (sensor2 < 4){
                            posibleVolea++
                        }
                    }
                    MGRevisado = true
                }
            }
        }
        if((!golpeEnCurso) && (tipoSensor == "GI")){
            giroSuma = sensor1+sensor2+sensor3
        }
        if((golpeEnCurso) && (tipoSensor == "GI")){
            giroSumaTemp = sensor1+sensor2+sensor3
        }
    }

    private fun finalizarGolpe() {//Actualizamos los valores tvGolpe en la pantalla
        tvGolpe[0].text= "G: $contadorGolpes" // Actualiza el contador de golpes totales
        // Comprueba el tipo de golpe y actualiza el contador correspondiente
        if (posibleDerecha > posibleReves && posibleDerecha > posibleVolea) {
            contadorDerecha++
            tvGolpe[1].text= "G: $contadorDerecha"
        } else if (posibleReves > posibleDerecha && posibleReves > posibleVolea) {
            contadorReves++
            tvGolpe[2].text= "G: $contadorReves"
        } else if (posibleVolea > posibleDerecha && posibleVolea > posibleReves) {
            contadorVolea++
            tvGolpe[3].text= "G: $contadorVolea"
        }
        // Reiniciamos las variables para el siguiente golpe
        posibleDerecha = 0
        posibleReves = 0
        posibleVolea = 0
        MGRevisado = false
        GIRevisado = false
    }
    private fun calcularAceleracionVectorial(sensor1: Float,sensor2: Float,sensor3: Float): Float {
        // Calcula la aceleración vectorial utilizando los valores de los sensores
        return Math.sqrt((sensor1 * sensor1 + sensor2 * sensor2 + sensor3 * sensor3).toDouble()).toFloat()
    }

    private fun readLastLinesFromFile(file: File, lineCount: Int): List<String> {
        val lines = mutableListOf<String>()
        file.bufferedReader().use { reader ->
            var line = reader.readLine()
            val buffer = CircularFifoBuffer<String>(lineCount) // Utiliza CircularFifoBuffer para almacenar solo las últimas lineas
            while (line != null) {
                buffer.add(line)
                line = reader.readLine()
            }
            lines.addAll(buffer)
        }
        return lines
    }
    // Función para formatear el timestamp como una cadena de fecha/hora legible
    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

}