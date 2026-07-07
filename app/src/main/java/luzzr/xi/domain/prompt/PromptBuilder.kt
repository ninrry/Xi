package luzzr.xi.domain.prompt

import luzzr.xi.domain.model.ThinkingLevel

object PromptBuilder {

    fun buildTranslatePrompt(sourceLang: String, targetLang: String, text: String): String = buildString {
        appendLine("You are a professional translator. Translate the following text from $sourceLang to $targetLang.")
        appendLine()
        appendLine("Rules:")
        appendLine("- Output ONLY valid JSON, no markdown, no explanations outside JSON")
        appendLine("- Preserve original formatting (line breaks, paragraphs)")
        appendLine("- Keep code/technical terms untranslated")
        appendLine("- Provide up to 3 alternative translations if ambiguous")
        appendLine()
        appendLine("Response format (strict JSON):")
        appendLine("""{"translation": "your translation here", "detected_language": "detected source language", "alternatives": ["alt1", "alt2"]}""")
        appendLine()
        appendLine("Text to translate:")
        appendLine(text)
    }

    fun buildEssayTextPrompt(text: String, thinkingLevel: ThinkingLevel): String = buildString {
        appendLine(getDepthInstruction(thinkingLevel))
        appendLine()
        append(essaySystemPrompt)
        appendLine()
        appendLine("Essay to correct:")
        appendLine(text)
    }

    fun buildEssayImagePrompt(thinkingLevel: ThinkingLevel): String = buildString {
        appendLine(getDepthInstruction(thinkingLevel))
        appendLine()
        append(essayImagePrompt)
    }

    fun buildEssayMultiImagePrompt(thinkingLevel: ThinkingLevel, imageCount: Int): String = buildString {
        appendLine(getDepthInstruction(thinkingLevel))
        appendLine()
        append(essayMultiImagePrompt(imageCount))
    }

    private fun getDepthInstruction(level: ThinkingLevel): String = when (level) {
        ThinkingLevel.LOW -> "Perform a basic correction. Fix only obvious grammar/spelling errors. Keep scoring brief."
        ThinkingLevel.MEDIUM -> "Perform a moderate correction. Fix grammar/spelling errors and suggest vocabulary improvements. Provide clear scoring."
        ThinkingLevel.HIGH -> "Perform a thorough, deep correction as a university English professor with 20 years experience. Analyze every aspect meticulously. Provide detailed explanations and comprehensive scoring."
    }

    private val essaySystemPrompt: String = buildString {
        appendLine("You are a university English professor with 20 years of experience grading essays.")
        appendLine()
        appendLine("Correct the following English essay. Score each dimension 0-25:")
        appendLine("- Grammar (spelling, punctuation, syntax)")
        appendLine("- Vocabulary (word choice, variety, precision)")
        appendLine("- Structure (organization, transitions, logical flow)")
        appendLine("- Style (sentence variety, tone, conciseness)")
        appendLine()
        appendLine("Response format (strict JSON):")
        appendLine("""{"grammar_errors":[{"original":"...","corrected":"...","explanation":"中文说明","line":1}],"vocabulary":[{"original":"...","suggested":"...","reason":"中文分析"}],"structure":{"organization":"中文分析","transitions":"中文分析","logical_flow":"中文分析"},"style":{"sentence_variety":"中文分析","tone":"中文分析","conciseness":"中文分析","academic_register":"中文分析"},"score":{"grammar":0,"vocabulary":0,"structure":0,"style":0,"total":0,"grade":""},"corrected_essay":"full corrected essay here","writing_tips":["中文建议1","中文建议2"]}}""")
        appendLine()
        appendLine("Important:")
        appendLine("- All analysis text must be in Chinese (中文)")
        appendLine("- If the input is not English, return: {\"error\":\"Input is not English\"}")
        appendLine("- Line numbers should refer to paragraph/line position")
    }

    private val essayImagePrompt: String = buildString {
        appendLine(essaySystemPrompt)
        appendLine()
        appendLine("STEP 1: Carefully read and transcribe ALL text visible in the image.")
        appendLine("STEP 2: Correct the transcribed essay using the JSON format above.")
        appendLine()
        appendLine("If the image is unreadable, blurry, or contains no English text, return: {\"error\":\"Unable to read image\"}")
    }

    private fun essayMultiImagePrompt(imageCount: Int): String = buildString {
        appendLine(essaySystemPrompt)
        appendLine()
        appendLine("You received $imageCount images of a multi-page document.")
        appendLine("STEP 1: Carefully read and combine ALL text from ALL images in order.")
        appendLine("STEP 2: Correct the complete combined essay using the JSON format above.")
        appendLine()
        appendLine("If any image is unreadable, return: {\"error\":\"Unable to read image\"}")
    }
}
