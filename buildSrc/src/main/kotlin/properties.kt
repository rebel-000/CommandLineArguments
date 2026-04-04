import org.gradle.api.Project

fun Project.getPluginProperty(name: String): String = findProperty("plugin.$name") as? String ?: error("Property `$name` is not defined in gradle.properties")
fun Project.tryGetPluginProperty(name: String): String? = findProperty("plugin.$name") as? String

fun Project.ppString(name: String): String = getPluginProperty(name)
fun Project.ppList(name: String): List<String> = ppString(name).split(",").map(String::trim).filter(String::isNotEmpty)

fun Project.ppWithString(name: String, action: (String) -> Unit) {1
    tryGetPluginProperty(name)?.let { action(it)}
}

fun Project.ppWithList(name: String, action: (List<String>) -> Unit) {
    tryGetPluginProperty(name)?.let { action(it.split(",").map(String::trim).filter(String::isNotEmpty))}
}