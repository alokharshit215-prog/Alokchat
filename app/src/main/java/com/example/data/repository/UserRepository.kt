package com.example.data.repository

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.example.data.model.DeviceContact
import com.example.data.model.PrivacyOption
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.UserProfile
import com.example.util.FormatUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(private val db: FirebaseDatabase) {

    private val usersRef = db.reference.child("users")
    private val usernamesRef = db.reference.child("usernames")
    private val phoneIndexRef = db.reference.child("phoneIndex")
    private val blockedRef = db.reference.child("blockedUsers")
    private val reportsRef = db.reference.child("reports")
    private val contactsRef = db.reference.child("contacts")

    fun observeUser(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(UserProfile::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeUser cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getUser(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            usersRef.child(uid).get().await().getValue(UserProfile::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "getUser failed: ${e.message}")
            null
        }
    }

    suspend fun ensureUserRecord(
        uid: String,
        email: String = "",
        phone: String = ""
    ): Result<UserProfile> {
        return try {
            val snap = usersRef.child(uid).get().await()
            val existing = snap.getValue(UserProfile::class.java)
            if (existing != null) {
                val updates = mutableMapOf<String, Any>()
                if (email.isNotBlank() && existing.email != email) {
                    updates["email"] = email
                }
                if (phone.isNotBlank() && existing.phone != phone) {
                    updates["phone"] = phone
                }
                if (updates.isNotEmpty()) {
                    usersRef.child(uid).updateChildren(updates).await()
                }
                val effectivePhone = phone.ifBlank { existing.phone }
                val phoneKey = FormatUtils.sanitizePhoneKey(effectivePhone)
                if (phoneKey.isNotBlank()) {
                    phoneIndexRef.child(phoneKey).setValue(uid).await()
                }
                return Result.success(
                    existing.copy(
                        email = email.ifBlank { existing.email },
                        phone = effectivePhone
                    )
                )
            }
            val now = System.currentTimeMillis()
            val emailPrefix = email.substringBefore("@")
                .lowercase()
                .replace(Regex("[^a-z0-9._]"), "")
            val defaultUsername = if (emailPrefix.length >= 3) {
                "${emailPrefix.take(12)}_${uid.take(4).lowercase()}"
            } else {
                "user_${uid.take(6).lowercase()}"
            }
            val profile = UserProfile(
                uid = uid,
                email = email,
                phone = phone,
                username = defaultUsername,
                displayName = "",
                about = "Hey there! I am using Alok Chat.",
                photoUrl = "",
                online = true,
                lastSeen = now,
                createdAt = now
            )
            usersRef.child(uid).setValue(profile).await()
            usernamesRef.child(defaultUsername).setValue(uid).await()
            val phoneKey = FormatUtils.sanitizePhoneKey(phone)
            if (phoneKey.isNotBlank()) {
                phoneIndexRef.child(phoneKey).setValue(uid).await()
            }
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "ensureUserRecord failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        uid: String,
        displayName: String,
        username: String,
        about: String,
        phone: String? = null,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9._]"), "")
            require(cleanUsername.length >= 3) { "Username must be at least 3 characters." }
            require(displayName.trim().isNotEmpty()) { "Display name cannot be empty." }

            val current = getUser(uid)
            val existingOwner = usernamesRef.child(cleanUsername).get().await().getValue(String::class.java)
            if (existingOwner != null && existingOwner != uid) {
                return Result.failure(Exception("Username @$cleanUsername is already taken."))
            }

            if (current != null && current.username.isNotBlank() && current.username != cleanUsername) {
                usernamesRef.child(current.username).removeValue().await()
            }
            usernamesRef.child(cleanUsername).setValue(uid).await()

            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim(),
                "username" to cleanUsername,
                "about" to about.trim().ifBlank { "Hey there! I am using Alok Chat." }
            )
            if (phone != null) {
                val trimmedPhone = phone.trim()
                val oldKey = FormatUtils.sanitizePhoneKey(current?.phone.orEmpty())
                val newKey = FormatUtils.sanitizePhoneKey(trimmedPhone)
                if (oldKey.isNotBlank() && oldKey != newKey) {
                    phoneIndexRef.child(oldKey).removeValue().await()
                }
                if (newKey.isNotBlank()) {
                    phoneIndexRef.child(newKey).setValue(uid).await()
                }
                updates["phone"] = trimmedPhone
            }
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }
            usersRef.child(uid).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile failed", e)
            Result.failure(e)
        }
    }

    suspend fun updatePhotoUrl(uid: String, photoUrl: String): Result<Unit> {
        return try {
            usersRef.child(uid).child("photoUrl").setValue(photoUrl).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhoneInProfile(uid: String, oldPhone: String, newPhone: String): Result<Unit> {
        return try {
            val oldKey = FormatUtils.sanitizePhoneKey(oldPhone)
            val newKey = FormatUtils.sanitizePhoneKey(newPhone)
            if (oldKey.isNotBlank() && oldKey != newKey) {
                phoneIndexRef.child(oldKey).removeValue().await()
            }
            if (newKey.isNotBlank()) {
                phoneIndexRef.child(newKey).setValue(uid).await()
            }
            usersRef.child(uid).child("phone").setValue(newPhone).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        uid: String,
        privacyPhoto: PrivacyOption,
        privacyLastSeen: PrivacyOption,
        privacyOnline: PrivacyOption,
        privacyStatus: PrivacyOption,
        readReceiptsEnabled: Boolean
    ): Result<Unit> {
        return try {
            usersRef.child(uid).updateChildren(
                mapOf(
                    "privacyPhoto" to privacyPhoto.name,
                    "privacyLastSeen" to privacyLastSeen.name,
                    "privacyOnline" to privacyOnline.name,
                    "privacyStatus" to privacyStatus.name,
                    "readReceiptsEnabled" to readReceiptsEnabled
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatusPrivacySettings(
        uid: String,
        mode: StatusPrivacyMode,
        excludedUids: Set<String>,
        allowedUids: Set<String>
    ): Result<Unit> {
        return try {
            val legacyOption = when (mode) {
                StatusPrivacyMode.EVERYONE -> PrivacyOption.EVERYONE
                StatusPrivacyMode.MY_CONTACTS, StatusPrivacyMode.MY_CONTACTS_EXCEPT -> PrivacyOption.CONTACTS
                StatusPrivacyMode.ONLY_SHARE_WITH -> PrivacyOption.NOBODY
            }
            usersRef.child(uid).updateChildren(
                mapOf(
                    "privacyStatus" to legacyOption.name,
                    "statusPrivacyMode" to mode.name,
                    "statusExcludedUids" to excludedUids.associateWith { true },
                    "statusAllowedUids" to allowedUids.associateWith { true }
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startPresenceTracking(uid: String): ValueEventListener? {
        if (uid.isBlank()) return null
        val connectedRef = db.getReference(".info/connected")
        val userRef = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userRef.child("online").onDisconnect().setValue(false)
                    userRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
                    userRef.child("online").setValue(true)
                    userRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Presence listener cancelled: ${error.message}")
            }
        }
        connectedRef.addValueEventListener(listener)
        return listener
    }

    fun stopPresenceTracking(uid: String, listener: ValueEventListener?) {
        if (listener != null) {
            db.getReference(".info/connected").removeEventListener(listener)
        }
        if (uid.isNotBlank()) {
            usersRef.child(uid).child("online").setValue(false)
            usersRef.child(uid).child("lastSeen").setValue(System.currentTimeMillis())
        }
    }

    fun syncFcmToken(uid: String) {
        if (uid.isBlank()) return
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    usersRef.child(uid).child("fcmToken").setValue(token)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM token fetch skipped: ${e.message}")
        }
    }

    suspend fun clearFcmToken(uid: String) {
        if (uid.isBlank()) return
        try {
            usersRef.child(uid).child("fcmToken").setValue("").await()
            usersRef.child(uid).child("online").setValue(false).await()
            usersRef.child(uid).child("lastSeen").setValue(System.currentTimeMillis()).await()
        } catch (_: Exception) {
        }
    }

    suspend fun searchRegisteredUsers(query: String, currentUid: String): List<UserProfile> {
        val clean = query.trim().lowercase().removePrefix("@")
        if (clean.isBlank()) return emptyList()
        return try {
            val snap = usersRef.get().await()
            snap.children.mapNotNull { it.getValue(UserProfile::class.java) }
                .filter { user ->
                    user.uid != currentUid && (
                        user.username.lowercase().contains(clean) ||
                            user.displayName.lowercase().contains(clean) ||
                            user.email.lowercase().contains(clean) ||
                            user.phone.replace(" ", "").contains(clean)
                        )
                }
                .take(40)
        } catch (e: Exception) {
            Log.e(TAG, "searchRegisteredUsers failed", e)
            emptyList()
        }
    }

    suspend fun listAllOtherUsers(currentUid: String): List<UserProfile> {
        return try {
            val snap = usersRef.get().await()
            snap.children.mapNotNull { it.getValue(UserProfile::class.java) }
                .filter { it.uid != currentUid }
                .sortedBy { it.bestDisplayName().lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun findRegisteredContacts(context: Context, currentUid: String): List<UserProfile> =
        withContext(Dispatchers.IO) {
            val deviceNumbers = mutableSetOf<String>()
            try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val raw = cursor.getString(idx) ?: continue
                        val normalized = FormatUtils.normalizePhoneE164(raw)
                        if (normalized.isNotBlank()) deviceNumbers.add(normalized)
                        val last10 = raw.replace(Regex("[^0-9]"), "").takeLast(10)
                        if (last10.length == 10) deviceNumbers.add(last10)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reading contacts failed: ${e.message}")
            }

            if (deviceNumbers.isEmpty()) return@withContext emptyList()
            val allUsers = listAllOtherUsers(currentUid)
            val matched = allUsers.filter { user ->
                val userPhone = FormatUtils.sanitizePhoneKey(user.phone)
                val userLast10 = userPhone.replace(Regex("[^0-9]"), "").takeLast(10)
                userPhone in deviceNumbers || (userLast10.length == 10 && userLast10 in deviceNumbers)
            }
            matched.forEach { contact ->
                contactsRef.child(currentUid).child(contact.uid).setValue(true)
            }
            matched
        }

    suspend fun readDeviceContactsForSharing(context: Context): List<DeviceContact> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<DeviceContact>()
            val seen = mutableSetOf<String>()
            try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        val num = cursor.getString(numIdx) ?: continue
                        val key = "$name|$num"
                        if (seen.add(key)) {
                            list.add(DeviceContact(name = name, phone = num))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "readDeviceContacts failed: ${e.message}")
            }
            list
        }

    fun observeSavedContactUids(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val ref = contactsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { child ->
                    if (child.getValue(Boolean::class.java) == true) child.key else null
                }.toSet()
                trySend(ids)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeBlockedUsers(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val ref = blockedRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }.toSet()
                trySend(ids)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun setUserBlocked(currentUid: String, targetUid: String, block: Boolean): Result<Unit> {
        return try {
            if (block) {
                blockedRef.child(currentUid).child(targetUid).setValue(true).await()
            } else {
                blockedRef.child(currentUid).child(targetUid).removeValue().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(reporterUid: String, targetUid: String, reason: String): Result<Unit> {
        return try {
            val id = reportsRef.push().key ?: System.currentTimeMillis().toString()
            reportsRef.child(id).setValue(
                mapOf(
                    "reporterId" to reporterUid,
                    "targetId" to targetUid,
                    "reason" to reason,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "UserRepository"
    }
}
