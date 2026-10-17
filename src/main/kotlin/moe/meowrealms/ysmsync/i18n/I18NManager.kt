package moe.meowrealms.ysmsync.i18n

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class I18NManager {
    private val loadedLanguageKeys: MutableMap<String, String> = ConcurrentHashMap()

    @Throws(IOException::class)
    fun loadLanguageFile(languageName: String) {
        val languageFileInStream = this.javaClass.classLoader
            .getResourceAsStream("lang/$languageName.lang")
            ?: throw IOException("Language file not found for $languageName!")

        loadedLanguageKeys.clear()

        BufferedReader(InputStreamReader(languageFileInStream, StandardCharsets.UTF_8)).use { lineReader ->
            var languageLine: String?

            while (lineReader.readLine().also { languageLine = it } != null) {
                val line = languageLine ?: continue
                if (line.isBlank() || line.startsWith("#")) {
                    continue
                }

                val languageLineSplit = line.split("=", limit = 2)
                if (languageLineSplit.size == 2) {
                    this.loadedLanguageKeys[languageLineSplit[0]] = languageLineSplit[1]
                    continue
                }

                throw IllegalArgumentException("Invalid language file format $line!")
            }
        }
    }

    fun i18n(
        key: String,
        subKeys: List<String> = emptyList(),
        args: List<Any> = emptyList()
    ): Component {
        if (subKeys.size != args.size) {
            throw IllegalArgumentException("Subkeys and args must be the same length")
        }

        val languageValue = this.loadedLanguageKeys[key]
            ?: throw IllegalArgumentException("Language key not found: $key")

        val builtResolvers = ArrayList<TagResolver>()
        for (idx in args.indices) {
            val arg = args[idx]
            builtResolvers.add(
                Placeholder.component(
                    subKeys[idx],
                    arg as? Component ?: Component.text(arg.toString())
                )
            )
        }

        return MiniMessage.miniMessage().deserialize(languageValue, *builtResolvers.toTypedArray())
    }
}
