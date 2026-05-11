package com.restonic4.ccpreprocess

/**
 * ConditionalCompilePreprocessor
 *
 * Processes Java source files and strips/keeps lines based on //CC directives.
 *
 * Supported syntax:
 *
 *   Single-line:
 *     //CC (condition)
 *     someCode();
 *
 *   Block:
 *     //CC (condition) {
 *     someCode();
 *     //CC}
 *
 *   Else-if / else chains:
 *     //CC (condition) {
 *     ...
 *     //CC} else if (condition2) {
 *     ...
 *     //CC} else {
 *     ...
 *     //CC}
 *
 * Condition variables:
 *   minecraft  - e.g. minecraft >= 1.20.1
 *   loader     - e.g. loader == forge | loader != fabric
 *   java       - e.g. java >= 17
 *
 * Operators: ==, !=, >=, <=, >, <
 * Logic:      and, or, not (can be nested with parentheses inside condition)
 */
class ConditionalCompilePreprocessor {

    final Map<String, String> context

    ConditionalCompilePreprocessor(Map<String, String> context) {
        this.context = context
    }

    /**
     * Preprocess a full Java source file string and return the result.
     * Lines that are excluded are replaced with blank lines to preserve line numbers
     * (useful for stack traces and IDE debugging).
     */
    String process(String source) {
        List<String> lines = source.readLines()
        List<String> output = new ArrayList<>(lines.size())

        // Stack entries: [active: bool, done: bool]
        //   active = currently including lines
        //   done   = a branch already matched (skip remaining else-if/else)
        Deque<Map> stack = new ArrayDeque<>()

        int i = 0
        while (i < lines.size()) {
            String raw = lines[i]
            String trimmed = raw.trim()

            // Block opener: //CC (condition) {
            if (trimmed ==~ /\/\/CC\s*\(.+\)\s*\{.*/) {
                String condStr = extractCondition(trimmed)
                boolean result = currentlyActive(stack) && evaluate(condStr)
                stack.push([active: result, done: result])
                output.add('')
                i++
                continue
            }

            // Block closer with else-if: //CC} else if (condition) {
            if (trimmed ==~ /\/\/CC\}\s*else\s+if\s*\(.+\)\s*\{.*/) {
                if (stack.isEmpty()) throw new IllegalStateException("//CC} else if without matching //CC{ at line ${i+1}")
                Map top = stack.peek()
                boolean parentActive = stack.size() > 1 ? currentlyActive(stack.tail()) : true
                if (!top.done && parentActive) {
                    String condStr = extractCondition(trimmed)
                    boolean result = evaluate(condStr)
                    top.active = result
                    top.done   = result
                } else {
                    top.active = false
                }
                output.add('')
                i++
                continue
            }

            // Block closer with else: //CC} else {
            if (trimmed ==~ /\/\/CC\}\s*else\s*\{.*/) {
                if (stack.isEmpty()) throw new IllegalStateException("//CC} else without matching //CC{ at line ${i+1}")
                Map top = stack.peek()
                boolean parentActive = stack.size() > 1 ? currentlyActive(stack.tail()) : true
                top.active = (!top.done) && parentActive
                output.add('')
                i++
                continue
            }

            // Block closer: //CC}
            if (trimmed ==~ /\/\/CC\}.*/) {
                if (stack.isEmpty()) throw new IllegalStateException("//CC} without matching //CC{ at line ${i+1}")
                stack.pop()
                output.add('')
                i++
                continue
            }

            // Single-line: //CC (condition)
            if (trimmed ==~ /\/\/CC\s*\(.+\)/) {
                String condStr = extractCondition(trimmed)
                boolean result = currentlyActive(stack) && evaluate(condStr)
                output.add('')
                i++
                if (i < lines.size()) {
                    output.add(result ? lines[i] : '')
                    i++
                }
                continue
            }

            // Normal line
            output.add(currentlyActive(stack) ? raw : '')
            i++
        }

        if (!stack.isEmpty()) {
            throw new IllegalStateException("Unclosed //CC{ block(s) detected. Make sure every //CC{ has a matching //CC}")
        }

        return output.join('\n')
    }

    // Helpers

    private boolean currentlyActive(Deque<Map> stack) {
        stack.isEmpty() || stack.every { it.active }
    }

    private boolean currentlyActive(Collection<Map> items) {
        items.isEmpty() || items.every { it.active }
    }

