package com.example.kfueityaps.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseConfig {
    const val SUPABASE_URL = "https://tnyjoineilqazzjdahls.supabase.co"
    const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRueWpvaW5laWxxYXp6amRhaGxzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY1OTIwNzUsImV4cCI6MjA4MjE2ODA3NX0.Z6nYlL9xtGALTVjRbwYQ1UzctS88GKtyGtWriS2xULw"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            httpEngine = OkHttp.create()
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}
