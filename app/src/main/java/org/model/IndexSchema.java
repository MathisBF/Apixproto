package org.model;


import org.parquet.ParquetReader;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.nio.file.Path;

import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.types.pojo.ArrowType.Int;
import org.apache.arrow.vector.types.pojo.ArrowType.FloatingPoint;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;



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
    private final Map<String, LuceneFieldType> fields;



    /**
     * Constructeur de IndexSchema, construit fields depuis un schéma Arrow.
     * 
     * @param schema Le schema Arrow.
     */
    public IndexSchema(Schema schema) {

        List<org.apache.arrow.vector.types.pojo.Field> arrowFields = schema.getFields();
        this.fields = new HashMap<>();

        for (org.apache.arrow.vector.types.pojo.Field f : arrowFields) {
            this.fields.put(f.getName(), arrowToLuceneField(f));
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
            this.fields.put(f.getName(), arrowToLuceneField(f));
        }
    }



    /**
     * Constructeur de IndexSchema, construit fields depuis un Map déjà construit.
     * 
     * @param fields Dictionnaire entre les noms et les types des champs.
     */
    public IndexSchema(Map<String, LuceneFieldType> fields) {
        this.fields = new HashMap<>(fields);
    }



    /**
     * Retourne le nom des champs et leurs types Lucene.
     * 
     * @return Un dictionnaire contenant le nom des champs en clé et leurs types Lucene en valeur.
     */
    public Map<String, LuceneFieldType> getFields() {
        return this.fields;
    }



    /**
     * Retourne le type Lucene d'un champ.
     * 
     * @param fieldName Le nom du champ.
     * @return Le type Lucene du champ.
     */
    public LuceneFieldType getFieldType(String fieldName) {
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
     * Determine le type de champ Lucene vers lequel convertir un champ Arrow.
     * 
     * @param arrowField Le champ Arrow.
     * @throws IllegalArgumentException
     * @return Le type de champ Lucene.
     */
    public static LuceneFieldType arrowToLuceneField(org.apache.arrow.vector.types.pojo.Field arrowField) throws IllegalArgumentException {

        switch (arrowField.getType().getTypeID()) {

            case Int:
                Int intType = (Int) arrowField.getType();
                switch (intType.getBitWidth()) {
                    case 8, 16, 32:
                        return LuceneFieldType.IntPoint;
                    case 64:
                        return LuceneFieldType.LongPoint;
                    default:
                        throw new IllegalArgumentException("Unsupported integer width");
                }

            case FloatingPoint:
                FloatingPoint floatType = (FloatingPoint) arrowField.getType();
                switch (floatType.getPrecision()) {
                    case SINGLE, HALF:
                        return LuceneFieldType.FloatPoint;
                    case DOUBLE:
                        return LuceneFieldType.DoublePoint;
                    default:
                        throw new IllegalArgumentException("Unsupported float precision");
                }

            case Bool:
                return LuceneFieldType.StringField;

            case Date:
                return LuceneFieldType.LongPoint;

            case Utf8, LargeUtf8:
                if (isFullTextField(arrowField)) {
                    return LuceneFieldType.TextField;
                } else {
                    return LuceneFieldType.StringField;
                }

            default:
                return LuceneFieldType.StringField;
        }
    }



    public static boolean isFullTextField(org.apache.arrow.vector.types.pojo.Field arrowField) {
        return switch (arrowField.getName()) {
            case "description", "titre", "contenue", "texte" -> true;
            default -> false;
        };
    }



    public void addField(Document doc, String fieldName, Object value) {

        LuceneFieldType type = getFieldType(fieldName);

        switch (type) {
            case TextField:
                doc.add(new TextField(fieldName, value.toString(), Field.Store.YES));
                break;

            case StringField:
                doc.add(new StringField(fieldName, value.toString(), Field.Store.YES));
                break;

            case IntPoint:
                doc.add(new IntPoint(fieldName, ((Number) value).intValue()));
                doc.add(new StoredField(fieldName, ((Number) value).intValue()));
                break;

            case LongPoint:
                doc.add(new LongPoint(fieldName, ((Number) value).longValue()));
                doc.add(new StoredField(fieldName, ((Number) value).longValue()));
                break;

            case FloatPoint:
                doc.add(new FloatPoint(fieldName, ((Number) value).floatValue()));
                doc.add(new StoredField(fieldName, ((Number) value).floatValue()));
                break;

            case DoublePoint:
                doc.add(new DoublePoint(fieldName, ((Number) value).doubleValue()));
                doc.add(new StoredField(fieldName, ((Number) value).doubleValue()));
                break;

            default:
                break;
        }
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
