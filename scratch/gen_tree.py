import os

repo_root = r"c:\Users\emirh\Desktop\bitirme\VecturAI"
docs_root = r"c:\Users\emirh\Desktop\bitirme\VecturAI\codebase-docs"

exclude_dirs = {".git", ".gradle", "build", ".kotlin", "codebase-docs", "bin"}

def generate_tree(startpath):
    tree = []
    for root, dirs, files in os.walk(startpath):
        dirs[:] = [d for d in dirs if d not in exclude_dirs]
        level = root.replace(startpath, '').count(os.sep)
        indent = '  ' * level
        tree.append(f"{indent}- {os.path.basename(root)}/")
        sub_indent = '  ' * (level + 1)
        for f in files:
            if not f.startswith("."):
                tree.append(f"{sub_indent}- {f}")
    return "\n".join(tree)

tree_content = "# Repository Tree\n\n"
tree_content += "Categories:\n"
tree_content += "- `apps/`: Mobile applications (Android/iOS)\n"
tree_content += "- `shared/`: Multiplatform core, data, and UI logic\n"
tree_content += "- `tools/`: Development and preprocessing utilities\n"
tree_content += "- `sample/`: Sample navigation data and assets\n"
tree_content += "- `scripts/`: Maintenance and helper scripts\n"
tree_content += "- `docs/`: Project documentation\n\n"
tree_content += "```text\n"
tree_content += generate_tree(repo_root)
tree_content += "\n```"

with open(os.path.join(docs_root, "repo-tree.md"), "w") as f:
    f.write(tree_content)
