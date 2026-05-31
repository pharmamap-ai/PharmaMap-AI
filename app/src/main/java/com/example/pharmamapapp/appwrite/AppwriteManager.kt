package com.example.pharmamapapp.appwrite

import android.content.Context
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.InputFile
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import com.example.pharmamapapp.model.Medicine
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppwriteManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var client: Client
        private set
    lateinit var account: Account
        private set
    lateinit var databases: Databases
        private set
    lateinit var storage: Storage
        private set

    fun init(context: Context) {
        client = Client(context.applicationContext)
            .setProject(AppwriteConfig.APPWRITE_PROJECT_ID)
            .setEndpoint(AppwriteConfig.APPWRITE_PUBLIC_ENDPOINT)

        account = Account(client)
        databases = Databases(client)
        storage = Storage(client)
    }

    // ── Auth ──

    fun login(email: String, password: String, callback: AuthCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    account.createEmailPasswordSession(email, password)
                }
                val user = withContext(Dispatchers.IO) { account.get() }
                callback.onSuccess(UserProfile(user.id, user.name, user.email, user.emailVerification))
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, email: String, password: String, callback: AuthCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    account.create(ID.unique(), email, password, name)
                    account.createEmailPasswordSession(email, password)
                }
                val user = withContext(Dispatchers.IO) { account.get() }
                callback.onSuccess(UserProfile(user.id, user.name, user.email, user.emailVerification))
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Registration failed")
            }
        }
    }

    fun getCurrentUser(callback: UserCallback) {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) { account.get() }
                callback.onSuccess(UserProfile(user.id, user.name, user.email, user.emailVerification))
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Not logged in")
            }
        }
    }

    fun logout(callback: SimpleCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { account.deleteSession("current") }
                callback.onSuccess()
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Logout failed")
            }
        }
    }

    // ── Profile ──

    fun updateName(name: String, callback: SimpleCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { account.updateName(name) }
                callback.onSuccess()
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to update name")
            }
        }
    }

    fun updatePrefs(prefs: Map<String, Any>, callback: SimpleCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { account.updatePrefs(prefs) }
                callback.onSuccess()
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to update preferences")
            }
        }
    }

    fun getPrefs(callback: PrefsCallback) {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) { account.get() }
                callback.onSuccess(user.prefs.data)
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to load preferences")
            }
        }
    }

    // ── Medicines CRUD ──

    private val medicinePermissions = listOf(
        Permission.read(Role.users()),
        Permission.update(Role.users()),
        Permission.delete(Role.users()),
    )

    fun addMedicine(medicine: Medicine, callback: MedicineCallback) {
        scope.launch {
            try {
                val doc = withContext(Dispatchers.IO) {
                    databases.createDocument(
                        databaseId = AppwriteConfig.DATABASE_ID,
                        collectionId = AppwriteConfig.COLLECTION_MEDICINES,
                        documentId = ID.unique(),
                        data = medicine.toMap(),
                        permissions = medicinePermissions,
                    )
                }
                callback.onSuccess(Medicine.fromDocument(doc))
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to add medicine")
            }
        }
    }

    fun updateMedicine(medicine: Medicine, callback: MedicineCallback) {
        scope.launch {
            try {
                val doc = withContext(Dispatchers.IO) {
                    databases.updateDocument(
                        databaseId = AppwriteConfig.DATABASE_ID,
                        collectionId = AppwriteConfig.COLLECTION_MEDICINES,
                        documentId = medicine.id,
                        data = medicine.toMap(),
                    )
                }
                callback.onSuccess(Medicine.fromDocument(doc))
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to update medicine")
            }
        }
    }

    fun deleteMedicine(medicineId: String, callback: SimpleCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    databases.deleteDocument(
                        databaseId = AppwriteConfig.DATABASE_ID,
                        collectionId = AppwriteConfig.COLLECTION_MEDICINES,
                        documentId = medicineId,
                    )
                }
                callback.onSuccess()
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to delete medicine")
            }
        }
    }

    fun getAllMedicines(callback: MedicineListCallback) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    databases.listDocuments(
                        databaseId = AppwriteConfig.DATABASE_ID,
                        collectionId = AppwriteConfig.COLLECTION_MEDICINES,
                        queries = listOf(Query.limit(500), Query.orderDesc("\$createdAt")),
                    )
                }
                val medicines = result.documents.map { Medicine.fromDocument(it) }
                callback.onSuccess(medicines)
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to load medicines")
            }
        }
    }

    fun updateQuantity(medicineId: String, newQuantity: Int, callback: SimpleCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    databases.updateDocument(
                        databaseId = AppwriteConfig.DATABASE_ID,
                        collectionId = AppwriteConfig.COLLECTION_MEDICINES,
                        documentId = medicineId,
                        data = mapOf("quantity" to newQuantity),
                    )
                }
                callback.onSuccess()
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to update quantity")
            }
        }
    }

    // ── Storage ──

    fun uploadFile(file: File, callback: FileUploadCallback) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    storage.createFile(
                        bucketId = AppwriteConfig.BUCKET_IMAGES,
                        fileId = ID.unique(),
                        file = InputFile.fromFile(file),
                        permissions = listOf(
                            Permission.read(Role.users()),
                            Permission.update(Role.users()),
                            Permission.delete(Role.users()),
                        ),
                    )
                }
                callback.onSuccess(result.id)
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to upload file")
            }
        }
    }

    fun deleteFile(fileId: String, callback: SimpleCallback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    storage.deleteFile(
                        bucketId = AppwriteConfig.BUCKET_IMAGES,
                        fileId = fileId,
                    )
                }
                callback.onSuccess()
            } catch (e: AppwriteException) {
                callback.onError(e.message ?: "Failed to delete file")
            }
        }
    }

    fun getFilePreviewUrl(fileId: String): String {
        if (fileId.isEmpty()) return ""
        return "${AppwriteConfig.APPWRITE_PUBLIC_ENDPOINT}/storage/buckets/${AppwriteConfig.BUCKET_IMAGES}/files/$fileId/preview?project=${AppwriteConfig.APPWRITE_PROJECT_ID}&width=400&height=400"
    }

    fun downloadFile(fileId: String, callback: FileDownloadCallback) {
        if (fileId.isEmpty()) { callback.onError("No file ID"); return }
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    storage.getFileView(
                        bucketId = AppwriteConfig.BUCKET_IMAGES,
                        fileId = fileId,
                    )
                }
                callback.onSuccess(bytes)
            } catch (e: AppwriteException) {
                android.util.Log.e("AppwriteManager", "Download failed for $fileId: ${e.message}")
                callback.onError(e.message ?: "Failed to download file")
            }
        }
    }

    // ── Callbacks ──

    interface AuthCallback {
        fun onSuccess(profile: UserProfile)
        fun onError(message: String)
    }

    interface UserCallback {
        fun onSuccess(profile: UserProfile)
        fun onError(message: String)
    }

    interface SimpleCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    interface MedicineCallback {
        fun onSuccess(medicine: Medicine)
        fun onError(message: String)
    }

    interface MedicineListCallback {
        fun onSuccess(medicines: List<Medicine>)
        fun onError(message: String)
    }

    interface PrefsCallback {
        fun onSuccess(prefs: Map<String, @JvmSuppressWildcards Any>)
        fun onError(message: String)
    }

    interface FileUploadCallback {
        fun onSuccess(fileId: String)
        fun onError(message: String)
    }

    interface FileDownloadCallback {
        fun onSuccess(bytes: ByteArray)
        fun onError(message: String)
    }
}
