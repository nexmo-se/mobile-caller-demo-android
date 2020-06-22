package com.nexmo.mobilecallerdemo.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ApiService {
    companion object {
        const val TAG = "ApiService"

        const val HOST = "https://mobile-caller-demo.herokuapp.com"
    }

    fun registerToken(mobileNumber: String, token: String): String {
        val url = "$HOST/push/$mobileNumber/register"

        val bodyObject = JSONObject()
        bodyObject.put("token", token)
        val bodyJson = bodyObject.toString()

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val body = bodyJson.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    fun createSession(): String {
        val url = "$HOST/opentok/createSession"

        val bodyObject = JSONObject()
        val bodyJson = bodyObject.toString()

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val body = bodyJson.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    fun notifyCallee(from: String, to: String, sessionId: String): String {
        val url = "$HOST/opentok/notifyCallee"

        val bodyObject = JSONObject()
        bodyObject.put("from", from)
        bodyObject.put("to", to)
        bodyObject.put("sessionId", sessionId)
        val bodyJson = bodyObject.toString()

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val body = bodyJson.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    fun rejectCall(to: String): String {
        val url = "$HOST/opentok/reject"

        val bodyObject = JSONObject()
        bodyObject.put("to", to)
        val bodyJson = bodyObject.toString()

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val body = bodyJson.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }
}