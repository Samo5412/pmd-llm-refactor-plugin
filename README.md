# PMD-LLM Refactor Plugin
## Overview
The PMD-LLM Refactor Plugin is an IntelliJ IDEA plugin specifically designed to reduce code complexity in Java projects. It combines PMD static code analysis with Large Language Model (LLM) capabilities to identify complex code patterns and provide intelligent refactoring suggestions that simplify your code.

## Features
- **Code Complexity Analysis**: Focuses specifically on code complexity metrics and issues
- **File-Based Analysis**: Analyzes individual files rather than entire projects for targeted improvements
- **LLM-Powered Refactoring**: Leverages AI to generate context-aware refactoring suggestions
- **PMD Integration**: Uses PMD rules specifically related to code complexity detection
- **Customizable Settings**: Configure LLM parameters (API key, model name, temperature, token limit) and PMD ruleset specifications
- **In-IDE Refactoring**: View and implement refactoring suggestions directly within IntelliJ IDEA's interface.

## Getting Started

Download the latest version from the [GitHub releases page](https://github.com/Samo5412/pmd-llm-refactor-plugin/releases/tag/v1.0.0-alpha) which contains all installation instructions and prerequisites.

## Understanding the Analysis
The plugin identifies complexity issues such as:
- **High Cyclomatic Complexity**: Measures the number of independent paths through a method, highlighting code with too many decision points
- **Cognitive Complexity**: Detects code that is difficult to understand due to nested structures and flow control
- **NPath Complexity**: Identifies methods with too many possible execution paths
- **Excessive Public Count**: Flags classes with too many public methods, indicating potential design issues

The suggestions are automatically tailored based on the specific PMD rules that were violated in the code. 

## Important Notice About LLM-Generated Code
When using this plugin to refactor your code with AI assistance, please be aware of the following important considerations:
### Limitations of AI-Generated Code
- **No Guarantee of Functional Correctness**: While the LLM may successfully reduce code complexity, it cannot guarantee that the refactored code will maintain identical functionality.
- **Quality Depends on Model Capabilities**: The quality of refactoring suggestions varies based on the LLM model used, available context, and complexity of the original code.
- **Testing is Essential**: Always thoroughly test any refactored code before integrating it into your codebase. All unit tests should pass and functionality should be verified.
- **You Are Responsible for Applied Changes**: You are responsible for reviewing and verifying any changes before implementing them.

### Best Practices
1. Review all suggestions carefully before applying them
2. Run your test suite after implementing changes
3. Consider the suggested changes as recommendations rather than definitive solutions
4. Use the plugin as a learning tool to understand how code can be restructured

By using this plugin, you acknowledge these limitations and accept responsibility for any code changes implemented in your project.

## Development Status
**Note:** This plugin is currently under development. The provided JAR file is reasonably stable and can be used, but you may encounter occasional issues. We appreciate your feedback to help improve the plugin.

## Feedback and Issues
If you encounter any problems or have suggestions for improvements:
* Submit detailed bug reports including:
    - Steps to reproduce the issue
    - Expected vs. actual behavior
    - IntelliJ IDEA version and OS details
    - Any relevant error messages or logs

