package org.model;


import org.parquet.ParquetReader;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;

import java.nio.file.Path;

import org.apache.arrow.vector.types.pojo.Schema;



/**
 * Fais le lien entre les champs Arrow et Lucene.
 */
public class IndexSchema {

    /**
     * Enumération des types de champs Lucene.
     */
    public enum LuceneFieldType {TextField, StringField, IntField, LongField,FloatField, DoubleField};
    /**
     * Les noms des champs et leur type Lucene.
     */
    private final Map<String, LuceneFieldType> fields;



    /**
     * Constructeur de IndexSchema, construit fields.
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
     * Constructeur de IndexSchema, construit fields.
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
     * Determine le type de champ Lucene vers lequel convertir un champ Arrow.
     * 
     * @param arrowField Le champ Arrow.
     * @return Le type de champ Lucene.
     */
    public static LuceneFieldType arrowToLuceneField(org.apache.arrow.vector.types.pojo.Field arrowField) {

        //if (arrowField.getType().getTypeID() != null in {ArrowType.Utf8, ArrowType.LargeUtf8}) {
        //    return null;
        //} else {
            return switch (arrowField.getType().getTypeID()) {
                case Int -> LuceneFieldType.LongField;
                case FloatingPoint -> LuceneFieldType.DoubleField;
                case Bool -> LuceneFieldType.StringField;
                case Utf8, LargeUtf8 -> LuceneFieldType.StringField; // ou TEXT selon le champ
                default -> LuceneFieldType.StringField;
            };
        //}
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
     * Méthode toString de IndexSchema.
     */
    @Override
    public String toString() {
        String result = "";
        for (String fieldName : this.fields.keySet()) {
            result += fieldName + " : " + this.fields.get(fieldName) + "\n";
        }
        return result;
    }



    /**
     * Méthode equals de IndexSchema.
     */
    @Override
    public boolean equals(Object o) {
        return this.fields.equals(o);
    }
}
