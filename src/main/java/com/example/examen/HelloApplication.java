package com.example.tiendaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tiendaapp.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    companion object {
        val customBitmaps = mutableMapOf<String, Bitmap>()
    }

    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        firebaseManager = FirebaseManager()

        setContent {
            TiendaApp(firebaseManager)
        }
    }
}

@Composable
fun TiendaApp(firebaseManager: FirebaseManager, context: Context = LocalContext.current) {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var isAdmin by rememberSaveable { mutableStateOf(false) }
    var userId by rememberSaveable { mutableStateOf("") }

    // Lista de todas las imágenes disponibles en la aplicación para seleccionar
    val todasLasImagenes = remember {
        listOf(
            R.drawable.llavero1, R.drawable.llavero2, R.drawable.llavero3, R.drawable.llavero4,
            R.drawable.maleta1, R.drawable.maleta2, R.drawable.maleta3, R.drawable.maleta4,
            R.drawable.straps1, R.drawable.straps2, R.drawable.straps3, R.drawable.straps4
        )
    }

    // Estado para cargar productos de Firebase
    var productos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar productos de Firebase cuando la app inicia
    LaunchedEffect(key1 = Unit) {
        firebaseManager.getProductos { productosFirebase ->
            // Convertir de ProductoFirebase a Producto
            productos = productosFirebase.map { productoFirebase ->
                val imagenesList = mutableListOf<Int>()

                // Usar imágenes por defecto si no hay suficientes en Firebase
                while (imagenesList.size < 4) {
                    imagenesList.add(todasLasImagenes[imagenesList.size % todasLasImagenes.size])
                }

                // Cargar imágenes personalizadas si existen
                productoFirebase.imagenesBase64.forEach { (key, base64) ->
                    val bitmap = firebaseManager.downloadImage(base64)
                    if (bitmap != null) {
                        MainActivity.customBitmaps[key] = bitmap
                    }
                }

                Producto(
                    id = productoFirebase.id,
                    nombre = productoFirebase.nombre,
                    imagenes = imagenesList.take(4),
                    precioNormal = productoFirebase.precioNormal,
                    precioPremium = productoFirebase.precioPremium,
                    stockNormal = productoFirebase.stockNormal,
                    stockPremium = productoFirebase.stockPremium
                )
            }

            // Si no hay productos en Firebase, usar datos iniciales
            if (productos.isEmpty()) {
                productos = listOf(
                    Producto(
                        "llaveros-gym1",
                        "Llaveros Gym",
                        listOf(R.drawable.llavero1, R.drawable.llavero2, R.drawable.llavero3, R.drawable.llavero4),
                        100.0, 150.0, 10, 5
                    ),
                    Producto(
                        "maletas-deportivas1",
                        "Maletas Deportivas Black",
                        listOf(R.drawable.maleta1, R.drawable.maleta2, R.drawable.maleta3, R.drawable.maleta4),
                        600.0, 900.0, 15, 8
                    ),
                    Producto(
                        "straps-deportivos1",
                        "Straps Deportivos",
                        listOf(R.drawable.straps1, R.drawable.straps2, R.drawable.straps3, R.drawable.straps4),
                        200.0, 400.0, 20, 10
                    )
                )

                // Guardar productos iniciales en Firebase
                productos.forEach { producto ->
                    val productoFirebase = ProductoFirebase(
                        id = producto.id,
                        nombre = producto.nombre,
                        imagenes = listOf(), // No guardamos los resource IDs
                        precioNormal = producto.precioNormal,
                        precioPremium = producto.precioPremium,
                        stockNormal = producto.stockNormal,
                        stockPremium = producto.stockPremium
                    )

                    // Usar coroutine scope para llamar a suspend function
                    CoroutineScope(Dispatchers.IO).launch {
                        firebaseManager.saveProducto(productoFirebase)
                    }
                }
            }

            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLogin = { username, password ->
                if ((username == "user123" && password == "123") || (username == "admin123" && password == "123")) {
                    isLoggedIn = true
                    isAdmin = username == "admin123"
                    userId = username
                }
            }
        )
    } else {
        if (isAdmin) {
            AdminView(
                productos = productos,
                todasLasImagenes = todasLasImagenes,
                onPriceChange = { id, newNormalPrice, newPremiumPrice, newStockNormal, newStockPremium ->
                    // Actualizar producto localmente
                    productos = productos.map { producto ->
                        if (producto.id == id) {
                            producto.copy(
                                precioNormal = newNormalPrice,
                                precioPremium = newPremiumPrice,
                                stockNormal = newStockNormal,
                                stockPremium = newStockPremium
                            )
                        } else {
                            producto
                        }
                    }

                    // Actualizar en Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        val updates = mapOf(
                            "precioNormal" to newNormalPrice,
                            "precioPremium" to newPremiumPrice,
                            "stockNormal" to newStockNormal,
                            "stockPremium" to newStockPremium
                        )
                        firebaseManager.updateProducto(id, updates)
                    }
                },
                onImageChange = { id, index, newImageId ->
                    // Only change predefined images, not custom ones
                    if (newImageId != -1) {
                        productos = productos.map { producto ->
                            if (producto.id == id) {
                                val nuevasImagenes = producto.imagenes.toMutableList()
                                if (index < nuevasImagenes.size) {
                                    nuevasImagenes[index] = newImageId
                                }
                                producto.copy(imagenes = nuevasImagenes)
                            } else {
                                producto
                            }
                        }
                    }
                },
                onCustomImageChange = { id, index, bitmap ->
                    // Store bitmap with unique key
                    val key = "${id}_${index}"
                    MainActivity.customBitmaps[key] = bitmap

                    // Guardar la imagen en Firebase como Base64
                    CoroutineScope(Dispatchers.IO).launch {
                        val base64Image = firebaseManager.uploadImage(bitmap)
                        val updates = mapOf("imagenesBase64/$key" to base64Image)
                        firebaseManager.updateProducto(id, updates)
                    }
                },
                onLogout = { isLoggedIn = false },
                context = context,
                firebaseManager = firebaseManager
            )
        } else {
            UserView(
                productos = productos,
                onLogout = { isLoggedIn = false },
                onCompra = { carrito ->
                    // Reducir el stock de los productos comprados localmente
                    productos = productos.map { producto ->
                        val itemCarrito = carrito[producto.id]
                        if (itemCarrito != null) {
                            when (itemCarrito.paquete) {
                                "Normal" -> producto.copy(stockNormal = producto.stockNormal - itemCarrito.cantidad)
                                "Premium" -> producto.copy(stockPremium = producto.stockPremium - itemCarrito.cantidad)
                                else -> producto
                            }
                        } else {
                            producto
                        }
                    }

                    // Actualizar stock en Firebase y registrar la venta
                    CoroutineScope(Dispatchers.IO).launch {
                        // Actualizar stock de cada producto
                        carrito.forEach { (id, item) ->
                            val producto = productos.find { it.id == id }
                            if (producto != null) {
                                val updates = when (item.paquete) {
                                    "Normal" -> mapOf("stockNormal" to producto.stockNormal)
                                    "Premium" -> mapOf("stockPremium" to producto.stockPremium)
                                    else -> mapOf<String, Any>()
                                }
                                firebaseManager.updateProducto(id, updates)
                            }
                        }

                        // Registrar la venta
                        val itemsVenta = carrito.values.map { item ->
                            ItemVenta(
                                productoId = item.id,
                                nombre = item.nombre,
                                paquete = item.paquete,
                                precio = item.precio,
                                cantidad = item.cantidad
                            )
                        }

                        val totalVenta = carrito.values.sumOf { it.precio * it.cantidad }

                        val venta = Venta(
                            userId = userId,
                            fecha = System.currentTimeMillis(),
                            items = itemsVenta,
                            total = totalVenta
                        )

                        firebaseManager.saveVenta(venta)
                    }
                },
                firebaseManager = firebaseManager
            )
        }
    }
}
@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (loginError) {
            Text(
                text = "Usuario o contraseña incorrectos. Intenta de nuevo.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Button(
            onClick = {
                if ((username == "user123" && password == "123") || (username == "admin123" && password == "123")) {
                    loginError = false
                    onLogin(username, password)
                } else {
                    loginError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEEB)) // Cambiar color a azul celeste
        ) {
            Text("Iniciar Sesión")
        }
    }
}

