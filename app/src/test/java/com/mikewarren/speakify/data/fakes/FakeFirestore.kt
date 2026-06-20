package com.mikewarren.speakify.data.fakes

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk

class FakeFirestore {
    private val data = mutableMapOf<String, Any?>()
    val mock: FirebaseFirestore = mockk(relaxed = true)

    init {
        every { mock.collection(any()) } answers {
            createMockCollection(firstArg())
        }
        every { mock.enableNetwork() } returns Tasks.forResult(null)
    }

    private fun createMockCollection(collectionPath: String): CollectionReference {
        val collectionMock = mockk<CollectionReference>(relaxed = true)
        
        every { collectionMock.document(any()) } answers {
            createMockDocument("$collectionPath/${firstArg<String>()}")
        }

        every { collectionMock.get() } answers {
            Tasks.forResult(createMockQuerySnapshot(collectionPath))
        }

        return collectionMock
    }

    private fun createMockDocument(documentPath: String): DocumentReference {
        val documentMock = mockk<DocumentReference>(relaxed = true)
        val docId = documentPath.substringAfterLast("/")
        
        every { documentMock.id } returns docId
        every { documentMock.path } returns documentPath

        every { documentMock.collection(any()) } answers {
            createMockCollection("$documentPath/${firstArg<String>()}")
        }

        every { documentMock.get() } answers {
            Tasks.forResult(createMockDocumentSnapshot(documentPath))
        }

        every { documentMock.set(any()) } answers {
            data[documentPath] = firstArg()
            Tasks.forResult(null)
        }

        every { documentMock.delete() } answers {
            deleteInternal(documentPath)
            Tasks.forResult(null)
        }

        return documentMock
    }

    private fun deleteInternal(documentPath: String) {
        data.remove(documentPath)
        // Also remove subcollections (simplified)
        data.keys.toList().forEach { key ->
            if (key.startsWith("$documentPath/")) {
                data.remove(key)
            }
        }
    }

    private fun createMockDocumentSnapshot(path: String): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        val docData = data[path]
        val docId = path.substringAfterLast("/")
        
        every { snapshot.exists() } returns (docData != null)
        every { snapshot.id } returns docId
        every { snapshot.reference } returns createMockDocument(path)
        
        every { snapshot.get(any<String>()) } answers {
            val field = firstArg<String>()
            if (docData is Map<*, *>) {
                docData[field]
            } else {
                null
            }
        }
        
        every { snapshot.getString(any()) } answers { snapshot.get(firstArg<String>()) as? String }
        every { snapshot.getBoolean(any()) } answers { snapshot.get(firstArg<String>()) as? Boolean }
        every { snapshot.getLong(any()) } answers { (snapshot.get(firstArg<String>()) as? Number)?.toLong() }
        
        every { snapshot.toObject(any<Class<Any>>()) } answers {
            docData
        }
        
        return snapshot
    }

    private fun createMockQuerySnapshot(collectionPath: String): QuerySnapshot {
        val snapshot = mockk<QuerySnapshot>(relaxed = true)
        // Only get direct children of this collection
        val documents = data.keys
            .filter { it.startsWith("$collectionPath/") && it.substringAfter("$collectionPath/").count { char -> char == '/' } == 0 }
            .map { createMockDocumentSnapshot(it) }
        
        every { snapshot.documents } returns documents
        return snapshot
    }

    fun setData(path: String, value: Any?) {
        data[path] = value
    }

    fun getData(path: String): Any? = data[path]

    fun dump() {
        println("FakeFirestore Dump:")
        data.forEach { (path, value) ->
            println("  $path: $value")
        }
    }
    
    fun hasDocument(path: String): Boolean = data.containsKey(path)
}
