import sys
import os
import glob
import re

if len(sys.argv) < 3:
    print("Использование: python update_version.py <старая_версия> <новая_версия>")
    sys.exit(1)

# Исправлено: извлекаем конкретные элементы из массива аргументов
old_v = sys.argv[1]
new_v = sys.argv[2]

# ==================== НАСТРОЙКА ПУТЕЙ И МАСОК ====================
FILES_TO_UPDATE = [
    ('./demo', '*.yaml'),
    ('./demo', '*.yml'),
    ('./docker', '*.bash'),
    ('./docker', 'Dockerfile'),
    ('.', 'pom.xml')
]
# =================================================================

print(f"Рекурсивное обновление версий с {old_v} на {new_v}...")

for directory, mask in FILES_TO_UPDATE:
    search_path = os.path.join(directory, '**', mask)
    found_files = glob.glob(search_path, recursive=True)

    for filepath in found_files:
        if not os.path.isfile(filepath):
            continue

        try:
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()

            # Стратегия замены в зависимости от расширения файла
            if filepath.endswith(('.yaml', '.yml')):
                pattern = rf'(\bversion\s*:\s*["\']?){re.escape(old_v)}(["\']?)'
                new_content = re.sub(pattern, rf'\g<1>{new_v}\g<2>', content)

            elif filepath.endswith('.xml'):
                pattern = rf'(<version>){re.escape(old_v)}(</version>)'
                new_content = re.sub(pattern, rf'\g<1>{new_v}\g<2>', content)

            else:
                new_content = content.replace(old_v, new_v)

            if content != new_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"✔ Обновлен: {filepath}")
            else:
                print(f"  Пропущен (совпадений не найдено): {filepath}")

        except Exception as e:
            print(f"❌ Ошибка при обработке {filepath}: {e}")

print("Готово!")
