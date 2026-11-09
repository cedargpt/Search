"""Reviewed public source allowlist, shared by packaging and release checks."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ROOT_FILES = (
    'README.md', 'LICENSE', 'LICENSING.md', 'PRIVACY.md', 'CONTRIBUTING.md', 'SECURITY.md',
    'THIRD_PARTY_NOTICES.md', 'build.gradle', 'settings.gradle', 'gradle.properties',
    'app/build.gradle', 'gradlew', 'gradlew.bat', '.gitignore', '.gitattributes',
    'gradle/wrapper/gradle-wrapper.properties', 'gradle/wrapper/gradle-wrapper.jar',
    'gradle/LICENSE-APACHE-2.0.txt', 'docs/RESEARCH.md', 'docs/TESTING.md',
    'docs/SECURITY_REVIEW.md', 'docs/WIDGETS.md',
    # Reviewed empty-home screenshots and a synthetic calibration fixture only.
    'docs/home-yisou-0.3.0.png', 'docs/calibration-0.3.1.png',
    'docs/home-yisou-0.3.3.png',
    'docs/home-yisou-0.3.5.png', 'docs/icon-yisou-0.3.5.png',
    'docs/home-yisou-0.3.6.png', 'docs/icon-yisou-0.3.6.png',
    'docs/home-yisou-0.3.7.png',
    'docs/widgets-yisou-0.4.0.png',
    'docs/home-yisou-0.5.0.png', 'docs/settings-yisou-0.5.0.png',
    'docs/widget-yisou-0.5.0.png', 'docs/widget-yisou-0.5.0-scrolled.png',
    'docs/PLATFORM_ICONS.md',
    'docs/settings-light-yisou-0.7.0.png',
    'docs/settings-dark-yisou-0.7.0.png',
    'docs/widget-input-dark-yisou-0.7.0.png',
    'docs/more-yisou-0.8.0.png',
    'docs/widget-yisou-0.8.0-scrolled.png',
    'docs/widget-middle-yisou-0.8.1.png',
    'docs/widget-middle-yisou-0.8.2.png',
    'docs/settings-yisou-0.6.1.png',
    'docs/more-yisou-0.6.1.png',
    'docs/home-yisou-0.6.0.png',
    'docs/more-yisou-0.6.0.png',
    'docs/order-yisou-0.6.0.png',
    'docs/widget-input-yisou-0.6.0.png',
    'docs/widget-yisou-0.6.0.png',
    'docs/widget-yisou-0.6.0-scrolled.png',
    # Search 1.2.0 removed the bundled CJK font (see THIRD_PARTY_NOTICES.md);
    # only the licence text of the former font stays for historical reference.
    'app/src/main/res/raw/noto_serif_sc_ofl.txt',
    # Launcher icons generated for this fork (Search), reviewed in 1.2.0.
    'app/src/main/res/mipmap-mdpi/ic_launcher.png',
    'app/src/main/res/mipmap-mdpi/ic_launcher_foreground.png',
    'app/src/main/res/mipmap-hdpi/ic_launcher.png',
    'app/src/main/res/mipmap-hdpi/ic_launcher_foreground.png',
    'app/src/main/res/mipmap-xhdpi/ic_launcher.png',
    'app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png',
    'app/src/main/res/mipmap-xxhdpi/ic_launcher.png',
    'app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png',
    'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png',
    'app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png',
    'app/src/main/res/raw/agpl_3.txt',
)
SOURCE_DIRS = {
    'app/src': {'.java', '.xml'},
    'scripts': {'.py', '.ps1'},
    'tests': {'.py', '.java', '.xml'},
    '.github': {'.yml', '.yaml', '.md'},
}

def collect():
    selected = set(ROOT_FILES)
    for folder, suffixes in SOURCE_DIRS.items():
        for path in (ROOT / folder).rglob('*'):
            if '__pycache__' in path.parts or not path.is_file():
                continue
            if path.relative_to(ROOT).as_posix() in selected:
                continue
            if path.suffix not in suffixes:
                raise ValueError('Unreviewed source file: ' + str(path.relative_to(ROOT)))
            selected.add(path.relative_to(ROOT).as_posix())
    for name in selected:
        path = ROOT / name
        if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(ROOT):
            raise ValueError('Unsafe or missing public source: ' + name)
    return sorted(selected)

if __name__ == '__main__':
    print('\n'.join(collect()))
