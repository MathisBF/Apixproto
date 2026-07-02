package org.model;


import org.model.IndexSchema.LuceneFieldType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.arrow.vector.types.pojo.ArrowType;
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



public class FieldDescriptor {

    private final String fieldName;
    private final LuceneFieldType luceneType;
    private final ArrowType arrowType;
    private final boolean indexed;
    private final boolean stored;
    private final boolean analyzed;



    /**
     * Constructeur de FieldDescriptor.
     * Construit les métadonnées d'un champ de l'index à partir du champ Arrow correspondant.
     * 
     * @param arrowField Le champ Arrow.
     * @throws IllegalArgumentException
     * @return Le type de champ Lucene.
     */
    public FieldDescriptor(org.apache.arrow.vector.types.pojo.Field arrowField) throws IllegalArgumentException {

        this.fieldName = arrowField.getName();
        this.luceneType = FieldDescriptor.arrowToLuceneField(arrowField);
        this.arrowType = arrowField.getType();
        this.indexed = true;
        this.stored = true;
        this.analyzed = (luceneType == LuceneFieldType.TextField);
    }



    /**
     * Le constructeur de FieldDescriptor via Json.
     * 
     * @param fieldName Le nom du champ.
     * @param luceneType Le type arrow à l'origine du champ.
     * @param arrowType Le type Lucene du champ.
     * @param indexed Est-ce que le champ est indexé ou non.
     * @param stored Est-ce que le champ est stocké ou non.
     * @param analyzed Est-ce que le champ nécessite un Analyzer ou non.
     */
    @JsonCreator
    public FieldDescriptor(
        @JsonProperty("fieldName") String fieldName,
        @JsonProperty("luceneType") LuceneFieldType luceneType,
        @JsonProperty("arrowType") ArrowType arrowType,
        @JsonProperty("indexed") boolean indexed,
        @JsonProperty("stored") boolean stored,
        @JsonProperty("analyzed") boolean analyzed
    ) {
        this.fieldName = fieldName;
        this.luceneType = luceneType;
        this.arrowType = arrowType;
        this.indexed = indexed;
        this.stored = stored;
        this.analyzed = analyzed;
    }



    /**
     * 
     * @return
     */
    @JsonProperty("fieldName")
    public String getFieldName() {
        return this.fieldName;
    }



    /**
     * 
     * @return
     */
    @JsonProperty("luceneType")
    public LuceneFieldType getLuceneFieldType() {
        return this.luceneType;
    }



    /**
     * 
     * @return
     */
    @JsonProperty("arrowType")
    public ArrowType getArrowType() {
        return this.arrowType;
    }



    /**
     * 
     * @return
     */
    @JsonProperty("indexed")
    public boolean isIndexed() {
        return this.indexed;
    }



    /**
     * 
     * @return
     */
    @JsonProperty("stored")
    public boolean isStored() {
        return this.stored;
    }



    /**
     * 
     * @return
     */
    @JsonProperty("analyzed")
    public boolean isAnalyzed() {
        return this.analyzed;
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



    /**
     * Determine si un champ Arrow de type chaîne de caractère doit être interprété
     * comme un champ Lucene TextField ou StringField.
     * 
     * @param arrowField Le champ Arrow.
     * @return true pour TextField ou false pour StringField.
     * @throws IllegalArgumentException 
     */
    private static boolean isFullTextField(org.apache.arrow.vector.types.pojo.Field arrowField) throws IllegalArgumentException {
        
        switch (arrowField.getType().getTypeID()) {
            case Utf8, LargeUtf8:
                break;
            default:
                throw new IllegalArgumentException(
                    "arrowField TypeID should be Utf8 or LargeUtf8."
                );
        }

        return switch (arrowField.getName()) {
            case "description", "titre", "contenue", "texte" -> true;
            default -> false;
        };
    }



    /**
     * Ajoute le champ à un document Lucene.
     * 
     * @param doc Le document.
     * @param value La valeur du champ.
     */
    public void addToDocument(Document doc, Object value) {

        LuceneFieldType type = this.getLuceneFieldType();

        switch (type) {
            case TextField:
                doc.add(new TextField(this.fieldName, value.toString(), Field.Store.YES));
                break;

            case StringField:
                doc.add(new StringField(this.fieldName, value.toString(), Field.Store.YES));
                break;

            case IntPoint:
                doc.add(new IntPoint(this.fieldName, ((Number) value).intValue()));
                doc.add(new StoredField(this.fieldName, ((Number) value).intValue()));
                break;

            case LongPoint:
                doc.add(new LongPoint(this.fieldName, ((Number) value).longValue()));
                doc.add(new StoredField(this.fieldName, ((Number) value).longValue()));
                break;

            case FloatPoint:
                doc.add(new FloatPoint(this.fieldName, ((Number) value).floatValue()));
                doc.add(new StoredField(this.fieldName, ((Number) value).floatValue()));
                break;

            case DoublePoint:
                doc.add(new DoublePoint(this.fieldName, ((Number) value).doubleValue()));
                doc.add(new StoredField(this.fieldName, ((Number) value).doubleValue()));
                break;

            default:
                break;
        }
    }



    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        
        if (!(o instanceof FieldDescriptor))
            return false;

        FieldDescriptor other = (FieldDescriptor) o;
        boolean testName = this.fieldName.equals(other.getFieldName());
        boolean testLuceneType = this.luceneType.equals(other.getLuceneFieldType());
        boolean testArrowType = this.arrowType.equals(other.getArrowType());
        boolean testIndexed = this.indexed == other.isIndexed();
        boolean testStored = this.stored == other.isStored();
        boolean testAnalyzed = this.analyzed == other.isAnalyzed();

        return testName && testLuceneType && testArrowType && testIndexed && testStored && testAnalyzed;
    }
}