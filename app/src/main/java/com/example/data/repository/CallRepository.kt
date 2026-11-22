package com.example.data.repository

import com.example.data.model.CallDirection
import com.example.data.model.CallHistoryEntry
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.IceCandidateData
import com.example.data.model.UserProfile
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CallRepository(private val db: FirebaseDatabase) {

    private val callsRef = db.reference.child("calls")
    private val userCallsRef = db.reference.child("userCalls")
    private val callHistoryRef = db.reference.child("callHistory")
    private val notificationsRef = db.reference.child("notifications")

    suspend fun startCall(caller: UserProfile, callee: UserProfile, isVideo: Boolean): Result<CallSession> {
        return try {
            val callId = callsRef.push().key
                ?: return Result.failure(Exception("Could not create call ID"))
            val now = System.currentTimeMillis()
            val session = CallSession(
                callId = callId,
                callerId = caller.uid,
                callerName = caller.bestDisplayName(),
                callerPhoto = caller.photoUrl,
                calleeId = callee.uid,
                calleeName = callee.bestDisplayName(),
                calleePhoto = callee.photoUrl,
                video = isVideo,
                status = CallStatus.RINGING.name,
                createdAt = now
            )
            callsRef.child(callId).setValue(session).await()
            userCallsRef.child(callee.uid).child(callId).setValue(session).await()
            notificationsRef.child(callee.uid).child("call_$callId").setValue(
                mapOf(
                    "type" to "incoming_call",
                    "callId" to callId,
                    "title" to session.callerName,
                    "isVideo" to isVideo,
                    "timestamp" to now
                )
            )
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeIncomingCalls(uid: String): Flow<CallSession?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val ref = userCallsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val ringing = snapshot.children.mapNotNull { it.getValue(CallSession::class.java) }
                    .firstOrNull {
                        it.status == CallStatus.RINGING.name && (now - it.createdAt) < 90_000L
                    }
                trySend(ringing)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeCallSession(callId: String): Flow<CallSession?> = callbackFlow {
        val ref = callsRef.child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(CallSession::class.java))
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun setOfferSdp(callId: String, sdp: String) {
        callsRef.child(callId).child("offerSdp").setValue(sdp).await()
    }

    suspend fun setAnswerSdp(callId: String, sdp: String) {
        callsRef.child(callId).updateChildren(
            mapOf(
                "answerSdp" to sdp,
                "status" to CallStatus.ACCEPTED.name,
                "startedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateCallStatus(call: CallSession, status: CallStatus) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>("status" to status.name)
        if (status == CallStatus.ENDED || status == CallStatus.REJECTED || status == CallStatus.MISSED) {
            updates["endedAt"] = now
        }
        callsRef.child(call.callId).updateChildren(updates)
        userCallsRef.child(call.calleeId).child(call.callId).removeValue()
    }

    fun pushIceCandidate(callId: String, isCaller: Boolean, candidate: IceCandidateData) {
        val branch = if (isCaller) "callerCandidates" else "calleeCandidates"
        callsRef.child(callId).child(branch).push().setValue(candidate)
    }

    fun observeRemoteIceCandidates(callId: String, isCaller: Boolean): Flow<IceCandidateData> = callbackFlow {
        val remoteBranch = if (isCaller) "calleeCandidates" else "callerCandidates"
        val ref = callsRef.child(callId).child(remoteBranch)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(IceCandidateData::class.java)?.let { trySend(it) }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun logCallHistory(
        currentUid: String,
        peerId: String,
        peerName: String,
        peerPhoto: String,
        isVideo: Boolean,
        direction: CallDirection,
        durationSeconds: Long
    ) {
        try {
            val id = callHistoryRef.child(currentUid).push().key ?: System.currentTimeMillis().toString()
            val entry = CallHistoryEntry(
                id = id,
                peerId = peerId,
                peerName = peerName,
                peerPhoto = peerPhoto,
                video = isVideo,
                direction = direction.name,
                durationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis()
            )
            callHistoryRef.child(currentUid).child(id).setValue(entry).await()
        } catch (_: Exception) {
        }
    }

    fun observeCallHistory(uid: String): Flow<List<CallHistoryEntry>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = callHistoryRef.child(uid).orderByChild("timestamp").limitToLast(100)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(CallHistoryEntry::class.java) }
                    .sortedByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
