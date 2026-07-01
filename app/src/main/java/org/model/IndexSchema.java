package org.model;


import org.parquet.ParquetReader;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.arrow.vector.types.pojo.Schema;



/**
 * Fais le lien entre les champs Arrow et Lucene.
 */
public class IndexSchema {

    /**
     * Enumération des types de champs Lucene.
     */
    public enum LuceneFieldType {
        TextField, StringField, IntPoint, LongPoint, FloatPoint, DoublePoint
    };
    /**
     * Les noms des champs et leur type Lucene.
     */
    private final Map<String, FieldDescriptor> fields;



    /**
     * Constructeur de IndexSchema, construit fields depuis un schéma Arrow.
     * 
     * @param schema Le schema Arrow.
     */
    public IndexSchema(Schema schema) {

        List<org.apache.arrow.vector.types.pojo.Field> arrowFields = schema.getFields();
        this.fields = new HashMap<>();

        for (org.apache.arrow.vector.types.pojo.Field f : arrowFields) {
            this.fields.put(
                f.getName(), new FieldDescriptor(f)
            );
        }
    }



    /**
     * Constructeur de IndexSchema, construit fields depuis un fichier Parquet.
     * 
     * @param parquetPath Le chemin vers le fichier source.
     * @throws Exception 
     */
    public IndexSchema(Path parquetPath) throws Exception {

        List<org.apache.arrow.vector.types.pojo.Field> arrowFields = ParquetReader.readSchema(parquetPath).getFields();
        this.fields = new HashMap<>();

        for (org.apache.arrow.vector.types.pojo.Field f : arrowFields) {
            this.fields.put(
                f.getName(), new FieldDescriptor(f)
            );
        }
    }



    /**
     * Constructeur de IndexSchema, construit fields depuis un Map déjà construit.
     * 
     * @param fields Dictionnaire entre les noms et les types des champs.
     */
    public IndexSchema(Map<String, FieldDescriptor> fields) {
        this.fields = new HashMap<>(fields);
    }



    /**
     * Retourne le nom des champs et leurs descripteurs.
     * 
     * @return Un dictionnaire contenant le nom des champs en clé et leurs types Lucene en valeur.
     */
    public Map<String, FieldDescriptor> getFields() {
        return this.fields;
    }



    /**
     * Retourne le descripteur d'un champ à partir de son nom.
     * 
     * @param fieldName Le nom du champ.
     * @return Le descripteur.
     */
    public FieldDescriptor getField(String fieldName) {
        return this.fields.get(fieldName);
    }



    /**
     * Retourne les noms des champs.
     * 
     * @return Un set contenant les noms des champs.
     */
    public Set<String> getFieldsName() {
        return this.fields.keySet();
    }



    /**
     * Sauvegarde le schema dans un fichier json.
     * 
     * @param indexPath Le chemin vers le répertoire contenant l'index.
     * @throws IOException
     */
    public void save(Path indexPath) throws IOException {

        Files.createDirectories(indexPath);

        ObjectMapper mapper = new ObjectMapper();

        mapper.writerWithDefaultPrettyPrinter()
        .writeValue(indexPath.resolve("schema.json").toFile(), fields);
    }



    /**
     * Charge le schema de l'index depuis ce dernier.
     * 
     * @param indexPath Le chemin vers l'index.
     * @return Un nouvel objet IndexSchema.
     * @throws StreamReadException
     * @throws DatabindException
     * @throws IOException
     */
    public static IndexSchema load(Path indexPath) throws StreamReadException, DatabindException, IOException {
        
        ObjectMapper mapper = new ObjectMapper();

        Map<String, FieldDescriptor> map = mapper.readValue(
            indexPath.resolve("schema.json").toFile(),
            new TypeReference<Map<String, FieldDescriptor>>() {}
        );

        return new IndexSchema(map);
    }



    @Override
    public String toString() {
        String result = "";
        for (String fieldName : this.fields.keySet()) {
            result += fieldName + " : " + this.fields.get(fieldName) + "\n";
        }
        return result;
    }



    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof IndexSchema))
            return false;

        IndexSchema other = (IndexSchema) o;
        return this.fields.equals(other.fields);
    }



    @Override
    public int hashCode() {
        return fields.hashCode();
    }
}
