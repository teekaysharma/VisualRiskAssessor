# Contributing to VisualRiskAssessor

Thank you for your interest in contributing to VisualRiskAssessor! This document provides guidelines for contributing to the project.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for all contributors.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the issue
- **Expected behavior** vs actual behavior
- **Device information** (model, Android version)
- **App version**
- **Screenshots or logs** if applicable

Example:
```
Title: App crashes when selecting large images from gallery

Description: When selecting images larger than 10MB from the photo library on Android 12,
the app crashes with OutOfMemoryError.

Steps to Reproduce:
1. Open the app
2. Tap "Choose from Library"
3. Select an image larger than 10MB
4. App crashes

Expected: Image should be processed successfully
Actual: App crashes

Device: Samsung Galaxy S21, Android 12
App Version: 1.0.0
```

### Suggesting Enhancements

Enhancement suggestions are welcome! Please include:

- **Clear description** of the proposed feature
- **Use case** explaining why it's useful
- **Proposed implementation** (if you have ideas)
- **Mockups or examples** (if applicable)

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch**:
   ```bash
   git checkout -b feature/amazing-feature
   ```

3. **Make your changes**:
   - Follow the code style guidelines
   - Add tests for new functionality
   - Update documentation

4. **Commit your changes**:
   ```bash
   git commit -m "feat: add amazing feature"
   ```

5. **Push to your fork**:
   ```bash
   git push origin feature/amazing-feature
   ```

6. **Open a Pull Request**

## Development Guidelines

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Use proper indentation (4 spaces)

### Commit Messages

Use conventional commit format:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Adding or updating tests
- `chore:` Maintenance tasks

Examples:
```
feat: add PDF export functionality
fix: resolve camera crash on Android 11
docs: update installation instructions
refactor: improve risk calculation performance
test: add unit tests for HazardDetector
```

### Testing Requirements

- Add unit tests for new business logic
- Add instrumentation tests for UI changes
- Ensure all tests pass before submitting PR
- Aim for reasonable test coverage

### Documentation

- Update README.md for user-facing changes
- Update DEVELOPMENT.md for developer-facing changes
- Add inline comments for complex code
- Update API documentation if applicable

## Pull Request Checklist

Before submitting your PR, ensure:

- [ ] Code follows the project's style guidelines
- [ ] Self-review of code completed
- [ ] Comments added to complex code sections
- [ ] Documentation updated
- [ ] Tests added/updated and passing
- [ ] No new warnings or errors
- [ ] Branch is up to date with main

## Priority Areas for Contribution

We especially welcome contributions in these areas:

### High Priority
- Improved ML model accuracy
- Enhanced hazard detection algorithms
- Performance optimizations
- Accessibility improvements
- Bug fixes

### Medium Priority
- New hazard types
- UI/UX enhancements
- Additional report formats (PDF, Excel)
- Multi-language support
- Dark theme support

### Nice to Have
- Historical trend analysis
- Cloud storage integration
- Team collaboration features
- Augmented reality features
- Custom training data support

## Questions?

If you have questions about contributing:

- Check the [DEVELOPMENT.md](DEVELOPMENT.md) guide
- Review existing issues and PRs
- Create a new issue with the "question" label

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Recognition

Contributors will be recognized in:
- GitHub contributors page
- Release notes (for significant contributions)
- Project documentation (for major features)

Thank you for contributing to making workplaces safer! 🛡️
