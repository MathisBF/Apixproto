package org.model;

import java.util.HashMap;
import java.util.Map;



/**
 * La classe ParquetRow représente une ligne de données Parquet.
 */
public class ParquetRow {

    /**
     * Dictionnaire contenant les données.
     * Le nom du champ en clé, et la valeur de la donnée en valeur du dictionnaire.
     */
    private final Map<String, Object> fields;



    /**
     * Constructeur de ParquetRow.
     */
    public ParquetRow() {
        this.fields = new HashMap<>();
    }



    /**
     * Ajoute une donnée (colonne).
     * 
     * @param key Le champ.
     * @param value La valeur.
     */
    public void put(String key, Object value) {
        fields.put(key, value);
    }



    /**
     * Retourne la valeur d'une donnée à partir de son champ.
     * 
     * @param key Le nom du champ.
     * @return La valeur de la donnée.
     */
    public Object get(String key) {
        return fields.get(key);
    }



    /**
     * Retourne les données sous forme de dictionnaire.
     * 
     * @return Les données.
     */
    public Map<String, Object> getFields() {
        return fields;
    }



    /**
     * Retourne l'objet sous forme de chaîne de charactères.
     */
    @Override
    public String toString() {
        return fields.toString();
    }
}