package com.example.pharmamapapp.model

import io.appwrite.models.Document
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Medicine(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val shelf: String = "",
    val category: String = "",
    val expiryDate: String = "",
    val manufacturer: String = "",
    val description: String = "",
    val minStock: Int = 5,
    val notes: String = "",
    val imageId: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
) : Serializable {

    val isLowStock: Boolean
        get() = quantity <= minStock

    val isExpired: Boolean
        get() {
            if (expiryDate.isEmpty()) return false
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expiry = sdf.parse(expiryDate) ?: return false
                expiry.before(Calendar.getInstance().time)
            } catch (_: Exception) { false }
        }

    val isExpiringSoon: Boolean
        get() {
            if (expiryDate.isEmpty()) return false
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expiry = sdf.parse(expiryDate) ?: return false
                val now = Calendar.getInstance().time
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 30)
                expiry.after(now) && expiry.before(cal.time)
            } catch (_: Exception) { false }
        }

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "code" to code,
        "price" to price,
        "quantity" to quantity,
        "shelf" to shelf,
        "category" to category,
        "expiry_date" to expiryDate,
        "manufacturer" to manufacturer,
        "description" to description,
        "min_stock" to minStock,
        "notes" to notes,
        "image_id" to imageId,
    )

    companion object {
        fun fromDocument(doc: Document<Map<String, Any>>): Medicine {
            val d = doc.data
            return Medicine(
                id = doc.id,
                name = d["name"] as? String ?: "",
                code = d["code"] as? String ?: "",
                price = (d["price"] as? Number)?.toDouble() ?: 0.0,
                quantity = (d["quantity"] as? Number)?.toInt() ?: 0,
                shelf = d["shelf"] as? String ?: "",
                category = d["category"] as? String ?: "",
                expiryDate = d["expiry_date"] as? String ?: "",
                manufacturer = d["manufacturer"] as? String ?: "",
                description = d["description"] as? String ?: "",
                minStock = (d["min_stock"] as? Number)?.toInt() ?: 5,
                notes = d["notes"] as? String ?: "",
                imageId = d["image_id"] as? String ?: "",
                createdAt = doc.createdAt,
                updatedAt = doc.updatedAt,
            )
        }
    }
}
