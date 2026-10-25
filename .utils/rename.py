import os

# Константы для переименования (добавлена вариация UPPERCASE)
OLD_LOWER, OLD_CAMEL, OLD_UPPER = "jarina", "Jarina", "JARINA"
NEW_LOWER, NEW_CAMEL, NEW_UPPER = "yarina", "Yarina", "YARINA"

# Расширения для текстовой замены (добавлен .xml для файлов внутри .idea)
TEXT_EXTS = {'.tpl','.jsx','.js','.html','.puml','.conf','.bash','.sh','.xml', '.iml', '.pom', '.yaml', '.yml', '.md', '.java', '.properties', '.gradle', '.txt', '.json'}
# Строгий черный список (папка .idea отсюда убрана)
IGNORE_DIRS = {'target', 'build', '.git', '.gradle'}

def replace_text_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()

        # Добавлена замена для UPPERCASE версии
        new_content = (content
                       .replace(OLD_UPPER, NEW_UPPER)
                       .replace(OLD_CAMEL, NEW_CAMEL)
                       .replace(OLD_LOWER, NEW_LOWER))

        if content != new_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"[Текст] Обновлен: {file_path}")
    except Exception as e:
        print(f"[Ошибка файла] {file_path}: {e}")

def rename_project(root_dir):
    for dirpath, dirnames, filenames in os.walk(root_dir, topdown=False):
        # Жесткий пропуск target и кэшей
        if any(ignore in dirpath.split(os.sep) for ignore in IGNORE_DIRS):
            continue

        # Исключаем из обхода на следующих шагах
        dirnames[:] = [d for d in dirnames if d not in IGNORE_DIRS]

        # 1. Текстовая замена и переименование файлов (включая файлы внутри .idea)
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            _, ext = os.path.splitext(filename)

            if ext.lower() in TEXT_EXTS or filename in ['pom.xml', 'Dockerfile', 'Makefile']:
                replace_text_in_file(file_path)

            # Проверка на наличие любой из трех вариаций в имени файла
            if OLD_LOWER in filename or OLD_CAMEL in filename or OLD_UPPER in filename:
                new_filename = (filename
                                .replace(OLD_UPPER, NEW_UPPER)
                                .replace(OLD_CAMEL, NEW_CAMEL)
                                .replace(OLD_LOWER, NEW_LOWER))
                os.rename(file_path, os.path.join(dirpath, new_filename))
                print(f"[Файл] Переименован: {filename} -> {new_filename}")

        # 2. Переименование папок (модулей, пакетов)
        for dirname in dirnames:
            # Проверка на наличие любой из трех вариаций в имени папки
            if OLD_LOWER in dirname or OLD_CAMEL in dirname or OLD_UPPER in dirname:
                dir_path = os.path.join(dirpath, dirname)
                new_dirname = (dirname
                               .replace(OLD_UPPER, NEW_UPPER)
                               .replace(OLD_CAMEL, NEW_CAMEL)
                               .replace(OLD_LOWER, NEW_LOWER))
                os.rename(dir_path, os.path.join(dirpath, new_dirname))
                print(f"[Папка] Переименована: {dirname} -> {new_dirname}")

if __name__ == "__main__":
    project_root = os.path.dirname(os.path.abspath(__file__))
    print(f"Запуск рефакторинга (включая .idea, исключая target) в: {project_root}")
    rename_project(project_root)
    print("Рефакторинг проекта успешно завершен!")
