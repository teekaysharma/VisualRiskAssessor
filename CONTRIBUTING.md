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
- **Browser and OS** (e.g. Chrome 128 / Android, Safari / iOS)
- **AI provider in use**, if relevant (Groq / Anthropic / Gemini / demo mode)
- **Screenshots, console errors, or exported report** if applicable

Example:
```
Title: PDF export omits the risk matrix on Safari

Description: Exporting a completed assessment to PDF on Safari/iOS produces
a report with the hazard register and HOC table, but the risk matrix page
is blank.

Steps to Reproduce:
1. Complete an assessment with at least one hazard
2. Tap "Export PDF"
3. Open the generated PDF

Expected: Risk matrix renders with hazard markers
Actual: Risk matrix section is blank

Browser: Safari 17, iOS 17.4
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

- Plain vanilla JS/HTML/CSS in `index.html` — no framework, no build step
  for the app itself; keep it that way unless discussed first
- Use meaningful variable and function names
- Add comments only where the *why* isn't obvious from the code — not for
  what the code does
- Keep functions small and focused
- 2-space indentation, matching the existing file

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

- `packages/risk-core` has a real test suite (`npm test`) — add tests there
  for any scoring/banding change
- `index.html` has no automated test suite yet — verify changes live in a
  browser (see [DEVELOPMENT.md](DEVELOPMENT.md)) and describe what you
  tested in the PR
- Ensure `npm test` passes before submitting a PR that touches `risk-core`

### Documentation

- Update README.md for user-facing changes
- Update DEVELOPMENT.md for developer-facing changes
- Add inline comments only where the *why* isn't obvious from the code

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
- Additional sourced hazard-category checklists in the AI prompt (see
  [DEVELOPMENT.md](DEVELOPMENT.md) for the pattern and citation expectations)
- Automated tests for `index.html` (currently manual/live-browser only)
- Accessibility improvements
- Bug fixes

### Medium Priority
- New hazard types in `hazardDB`
- UI/UX enhancements
- CSV/Excel export of History
- Multi-language support (Arabic, given the primary UAE audience)

### Nice to Have
- Historical trend analysis across repeat assessments of the same site
- Team collaboration features
- Custom training data support

## Questions?

If you have questions about contributing:

- Check the [DEVELOPMENT.md](DEVELOPMENT.md) guide
- Review existing issues and PRs
- Create a new issue with the "question" label

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

## Recognition

Contributors will be recognized in:
- GitHub contributors page
- Release notes (for significant contributions)
- Project documentation (for major features)

Thank you for contributing to making workplaces safer! 🛡️
