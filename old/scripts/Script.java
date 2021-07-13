package dovtech.logiscript.scripts;

import dovtech.logiscript.utils.DateUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class Script {

    private ScriptData scriptData;
    private String text;

    public Script(ScriptData scriptData, String text) {
        this.scriptData = scriptData;
        this.text = text;
    }

    public ScriptData getScriptData() {
        return scriptData;
    }

    public String getText() {
        return text;
    }

    public String getUploader() {
        return scriptData.uploader;
    }

    public String getDateCreated() {
        return DateUtils.getTimeFormatted(new Date(scriptData.timestamp));
    }

    public HashMap<String, Long> getConnectionMap() {
        return scriptData.connectionMap;
    }

    public long getScriptId() {
        return scriptData.scriptId;
    }

    public String getScriptName() {
        return scriptData.scriptName;
    }

    public static Script readFile(File scriptFile) throws IOException {
        RandomAccessFile file = new RandomAccessFile(scriptFile, "rw");
        file.seek(0);
        file.readLine(); //Header Start
        long scriptId = file.readLong();
        String scriptName = file.readLine();
        String uploader = file.readLine();
        long timestamp = file.readLong();
        int connections = file.readInt();
        HashMap<String, Long> connectionMap = new HashMap<>();
        for(int i = 0; i < connections; i ++) connectionMap.put(file.readLine(), file.readLong());
        ScriptData scriptData = new ScriptData(scriptId, scriptName, uploader, timestamp, connectionMap);
        file.readLine(); //Header End

        StringBuilder builder = new StringBuilder();
        String nextLine = file.readLine();
        while(nextLine != null) {
            builder.append(nextLine);
            if(!nextLine.endsWith("\n")) builder.append('\n');
            nextLine = file.readLine();
        }
        return new Script(scriptData, builder.toString().trim());
    }

    public static class ScriptData {

        public long scriptId;
        public String scriptName;
        public String uploader;
        public long timestamp;
        public HashMap<String, Long> connectionMap;

        public ScriptData(String scriptName, String uploader) {
            this.scriptId = new Random(new Object().hashCode()).nextLong();
            this.scriptName = scriptName;
            this.uploader = uploader;
            this.timestamp = System.currentTimeMillis();
            this.connectionMap = new HashMap<>();
        }

        public ScriptData(long scriptId, String scriptName, String uploader, long timestamp, HashMap<String, Long> connectionMap) {
            this.scriptId = scriptId;
            this.scriptName = scriptName;
            this.uploader = uploader;
            this.timestamp = timestamp;
            this.connectionMap = connectionMap;
        }
    }
}
