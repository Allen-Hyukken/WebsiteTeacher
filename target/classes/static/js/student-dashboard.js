/**
 * STUDENT DASHBOARD - JAVASCRIPT
 * Functions for student pages including code editor and quiz interactions
 * FIXED: Auto-completion disabled, line numbers sync properly
 */

// ============================================
// CLASS CODE COPY FUNCTIONALITY
// ============================================

/**
 * Copy class code to clipboard
 * @param {string} codeElementId - ID of element containing the code
 * @param {string} feedbackElementId - ID of feedback element to show confirmation
 */
function copyClassCode(codeElementId, feedbackElementId) {
    const codeElement = document.getElementById(codeElementId);
    const feedbackElement = document.getElementById(feedbackElementId);

    if (!codeElement) {
        console.error('Code element not found:', codeElementId);
        return;
    }

    const codeToCopy = codeElement.textContent.trim();

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(codeToCopy).then(() => {
            showFeedback(feedbackElement);
        }).catch(err => {
            console.error('Failed to copy text using Clipboard API:', err);
            fallbackCopy(codeElement, codeToCopy, feedbackElement);
        });
    } else {
        fallbackCopy(codeElement, codeToCopy, feedbackElement);
    }
}

/**
 * Fallback copy method using execCommand
 */
function fallbackCopy(codeElement, codeToCopy, feedbackElement) {
    try {
        const range = document.createRange();
        range.selectNodeContents(codeElement);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        document.execCommand('copy');
        selection.removeAllRanges();

        if (feedbackElement) {
            feedbackElement.textContent = 'Copied!';
            showFeedback(feedbackElement);
        }
    } catch (fallbackErr) {
        console.error('Fallback copy failed: ', fallbackErr);
        alert(`Failed to copy code. Please manually copy: ${codeToCopy}`);
    }
}

/**
 * Show feedback message temporarily
 */
function showFeedback(feedbackElement) {
    if (feedbackElement) {
        feedbackElement.style.display = 'inline';
        setTimeout(() => {
            feedbackElement.style.display = 'none';
        }, 2000);
    }
}

// ============================================
// SYNTAX HIGHLIGHTING FOR CODE
// ============================================

/**
 * Highlight code syntax with colors
 * @param {string} code - The code to highlight
 * @returns {string} HTML string with syntax highlighting
 */
