package com.example.pharmamapapp.appwrite

/**
 * Appwrite configuration.
 *
 * SETUP INSTRUCTIONS - Create these in Appwrite Console:
 *
 * 1. Create Database: ID = "pharmamap_db"
 * 2. Create Collection: ID = "medicines"
 * 3. Add these attributes to the "medicines" collection:
 *    - name        (string, size 256, required)
 *    - code        (string, size 128)
 *    - price       (float, required)
 *    - quantity    (integer, required)
 *    - shelf       (string, size 64)
 *    - category    (string, size 128)
 *    - expiry_date (string, size 32)
 *    - manufacturer(string, size 256)
 *    - description (string, size 1024)
 *    - min_stock   (integer)
 *    - notes       (string, size 512)
 * 4. Set Collection Permissions:
 *    Role: Users → Create, Read, Update, Delete
 * 5. Enable Auth → Email/Password in Auth Settings
 */
object AppwriteConfig {
    const val APPWRITE_PROJECT_ID = "6a1c0de6001d75c4f9ab"
    const val APPWRITE_PROJECT_NAME = "PharmaMap"
    const val APPWRITE_PUBLIC_ENDPOINT = "https://fra.cloud.appwrite.io/v1"

    @Suppress("unused")
    const val APPWRITE_API_KEY =
        "standard_61089e6858789ea4fde4e18002ba00f32be2a6a9a6e7f9b8e8a92aa0f7502de658c676262a8b69958b0cb632fb3bacd2fd0ca5ccd5ff1be4bae700098cb83620a1826933f083722d1c59844994f71d86f172fff23ebe723dd9539eb461291f3e8b630d8851443fdc0da0dfca08a2239f93309d939a4e60443685f108c3a3ca42"

    const val DATABASE_ID = "pharmamap_db"
    const val COLLECTION_MEDICINES = "medicines"
    const val BUCKET_IMAGES = "images"
}
