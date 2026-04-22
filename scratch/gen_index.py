import os
import json

repo_root = r"c:\Users\emirh\Desktop\bitirme\vecturai"
docs_root = r"c:\Users\emirh\Desktop\bitirme\vecturai\codebase-docs"

exclude_dirs = {".git", ".gradle", "build", ".kotlin", "bin"}

def classify(path):
    rel_path = os.path.relpath(path, repo_root)
    name = os.path.basename(path)
    
    if "codebase-docs" in rel_path:
        return "docs"
    if ".github" in rel_path:
        return "config" if name.endswith((".yml", ".yaml")) else "script"
    if "gradle" in rel_path or name.endswith((".gradle.kts", ".properties", "gradlew", "gradlew.bat")):
        return "build/tooling"
    if name in [".gitignore", "LICENSE", "Makefile"]:
        return "build/tooling"
    if name.endswith((".md", ".txt")):
        return "docs"
    if name.endswith((".kt", ".kts", ".swift")):
        if "test" in rel_path:
            return "test"
        return "source"
    if name.endswith((".json", ".xml", ".plist")):
        if "test" in rel_path:
            return "test"
        return "config"
    if name.endswith((".png", ".jpg", ".jpeg", ".gif", ".arreferenceimage", ".xcassets", ".arreferenceimage", ".arreferenceimage")):
        return "asset"
    
    return "unresolved"

def get_status(path, category):
    rel_path = os.path.relpath(path, repo_root)
    if "codebase-docs" in rel_path:
        return "skipped_with_reason"
    if category in ["generated", "vendor/third_party", "asset", "binary/cache/other"]:
        return "indexed_minimal"
    return "unresolved"

def get_reason(path, category, status):
    if status == "skipped_with_reason":
        return "Self-documentation directory"
    if status == "indexed_minimal":
        if category == "generated": return "Generated build artifact"
        if category == "asset": return "Binary asset"
        if category == "binary/cache/other": return "Tooling cache"
    return None

inventory = {
    "metadata": {
        "repository": "vecturai",
        "root": repo_root,
        "description": "Indoor navigation system with AR support"
    },
    "folders": [],
    "files": []
}

for root, dirs, files in os.walk(repo_root):
    dirs[:] = [d for d in dirs if d not in exclude_dirs]
    rel_root = os.path.relpath(root, repo_root)
    if rel_root == ".": rel_root = ""
    
    inventory["folders"].append({
        "path": rel_root,
        "status": "mapped" if rel_root == "" else "unresolved"
    })
    
    for file in files:
        full_path = os.path.join(root, file)
        rel_path = os.path.relpath(full_path, repo_root)
        category = classify(full_path)
        status = get_status(full_path, category)
        reason = get_reason(full_path, category, status)
        
        inventory["files"].append({
            "path": rel_path,
            "category": category,
            "status": status,
            "reason": reason,
            "feature_tags": [],
            "artifact_link": f"codebase-docs/files/{rel_path.replace(os.sep, '_')}.md" if status == "unresolved" else None
        })

with open(os.path.join(docs_root, "codebase-index.json"), "w") as f:
    json.dump(inventory, f, indent=2)
