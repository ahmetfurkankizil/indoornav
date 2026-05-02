import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

fun main() {
    val dotenv = Dotenv.configure().directory("tools/admin-api").load()
    val apiKey = dotenv["ANTHROPIC_API_KEY"]
    
    if (apiKey == null) {
        println("ERROR: ANTHROPIC_API_KEY not found in tools/admin-api/.env")
        return
    }
    
    println("Key found (len=${apiKey.length})")
    
    val client = HttpClient(Java)
    
    runBlocking {
        try {
            val response = client.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "model": "claude-3-5-sonnet-20241022",
                        "max_tokens": 10,
                        "messages": [{"role": "user", "content": "Hello"}]
                    }
                """.trimIndent())
            }
            println("Status: ${response.status}")
            println("Body: ${response.bodyAsText()}")
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
