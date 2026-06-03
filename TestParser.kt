import java.io.File

fun main() {
    val content = File("test.m3u").readText()
    val entries = content.split("#EXTINF:")
    println("Entries count: ${entries.size}")
    
    var validCount = 0
    for (i in 1 until minOf(entries.size, 10)) {
        val entry = entries[i].trim()
        if (entry.isEmpty()) continue
        
        val commaIndex = entry.indexOf(',')
        if (commaIndex == -1) continue
        
        val attributesPart = entry.substring(0, commaIndex)
        val nameAndUrlPart = entry.substring(commaIndex + 1).trim()
        
        // Extract group-title
        val groupRegex = """group-title="(.*?)"""".toRegex()
        val currentGroup = groupRegex.find(attributesPart)?.groups?.get(1)?.value?.trim() ?: "Uncategorized"
        
        var currentName = ""
        var streamUrl = ""
        
        val httpIndex = nameAndUrlPart.indexOf("http://")
        val httpsIndex = nameAndUrlPart.indexOf("https://")
        
        val urlStart = when {
            httpIndex != -1 && httpsIndex != -1 -> minOf(httpIndex, httpsIndex)
            httpIndex != -1 -> httpIndex
            httpsIndex != -1 -> httpsIndex
            else -> -1
        }
        
        if (urlStart != -1) {
            val rawName = nameAndUrlPart.substring(0, urlStart).trim()
            currentName = rawName.split("\n").firstOrNull { !it.trim().startsWith("#") }?.trim() ?: rawName
            
            val rawUrl = nameAndUrlPart.substring(urlStart).trim()
            streamUrl = rawUrl.split("\\s+".toRegex()).firstOrNull() ?: rawUrl
        }
        
        println("Channel: $currentName | URL: $streamUrl | Group: $currentGroup")
        validCount++
    }
    println("Parsed $validCount channels.")
}
