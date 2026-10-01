package com.wayhat.waycore.ai

/**
 * Modelos locales de [litert-community](https://huggingface.co/litert-community)
 * en formato `.litertlm` para LiteRT-LM. Se descargan dentro de la app.
 */
enum class ModelCatalog(
    val repo: String,
    val fileName: String,
    val label: String,
    val size: String
) {
    GEMMA_270M(
        repo = "gemma-3-270m-it",
        fileName = "gemma3-270m-it-q8.litertlm",
        label = "Gemma 3 270M (rápida)",
        size = "304 MB"
    ),
    QWEN_1_5B(
        repo = "Qwen2.5-1.5B-Instruct",
        fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        label = "Qwen2.5 1.5B (español, ideal)",
        size = "1.6 GB"
    ),
    GEMMA_4_E2B(
        repo = "gemma-4-E2B-it-litert-lm",
        fileName = "gemma-4-E2B-it.litertlm",
        label = "Gemma 4 E2B (recomendada)",
        size = "2.59 GB"
    );

    /** URL oficial de descarga en Hugging Face. */
    val url: String
        get() = "https://huggingface.co/litert-community/$repo/resolve/main/$fileName"
}
