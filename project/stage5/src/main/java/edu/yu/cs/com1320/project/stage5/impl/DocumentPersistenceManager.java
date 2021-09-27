package edu.yu.cs.com1320.project.stage5.impl;
import javax.xml.bind.DatatypeConverter;
import com.google.gson.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File baseDir;

    public DocumentPersistenceManager(File baseDir){
        if(baseDir == null){
            this.baseDir = new File(System.getProperty("user.dir"));
        }else{
            this.baseDir = baseDir;
        }
    }



    private class JsonSerializer implements com.google.gson.JsonSerializer<Document>{

        @Override
        public JsonElement serialize(Document document, Type type, JsonSerializationContext jsonSerializationContext) {
            Gson gson = new Gson();
            JsonObject jsonObject = new JsonObject();
            /*
            1. The Contents (Either gonna be text or byte[]
            2. The URI (String)
            3. The WordCountMap
             */
            /*
            for The contents
            if getting the byte[] is != null,
            Base64encode it as a string, then add property
            else, just add property
             */
            if(document.getDocumentTxt() == null){ //ByteArray
                byte[] bytes = document.getDocumentBinaryData();
                String base64Encoded = DatatypeConverter.printBase64Binary(bytes);
                jsonObject.addProperty("byteArray", base64Encoded);
            }else{

                jsonObject.addProperty("txt", document.getDocumentTxt());
            }
            /*
            URI built in .toString method
            Java methods - getPath()/ getHost()
            make sure to add.Json
             */
            jsonObject.addProperty("URI", document.getKey().toString());
            /*
            gson.toJson
             */
            String wordMapAsAString = gson.toJson(document.getWordMap());
            jsonObject.addProperty("wordMap", wordMapAsAString);
            return jsonObject;

        }
    }

    private class JsonDeSerializer implements JsonDeserializer<Document>{

        @Override
        public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            /*
            We want to get only the three things we labeled in serialize
            create a new DocumentImpl

            Within JsonElement Library,
             */
            boolean yestxt = true;
            Gson gson = new Gson();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String txt = null;
            byte[] base64Decoded = null;
            try {
                txt = jsonObject.get("txt").getAsString();
            } catch (JsonParseException e) {
                String bytesString = jsonObject.get("byteArray").getAsString();
                yestxt = false;
                base64Decoded = DatatypeConverter.parseBase64Binary(bytesString);
            }
            String uriString = jsonObject.get("URI").getAsString();
            URI uri = null;
            try {
                uri = new URI(uriString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            String wordMapAsAString = jsonObject.get("wordMap").getAsString();
            Map<String, Integer> mapClass = new HashMap<>();
            Map<String, Integer> wordMap = gson.fromJson(wordMapAsAString, mapClass.getClass());

            if(yestxt){
                return new DocumentImpl(uri, txt);
            }else{
                return new DocumentImpl(uri, base64Decoded);
            }

        }
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        if(uri == null || val == null){
            throw new IllegalArgumentException("URI or Doc is null");
        }
        JsonSerializer jsonSerializer = new JsonSerializer();
        JsonElement jsonElement = jsonSerializer.serialize(val, Document.class, null);

        /*
        Once you've got the Json Object
        Must place it in the correct file.
         */
        //Pass in base directory, then converted Uri and .json
        String host = uri.getHost(); // www.gabe.com
        String path = uri.getPath(); // /is/the/coolest/object
        String completed = (host+path+".json"); // www.gabe.com/is/the/coolest/object.json
        File file = new File(this.baseDir, completed); //in memory
        file.getParentFile().mkdirs(); //Directories on the computer
        FileWriter fileWriter = new FileWriter(file); // on the computer
        fileWriter.write(jsonElement.toString());
        fileWriter.flush();
        fileWriter.close();

    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        if(uri == null){
            throw new IllegalArgumentException("Null URI");
        }
        JsonDeSerializer jsonDeSerializer = new JsonDeSerializer();

        String host = uri.getHost(); // www.gabe.com
        String path = uri.getPath(); // /is/the/coolest/object
        String completed = (host+path+".json"); // www.gabe.com/is/the/coolest/object.json
        File file = new File(this.baseDir, completed); //in memory

        if(!file.exists()){
            return null;
        }
        FileReader fileReader = new FileReader(file);
        JsonElement jsonElement = JsonParser.parseReader(fileReader);
        Document toBeReturned = jsonDeSerializer.deserialize(jsonElement, Document.class, null);
        fileReader.close();

        this.delete(uri);
        return toBeReturned;
        /*
        Get back a JsonElement from a file, then convert it back
         */
    }

    /**
     * delete the file stored on disk that corresponds to the given key
     * @param uri
     * @return true or false to indicate if deletion occurred or not
     * @throws IOException
     */
    @Override
    public boolean delete(URI uri) throws IOException {
        if(uri == null){
            throw new IllegalArgumentException("Null URI");
        }
        String host = uri.getHost(); // www.gabe.com
        String path = uri.getPath(); // /is/the/coolest/object
        String completed = (host+path+".json"); // www.gabe.com/is/the/coolest/object.json
        File file = new File(this.baseDir, completed); //in memory

        if(!file.exists()){
            return false;
        }else{
            return file.delete();

        }

    }







}
