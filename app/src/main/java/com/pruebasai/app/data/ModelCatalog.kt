package com.pruebasai.app.data

/**
 * Catálogo de modelos de IA locales listos para descargar desde Hugging Face.
 *
 * Todos los enlaces apuntan a archivos `.litertlm` (formato oficial de LiteRT-LM)
 * alojados en la organización `litert-community`, sin registro ni token.
 */
data class ModelEntry(
    val id: String,
    val name: String,
    val description: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val badge: String? = null,
)

object ModelCatalog {

    val models: List<ModelEntry> = listOf(
        ModelEntry(
            id = "gemma3-270m",
            name = "Gemma 3 270M (q8)",
            description = "Modelo diminuto para probar la app en cualquier móvil. Respuestas simples y muy rápidas.",
            fileName = "gemma3-270m-it-q8.litertlm",
            url = "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.litertlm",
            sizeBytes = 304_005_120L,
            badge = "Para empezar",
        ),
        ModelEntry(
            id = "qwen25-1_5b",
            name = "Qwen 2.5 1.5B Instruct (q8)",
            description = "Gran equilibrio entre calidad y tamaño. Muy buen español y multilingüe.",
            fileName = "Qwen2.5-1.5B-Instruct_q8.litertlm",
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeBytes = 1_597_931_520L,
            badge = "Buen español",
        ),
        ModelEntry(
            id = "gemma4-e2b",
            name = "Gemma 4 E2B",
            description = "El recomendado: última generación de Gemma para dispositivos. Funciona con CPU y GPU.",
            fileName = "gemma-4-E2B-it.litertlm",
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            sizeBytes = 2_588_147_712L,
            badge = "Recomendado",
        ),
        ModelEntry(
            id = "gemma4-e2b-gpu",
            name = "Gemma 4 E2B (GPU)",
            description = "Variante optimizada para aceleración GPU (más ligera y veloz en GPUs compatibles).",
            fileName = "gemma-4-E2B-it-gpu.litertlm",
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm",
            sizeBytes = 2_008_432_640L,
        ),
        ModelEntry(
            id = "gemma4-e4b",
            name = "Gemma 4 E4B",
            description = "Máxima calidad de la lista. Más lento y pesado; ideal si tu móvil tiene 8 GB+ de RAM.",
            fileName = "gemma-4-E4B-it.litertlm",
            url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            sizeBytes = 3_659_530_240L,
            badge = "Máxima calidad",
        ),
    )

    /** Extrae el nombre de archivo de una URL personalizada. */
    fun fileNameFromUrl(url: String): String? {
        val cleaned = url.trim().substringBefore("?").substringBefore("#")
        val name = cleaned.substringAfterLast("/")
        return if (name.isNotBlank() &&
            (name.endsWith(".litertlm") || name.endsWith(".task"))
        ) name else null
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "?"
        val kb = 1024.0
        return when {
            bytes >= kb * kb * kb -> String.format("%.2f GB", bytes / (kb * kb * kb))
            bytes >= kb * kb -> String.format("%.0f MB", bytes / (kb * kb))
            else -> String.format("%.0f KB", bytes / kb)
        }
    }
}
