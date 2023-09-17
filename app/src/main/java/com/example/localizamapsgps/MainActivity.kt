package com.example.localizamapsgps

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.localizamapsgps.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference

    /* coordenadas padroes (centro do mapa) */
    private var origem = LatLng(0.0, 0.0)
    private var destino: LatLng = LatLng(0.0, 0.0)
    private var googleMap: GoogleMap? = null

    /* Referencia ao cliente de provedor de localização fundida
    * usado para acessar a localizacao do dispositivo */
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var getCoordenada: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCoordenada = findViewById(R.id.coord_usuario)

        /*  Obtendo referencia ao fragmento do mapa definido no layout */
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync { mMap ->
            googleMap = mMap
        }
        /* Inicializando o cliente de provedor de localizacao fundida */
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        /* Ao clicar no botao, sera exibido os marcadores com os pontos de origem e destino */
        binding.button.setOnClickListener {
            posicaoAtualUsuario()
            posicaoAnimal()
        }
    }


    private var marcadorOrigem: Marker? = null // Referencia ao marcador de origem
    private var marcadorDestino: Marker? = null // Referencia ao marcador de destino

    /* Funcao que adiciona marcador no ponto de origem e de destino */
    private fun addMarkers(googleMap: GoogleMap) {
        /* Adicionando marcador de origem apos a leitura da posicao atual */
        if (origem.latitude != 0.0 && origem.longitude != 0.0) {

            /* Criando marcador para o origem */
            marcadorOrigem = googleMap.addMarker(
                MarkerOptions()
                    /* Definindo a posicao do marcador via coordenadas */
                    .position(origem)
                    .title("Sua Localização")
            )
        }

        /* Removendo marcador de destino, caso exista no mapa */
        marcadorDestino?.remove()

        /* Adicionando marcador de destino apos a leitura dos dados do Firebase */
        if (destino.latitude != 0.0 && destino.longitude != 0.0) {

            /* Criando marcador para o destino */
            marcadorDestino = googleMap.addMarker(
                MarkerOptions()
                    /* Definindo a posicao do marcador via coordenadas */
                    .position(destino)
                    .title("Animal")
            )
        }
    }


    /* Funcao que mostra a posicao atual do usuario consultando o GPS */
    private fun posicaoAtualUsuario() {

        /* verificando permissoes de localizacao */
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        /* Pegando as coordenadas da posicao atual do usuario */
        val localizacao = fusedLocationClient.lastLocation
        localizacao.addOnSuccessListener {
            if (it != null) {
                /* TextView para Debug com as coordenadas do usuario */
                val texLat = it.latitude.toString() + "," + it.longitude.toString()
                getCoordenada.text = texLat

                /* Atualizando marcador para a nova posicao */
                origem = LatLng(it.latitude, it.longitude)
                googleMap?.let { addMarkers(it) }
            }
            //googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(origem, 15f))
        }
    }

    /* Funcao que mostra a posicao do animal consultando o Firebase */
    private fun posicaoAnimal() {

        /* Obtendo referencia ao no "Animal" via Firebase Realtime Databas */
        database = FirebaseDatabase.getInstance().getReference("Animal")

        /* Obtendo valor do no "GPS" via Firebase Realtime Databas */
        database.child("GPS").get().addOnSuccessListener {
            if( it.exists() ){
                /* Pegando a string referente as coordenadas da posicao atual do animal */
                val gps: String = it.value.toString()

                /* Dividindo a string nas coordenadas de latitude e longitude */
                val coordenadas = gps.split(",")

                if (coordenadas.size == 2) {
                    val latitude = coordenadas[0].toDoubleOrNull()
                    val longitude = coordenadas[1].toDoubleOrNull()

                    if (latitude != null && longitude != null) {
                        /* TextView para Debug com as coordenadas */
                        binding.coordAnimal.setText("$latitude,$longitude")

                        /* Atualizando a posicao de destino */
                        destino = LatLng(latitude.toString().toDouble(), longitude.toString().toDouble())

                        /* Atualizando marcador para a nova posicao */
                        googleMap?.let { addMarkers(it) }
                    } else {
                        Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Formato de coordenadas inválido", Toast.LENGTH_LONG).show()
                }
                Toast.makeText(this, "Posicao atual do animal", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Path Animal/GPS não existe", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "FAILED", Toast.LENGTH_LONG).show()
        }
    }
}