# Contributing to Spacemaker

Thank you for your interest in contributing to Spacemaker! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/Spacemaker.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test your changes thoroughly
6. Commit your changes: `git commit -m "Add feature: description"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Open a Pull Request

## Development Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 34+
- JDK 17
- Git
- An ARCore-compatible Android device for testing

### Building the Project
```bash
./gradlew clean build
```

### Running Tests
```bash
./gradlew test
```

## Code Style

### Kotlin Style Guide
We follow the [Kotlin official style guide](https://kotlinlang.org/docs/coding-conventions.html).

Key points:
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and function names
- Add KDoc comments for public APIs

### XML Resources
- Use lowercase with underscores for resource names
- Organize resources by type
- Use descriptive IDs

## Commit Messages

Follow the conventional commits specification:

```
type(scope): subject

body (optional)

footer (optional)
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

Example:
```
feat(ar): add mesh generation from point cloud

Implement triangle mesh generation using Delaunay triangulation
for better surface representation from captured point clouds.

Closes #123
```

## Pull Request Process

1. Update the README.md with details of changes if applicable
2. Update the ARCHITECTURE.md if you change the architecture
3. Add tests for new features
4. Ensure all tests pass
5. Update documentation
6. Request review from maintainers

## Testing Guidelines

### Unit Tests
- Write tests for all utility functions
- Test edge cases and error conditions
- Use descriptive test names

### Integration Tests
- Test ARCore integration
- Test file I/O operations
- Test permission flows

### Manual Testing
Test on multiple devices:
- Different Android versions
- Different screen sizes
- Different ARCore capabilities

## Feature Requests

We welcome feature requests! Please:
1. Check if the feature already exists or is planned
2. Open an issue with detailed description
3. Explain the use case and benefits
4. Provide examples if possible

## Bug Reports

When reporting bugs, please include:
1. Device model and Android version
2. ARCore version
3. Steps to reproduce
4. Expected behavior
5. Actual behavior
6. Screenshots or videos if applicable
7. Logcat output if relevant

## Areas for Contribution

We especially welcome contributions in these areas:

### High Priority
- 2D floorplan generation from point clouds
- Multi-format export (PLY, OBJ)
- Room detection and labeling
- Measurement tools
- Performance optimizations

### Medium Priority
- Cloud storage integration
- Collaborative scanning
- AR mesh visualization
- Advanced filtering algorithms
- Batch processing of scans

### Documentation
- Tutorial videos
- Example projects
- API documentation
- Translation to other languages

## Code Review Process

All submissions require review:
1. Maintainers will review your PR within 1 week
2. Address any requested changes
3. Once approved, maintainers will merge

## Community

- Be respectful and inclusive
- Follow the code of conduct
- Help others learn and grow
- Share your knowledge

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## Questions?

Feel free to open an issue with the label "question" if you need help or clarification.

Thank you for contributing to Spacemaker!
