package luamade.manager;

import luamade.LuaMade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages documentation for the mod, loading markdown files from the JAR
 * and converting them to in-game glossary entries.
 */
public class DocumentationManager {
    
    private static final Map<String, String> documentationCache = new HashMap<>();
    
    /**
     * Loads a markdown file from the JAR and returns its content.
     * 
     * @param path The path to the markdown file, relative to the docs/markdown directory
     * @return The content of the markdown file, or null if the file could not be loaded
     */
    public static String loadMarkdownFile(String path) {
        if (documentationCache.containsKey(path)) {
            return documentationCache.get(path);
        }
        
        try {
            String fullPath = "docs/markdown/" + path;
            InputStream inputStream = DocumentationManager.class.getClassLoader().getResourceAsStream(fullPath);
            
            if (inputStream == null) {
                LuaMade.log.log(Level.WARNING, "Could not find markdown file: " + fullPath);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            reader.close();
            
            String markdownContent = content.toString();
            documentationCache.put(path, markdownContent);
            
            return markdownContent;
        } catch (IOException e) {
            LuaMade.log.log(Level.WARNING, "Error loading markdown file: " + path, e);
            return null;
        }
    }
    
    /**
     * Extracts the title from a markdown file.
     * The title is assumed to be the first line of the file, starting with "# ".
     * 
     * @param markdownContent The content of the markdown file
     * @return The title of the markdown file, or null if no title was found
     */
    public static String extractTitle(String markdownContent) {
        if (markdownContent == null || markdownContent.isEmpty()) {
            return null;
        }
        
        String[] lines = markdownContent.split("\n");
        
        if (lines.length > 0 && lines[0].startsWith("# ")) {
            return lines[0].substring(2).trim();
        }
        
        return null;
    }
    
    /**
     * Extracts the content from a markdown file.
     * The content is assumed to be everything after the title.
     * 
     * @param markdownContent The content of the markdown file
     * @return The content of the markdown file, or null if no content was found
     */
    public static String extractContent(String markdownContent) {
        if (markdownContent == null || markdownContent.isEmpty()) {
            return null;
        }
        
        String[] lines = markdownContent.split("\n");
        
        if (lines.length <= 1) {
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        
        for (int i = 1; i < lines.length; i++) {
            content.append(lines[i]);
            
            if (i < lines.length - 1) {
                content.append("\n");
            }
        }
        
        return content.toString().trim();
    }
    
    /**
     * Lists all markdown files in a directory.
     * 
     * @param directory The directory to list, relative to the docs/markdown directory
     * @return An array of file paths, or an empty array if the directory could not be listed
     */
    public static String[] listMarkdownFiles(String directory) {
        try {
            String fullPath = "docs/markdown/" + directory;
            InputStream inputStream = DocumentationManager.class.getClassLoader().getResourceAsStream(fullPath);
            
            if (inputStream == null) {
                LuaMade.log.log(Level.WARNING, "Could not find directory: " + fullPath);
                return new String[0];
            }
            
            // This is a bit of a hack, but it works for listing resources in a JAR file
            // We can't use File.list() because the resources are inside the JAR
            // Instead, we use the ClassLoader to get a list of all resources and filter them
            
            // Get all resources in the JAR
            java.util.Enumeration<java.net.URL> resources = DocumentationManager.class.getClassLoader().getResources("");
            
            // Filter resources that are in the specified directory and have a .md extension
            java.util.List<String> files = new java.util.ArrayList<>();
            
            while (resources.hasMoreElements()) {
                java.net.URL url = resources.nextElement();
                java.io.File file = new java.io.File(url.getFile());
                
                if (file.isDirectory()) {
                    java.io.File[] dirFiles = file.listFiles();
                    
                    if (dirFiles != null) {
                        for (java.io.File dirFile : dirFiles) {
                            String path = dirFile.getPath();
                            
                            if (path.startsWith(fullPath) && path.endsWith(".md")) {
                                files.add(path.substring(fullPath.length() + 1));
                            }
                        }
                    }
                }
            }
            
            return files.toArray(new String[0]);
        } catch (IOException e) {
            LuaMade.log.log(Level.WARNING, "Error listing markdown files in directory: " + directory, e);
            return new String[0];
        }
    }
}