    /**
     * Extract the condition string from a //CC line.
     * Finds the outermost (...) pair, supporting nested parens like not(loader == fabric).
     *
     * Examples:
     *   //CC (minecraft >= 1.20.1)          → "minecraft >= 1.20.1"
     *   //CC (minecraft >= 1.20.1) {        → "minecraft >= 1.20.1"
     *   //CC} else if (minecraft >= 1.20.1) { → "minecraft >= 1.20.1"
     *   //CC (not(loader == fabric))        → "not(loader == fabric)"
     */
    private String extractCondition(String line) {
        int start = line.indexOf('(')
        if (start < 0) throw new IllegalArgumentException("Cannot parse condition from: $line")
        int depth = 0
        int end = -1
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i)
            if (c == '(' as char) depth++
            else if (c == ')' as char) {
                depth--
                if (depth == 0) { end = i; break }
            }
        }
        if (end < 0) throw new IllegalArgumentException("Unmatched parentheses in: $line")
        return line.substring(start + 1, end).trim()
    }

    /**
     * Evaluate a condition expression.
     * Handles: and, or, not(...), parentheses, and leaf comparisons.
     */
    boolean evaluate(String expr) {
        expr = expr.trim()

        // not(...)
        if (expr.startsWith('not(') && expr.endsWith(')')) {
            return !evaluate(expr.substring(4, expr.length() - 1))
        }

        // Split by top-level 'or'
        List<String> orParts = splitTopLevel(expr, 'or')
        if (orParts.size() > 1) {
            return orParts.any { evaluate(it.trim()) }
        }

        // Split by top-level 'and'
        List<String> andParts = splitTopLevel(expr, 'and')
        if (andParts.size() > 1) {
            return andParts.every { evaluate(it.trim()) }
        }

        // Strip surrounding parentheses
        if (expr.startsWith('(') && expr.endsWith(')')) {
            return evaluate(expr.substring(1, expr.length() - 1))
        }

        // Leaf comparison: variable op value
        return evaluateLeaf(expr)
    }

    /**
     * Split expr by keyword (and/or) only at the top level (not inside parens).
     */
    private List<String> splitTopLevel(String expr, String keyword) {
        List<String> parts = []
        int depth = 0
        int start = 0
        int len = expr.length()
        int klen = keyword.length()

        for (int i = 0; i < len; i++) {
            char c = expr.charAt(i)
            if (c == '(' as char) depth++
            else if (c == ')' as char) depth--
            else if (depth == 0 && i + klen <= len) {
                String candidate = expr.substring(i, i + klen).toLowerCase()
                if (candidate == keyword) {
                    boolean leftOk  = (i == 0)          || (expr.charAt(i - 1) == ' ' as char)
                    boolean rightOk = (i + klen >= len) || (expr.charAt(i + klen) == ' ' as char)
                    if (leftOk && rightOk) {
                        parts << expr.substring(start, i).trim()
                        start = i + klen
                        i = start - 1
                    }
                }
            }
        }
        parts << expr.substring(start).trim()
        return parts.size() > 1 ? parts : [expr]
    }

    /**
     * Evaluate a leaf expression like "minecraft >= 1.20.1" or "loader == forge"
     */
    private boolean evaluateLeaf(String expr) {
        // operators longest first to avoid partial matches on e.g. '>' vs '>='
        for (String op : ['>=', '<=', '!=', '==', '>', '<']) {
            int idx = expr.indexOf(op)
            if (idx < 0) continue

            String varName = expr.substring(0, idx).trim().toLowerCase()
            String rawValue = expr.substring(idx + op.length()).trim()

            String contextValue = context[varName]
            if (contextValue == null) {
                throw new IllegalArgumentException("Unknown CC variable '$varName'. Known variables: ${context.keySet()}")
            }

            return compare(contextValue, op, rawValue)
        }
        throw new IllegalArgumentException("Cannot parse leaf condition: '$expr'")
    }

    /**
     * Compare two values. If both look like version strings / numbers, compare
     * numerically. Otherwise compare as strings (useful for loader == forge).
     */
    private boolean compare(String left, String op, String right) {
        try {
            int cmp = compareVersions(left, right)
            switch (op) {
                case '==': return cmp == 0
                case '!=': return cmp != 0
                case '>=': return cmp >= 0
                case '<=': return cmp <= 0
                case '>':  return cmp > 0
                case '<':  return cmp < 0
            }
        } catch (ignored) {
            // fall through to string compare
        }

        int cmp = left.compareToIgnoreCase(right)
        switch (op) {
            case '==': return cmp == 0
            case '!=': return cmp != 0
            default: throw new IllegalArgumentException("Operator '$op' is not valid for string comparison of '$left' and '$right'")
        }
    }

    /**
     * Compare two version strings like "1.20.1" or "47" numerically,
     * component by component. Returns negative / zero / positive like Comparator.
     */
    static int compareVersions(String a, String b) {
        if (a ==~ /\d+/ && b ==~ /\d+/) {
            return Integer.compare(a.toInteger(), b.toInteger())
        }
        String[] partsA = a.split(/\./)
        String[] partsB = b.split(/\./)
        int len = Math.max(partsA.length, partsB.length)
        for (int i = 0; i < len; i++) {
            int va = i < partsA.length ? partsA[i].toInteger() : 0
            int vb = i < partsB.length ? partsB[i].toInteger() : 0
            if (va != vb) return Integer.compare(va, vb)
        }
        return 0
    }
}