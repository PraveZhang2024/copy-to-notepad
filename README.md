# copy-to-notepad

![Build](https://github.com/PraveZhang2024/copy-to-notepad/workflows/Build/badge.svg)

<!-- Plugin description -->
CopyOut! exports text from IntelliJ IDEA and PyCharm (2025.3 / build 253) and opens it with `VBCopyO.exe`.

Right-click **CopyOut!** in the editor, Project view, or Terminal:

- Editor selection is written to a temp `.properties` file.
- Editor with no selection uses the clipboard.
- A Project-view file exports the whole file; binary files show a balloon, folders hide the action.
- Terminal selection is exported the same way.
<!-- Plugin description end -->

`VBCopyO.exe` must be placed at `src/main/resources/VBCopyO.exe` before building. The plugin copies it from the plugin jar to the system temp directory at runtime.

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "copy-to-notepad"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Build `copy-to-notepad-0.1.0.zip` with `.\gradlew.bat buildPlugin`, then install it with
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
