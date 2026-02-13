import os
import codecs

def remove_bom_from_files(directory):
    count = 0
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'rb') as f:
                        content = f.read()
                    
                    if content.startswith(codecs.BOM_UTF8):
                        new_content = content[len(codecs.BOM_UTF8):]
                        with open(file_path, 'wb') as f:
                            f.write(new_content)
                        print(f"Removed BOM from: {file}")
                        count += 1
                except Exception as e:
                    print(f"Error processing {file}: {e}")
    print(f"Total files cleaned: {count}")

if __name__ == "__main__":
    remove_bom_from_files(r"d:\Project\nfgplus\app\src\main\java")
