#!/bin/bash

LICENSE_FILE="NOTICE"

#Java
find . -name "*.java" | while read -r file; do
    if ! grep -q "Copyright" "$file"; then
        echo "/*" > tmp_header.txt
        cat "$LICENSE_FILE" | sed 's/^/ * /' >> tmp_header.txt
        echo " */" >> tmp_header.txt
        echo "" >> tmp_header.txt
        cat "$file" >> tmp_header.txt
        mv tmp_header.txt "$file"
        echo "Добавлен копирайт в: $file"
    fi
done

#Shell (bash, sh)
find . \( -name "*.sh" -o -name "*.bash" \) | while read -r file; do
    if ! grep -q "Copyright" "$file"; then
        # сохраняем shebang (#!/bin/bash) первой строкой
        head -n 1 "$file" > tmp_header.txt
        echo "" >> tmp_header.txt
        cat "$LICENSE_FILE" | sed 's/^/# /' >> tmp_header.txt
        echo "" >> tmp_header.txt
        tail -n +2 "$file" >> tmp_header.txt
        mv tmp_header.txt "$file"
        echo "Добавлен копирайт в: $file"
    fi
done