function highlightSyntax(code) {
    const keywords = ['function', 'const', 'let', 'var', 'if', 'else', 'for', 'while', 'return',
        'class', 'new', 'this', 'import', 'export', 'from', 'async', 'await',
        'try', 'catch', 'throw', 'break', 'continue', 'switch', 'case', 'default',
        'typeof', 'instanceof', 'delete', 'void', 'yield', 'in', 'of', 'do',
        'public', 'private', 'protected', 'static', 'final', 'abstract', 'interface',
        'def', 'print', 'input', 'range', 'len', 'str', 'int', 'float', 'True', 'False', 'None',
        'and', 'or', 'not', 'is', 'pass', 'lambda', 'with', 'as', 'except', 'finally',
        'include', 'using', 'namespace', 'cout', 'cin', 'std', 'vector', 'string'];

    let highlighted = code;

    // Escape HTML entities
    highlighted = highlighted.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    // Highlight comments (// and #)
    highlighted = highlighted.replace(/(\/\/.*$|#.*$)/gm, '<span class="comment">$1</span>');
    highlighted = highlighted.replace(/(\/\*[\s\S]*?\*\/)/g, '<span class="comment">$1</span>');

    // Highlight strings
    highlighted = highlighted.replace(/("(?:[^"\\]|\\.)*")/g, '<span class="string">$1</span>');
    highlighted = highlighted.replace(/('(?:[^'\\]|\\.)*')/g, '<span class="string">$1</span>');
    highlighted = highlighted.replace(/(`(?:[^`\\]|\\.)*`)/g, '<span class="string">$1</span>');

    // Highlight numbers
    highlighted = highlighted.replace(/\b(\d+\.?\d*)\b/g, '<span class="number">$1</span>');

    // Highlight keywords
    keywords.forEach(keyword => {
        const regex = new RegExp('\\b(' + keyword + ')\\b', 'g');
        highlighted = highlighted.replace(regex, '<span class="keyword">$1</span>');
    });

    // Highlight function names
    highlighted = highlighted.replace(/\b([a-zA-Z_$][a-zA-Z0-9_$]*)\s*(?=\()/g, '<span class="function">$1</span>');

    // Highlight properties
    highlighted = highlighted.replace(/\.([a-zA-Z_$][a-zA-Z0-9_$]*)/g, '.<span class="property">$1</span>');

    return highlighted;
}

/**
 * Update line numbers for code editor
 * @param {HTMLTextAreaElement} textarea - The textarea element
 * @param {HTMLElement} lineNumbersDiv - The div showing line numbers
 */
function updateLineNumbers(textarea, lineNumbersDiv) {
    if (!lineNumbersDiv || !textarea) return;

    const lines = textarea.value.split('\n');
    const lineCount = lines.length;

    let numbers = '';
    for (let i = 1; i <= lineCount; i++) {
        numbers += i + '\n';
    }
    lineNumbersDiv.textContent = numbers;
}

// ============================================
// CODE EDITOR INITIALIZATION FOR STUDENTS
// FIXED: Auto-completion disabled, line numbers sync
// ============================================

/**
 * Initialize student code editor for quiz taking
 */
function initializeStudentCodeEditors() {
    const codeEditors = document.querySelectorAll('.code-editor');

    codeEditors.forEach((editor, index) => {
        const wrapper = editor.closest('.code-editor-wrapper');
        if (!wrapper) return;

        const lineNumbers = wrapper.querySelector('.line-numbers');
        const highlight = wrapper.querySelector('.code-highlight');

        // CRITICAL FIX: Disable all auto-completion and suggestions
        editor.setAttribute('autocomplete', 'off');
        editor.setAttribute('autocorrect', 'off');
        editor.setAttribute('autocapitalize', 'off');
        editor.setAttribute('spellcheck', 'false');

        // Additional browser-specific attributes
        editor.setAttribute('data-gramm', 'false'); // Disable Grammarly
        editor.setAttribute('data-gramm_editor', 'false');
        editor.setAttribute('data-enable-grammarly', 'false');

        /**
         * Update syntax highlighting and line numbers
         */
        function updateEditorDisplay() {
            if (highlight) {
                highlight.innerHTML = highlightSyntax(editor.value);
            }
            if (lineNumbers) {
                updateLineNumbers(editor, lineNumbers);
            }
        }

        /**
         * Handle Tab key for indentation
         */
        editor.addEventListener('keydown', function(e) {
            if (e.key === 'Tab') {
                e.preventDefault();
                const start = this.selectionStart;
                const end = this.selectionEnd;
                const value = this.value;

                // Insert 4 spaces
                this.value = value.substring(0, start) + '    ' + value.substring(end);

                // Move cursor after inserted spaces
                this.selectionStart = this.selectionEnd = start + 4;

                // Update display
                updateEditorDisplay();
            }
        });

        /**
         * Update on every input (FIXED: Line numbers now sync properly)
         */
        editor.addEventListener('input', function() {
            updateEditorDisplay();
        });

        /**
         * Handle paste events
         */
        editor.addEventListener('paste', function() {
            setTimeout(() => {
                updateEditorDisplay();
            }, 0);
        });

        /**
         * Sync scrolling between editor and line numbers/highlight
         */
        editor.addEventListener('scroll', function() {
            if (highlight) {
                highlight.scrollTop = this.scrollTop;
                highlight.scrollLeft = this.scrollLeft;
            }
            if (lineNumbers) {
                lineNumbers.scrollTop = this.scrollTop;
            }
        });

        // Initial render
        updateEditorDisplay();
    });
}

// ============================================
// QUIZ FORM VALIDATION
// ============================================

/**
 * Validate quiz form before submission
 */
function initializeQuizValidation() {
    const quizForm = document.querySelector('form[action*="/submit"]');

    if (quizForm) {
        quizForm.addEventListener('submit', function(e) {
            // Check if all required fields are filled
            const requiredFields = this.querySelectorAll('[required]');
            let emptyFields = [];

            requiredFields.forEach(field => {
                if (field.type === 'radio') {
                    const radioGroup = this.querySelectorAll(`[name="${field.name}"]`);
                    const isChecked = Array.from(radioGroup).some(radio => radio.checked);
                    if (!isChecked) {
                        emptyFields.push(field.name);
                    }
                } else if (!field.value.trim()) {
                    emptyFields.push(field.name || 'unnamed field');
                }
            });

            // Check coding questions specifically
            const codingQuestions = this.querySelectorAll('textarea.code-editor');
            let emptyCodingQuestions = 0;

            codingQuestions.forEach(textarea => {
                if (!textarea.value.trim()) {
                    emptyCodingQuestions++;
                }
            });

            // Warn about empty coding questions
            if (emptyCodingQuestions > 0) {
                const confirmSubmit = confirm(
                    `You have ${emptyCodingQuestions} empty coding question(s). Are you sure you want to submit?`
                );
                if (!confirmSubmit) {
                    e.preventDefault();
                    return false;
                }
            }

            // Final confirmation
            if (emptyFields.length === 0 || confirm('Are you sure you want to submit your answers? You cannot change them after submission.')) {
                return true;
            } else {
                e.preventDefault();
                return false;
            }
        });
    }
}

// ============================================
// INITIALIZATION
// ============================================

/**
 * Initialize all student dashboard functionality when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize code editors for coding questions
    initializeStudentCodeEditors();

    // Initialize quiz form validation
    initializeQuizValidation();

    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const closeBtn = alert.querySelector('.close');
            if (closeBtn) closeBtn.click();
        }, 5000);
    });

    // Prevent accidental page navigation during quiz
    const quizPage = document.querySelector('form[action*="/submit"]');
    if (quizPage) {
        let formSubmitted = false;

        quizPage.addEventListener('submit', function() {
            formSubmitted = true;
        });

        window.addEventListener('beforeunload', function(e) {
            if (!formSubmitted) {
                e.preventDefault();
                e.returnValue = 'You have unsaved quiz answers. Are you sure you want to leave?';
                return e.returnValue;
            }
        });
    }

    console.log('Student Dashboard initialized - Auto-completion disabled, line numbers fixed');
});