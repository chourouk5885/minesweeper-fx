# Contributing to Minesweeper FX

Thanks for wanting to help! Bug reports, ideas and pull requests are all welcome.

## Reporting a bug

Open an issue and include:

- what you did, what you expected, and what happened instead
- your operating system and `java -version`
- any error output from the terminal

## Making a change

1. Fork the repository and create a branch: `git checkout -b my-change`
2. Make your change. Keep game rules in `model/` (no JavaFX imports there) and UI code in `view/`.
3. Add or update tests in `src/test/java` for any rule change.
4. Check everything passes: `mvn test`
5. Try it: `mvn javafx:run`
6. Open a pull request describing what changed and why.

## Code style

- Java 17, 4-space indentation (see `.editorconfig`)
- Javadoc on public classes and methods
- Named constants instead of magic numbers
- New colours go in `Theme.java` for both themes, never hard-coded in views
