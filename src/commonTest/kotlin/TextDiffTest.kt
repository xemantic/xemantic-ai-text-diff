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

import com.xemantic.kotlin.test.assert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * These tests are verifying that the [diff] function output is correct.
 * The [diff] function compares the original and the revised strings passed as parameters
 * and returns a description of differences.
 *
 * The output format is intended to be as easy as possible to process
 * by the Large Language Model (LLM) like Claude from Anthropic.
 *
 * Test cases are using various formats, like Markdown or HTML, however the difference
 * logic and produced description should be format agnostic by principle.
 */
class TextDiffTest {

    @Test
    fun `should have empty result if strings are the same`() {
        assert(diff(original = "foo", revised = "foo").isEmpty())
    }

    /**
     * This test case shows the LLM analyzing the code how to interpret trailing whitespaces
     * in multiline strings in Kotlin. Basically the top and the bottom triple quotes are discarded.
     */
    @Test
    fun `should have empty result if multiline string is the same as simple string`() {
        assert(
            diff(
                original = """
                    foo
                """.trimIndent(),
                revised = "foo"
            ).isEmpty()
        )
    }

    @Test
    fun `should report if strings are different`() = assertDifference(
        original = "foo",
        revised = "bar",
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ foo
            └─ differs from revised
            │ bar
            └─ differences
            │ • line 1:
            | [-f-][+b+][-o-][+a+][-o-][+r+]
            └─
        """.trimIndent()
    )

    @Test
    fun `should demonstrate ordered reporting of changes`() = assertDifference(
        original = """
            Initial unchanged line
            This line will change
            This block of lines
            will be removed entirely
            and replaced with new content
            Line with minor change
            Another unchanged line
            This will be modified
            Block to remove
            starts here
            ends here
            Final unchanged line
        """.trimIndent(),
        revised = """
            Initial unchanged line
            This line has changed
            Completely new block
            of content appears
            right here
            Line with minor Edit
            Another unchanged line
            This has been modified
            Additional content
            at the end
            Final unchanged line
        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ Initial unchanged line
            │ This line will change
            │ This block of lines
            │ will be removed entirely
            │ and replaced with new content
            │ Line with minor change
            │ Another unchanged line
            │ This will be modified
            │ Block to remove
            │ starts here
            │ ends here
            │ Final unchanged line
            └─ differs from revised
            │ Initial unchanged line
            │ This line has changed
            │ Completely new block
            │ of content appears
            │ right here
            │ Line with minor Edit
            │ Another unchanged line
            │ This has been modified
            │ Additional content
            │ at the end
            │ Final unchanged line
            └─ differences
            │ • line 2:
            | This line [-w-][+h+][-i-][+a+][-l-][+s+][-l-] change[+d+]
            │ • removed line 3:
            | This block of lines
            | will be removed entirely
            | and replaced with new content
            │ • added after line 2:
            | Completely new block
            | of content appears
            | right here
            │ • line 6:
            | Line with minor [-c-][+E+][-h-][+d+][-a-][+i+][-n-][+t+][-g-][-e-]
            │ • line 8:
            | This [-w-][+h+][-i-][+a+][-l-][+s+][-l-] be[+e+][+n+]modified
            │ • removed line 9:
            | Block to remove
            | starts here
            | ends here
            │ • added after line 8:
            | Additional content
            | at the end
            └─
        """.trimIndent()
    )

    @Test
    fun `should show differences in trailing whitespace`() = assertDifference(
        original = """
            Line with no space
            Line with one space 
            Line with two spaces  
            No newline at the end
        """.trimIndent(),
        revised = """
            Line with no space
            Line with one space
            Line with two spaces
            No newline at the end

        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ Line with no space
            │ Line with one space 
            │ Line with two spaces  
            │ No newline at the end
            └─ differs from revised
            │ Line with no space
            │ Line with one space
            │ Line with two spaces
            │ No newline at the end
            │ 
            └─ differences
            │ • line 2:
            | Line with one space[- -]
            │ • line 3:
            | Line with two spaces[- -][- -]
            │ • added after line 4:
            | 
            └─
        """.trimIndent()
    )

    @Test
    fun `should fail even if only whitespaces are different`() = assertDifference(
        original = """
            <div>
                <p>Hello</p>
            </div>
        """.trimIndent(),
        revised = """
            <div>
               <p>Hello</p>
            </div>
        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ <div>
            │     <p>Hello</p>
            │ </div>
            └─ differs from revised
            │ <div>
            │    <p>Hello</p>
            │ </div>
            └─ differences
            │ • line 2:
            |    [- -]<p>Hello</p>
            └─
        """.trimIndent()
    )

    @Test
    fun `should handle line additions and removals`() = assertDifference(
        original = """
            First line
            Second line
            Line to be removed
            Third line
            Another line to remove
            Fourth line
            Fifth line
        """.trimIndent(),
        revised = """
            First line
            Second line
            Third line
            New line here
            Fourth line
            Another new line
            Fifth line
            Added at the end
        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ First line
            │ Second line
            │ Line to be removed
            │ Third line
            │ Another line to remove
            │ Fourth line
            │ Fifth line
            └─ differs from revised
            │ First line
            │ Second line
            │ Third line
            │ New line here
            │ Fourth line
            │ Another new line
            │ Fifth line
            │ Added at the end
            └─ differences
            │ • removed line 3:
            | Line to be removed
            │ • added after line 3:
            | New line here
            │ • removed line 5:
            | Another line to remove
            │ • added after line 5:
            | Another new line
            │ • added after line 7:
            | Added at the end
            └─
        """.trimIndent()
    )

    @Test
    fun `should fail if multiline strings are different`() = assertDifference(
        original = """
            # Heading

            This is a paragraph
            with two lines.

            * List item 1
            * List item 2
        """.trimIndent(),
        revised = """
            # Heading

            This is paragraph
            with 2 lines.

            * List item 1
            * List item three
        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ # Heading
            │ 
            │ This is a paragraph
            │ with two lines.
            │ 
            │ * List item 1
            │ * List item 2
            └─ differs from revised
            │ # Heading
            │ 
            │ This is paragraph
            │ with 2 lines.
            │ 
            │ * List item 1
            │ * List item three
            └─ differences
            │ • line 3:
            | This is [-a-][- -]paragraph
            │ • line 4:
            | with [-t-][+2+][-w-][-o-] lines.
            │ • line 7:
            | * List item [-2-][+t+][+h+][+r+][+e+][+e+]
            └─
        """.trimIndent()
    )

    @Test
    fun `should handle complex differences`() = assertDifference(
        original = """
            <!DOCTYPE html>
            <html>
              <head>
                <title>Test Page</title>
                <script>
                  // Old script to be removed
                  console.log("old");
                </script>
              </head>
              <body>
                <div class="container">
                  <h1>Hello World</h1>
                  <p>This is a test.</p>
                  <p>Another paragraph to remove.</p>
                </div>
              </body>
            </html>
        """.trimIndent(),
        revised = """
            <!DOCTYPE html>
            <html>
              <head>
                <title>Test Page</title>
                <meta charset="utf-8">
              </head>
              <body>
                <div class="main-container">
                  <h1>Hello, World!</h1>
                  <p>This is a test.</p>
                </div>
                <footer>
                  Copyright (c) Xemantic
                </footer>
              </body>
            </html>
        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ <!DOCTYPE html>
            │ <html>
            │   <head>
            │     <title>Test Page</title>
            │     <script>
            │       // Old script to be removed
            │       console.log("old");
            │     </script>
            │   </head>
            │   <body>
            │     <div class="container">
            │       <h1>Hello World</h1>
            │       <p>This is a test.</p>
            │       <p>Another paragraph to remove.</p>
            │     </div>
            │   </body>
            │ </html>
            └─ differs from revised
            │ <!DOCTYPE html>
            │ <html>
            │   <head>
            │     <title>Test Page</title>
            │     <meta charset="utf-8">
            │   </head>
            │   <body>
            │     <div class="main-container">
            │       <h1>Hello, World!</h1>
            │       <p>This is a test.</p>
            │     </div>
            │     <footer>
            │       Copyright (c) Xemantic
            │     </footer>
            │   </body>
            │ </html>
            └─ differences
            │ • removed line 5:
            |     <script>
            |       // Old script to be removed
            |       console.log("old");
            |     </script>
            │ • added after line 4:
            │     <meta charset="utf-8">
            │ • line 8:
            |     <div class="[+m+][+a+][+i+][+n+][+-+]container">
            │ • line 9:
            |       <h1>Hello[+,+] World[+!+]</h1>
            │ • removed line 13:
            |       <p>Another paragraph to remove.</p>
            │ • added after line 11:
            |     <footer>
            |       Copyright (c) Xemantic
            |     </footer>
            └─
        """.trimIndent()
    )

    @Test
    fun `should handle differences in markdown content`() = assertDifference(
        original = """
            # Main Heading

            ## Sub-heading

            This is a paragraph with *italic* and
            **bold** text. It continues on the
            next line.

            * List item 1
            * List item 2

            > This is a blockquote
            > with multiple lines

            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent(),
        revised = """
            # Main Heading

            ## Sub-heading

            This is a paragraph with _italic_ and
            __bold__ text. It continues on the
            next line.

            * List item 1
            * List item 2

            > This is a blockquote
            > with multiple lines.

            ```kotlin
            fun main() {
              println("Hello");
            }
            ```
        """.trimIndent(),
        difference = """
            Text comparison failed:
            Format description:
            • [-d-] shows deleted character
            • [+a+] shows added character
            • Spaces are marked explicitly in changes
            • Changes are shown character by character
            • Multiple line additions are presented as complete lines
            • Changes are reported in line number order with related changes grouped together

            ┌─ original
            │ # Main Heading
            │ 
            │ ## Sub-heading
            │ 
            │ This is a paragraph with *italic* and
            │ **bold** text. It continues on the
            │ next line.
            │ 
            │ * List item 1
            │ * List item 2
            │ 
            │ > This is a blockquote
            │ > with multiple lines
            │ 
            │ ```kotlin
            │ fun main() {
            │     println("Hello")
            │ }
            │ ```
            └─ differs from revised
            │ # Main Heading
            │ 
            │ ## Sub-heading
            │ 
            │ This is a paragraph with _italic_ and
            │ __bold__ text. It continues on the
            │ next line.
            │ 
            │ * List item 1
            │ * List item 2
            │ 
            │ > This is a blockquote
            │ > with multiple lines.
            │ 
            │ ```kotlin
            │ fun main() {
            │   println("Hello");
            │ }
            │ ```
            └─ differences
            │ • line 5:
            | This is a paragraph with [-*-][+_+]italic[-*-][+_+] and
            │ • line 6:
            | [-*-][+_+][-*-][+_+]bold[-*-][+_+][-*-][+_+] text. It continues on the
            │ • line 13:
            | > with multiple lines[+.+]
            │ • line 17:
            |   [- -][- -]println("Hello")[+;+]
            └─
        """.trimIndent()
    )

    private fun assertDifference(
        original: String,
        revised: String,
        difference: String
    ) {
        val result = diff(original, revised)
        assertEquals(difference, result)
        if (result != difference) {
            fail("The actual difference message was:\n$result")
        }
    }
}