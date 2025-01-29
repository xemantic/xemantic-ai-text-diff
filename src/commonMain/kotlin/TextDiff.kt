/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.kotlin.test.compare

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.DeltaType

/**
 * Compares two strings and returns a formatted string describing their differences.
 * The output is designed to be easily processable by Large Language Models (LLMs)
 * like Claude from Anthropic.
 *
 * If strings are equal, returns an empty string. Otherwise returns a formatted
 * description of differences including:
 * 1. Format description
 * 2. Original text
 * 3. Revised text
 * 4. Detailed differences with line numbers and character-by-character changes
 *
 * @param original The original string to compare
 * @param revised The revised string to compare against
 * @return Formatted string describing differences or empty string if texts are equal
 */
public fun diff(
    original: String,
    revised: String,
): String {
    if (original == revised) return ""

    return buildString {
        append("""
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

        """.trimIndent())

        append("\n┌─ original\n")
        original.lines().forEach { line ->
            append("│ $line\n")
        }
        append("└─ differs from revised\n")
        revised.lines().forEach { line ->
            append("│ $line\n")
        }
        append("└─ differences\n")

        val origLines = original.lines()
        val revLines = revised.lines()

        // First find all line-level changes
        val lineDiff = diff(origLines, revLines)
        val changes = mutableListOf<Change>()
        var currentIndex = 0

        // Process each delta
        lineDiff.deltas.forEach { delta ->
            val sourcePosition = delta.source.position + 1 // Convert to 1-based line numbers
            val sourceLines = delta.source.lines
            val targetLines = delta.target.lines

            when (delta.type) {
                DeltaType.CHANGE -> {
                    // Process each line pair for character-level differences
                    val maxLines = maxOf(sourceLines.size, targetLines.size)
                    for (i in 0 until maxLines) {
                        val originalLine = sourceLines.getOrNull(i) ?: ""
                        val revisedLine = targetLines.getOrNull(i) ?: ""
                        
                        if (shouldShowCharacterDiff(originalLine, revisedLine)) {
                            changes.add(Change.Modified(sourcePosition + i, originalLine, revisedLine))
                        } else {
                            if (sourceLines.isNotEmpty()) {
                                changes.add(Change.Removed(sourcePosition + i, listOf(originalLine)))
                            }
                            if (targetLines.isNotEmpty()) {
                                changes.add(Change.Added(sourcePosition + i, listOf(revisedLine)))
                            }
                        }
                    }
                }
                DeltaType.DELETE -> {
                    changes.add(Change.Removed(sourcePosition, sourceLines))
                }
                DeltaType.INSERT -> {
                    changes.add(Change.Added(sourcePosition, targetLines))
                }
                else -> {} // EQUAL - no changes needed
            }
            currentIndex++
        }

        // Sort changes by line number first, then preserve order for related changes
        val sortedChanges = changes.sortedBy { change ->
            when (change) {
                is Change.Modified -> change.lineNumber
                is Change.Removed -> change.lineNumber
                is Change.Added -> change.lineNumber
            }
        }

        // Output changes in order
        sortedChanges.forEach { change ->
            when (change) {
                is Change.Modified -> {
                    append("│ • line ${change.lineNumber}:\n")
                    append("| ${buildCharacterDiff(change.originalLine, change.revisedLine)}\n")
                }
                is Change.Removed -> {
                    append("│ • removed line ${change.lineNumber}:\n")
                    change.lines.forEach { line ->
                        append("| $line\n")
                    }
                }
                is Change.Added -> {
                    append("│ • added after line ${change.lineNumber}:\n")
                    change.lines.forEach { line ->
                        append("| $line\n")
                    }
                }
            }
        }

        append("└─")
    }
}

private sealed class Change {
    data class Modified(
        val lineNumber: Int,
        val originalLine: String,
        val revisedLine: String
    ) : Change()

    data class Removed(
        val lineNumber: Int,
        val lines: List<String>
    ) : Change()

    data class Added(
        val lineNumber: Int,
        val lines: List<String>
    ) : Change()
}

private fun buildCharacterDiff(original: String, revised: String): String {
    if (original == revised) return original

    // Convert strings to lists of single characters
    val originalChars = original.map { it.toString() }
    val revisedChars = revised.map { it.toString() }

    val charDiff = diff(
        source = originalChars,
        target = revisedChars,
        includeEqualParts = true
    )

    val result = StringBuilder()
    var originalIndex = 0
    var revisedIndex = 0

    charDiff.deltas.forEach { delta ->
        // Add unchanged characters before this delta
        while (originalIndex < delta.source.position) {
            result.append(originalChars[originalIndex])
            originalIndex++
        }

        when (delta.type) {
            DeltaType.EQUAL -> {
                delta.source.lines.forEach { char ->
                    result.append(char)
                    originalIndex++
                    revisedIndex++
                }
            }
            DeltaType.DELETE, DeltaType.INSERT, DeltaType.CHANGE -> {
                // Treat all changes as potential replacements
                val sourceChars = delta.source.lines
                val targetChars = delta.target.lines
                val maxLen = maxOf(sourceChars.size, targetChars.size)

                // Pair deletions with insertions where possible
                for (i in 0 until maxLen) {
                    if (i < sourceChars.size) {
                        result.append("[-").append(sourceChars[i]).append("-]")
                        originalIndex++
                    }
                    if (i < targetChars.size) {
                        result.append("[+").append(targetChars[i]).append("+]")
                        revisedIndex++
                    }
                }
            }
            else -> {}
        }
    }

    // Add any remaining unchanged characters
    while (originalIndex < originalChars.size) {
        result.append(originalChars[originalIndex])
        originalIndex++
    }

    return result.toString()
}

private fun shouldShowCharacterDiff(original: String, revised: String): Boolean {
    // For very short strings, always show character diff
    if (original.length < 5 || revised.length < 5) return true
    
    // Calculate similarity using string matching
    val similarity = calculateSimilarity(original, revised)
    
    // Show character diff if strings are somewhat similar
    return similarity > 0.5
}

private fun calculateSimilarity(s1: String, s2: String): Double {
    val longer = if (s1.length > s2.length) s1 else s2
    val shorter = if (s1.length > s2.length) s2 else s1
    
    if (longer.isEmpty()) return 1.0
    
    // Count matching characters
    var matches = 0
    var start = 0
    for (c in shorter) {
        val index = longer.indexOf(c, start)
        if (index != -1) {
            matches++
            start = index + 1
        }
    }
    
    return matches.toDouble() / longer.length
}