@Composable
fun AdminView(
    productos: List<Producto>,
    todasLasImagenes: List<Int>,
    onPriceChange: (String, Double, Double, Int, Int) -> Unit,
    onImageChange: (String, Int, Int) -> Unit,
    onCustomImageChange: (String, Int, Bitmap) -> Unit,
    onLogout: () -> Unit,
    context: Context,
    firebaseManager: FirebaseManager  // Added parameter
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Modo Administrador", fontSize = 24.sp, modifier = Modifier.padding(16.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEEB)) // Cambiar color a azul celeste
        ) {
            Text("Cerrar Sesión")
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
            items(productos) { producto ->
                AdminProductoItem(
                    producto = producto,
                    todasLasImagenes = todasLasImagenes,
                    onPriceChange = { newNormalPrice, newPremiumPrice, newStockNormal, newStockPremium ->
                        onPriceChange(
                            producto.id,
                            newNormalPrice,
                            newPremiumPrice,
                            newStockNormal,
                            newStockPremium
                        )
                    },
                    onImageChange = { index, newImageId ->
                        onImageChange(producto.id, index, newImageId)
                    },
                    onCustomImageChange = { index, bitmap ->
                        onCustomImageChange(producto.id, index, bitmap)
                    },
                    context = context
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminProductoItem(
    producto: Producto,
    todasLasImagenes: List<Int>,
    onPriceChange: (Double, Double, Int, Int) -> Unit,
    onImageChange: (Int, Int) -> Unit,
    onCustomImageChange: (Int, Bitmap) -> Unit,
    context: Context
) {
    var normalPrice by remember { mutableStateOf(producto.precioNormal.toString()) }
    var premiumPrice by remember { mutableStateOf(producto.precioPremium.toString()) }
    var stockNormal by remember { mutableStateOf(producto.stockNormal.toString()) }
    var stockPremium by remember { mutableStateOf(producto.stockPremium.toString()) }
    val pagerState = rememberPagerState(pageCount = { producto.imagenes.size })
    val coroutineScope = rememberCoroutineScope()
    var showImageSelector by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }

    // Función para abrir la galería y seleccionar una imagen
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(context, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                onCustomImageChange(selectedImageIndex, bitmap)
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al cargar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product ID and Title
            Text(
                text = "ID: ${producto.id}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Image Pager Section
            Box(modifier = Modifier.size(200.dp)) {
                HorizontalPager(state = pagerState) { page ->
                    val customKey = "${producto.id}_$page"
                    val customBitmap = MainActivity.customBitmaps[customKey]

                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = "Imagen personalizada de ${producto.nombre}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    showImageSelector = true
                                    selectedImageIndex = page
                                }
                        )
                    } else {
                        Image(
                            painter = painterResource(id = producto.imagenes[page]),
                            contentDescription = "Imagen de ${producto.nombre}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    showImageSelector = true
                                    selectedImageIndex = page
                                }
                        )
                    }
                }
                // Page Indicator
                Text(
                    text = "${pagerState.currentPage + 1}/${producto.imagenes.size}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp),
                    color = Color.White
                )
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Anterior")
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < producto.imagenes.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < producto.imagenes.size - 1
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Siguiente")
                }
            }

            // Image Selection Button
            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Seleccionar imagen desde galería")
            }

            // Price and Stock Inputs
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = normalPrice,
                    onValueChange = { normalPrice = it },
                    label = { Text("Precio Normal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = premiumPrice,
                    onValueChange = { premiumPrice = it },
                    label = { Text("Precio Premium") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockNormal,
                    onValueChange = { stockNormal = it },
                    label = { Text("Stock Normal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockPremium,
                    onValueChange = { stockPremium = it },
                    label = { Text("Stock Premium") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Update Button
            Button(
                onClick = {
                    val newNormalPrice = normalPrice.toDoubleOrNull()
                    val newPremiumPrice = premiumPrice.toDoubleOrNull()
                    val newStockNormal = stockNormal.toIntOrNull()
                    val newStockPremium = stockPremium.toIntOrNull()

                    if (newNormalPrice != null && newPremiumPrice != null &&
                        newStockNormal != null && newStockPremium != null) {
                        onPriceChange(newNormalPrice, newPremiumPrice, newStockNormal, newStockPremium)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Actualizar Precios y Stock")
            }
        }
    }

    // Image Selector Dialog (kept the same as before)
    if (showImageSelector) {
        AlertDialog(
            onDismissRequest = { showImageSelector = false },
            title = { Text("Seleccionar nueva imagen") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        val chunkedImages = todasLasImagenes.chunked(3)
                        chunkedImages.forEach { rowImages ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowImages.forEach { imagenId ->
                                    Image(
                                        painter = painterResource(id = imagenId),
                                        contentDescription = "Seleccionar imagen",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .padding(4.dp)
                                            .clickable {
                                                onImageChange(selectedImageIndex, imagenId)
                                                showImageSelector = false
                                            }
                                    )
                                }
                                repeat(3 - rowImages.size) {
                                    Spacer(modifier = Modifier.size(80.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImageSelector = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


@Composable
fun UserView(
    productos: List<Producto>,
    onLogout: () -> Unit,
    onCompra: (Map<String, ItemCarrito>) -> Unit,
    firebaseManager: FirebaseManager  // Added parameter
)  {
    var productoSeleccionado by rememberSaveable { mutableStateOf<Producto?>(null) }
    var carrito by rememberSaveable { mutableStateOf<Map<String, ItemCarrito>>(emptyMap()) }
    var mostrarCarrito by rememberSaveable { mutableStateOf(false) }
    var mostrarAlertaStock by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Tienda GymLex", fontSize = 24.sp, modifier = Modifier.padding(16.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEEB)) // Cambiar color a azul celeste
        ) {
            Text("Cerrar Sesión")
        }
        Button(
            onClick = { mostrarCarrito = true },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEEB)) // Cambiar color a azul celeste
        ) {
            Text("Ver Carrito (${carrito.values.sumOf { it.cantidad }})")
        }

        if (mostrarAlertaStock) {
            AlertDialog(
                onDismissRequest = { mostrarAlertaStock = false },
                title = { Text("Stock insuficiente") },
                text = { Text("Algunos productos no tienen suficiente stock. Por favor, ajusta las cantidades o elimina artículos.") },
                confirmButton = {
                    Button(
                        onClick = { mostrarAlertaStock = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEEB)) // Cambiar color a azul celeste
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }

        if (mostrarCarrito) {
            CarritoView(
                carrito = carrito,
                productos = productos,
                onBack = { mostrarCarrito = false },
                onCompra = {
                    // Validar stock antes de comprar
                    val hayStockSuficiente = carrito.all { (id, item) ->
                        val producto = productos.find { it.id == id }
                        when (item.paquete) {
                            "Normal" -> (producto?.stockNormal ?: 0) >= item.cantidad
                            "Premium" -> (producto?.stockPremium ?: 0) >= item.cantidad
                            else -> false
                        }
                    }
                    if (hayStockSuficiente) {
                        onCompra(carrito)
                        // NO vaciar el carrito ni ocultar la vista aquí
                    } else {
                        mostrarAlertaStock = true
                    }
                },
                onCompraFinalizada = {
                    carrito = emptyMap() // Vaciar el carrito después de cerrar el diálogo
                    mostrarCarrito = false // Ocultar CarritoView después de cerrar el diálogo
                },
                onEliminar = { id ->
                    carrito = carrito - id
                },
                onCambiarCantidad = { id, nuevaCantidad ->
                    carrito = carrito.mapValues { (itemId, item) ->
                        if (itemId == id) {
                            item.copy(cantidad = nuevaCantidad)
                        } else {
                            item
                        }
                    }
                }
            )
        } else if (productoSeleccionado == null) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(productos) { producto ->
                    ProductoItem(producto) { productoSeleccionado = producto }
                }
            }
        } else {
            ProductoView(
                producto = productoSeleccionado!!,
                onBack = { productoSeleccionado = null },
                onAgregarAlCarrito = { item ->
                    val existingItem = carrito[item.id]
                    if (existingItem != null) {
                        carrito = carrito + (item.id to existingItem.copy(cantidad = existingItem.cantidad + 1))
                    } else {
                        carrito = carrito + (item.id to item.copy(cantidad = 1))
                    }
                }
            )
        }
    }
}

@Composable
fun CarritoView(
    carrito: Map<String, ItemCarrito>,
    productos: List<Producto>,
    onBack: () -> Unit,
    onCompra: () -> Unit,
    onCompraFinalizada: () -> Unit,
    onEliminar: (String) -> Unit,
    onCambiarCantidad: (String, Int) -> Unit
) {
    val costoTotal = carrito.values.sumOf { it.precio * it.cantidad }
    var mostrarMensajeCompra by remember { mutableStateOf(false) } // Estado para controlar la visibilidad del mensaje

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("Volver")
        }
        Text(text = "Carrito", fontSize = 24.sp, modifier = Modifier.padding(16.dp))

        carrito.forEach { (id, item) ->
            val producto = productos.find { it.id == id }
            val totalIndividual = item.precio * item.cantidad
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.nombre} - ${item.paquete} - $${item.precio} (x${item.cantidad})",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Total: $$totalIndividual",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Button(onClick = { onEliminar(id) }) {
                    Text("Eliminar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = item.cantidad.toString(),
                    onValueChange = { nuevaCantidad ->
                        val cantidad = nuevaCantidad.toIntOrNull() ?: 1
                        onCambiarCantidad(id, cantidad)
                    },
                    modifier = Modifier.width(60.dp)
                )
            }
        }

        Text(text = "Costo Total: $$costoTotal", fontSize = 18.sp, modifier = Modifier.padding(16.dp))
        Button(onClick = {
            onCompra() // Realizar la compra
            mostrarMensajeCompra = true // Mostrar el mensaje de felicitaciones
        }) {
            Text("Comprar")
        }
    }

    // Mostrar el AlertDialog si mostrarMensajeCompra es true
    if (mostrarMensajeCompra) {
        AlertDialog(
            onDismissRequest = {
                mostrarMensajeCompra = false
                onCompraFinalizada() // Llamar al callback después de cerrar el diálogo
            },
            title = { Text("Compra realizada exitosamente") },
            text = { Text("Felicidades, espera tus productos para lucir con estilo") },
            confirmButton = {
                Button(onClick = {
                    mostrarMensajeCompra = false
                    onCompraFinalizada() // Llamar al callback después de cerrar el diálogo
                }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

data class ItemCarrito(
    val id: String,
    val nombre: String,
    val paquete: String,
    val precio: Double,
    val cantidad: Int = 1
)

data class Producto(
    val id: String,
    val nombre: String,
    val imagenes: List<Int>,
    val precioNormal: Double,
    val precioPremium: Double,
    val stockNormal: Int,
    val stockPremium: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductoItem(producto: Producto, onClick: () -> Unit) {
    // Estado para el pager
    val pagerState = rememberPagerState(pageCount = { producto.imagenes.size })

    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HorizontalPager para mostrar las imágenes
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .size(150.dp)
                .padding(vertical = 8.dp)
        ) { page ->
            // Buscar la imagen personalizada (si existe)
            val customKey = "${producto.id}_$page"
            val customBitmap = MainActivity.customBitmaps[customKey]

            if (customBitmap != null) {
                // Mostrar imagen personalizada
                Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = "Imagen personalizada de ${producto.nombre}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Mostrar imagen predeterminada
                Image(
                    painter = painterResource(id = producto.imagenes[page]),
                    contentDescription = "Imagen de ${producto.nombre}",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Indicador de página actual
        Text(
            text = "${pagerState.currentPage + 1}/${producto.imagenes.size}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(text = producto.nombre, fontSize = 16.sp, modifier = Modifier.padding(4.dp))
        Text(text = "Desde: $${producto.precioNormal}", fontSize = 14.sp)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Stock Normal: ${producto.stockNormal}",
                fontSize = 12.sp,
                color = when {
                    producto.stockNormal > 10 -> MaterialTheme.colorScheme.primary
                    producto.stockNormal > 5 -> MaterialTheme.colorScheme.secondary
                    producto.stockNormal > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = "Stock Premium: ${producto.stockPremium}",
                fontSize = 12.sp,
                color = when {
                    producto.stockPremium > 10 -> MaterialTheme.colorScheme.primary
                    producto.stockPremium > 5 -> MaterialTheme.colorScheme.secondary
                    producto.stockPremium > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductoView(
    producto: Producto,
    onBack: () -> Unit,
    onAgregarAlCarrito: (ItemCarrito) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { producto.imagenes.size })
    var precio by remember { mutableStateOf<Double?>(null) }
    var paqueteSeleccionado by remember { mutableStateOf<String?>(null) }

    var grados by remember { mutableFloatStateOf(0f) }
    var isRotating by remember { mutableStateOf(false) }

    LaunchedEffect(isRotating) {
        if (isRotating) {
            while (grados < 360f) {
                grados += 5f
                delay(16)
            }
            grados = 0f
            isRotating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack, modifier = Modifier.padding(8.dp)) { Text("Volver") }

        HorizontalPager(state = pagerState, modifier = Modifier.size(250.dp)) { page ->
            Box(
                modifier = Modifier
                    .clickable {
                        isRotating = true
                    }
            ) {
                val customKey = "${producto.id}_$page"
                val customBitmap = MainActivity.customBitmaps[customKey]

                if (customBitmap != null) {
                    // Display custom bitmap
                    Image(
                        bitmap = customBitmap.asImageBitmap(),
                        contentDescription = "Imagen personalizada de ${producto.nombre}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = grados
                            }
                    )
                } else {
                    // Display resource image
                    Image(
                        painter = painterResource(id = producto.imagenes[page]),
                        contentDescription = "Imagen de ${producto.nombre}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = grados
                            }
                    )
                }
            }
        }

        Text(text = producto.nombre, fontSize = 20.sp, modifier = Modifier.padding(8.dp))

        // Stock display with color-coded availability
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Stock Normal: ${producto.stockNormal}",
                fontSize = 16.sp,
                color = when {
                    producto.stockNormal > 10 -> MaterialTheme.colorScheme.primary
                    producto.stockNormal > 5 -> MaterialTheme.colorScheme.secondary
                    producto.stockNormal > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = "Stock Premium: ${producto.stockPremium}",
                fontSize = 16.sp,
                color = when {
                    producto.stockPremium > 10 -> MaterialTheme.colorScheme.primary
                    producto.stockPremium > 5 -> MaterialTheme.colorScheme.secondary
                    producto.stockPremium > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }

        Row(modifier = Modifier.padding(8.dp)) {
            Button(
                onClick = {
                    precio = producto.precioNormal
                    paqueteSeleccionado = "Normal"
                },
                enabled = producto.stockNormal > 0
            ) {
                Text("Paquete Normal")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    precio = producto.precioPremium
                    paqueteSeleccionado = "Premium"
                },
                enabled = producto.stockPremium > 0
            ) {
                Text("Paquete Premium")
            }
        }

        precio?.let {
            Text(text = "Precio: $$it", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = {
                if (precio != null && paqueteSeleccionado != null) {
                    val stockSuficiente = when(paqueteSeleccionado) {
                        "Normal" -> producto.stockNormal > 0
                        "Premium" -> producto.stockPremium > 0
                        else -> false
                    }

                    if (stockSuficiente) {
                        onAgregarAlCarrito(ItemCarrito(producto.id, producto.nombre, paqueteSeleccionado!!, precio!!))
                        onBack()
                    }
                }
            },
            enabled = precio != null &&
                    ((paqueteSeleccionado == "Normal" && producto.stockNormal > 0) ||
                            (paqueteSeleccionado == "Premium" && producto.stockPremium > 0))
        ) {
            Text("Agregar al Carrito")
        }
    }
}
