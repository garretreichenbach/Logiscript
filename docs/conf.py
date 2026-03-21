# Configuration file for the Sphinx documentation builder.

project = "LuaMade"
copyright = "2026, VideoGoose, LupoCani"
author = "VideoGoose, LupoCani"
release = "1.0.0"

extensions = [
    "myst_parser",
]

templates_path = ["_templates"]
exclude_patterns = ["build", "Thumbs.db", ".DS_Store", "source"]

# Parse both Markdown and reStructuredText if needed.
source_suffix = {
    ".md": "markdown",
    ".rst": "restructuredtext",
}

root_doc = "index"

html_theme = "sphinx_rtd_theme"
html_title = "LuaMade Documentation"
html_static_path = ["_static"]

# Keep markdown links predictable and docs-friendly.
myst_enable_extensions = [
    "colon_fence",
    "deflist",
]