package com.example.tiendaapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirebaseManager {
    private val database = Firebase.database.reference

    // Guardar un producto en Firebase
    suspend fun saveProducto(producto: ProductoFirebase) {
        try {
            database.child("productos").child(producto.id).setValue(producto).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al guardar producto: ${e.message}")
        }
    }

    // Actualizar un producto en Firebase
    suspend fun updateProducto(id: String, updates: Map<String, Any>) {
        try {
            database.child("productos").child(id).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al actualizar producto: ${e.message}")
        }
    }

    // Obtener todos los productos
    fun getProductos(callback: (List<ProductoFirebase>) -> Unit) {
        database.child("productos").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productos = mutableListOf<ProductoFirebase>()
                for (productoSnapshot in snapshot.children) {
                    val producto = productoSnapshot.getValue(ProductoFirebase::class.java)
                    producto?.let { productos.add(it) }
                }
                callback(productos)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseManager", "Error al obtener productos: ${error.message}")
                callback(emptyList())
            }
        })
    }

    // Guardar una venta
    suspend fun saveVenta(venta: Venta) {
        try {
            val ventaId = UUID.randomUUID().toString()
            database.child("ventas").child(ventaId).setValue(venta).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al guardar venta: ${e.message}")
        }
    }

    // Subir imagen como Base64
    fun uploadImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val imageData = baos.toByteArray()
        return Base64.encodeToString(imageData, Base64.DEFAULT)
    }

    // Descargar imagen desde Base64
    fun downloadImage(base64: String): Bitmap? {
        return try {
            val imageData = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al descargar imagen: ${e.message}")
            null
        }
    }
}

// Modelo para productos en Firebase
data class ProductoFirebase(
    val id: String = "",
    val nombre: String = "",
    val imagenes: List<String> = emptyList(), // URLs o base64 de las imágenes
    val imagenesBase64: Map<String, String> = emptyMap(), // Para imágenes personalizadas
    val precioNormal: Double = 0.0,
    val precioPremium: Double = 0.0,
    val stockNormal: Int = 0,
    val stockPremium: Int = 0
)

// Modelo para ventas
data class Venta(
    val userId: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val items: List<ItemVenta> = emptyList(),
    val total: Double = 0.0
)

data class ItemVenta(
    val productoId: String = "",
    val nombre: String = "",
    val paquete: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 0
)
