import os
import xml.etree.ElementTree as ET


def extract_dependencies(root_dir):
    # Словарь для хранения результатов: {путь_к_pom: [список_зависимостей]}
    all_dependencies = {}

    # Рекурсивный обход папок
    for dirpath, _, filenames in os.walk(root_dir):
        if "pom.xml" in filenames:
            pom_path = os.path.join(dirpath, "pom.xml")
            dependencies = parse_pom(pom_path)
            if dependencies:
                all_dependencies[pom_path] = dependencies

    return all_dependencies


def parse_pom(file_path):
    dependencies = []
    try:
        # Парсим XML-структуру
        tree = ET.parse(file_path)
        root = tree.getroot()

        # В pom.xml всегда есть пространство имен (Namespace), извлекаем его
        ns = {"maven": root.tag.split("}")[0].strip("{")} if "}" in root.tag else {}

        # Ищем все теги <dependency> внутри <dependencies>
        # Конструкция '//maven:dependency' ищет их на любом уровне вложенности
        search_path = (
            ".//maven:dependency" if ns else ".//dependency"
        )

        for dep in root.findall(search_path, ns):
            # Извлекаем данные, убирая лишние пробелы. Если тега нет — пишем 'N/A'
            g_id = dep.find("maven:groupId", ns) if ns else dep.find("groupId")
            a_id = (
                dep.find("maven:artifactId", ns) if ns else dep.find("artifactId")
            )
            ver = dep.find("maven:version", ns) if ns else dep.find("version")

            group_id = g_id.text.strip() if g_id is not None else "N/A"
            artifact_id = a_id.text.strip() if a_id is not None else "N/A"
            version = ver.text.strip() if ver is not None else "N/A"

            dependencies.append(
                {
                    "groupId": group_id,
                    "artifactId": artifact_id,
                    "version": version,
                }
            )

    except Exception as e:
        print(f"Ошибка при чтении файла {file_path}: {e}")

    return dependencies


# --- Запуск скрипта ---
if __name__ == "__main__":
    # Укажите путь к корневой папке вашего проекта ('.' означает текущую папку)
    project_root = "."

    results = extract_dependencies(project_root)

    # Красивый вывод результата
    for pom, deps in results.items():
        print(f"\n Найдено в: {pom}")
        print("-" * 60)
        for d in deps:
            print(
                f"  - {d['groupId']}:{d['artifactId']} (Версия: {d['version']})"
            )
